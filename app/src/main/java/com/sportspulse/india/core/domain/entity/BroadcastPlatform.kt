package com.sportspulse.india.core.domain.entity

/**
 * Broadcast platforms available in India.
 *
 * @param displayName       Human-readable channel group name shown in the UI.
 * @param defaultDeeplink   Intent URI to launch the platform's app directly (null = not available).
 * @param playStorePackage  Package name used to build a Play Store fallback URL.
 * @param isFreeByDefault   Whether the platform's base tier is free-to-air.
 */
enum class BroadcastPlatform(
    val displayName: String,
    val defaultDeeplink: String?,
    val playStorePackage: String,
    val isFreeByDefault: Boolean
) {
    JIOSTAR(
        displayName = "JioStar / Star Sports",
        defaultDeeplink = "hotstar://",
        playStorePackage = "in.startv.hotstar",
        isFreeByDefault = false
    ),
    ZEE5(
        displayName = "Zee Sports / Zee5",
        defaultDeeplink = "zee5://",
        playStorePackage = "com.zee5.android",
        isFreeByDefault = false
    ),
    FANCODE(
        displayName = "FanCode",
        defaultDeeplink = "fancode://",
        playStorePackage = "com.dreamsports.fancode",
        isFreeByDefault = false
    ),
    DD_SPORTS(
        displayName = "DD Sports (Free)",
        defaultDeeplink = null,
        playStorePackage = "com.dd.doordarshanapp",
        isFreeByDefault = true
    ),
    SONY_LIV(
        displayName = "Sony LIV",
        defaultDeeplink = "sonyliv://",
        playStorePackage = "com.sony.tay",
        isFreeByDefault = false
    ),
    UNKNOWN(
        displayName = "Unknown Platform",
        defaultDeeplink = null,
        playStorePackage = "",
        isFreeByDefault = false
    );

    /** Play Store URL for this platform. */
    fun playStoreUrl(): String =
        "https://play.google.com/store/apps/details?id=$playStorePackage"

    companion object {
        fun fromString(value: String): BroadcastPlatform =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}
