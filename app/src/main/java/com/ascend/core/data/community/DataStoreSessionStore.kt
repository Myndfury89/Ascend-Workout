package com.ascend.core.data.community

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.communitySessionDataStore by preferencesDataStore(name = "community_session")

/**
 * Local, DataStore-backed persistence of the link between this device's player and a signed-in remote
 * identity. Purely local — it holds no remote/SDK types — which is what lets identity be a link rather
 * than a data migration: the local Room key is never touched.
 */
@Singleton
class DataStoreSessionStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : SessionStore {
        override fun linkedRemoteUserId(): Flow<RemoteUserId?> =
            context.communitySessionDataStore.data.map { prefs ->
                prefs[LINKED_REMOTE_USER_ID]?.let(::RemoteUserId)
            }

        override suspend fun setLinkedRemoteUserId(id: RemoteUserId?) {
            context.communitySessionDataStore.edit { prefs ->
                if (id == null) prefs.remove(LINKED_REMOTE_USER_ID) else prefs[LINKED_REMOTE_USER_ID] = id.value
            }
        }

        private companion object {
            val LINKED_REMOTE_USER_ID = stringPreferencesKey("linked_remote_user_id")
        }
    }
