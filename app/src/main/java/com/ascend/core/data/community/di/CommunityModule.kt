package com.ascend.core.data.community.di

import com.ascend.BuildConfig
import com.ascend.core.data.community.DataStoreSessionStore
import com.ascend.core.data.community.SupabaseAuthGateway
import com.ascend.core.data.community.SupabaseProfileGateway
import com.ascend.core.domain.community.AuthGateway
import com.ascend.core.domain.community.DefaultIdentityResolver
import com.ascend.core.domain.community.IdentityResolver
import com.ascend.core.domain.community.ProfileGateway
import com.ascend.core.domain.community.SessionStore
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
}
