package com.ascend.core.data.community

import com.ascend.core.domain.community.RemoteProfile
import com.ascend.core.domain.community.RemoteUserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Data-layer serialization shape for the `profiles` row. Kept out of the domain model. */
@Serializable
data class ProfileDto(
    val id: String,
    val handle: String? = null,
    @SerialName("display_name") val displayName: String? = null,
) {
    fun toDomain(): RemoteProfile = RemoteProfile(RemoteUserId(id), handle, displayName)
}
