package com.ascend.core.data.community

import com.ascend.core.domain.community.RemoteErrorKind
import com.ascend.core.domain.community.RemoteResult
import java.io.IOException

// Shared remote-call plumbing for the community gateways, so Offline stays distinguishable from a
// remote Failure and an unconfigured/signed-out backend degrades gracefully (never crashes the loop).

internal suspend fun <T> runRemoteCall(block: suspend () -> RemoteResult<T>): RemoteResult<T> =
    try {
        block()
    } catch (e: IOException) {
        RemoteResult.Offline
    } catch (e: Exception) {
        RemoteResult.Failure(RemoteErrorKind.UNKNOWN, e.message)
    }

internal fun <T> notConfigured(): RemoteResult<T> = RemoteResult.Failure(RemoteErrorKind.UNKNOWN, "Community backend is not configured.")

internal fun <T> notSignedIn(): RemoteResult<T> = RemoteResult.Failure(RemoteErrorKind.AUTH, "Not signed in.")
