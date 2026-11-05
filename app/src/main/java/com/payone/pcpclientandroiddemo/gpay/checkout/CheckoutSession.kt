package com.payone.pcpclientandroiddemo.gpay.checkout

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.gson.Gson
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressPayment
import java.util.concurrent.Executors

/** Persisted non-sensitive identity/configuration shared by Activity and Wallet service. */
class CheckoutSession(context: Context) {
    // Retain the previous guard: upgrading must not release an unresolved payment.
    private val prefs = context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE)
    fun state(): String = prefs.getString("state", "IDLE")!!
    fun reference(): String = requireNotNull(prefs.getString("reference", null))
    fun config(): CheckoutConfig = Gson().fromJson(prefs.getString("config", null), CheckoutConfig::class.java).copy()
    fun mode(): CheckoutMode = CheckoutMode.valueOf(requireNotNull(prefs.getString("mode", null)))
    fun shippingOption(): String = requireNotNull(prefs.getString("shippingOption", null))
    fun outcome(): CheckoutOutcome? = prefs.getString("outcome", null)?.let { Gson().fromJson(it, CheckoutOutcome::class.java) }
    fun observe(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)
    fun stopObserving(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)
    fun isSubmitting(): Boolean = submitting
    fun failure(): String? = prefs.getString("failure", null)

    fun start(reference: String, mode: CheckoutMode, config: CheckoutConfig, shippingOption: String) = synchronized(lock) {
        check(!submitting && state() != "OPEN") { "Checkout is still running" }
        config.total(config.countryCode, shippingOption)
        val history = prefs.getStringSet("history", emptySet())!!.toMutableSet()
        if (state() in setOf("SUBMITTED", "CREATED", "CONFIRMED")) {
            history.add(Gson().toJson(mapOf("reference" to reference(), "state" to state(), "outcome" to outcome())))
        }
        check(prefs.edit().putString("reference", reference).putString("state", "OPEN")
            .putString("mode", mode.name).putString("config", Gson().toJson(config))
            .putString("shippingOption", shippingOption).putStringSet("history", history).remove("outcome").remove("failure").commit())
    }

    fun cancelBeforeSubmission() {
        if (state() == "OPEN") check(prefs.edit().putString("state", "IDLE").commit())
    }

    /** Call on CheckoutTasks' serial worker. Never persists, logs or replays payment data. */
    fun submit(payment: GooglePayExpressPayment): Boolean {
        synchronized(lock) {
            if (state() == "CONFIRMED") return true
            if (state() != "OPEN" || submitting) return false
            submitting = true
        }
        try {
            val config = config()
            CheckoutBackend.createRequest(payment, reference(), config)
            check(prefs.edit().putString("state", "SUBMITTED").commit())
            val result = CheckoutBackend(config).createCheckout(payment, reference())
            check(prefs.edit().putString("outcome", Gson().toJson(result))
                .putString("state", if (result.authorized) "CONFIRMED" else "CREATED").commit())
            return result.authorized
        } catch (error: CheckoutBackendException) {
            check(prefs.edit().putString("failure", error.message).commit())
            throw error
        } finally {
            synchronized(lock) {
                submitting = false
                // Notify recreated Activities even when HTTP failure leaves state SUBMITTED.
                check(prefs.edit().putLong("completion", System.nanoTime()).commit())
            }
        }
    }

    private companion object {
        val lock = Any()
        @Volatile var submitting = false
    }
}

/** Serializes backend work and callbacks; holds no checkout state. */
object CheckoutTasks {
    private val worker = Executors.newSingleThreadExecutor()
    fun <T> run(block: () -> T): Task<T> {
        val source = TaskCompletionSource<T>()
        worker.execute {
            try { source.setResult(block()) } catch (error: Exception) { source.setException(error) }
        }
        return source.task
    }
}
