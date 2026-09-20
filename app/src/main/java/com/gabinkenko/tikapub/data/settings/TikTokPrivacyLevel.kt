package com.gabinkenko.tikapub.data.settings

/**
 * Mirrors the `privacy_level` values accepted by TikTok's Content Posting API
 * (POST /v2/post/publish/video/init/). Which values are actually usable depends on the
 * TikTok account (creator_info/query fields) and whether your app has passed TikTok's audit -
 * unaudited apps are generally restricted to [SELF_ONLY]. See README.md.
 */
enum class TikTokPrivacyLevel(val apiValue: String, val label: String) {
    PUBLIC_TO_EVERYONE("PUBLIC_TO_EVERYONE", "Public"),
    MUTUAL_FOLLOW_FRIENDS("MUTUAL_FOLLOW_FRIENDS", "Amis (abonnements mutuels)"),
    FOLLOWER_OF_CREATOR("FOLLOWER_OF_CREATOR", "Abonnés uniquement"),
    SELF_ONLY("SELF_ONLY", "Privé (visible par vous seul)");

    companion object {
        fun fromApiValue(value: String?): TikTokPrivacyLevel =
            entries.firstOrNull { it.apiValue == value } ?: SELF_ONLY
    }
}
