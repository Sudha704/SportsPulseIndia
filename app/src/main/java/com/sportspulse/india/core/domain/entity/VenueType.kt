package com.sportspulse.india.core.domain.entity

/**
 * Category of venue returned by the Hyper-Local Venue Tracker.
 *
 * @param displayName  Label shown in the filter chip and venue card.
 * @param iconResName  Resource name of the vector drawable icon (resolved at runtime).
 */
enum class VenueType(val displayName: String, val iconResName: String) {
    TURF("Football / Cricket Turf", "ic_turf"),
    BADMINTON_COURT("Badminton Court", "ic_badminton"),
    INDOOR_COMPLEX("Indoor Sports Complex", "ic_indoor"),
    SWIMMING_POOL("Swimming Pool", "ic_pool"),
    STADIUM("Stadium", "ic_stadium"),
    GYM("Gym / Fitness Centre", "ic_gym"),
    UNKNOWN("Sports Venue", "ic_venue_generic");

    companion object {
        fun fromString(value: String): VenueType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}
