package com.sportspulse.india.core.data.dto

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// CricAPI DTOs
// Docs: https://cricapi.com/how-to-use/
// ─────────────────────────────────────────────────────────────────────────────

data class CricApiCurrentMatchesResponse(
    @SerializedName("apikey")  val apiKey: String?,
    @SerializedName("data")    val data: List<CricApiMatch>?,
    @SerializedName("status")  val status: String?,
    @SerializedName("info")    val info: CricApiInfo?
)

data class CricApiMatch(
    @SerializedName("id")          val id: String,
    @SerializedName("name")        val name: String,
    @SerializedName("matchType")   val matchType: String?,   // "T20", "ODI", "Test", etc.
    @SerializedName("status")      val status: String?,      // "Match not started" | "live" | "result"
    @SerializedName("venue")       val venue: String?,
    @SerializedName("date")        val date: String?,
    @SerializedName("dateTimeGMT") val dateTimeGmt: String?,
    @SerializedName("teams")       val teams: List<String>?,
    @SerializedName("teamInfo")    val teamInfo: List<CricApiTeamInfo>?,
    @SerializedName("score")       val score: List<CricApiScore>?,
    @SerializedName("series_id")   val seriesId: String?,
    @SerializedName("fantasyEnabled") val fantasyEnabled: Boolean?
)

data class CricApiTeamInfo(
    @SerializedName("name")      val name: String?,
    @SerializedName("shortname") val shortName: String?,
    @SerializedName("img")       val img: String?
)

data class CricApiScore(
    @SerializedName("r")       val runs: Int?,
    @SerializedName("w")       val wickets: Int?,
    @SerializedName("o")       val overs: Double?,
    @SerializedName("inning")  val inning: String?
)

data class CricApiInfo(
    @SerializedName("hitsToday")    val hitsToday: Int?,
    @SerializedName("hitsUsed")     val hitsUsed: Int?,
    @SerializedName("hitsLimit")    val hitsLimit: Int?,
    @SerializedName("credits")      val credits: Int?,
    @SerializedName("server")       val server: Int?,
    @SerializedName("offsetRows")   val offsetRows: Int?,
    @SerializedName("totalRows")    val totalRows: Int?,
    @SerializedName("queryTime")    val queryTime: Double?,
    @SerializedName("s")            val s: Int?,
    @SerializedName("cache")        val cache: Boolean?
)

data class CricApiMatchInfoResponse(
    @SerializedName("apikey")  val apiKey: String?,
    @SerializedName("data")    val data: CricApiMatch?,
    @SerializedName("status")  val status: String?
)

// ─────────────────────────────────────────────────────────────────────────────
// SportRadar DTOs (generic schedule response)
// ─────────────────────────────────────────────────────────────────────────────

data class SportRadarScheduleResponse(
    @SerializedName("sport_events") val sportEvents: List<SportRadarEvent>?
)

data class SportRadarEvent(
    @SerializedName("id")          val id: String,
    @SerializedName("scheduled")   val scheduled: String?,   // ISO-8601 UTC
    @SerializedName("start_time_tbd") val startTimeTbd: Boolean?,
    @SerializedName("status")      val status: String?,      // "scheduled" | "live" | "closed"
    @SerializedName("tournament")  val tournament: SportRadarTournament?,
    @SerializedName("competitors") val competitors: List<SportRadarCompetitor>?,
    @SerializedName("venue")       val venue: SportRadarVenue?
)

data class SportRadarTournament(
    @SerializedName("id")   val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("sport") val sport: SportRadarSport?
)

data class SportRadarSport(
    @SerializedName("id")   val id: String?,
    @SerializedName("name") val name: String?
)

data class SportRadarCompetitor(
    @SerializedName("id")           val id: String?,
    @SerializedName("name")         val name: String?,
    @SerializedName("abbreviation") val abbreviation: String?,
    @SerializedName("qualifier")    val qualifier: String?,  // "home" | "away"
    @SerializedName("country")      val country: String?
)

data class SportRadarVenue(
    @SerializedName("id")       val id: String?,
    @SerializedName("name")     val name: String?,
    @SerializedName("city")     val city: String?,
    @SerializedName("country")  val country: String?,
    @SerializedName("capacity") val capacity: Int?
)

// ─────────────────────────────────────────────────────────────────────────────
// Football-data.org DTOs
// ─────────────────────────────────────────────────────────────────────────────

data class FootballMatchesResponse(
    @SerializedName("filters")  val filters: Map<String, String>?,
    @SerializedName("resultSet") val resultSet: FootballResultSet?,
    @SerializedName("matches")  val matches: List<FootballMatch>?
)

data class FootballResultSet(
    @SerializedName("count")  val count: Int?,
    @SerializedName("first")  val first: String?,
    @SerializedName("last")   val last: String?,
    @SerializedName("played") val played: Int?
)

