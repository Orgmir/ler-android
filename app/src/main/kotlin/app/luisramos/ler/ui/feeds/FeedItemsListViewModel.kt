package app.luisramos.ler.ui.feeds

import androidx.lifecycle.*
import app.luisramos.ler.data.SelectAll
import app.luisramos.ler.data.toBoolean
import app.luisramos.ler.domain.*
import app.luisramos.ler.ui.ScaffoldViewModel
import app.luisramos.ler.ui.event.Event
import app.luisramos.ler.ui.event.postEvent
import app.luisramos.ler.ui.views.UiState
import app.luisramos.ler.ui.views.data
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FeedItemsListViewModel(
    val parentViewModel: ScaffoldViewModel,
    private val fetchFeedsUseCase: FetchFeedItemsUseCase,
    private val setUnreadFeedItemUseCase: SetUnreadFeedItemUseCase,
    private val deleteFeedUseCase: DeleteFeedUseCase,
    private val preferences: Preferences
) : ViewModel() {

    val uiState = MutableLiveData<UiState<SelectAll>>()

    val goToExternalBrowser = MutableLiveData<Event<String>>()
    val updateListPosition = MutableLiveData<Int>()
    val showReadFeedItems = MutableLiveData(preferences.showReadFeedItems)
    val isDeleteMenuOptionVisible = parentViewModel.selectedFeed.map { it != -1L }
    val showDeleteConfirmation = MutableLiveData<Event<String>>()

    private var fetchJob: Job? = null

    private val observer = Observer<Long> { loadData(it) }

    init {
        parentViewModel.selectedFeed.observeForever(observer)
    }

    override fun onCleared() {
        parentViewModel.selectedFeed.removeObserver(observer)
        super.onCleared()
    }

    fun tappedItem(position: Int) = viewModelScope.launch {
        getItem(position)?.let {
            setUnreadFeedItemUseCase.setUnread(it.id, false)
//            updateListPosition.value = position
            goToExternalBrowser.postEvent(it.link)
        }
    }

    fun toggleUnreadFilter() {
        preferences.showReadFeedItems = !preferences.showReadFeedItems
        showReadFeedItems.value = preferences.showReadFeedItems
        reloadData()
    }

    fun markAllAsRead() = viewModelScope.launch {
        parentViewModel.selectedFeed.value?.let {
            setUnreadFeedItemUseCase.setUnreadForFeedId(it, false)
        }
    }

    fun toggleUnread(position: Int) = viewModelScope.launch {
        getItem(position)?.let {
            setUnreadFeedItemUseCase.setUnread(it.id, !it.unread.toBoolean())
//            updateListPosition.value = position
        }
    }

    fun tapDeleteFeed() {
        val title = parentViewModel.title.value ?: return
        if (title == "All") return

        showDeleteConfirmation.postEvent(title)
    }

    fun deleteSelectedFeed() = viewModelScope.launch {
        val id = parentViewModel.selectedFeed.value
        id?.let { deleteFeedUseCase.deleteFeed(id) }
        parentViewModel.selectedFeed.value = -1 // reset to "All" filter
    }

    private fun reloadData() {
        loadData(parentViewModel.selectedFeed.value ?: -1)
    }

    private fun loadData(feedId: Long) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            uiState.value = UiState.Loading
            val showRead = if (preferences.showReadFeedItems) 1L else 0L
            fetchFeedsUseCase.fetch(feedId, showRead).collect { result ->
                uiState.value = result.fold(
                    onFailure = { UiState.Error("Failed loading feeds") },
                    onSuccess = { UiState.Success(it) }
                )
            }
        }
    }

    private fun getItem(position: Int): SelectAll? = uiState.value?.data?.getOrNull(position)
}