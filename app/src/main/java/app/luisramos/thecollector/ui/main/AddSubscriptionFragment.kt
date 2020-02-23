package app.luisramos.thecollector.ui.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import app.luisramos.thecollector.R
import app.luisramos.thecollector.ui.main.AddSubscriptionViewModel.UiState
import kotlinx.android.synthetic.main.fragment_add_subscription.*
import kotlinx.android.synthetic.main.main_fragment.recyclerView

class AddSubscriptionFragment : BaseFragment() {

    companion object {
        fun newInstance() = AddSubscriptionFragment()
    }

    private val viewModel: AddSubscriptionViewModel by viewModels()
    private val adapter = StackedLabelsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_subscription, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        editText.setOnEditorActionListener { _, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_UNSPECIFIED -> {
                    if (event == null || event.action == KeyEvent.ACTION_UP) {
                        viewModel.fetchFeeds(editText.text.toString())
                    }
                    true
                }
                else -> false
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner, Observer {
            swipeRefreshLayout.isRefreshing = it is UiState.Loading
            when (it) {
                is UiState.ShowError -> Toast.makeText(
                    requireContext(),
                    it.errorMsg,
                    Toast.LENGTH_SHORT
                ).show()
                is UiState.Loaded -> adapter.items = it.items
            }
        })
    }
}