package com.ascend.core.data.community

import com.ascend.core.domain.community.FriendEdge
import com.ascend.core.domain.community.FriendshipId
import com.ascend.core.domain.community.FriendshipStatus
import com.ascend.core.domain.community.ProfileCard
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.ReportReason
import com.ascend.core.domain.community.ShareSettings
import com.ascend.core.domain.community.ShareVisibility
import com.ascend.core.domain.community.SharedAffinity
import com.ascend.core.domain.community.SharedProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Data-layer serialization shapes for the P2A tables. Kept out of the domain; snake_case matches the
// Supabase columns. Pure mappers below are unit-tested without any live client.

@Serializable
data class FriendshipDto(
    val id: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("addressee_id") val addresseeId: String,
    val status: String,
) {
    /** Map to a domain edge relative to [selfId]; incoming = I was the addressee (I may accept). */
    fun toEdge(selfId: String): FriendEdge =
        FriendEdge(
            id = FriendshipId(id),
            other = RemoteUserId(if (requesterId == selfId) addresseeId else requesterId),
            status = if (status == "ACCEPTED") FriendshipStatus.ACCEPTED else FriendshipStatus.PENDING,
            incoming = addresseeId == selfId,
        )
}

/** Insert shape: id + timestamps are server-defaulted; RLS requires requester_id = auth.uid(). */
@Serializable
data class FriendshipInsertDto(
    @SerialName("requester_id") val requesterId: String,
    @SerialName("addressee_id") val addresseeId: String,
    val status: String = "PENDING",
)

@Serializable
data class BlockDto(
    @SerialName("blocker_id") val blockerId: String,
    @SerialName("blocked_id") val blockedId: String,
)

@Serializable
data class ShareSettingsDto(
    @SerialName("user_id") val userId: String? = null,
    val visibility: String = "PRIVATE",
    @SerialName("share_build_identity") val shareBuildIdentity: Boolean = false,
    @SerialName("share_class_progress") val shareClassProgress: Boolean = false,
) {
    fun toDomain(): ShareSettings =
        ShareSettings(
            visibility = if (visibility == "FRIENDS") ShareVisibility.FRIENDS else ShareVisibility.PRIVATE,
            shareBuildIdentity = shareBuildIdentity,
            shareClassProgress = shareClassProgress,
        )

    companion object {
        fun from(
            userId: String,
            settings: ShareSettings,
        ): ShareSettingsDto =
            ShareSettingsDto(
                userId = userId,
                visibility = settings.visibility.name,
                shareBuildIdentity = settings.shareBuildIdentity,
                shareClassProgress = settings.shareClassProgress,
            )
    }
}

@Serializable
data class SharedAffinityDto(
    @SerialName("class_id") val classId: String,
    val affinity: Double,
) {
    fun toDomain(): SharedAffinity = SharedAffinity(classId, affinity)

    companion object {
        fun from(a: SharedAffinity): SharedAffinityDto = SharedAffinityDto(a.classId, a.affinity)
    }
}

@Serializable
data class SharedProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("selected_class") val selectedClass: String? = null,
    @SerialName("class_level") val classLevel: Int? = null,
    val rank: String? = null,
    @SerialName("overall_level") val overallLevel: Int? = null,
    @SerialName("build_identity") val buildIdentity: String? = null,
    @SerialName("top_affinities") val topAffinities: List<SharedAffinityDto>? = null,
    @SerialName("avatar_body_base") val avatarBodyBase: String? = null,
) {
    fun toDomain(): SharedProfile =
        SharedProfile(
            userId = RemoteUserId(userId),
            selectedClass = selectedClass,
            classLevel = classLevel,
            rank = rank,
            overallLevel = overallLevel,
            buildIdentity = buildIdentity,
            topAffinities = topAffinities.orEmpty().map { it.toDomain() },
            avatarBodyBase = avatarBodyBase,
        )

    companion object {
        fun from(p: SharedProfile): SharedProfileDto =
            SharedProfileDto(
                userId = p.userId.value,
                selectedClass = p.selectedClass,
                classLevel = p.classLevel,
                rank = p.rank,
                overallLevel = p.overallLevel,
                buildIdentity = p.buildIdentity,
                topAffinities = p.topAffinities.map { SharedAffinityDto.from(it) },
                avatarBodyBase = p.avatarBodyBase,
            )
    }
}

@Serializable
data class ProfileCardDto(
    val id: String,
    val handle: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_body_base") val avatarBodyBase: String? = null,
) {
    fun toDomain(): ProfileCard = ProfileCard(RemoteUserId(id), handle, displayName, avatarBodyBase)
}

@Serializable
data class ReportDto(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("subject_id") val subjectId: String? = null,
    val reason: String,
    val note: String? = null,
) {
    companion object {
        fun from(
            reporterId: String,
            subject: RemoteUserId?,
            reason: ReportReason,
            note: String?,
        ): ReportDto = ReportDto(reporterId = reporterId, subjectId = subject?.value, reason = reason.name, note = note)
    }
}
