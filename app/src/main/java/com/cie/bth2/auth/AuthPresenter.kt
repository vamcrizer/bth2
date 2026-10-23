package com.cie.bth2.auth

import android.os.Handler
import android.os.Looper
import com.cie.bth2.data.AccountRepository.Account
import java.util.concurrent.Executors

/** MVP presenter: database work never blocks UI; detached screens receive no callbacks. */
class AuthPresenter(private val repository: AuthRepository, private var view: View?) {
    interface View {
        fun onBusy(busy: Boolean)
        fun onResult(account: Account?)
        fun onError(duplicateUsername: Boolean)
    }
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var busy = false

    fun login(username: String, password: String) = run { repository.login(username, password) }
    fun register(name: String, phone: String, email: String, username: String, password: String) =
        run { repository.register(name, phone, email, username, password) }
    fun restore(account: Account?) = run { repository.restore(account) }

    private fun run(operation: () -> Account?) {
        if (busy) return
        busy = true
        view?.onBusy(true)
        executor.execute {
            val result = runCatching(operation)
            handler.post {
                busy = false
                view?.onBusy(false)
                result.fold({ view?.onResult(it) }, {
                    view?.onError(it is AuthRepository.UsernameTakenException)
                })
            }
        }
    }

    fun detach() {
        view = null
        executor.shutdown()
    }
}
