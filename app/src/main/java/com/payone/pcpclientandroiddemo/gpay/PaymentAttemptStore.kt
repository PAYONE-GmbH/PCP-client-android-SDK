package com.payone.pcpclientandroiddemo.gpay

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.gson.Gson
import java.util.concurrent.Executors

/** Activity/service identity survives recreation. Never stores tokens or customer contacts. */
class PaymentAttemptStore(context: Context) {
    private val prefs = context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE)
    fun current(): PaymentAttempt? = prefs.getString("attempt", null)?.let { Gson().fromJson(it, PaymentAttempt::class.java) }
    fun blocked(): Boolean = current() != null || prefs.getString("state", "IDLE") != "IDLE"
    fun observe(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)
    fun stopObserving(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    fun start(reference: String, mode: String, config: ConfigService, option: String) = synchronized(lock) {
        check(!blocked()) { "Reconcile the previous checkout before starting a new order." }
        require(reference.isNotBlank() && reference.length <= 20)
        require(mode in setOf("STANDARD", "EXPRESS") && option in setOf("standard", "express"))
        save(PaymentAttempt(reference, mode, config.backendUrl, config.gatewayMerchantId, option))
    }

    fun selectShipping(reference: String, option: String) = synchronized(lock) {
        val attempt = requireNotNull(current())
        check(attempt.reference == reference && attempt.phase == "OPEN")
        require(option in setOf("standard", "express"))
        save(attempt.copy(shippingOption = option))
    }

    fun cancel(reference: String) = synchronized(lock) {
        val attempt = current()
        if (attempt?.reference == reference && attempt.phase == "OPEN") {
            check(prefs.edit().remove("attempt").commit())
        }
    }

    /** Persist before POST, including process death/network failures. Repeated callbacks never POST again. */
    fun beginSubmission(reference: String): Boolean = synchronized(lock) {
        val attempt = requireNotNull(current())
        check(attempt.reference == reference)
        if (attempt.phase != "OPEN") {
            return false
        }
        save(attempt.copy(phase = "SUBMITTED"))
        true
    }

    fun created(reference: String, result: PaymentOutcome) = synchronized(lock) {
        val attempt = requireNotNull(current())
        check(attempt.reference == reference && attempt.phase == "SUBMITTED")
        save(attempt.copy(phase = "CREATED", outcome = result))
    }

    fun reconcile(): PaymentOutcome {
        val attempt = requireNotNull(current()) { "Legacy checkout: reconcile its stored reference in the backend before clearing app data." }
        val outcome = requireNotNull(attempt.outcome) { "No checkout IDs received. Reconcile reference ${attempt.reference} in the backend." }
        val state = checkoutState(CommerceApiService(attempt.backendUrl).getCheckout(outcome.commerceCaseId, outcome.checkoutId).body)
        val updated = outcome.copy(state = state, redirectUrl = if (state == "pending") { outcome.redirectUrl } else { null })
        synchronized(lock) {
            check(current()?.reference == attempt.reference)
            save(attempt.copy(outcome = updated, phase = if (state in setOf("succeeded", "failed")) { "RESOLVED" } else { "CREATED" }))
        }
        return updated
    }

    fun newOrder() = synchronized(lock) {
        check(current()?.phase == "RESOLVED") { "Check backend status before starting a new order." }
        check(prefs.edit().remove("attempt").remove("state").commit())
    }

    private fun save(attempt: PaymentAttempt) {
        check(prefs.edit().putString("attempt", Gson().toJson(attempt)).commit())
    }

    private companion object { val lock = Any() }
}

data class PaymentAttempt(
    val reference: String, val mode: String, val backendUrl: String, val gatewayMerchantId: String,
    val shippingOption: String, val phase: String = "OPEN", val outcome: PaymentOutcome? = null,
)

/** Serial merchant work shared by the Activity and native Wallet callback service. */
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
