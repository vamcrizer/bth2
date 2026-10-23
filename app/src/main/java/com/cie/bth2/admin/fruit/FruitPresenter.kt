package com.cie.bth2.admin.fruit

import java.util.concurrent.Executors

/** Serializes database work off the UI thread; a detached view never receives callbacks. */
class FruitPresenter(private val repository: FruitRepository, private val view: View) {
    interface View {
        fun dispatch(action: () -> Unit)
        fun showFruits(items: List<Fruit>)
        fun showError()
        fun setBusy(busy: Boolean)
    }
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var detached = false

    fun refresh() = execute { }
    fun save(fruit: Fruit) = execute { repository.save(fruit) }
    fun delete(id: Long) = execute { repository.delete(id) }

    private fun execute(operation: () -> Unit) {
        view.setBusy(true)
        executor.execute {
            val result = runCatching { operation(); repository.all() }
            view.dispatch {
                if (!detached) {
                    view.setBusy(false)
                    result.onSuccess(view::showFruits).onFailure { view.showError() }
                }
            }
        }
    }

    fun close() {
        detached = true
        executor.execute { repository.close() }
        executor.shutdown()
    }
}
