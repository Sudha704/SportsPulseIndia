package com.sportspulse.india.core.domain.entity

/**
 * Represents a single broadcast slot for a sporting event.
 *
 * Instances are persisted via Room's type converters and transported
 * as JSON in the remote broadcast config JSON.
 *
 * @param channelName   Full channel name, e.g. "Star Sports 1 HD".
 * @param channelNumber Optional channel number on DTH/cable (e.g. 451 on Tata Play).
 * @param platform      Which streaming/broadcast platform hosts this event.
 * @param isFree        True when the event is free-to-air on this platform.
 * @param deeplinkUri   App deeplink URI. If null, falls back to Play Store URL.
 * @param streamingUrl  Web URL for direct browser streaming (optional).
 */
data class Broadcast(
    val channelName: String,
    val channelNumber: Int? = null,
    val platform: BroadcastPlatform,
    val isFree: Boolean,
    val deeplinkUri: String? = null,
    val streamingUrl: String? = null
) {
    /**
     * Returns the best available launch URI:
     *  1. deeplinkUri  (app deeplink)
     *  2. streamingUrl (web fallback)
     *  3. Play Store URL
     */
    fun bestLaunchUri(): String =
        deeplinkUri
            ?: streamingUrl
            ?: platform.playStoreUrl()
}
