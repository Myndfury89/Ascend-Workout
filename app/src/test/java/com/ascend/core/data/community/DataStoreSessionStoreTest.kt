package com.ascend.core.data.community

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.domain.community.RemoteUserId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DataStoreSessionStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = DataStoreSessionStore(context)

    @Test
    fun `starts unlinked`() =
        runTest {
            assertNull(store.linkedRemoteUserId().first())
        }

    @Test
    fun `linking then clearing round-trips`() =
        runTest {
            store.setLinkedRemoteUserId(RemoteUserId("uuid-99"))
            assertEquals(RemoteUserId("uuid-99"), store.linkedRemoteUserId().first())

            store.setLinkedRemoteUserId(null)
            assertNull(store.linkedRemoteUserId().first())
        }
}
