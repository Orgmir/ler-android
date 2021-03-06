package app.luisramos.ler.ui.event

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import app.luisramos.ler.di.observe

typealias EmptyEvent = Event<Any>

class Event<T>(
    private val value: T
) {
    var isConsumed = false
        private set

    fun consume(): T? = if (isConsumed) null else {
        isConsumed = true
        value
    }
}

fun <T> MutableLiveData<Event<T>>.postEvent(value: T) {
    this.value = Event(value)
}

fun MutableLiveData<EmptyEvent>.postEmptyEvent() {
    this.value = Event(Any())
}

fun <T> MutableLiveData<Event<T>>.observeEvent(owner: LifecycleOwner, observer: (T) -> Unit) {
    observe(owner, { event -> event.consume()?.let { observer.invoke(it) } })
}

fun <T> MutableLiveData<Event<T>>.observeEvent(view: View, observer: (T) -> Unit) {
    observe(view) { event -> event.consume()?.let { observer.invoke(it) } }
}