package com.payone.pcpclientandroiddemo

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Local checkout must not start before Android 17 grants LAN access. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class CheckoutNetworkPermissionTest {
    class Activity : BaseActivity() {
        fun checkout(action: () -> Unit) = withCheckoutNetworkAccess(action)
    }

    @Test fun checkoutWaitsForLocalNetworkPermission() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            var calls = 0
            activity.checkout { calls++ }
            assertEquals(0, calls)
            assertArrayEquals(arrayOf("android.permission.ACCESS_LOCAL_NETWORK"),
                shadowOf(activity).lastRequestedPermission.requestedPermissions)
            shadowOf(activity.application as Application).grantPermissions("android.permission.ACCESS_LOCAL_NETWORK")
            activity.checkout { calls++ }
            assertEquals(1, calls)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
