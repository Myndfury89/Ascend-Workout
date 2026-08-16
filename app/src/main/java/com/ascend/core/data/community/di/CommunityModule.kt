package com.ascend.core.data.community.di

import android.os.Build
import com.ascend.BuildConfig
import com.ascend.core.data.community.DataStoreSessionStore
import com.ascend.core.data.community.SupabaseAuthGateway
import com.ascend.core.data.community.SupabaseFriendGateway
import com.ascend.core.data.community.SupabaseFriendProfileGateway
import com.ascend.core.data.community.SupabaseModerationGateway
import com.ascend.core.data.community.SupabaseProfileGateway
import com.ascend.core.data.community.SupabaseShareSettingsGateway
import com.ascend.core.domain.community.AuthGateway
import com.ascend.core.domain.community.DefaultIdentityResolver
import com.ascend.core.domain.community.FriendGateway
import com.ascend.core.domain.community.FriendProfileGateway
import com.ascend.core.domain.community.IdentityResolver
import com.ascend.core.domain.community.ModerationGateway
import com.ascend.core.domain.community.ProfileGateway
import com.ascend.core.domain.community.SessionStore
import com.ascend.core.domain.community.ShareSettingsGateway
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

/**
 * Community backend wiring. The Supabase client is provided as NULLABLE: when the app is built without
 * backend config (empty SUPABASE_URL/ANON_KEY), it is null and the whole Community surface degrades to
 * unavailable — local-only play is completely unaffected. The Supabase SDK is referenced only here and
 * in the data-layer gateway implementations, never above the data layer.
 */
@Module
@InstallIn(SingletonComponent::class)
object CommunityBackendModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient? {
        // Never construct the live backend under Robolectric: creating the client starts the Auth
        // plugin's own background session coroutines, which would fail on the JVM and contaminate the
        // unit suite. Unit tests exercise the gateways with a null client (graceful degradation); the
        // live client is verified on-device.
        if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) return null
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank()) return null
        return createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
            install(Auth)
            install(Postgrest)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CommunityBindingModule {
    @Binds
    abstract fun bindSessionStore(impl: DataStoreSessionStore): SessionStore

    @Binds
    abstract fun bindIdentityResolver(impl: DefaultIdentityResolver): IdentityResolver

    @Binds
    abstract fun bindAuthGateway(impl: SupabaseAuthGateway): AuthGateway

    @Binds
    abstract fun bindProfileGateway(impl: SupabaseProfileGateway): ProfileGateway

    @Binds
    abstract fun bindFriendGateway(impl: SupabaseFriendGateway): FriendGateway

    @Binds
    abstract fun bindModerationGateway(impl: SupabaseModerationGateway): ModerationGateway

    @Binds
    abstract fun bindShareSettingsGateway(impl: SupabaseShareSettingsGateway): ShareSettingsGateway

    @Binds
    abstract fun bindFriendProfileGateway(impl: SupabaseFriendProfileGateway): FriendProfileGateway
}