data class FootballMatch(
    @SerializedName("id")          val id: Long,
    @SerializedName("utcDate")     val utcDate: String?,
    @SerializedName("status")      val status: String?,  // "SCHEDULED" | "LIVE" | "IN_PLAY" | "PAUSED" | "FINISHED"
    @SerializedName("matchday")    val matchday: Int?,
    @SerializedName("stage")       val stage: String?,
    @SerializedName("group")       val group: String?,
    @SerializedName("lastUpdated") val lastUpdated: String?,
    @SerializedName("competition") val competition: FootballCompetition?,
    @SerializedName("homeTeam")    val homeTeam: FootballTeam?,
    @SerializedName("awayTeam")    val awayTeam: FootballTeam?,
    @SerializedName("score")       val score: FootballScore?
)

data class FootballCompetition(
    @SerializedName("id")   val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("code") val code: String?,   // "ISL", "PL"
    @SerializedName("type") val type: String?,
    @SerializedName("emblem") val emblem: String?
)

data class FootballTeam(
    @SerializedName("id")        val id: Long?,
    @SerializedName("name")      val name: String?,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("tla")       val tla: String?,
    @SerializedName("crest")     val crest: String?  // URL to team badge
)

data class FootballScore(
    @SerializedName("winner")    val winner: String?,
    @SerializedName("duration")  val duration: String?,
    @SerializedName("fullTime")  val fullTime: FootballGoals?,
    @SerializedName("halfTime")  val halfTime: FootballGoals?
)

data class FootballGoals(
    @SerializedName("home") val home: Int?,
    @SerializedName("away") val away: Int?
)

// ─────────────────────────────────────────────────────────────────────────────
// Google Places DTOs
// ─────────────────────────────────────────────────────────────────────────────

data class PlacesNearbySearchResponse(
    @SerializedName("results")      val results: List<PlaceResult>?,
    @SerializedName("status")       val status: String?,
    @SerializedName("next_page_token") val nextPageToken: String?
)

data class PlaceResult(
    @SerializedName("place_id")       val placeId: String,
    @SerializedName("name")           val name: String?,
    @SerializedName("vicinity")       val vicinity: String?,
    @SerializedName("geometry")       val geometry: PlaceGeometry?,
    @SerializedName("rating")         val rating: Float?,
    @SerializedName("user_ratings_total") val userRatingsTotal: Int?,
    @SerializedName("opening_hours")  val openingHours: PlaceOpeningHoursBrief?,
    @SerializedName("photos")         val photos: List<PlacePhoto>?,
    @SerializedName("types")          val types: List<String>?
)

data class PlaceGeometry(
    @SerializedName("location") val location: LatLngDto?
)

data class LatLngDto(
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?
)

data class PlaceOpeningHoursBrief(
    @SerializedName("open_now") val openNow: Boolean?
)

data class PlacePhoto(
    @SerializedName("photo_reference") val photoReference: String?,
    @SerializedName("height")          val height: Int?,
    @SerializedName("width")           val width: Int?
)

data class PlaceDetailsResponse(
    @SerializedName("result") val result: PlaceDetailResult?,
    @SerializedName("status") val status: String?
)

data class PlaceDetailResult(
    @SerializedName("place_id")              val placeId: String?,
    @SerializedName("name")                  val name: String?,
    @SerializedName("formatted_address")     val formattedAddress: String?,
    @SerializedName("rating")                val rating: Float?,
    @SerializedName("user_ratings_total")    val userRatingsTotal: Int?,
    @SerializedName("opening_hours")         val openingHours: PlaceOpeningHoursDetail?,
    @SerializedName("photos")                val photos: List<PlacePhoto>?,
    @SerializedName("formatted_phone_number") val formattedPhoneNumber: String?,
    @SerializedName("website")               val website: String?,
    @SerializedName("geometry")              val geometry: PlaceGeometry?
)

data class PlaceOpeningHoursDetail(
    @SerializedName("open_now")    val openNow: Boolean?,
    @SerializedName("weekday_text") val weekdayText: List<String>?
)

// ─────────────────────────────────────────────────────────────────────────────
// Google Distance Matrix DTOs
// ─────────────────────────────────────────────────────────────────────────────

data class DistanceMatrixResponse(
    @SerializedName("status")            val status: String?,
    @SerializedName("origin_addresses")  val originAddresses: List<String>?,
    @SerializedName("destination_addresses") val destinationAddresses: List<String>?,
    @SerializedName("rows")              val rows: List<DistanceMatrixRow>?
)

data class DistanceMatrixRow(
    @SerializedName("elements") val elements: List<DistanceMatrixElement>?
)

data class DistanceMatrixElement(
    @SerializedName("status")   val status: String?,   // "OK" | "NOT_FOUND" | "ZERO_RESULTS"
    @SerializedName("duration") val duration: DistanceMatrixValue?,
    @SerializedName("distance") val distance: DistanceMatrixValue?
)

data class DistanceMatrixValue(
    @SerializedName("value") val value: Int?,    // seconds or metres
    @SerializedName("text")  val text: String?   // e.g. "22 mins" or "5.2 km"
)
