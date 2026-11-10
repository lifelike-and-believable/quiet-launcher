package quiet.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionsTest {

    // app-list#AC-9: checked against the merged manifest the build produces.
    @Test
    fun `AC-9 the app requests no internet permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions ?: emptyArray()

        assertFalse(requested.contains(Manifest.permission.INTERNET))
    }
}
