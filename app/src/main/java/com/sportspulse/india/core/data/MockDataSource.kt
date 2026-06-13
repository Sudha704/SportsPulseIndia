package com.sportspulse.india.core.data

import com.sportspulse.india.core.domain.entity.Broadcast
import com.sportspulse.india.core.domain.entity.BroadcastPlatform
import com.sportspulse.india.core.domain.entity.HierarchyLevel
import com.sportspulse.india.core.domain.entity.MatchAlert
import com.sportspulse.india.core.domain.entity.MatchStatus
import com.sportspulse.india.core.domain.entity.NewsItem
import com.sportspulse.india.core.domain.entity.SportEvent
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.entity.UserLocation
import com.sportspulse.india.core.domain.entity.Venue
import com.sportspulse.india.core.domain.entity.VenueType

/**
 * Static mock data used for:
 *  - Offline UI testing and preview composables
 *  - Emulator screenshots
 *  - Unit test fixtures
 *
 * All times are epoch-ms values relative to a reference date (2025-04-01 IST).
 */
object MockDataSource {

    // ─────────────────────────────────────────────────────────────────────────
    // Sport Events
    // ─────────────────────────────────────────────────────────────────────────

    val mockEvents: List<SportEvent> = listOf(

        // 1. India vs Australia – LIVE Test match (International)
        SportEvent(
            id = "mock_cricket_001",
            sport = SportType.CRICKET,
            title = "India vs Australia – 4th Test",
            homeTeam = "India",
            awayTeam = "Australia",
            homeTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/en/8/8d/Cricket_India_Crest.svg",
            awayTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/en/5/5c/Cricket_Australia_%28logo%29.svg",
            status = MatchStatus.LIVE,
            scoreOrTime = "IND 312/6 (87.4) | AUS 285",
            venue = "Narendra Modi Stadium",
            city = "Ahmedabad",
            country = "India",
            hierarchyLevel = HierarchyLevel.INTERNATIONAL,
            broadcasts = listOf(
                Broadcast(
                    channelName = "Star Sports 1 HD",
                    channelNumber = 451,
                    platform = BroadcastPlatform.JIOSTAR,
                    isFree = false,
                    deeplinkUri = "hotstar://sports/cricket"
                ),
                Broadcast(
                    channelName = "DD Sports",
                    channelNumber = 605,
                    platform = BroadcastPlatform.DD_SPORTS,
                    isFree = true
                )
            ),
            startTimeIst = System.currentTimeMillis() - 3_600_000L,
            competition = "Border-Gavaskar Trophy",
            geminiSummary = null,
            newsItems = listOf(
                NewsItem(
                    id = "news_001",
                    title = "Rohit Sharma scores blistering century on Day 3",
                    description = "Captain Rohit Sharma led India's fightback with a dominant 112-run innings.",
                    sourceUrl = "https://www.cricbuzz.com/cricket-news/mock",
                    sourceName = "Cricbuzz",
                    publishedAt = System.currentTimeMillis() - 1_800_000L,
                    sport = SportType.CRICKET
                )
            )
        ),

        // 2. MI vs CSK – IPL (National / LIVE)
        SportEvent(
            id = "mock_cricket_002",
            sport = SportType.CRICKET,
            title = "Mumbai Indians vs Chennai Super Kings",
            homeTeam = "MI",
            awayTeam = "CSK",
            homeTeamLogoUrl = "https://bcciplayerimages.s3.ap-south-1.amazonaws.com/ipl/MI/Logos/Roundbig/MIroundbig.png",
            awayTeamLogoUrl = "https://bcciplayerimages.s3.ap-south-1.amazonaws.com/ipl/CSK/Logos/Roundbig/CSKroundbig.png",
            status = MatchStatus.LIVE,
            scoreOrTime = "MI 142/4 (16.2 ov)",
            venue = "Wankhede Stadium",
            city = "Mumbai",
            country = "India",
            hierarchyLevel = HierarchyLevel.NATIONAL,
            broadcasts = listOf(
                Broadcast(
                    channelName = "Star Sports 1",
                    channelNumber = 450,
                    platform = BroadcastPlatform.JIOSTAR,
                    isFree = false,
                    deeplinkUri = "hotstar://sports/cricket/ipl"
                ),
                Broadcast(
                    channelName = "JioStar (Online)",
                    platform = BroadcastPlatform.JIOSTAR,
                    isFree = false,
                    deeplinkUri = "hotstar://",
                    streamingUrl = "https://www.hotstar.com/in/sports"
                )
            ),
            startTimeIst = System.currentTimeMillis() - 5_400_000L,
            competition = "IPL 2025"
        ),

        // 3. India vs South Korea – FIH Hockey (International / Upcoming)
        SportEvent(
            id = "mock_hockey_001",
            sport = SportType.HOCKEY,
            title = "India vs South Korea – FIH Pro League",
            homeTeam = "India",
            awayTeam = "South Korea",
            status = MatchStatus.UPCOMING,
            scoreOrTime = "Today, 7:30 PM IST",
            venue = "Major Dhyan Chand National Stadium",
            city = "New Delhi",
            country = "India",
            hierarchyLevel = HierarchyLevel.INTERNATIONAL,
            broadcasts = listOf(
                Broadcast(
                    channelName = "Star Sports 2",
                    channelNumber = 455,
                    platform = BroadcastPlatform.JIOSTAR,
                    isFree = false,
                    deeplinkUri = "hotstar://sports/hockey"
                ),
                Broadcast(
                    channelName = "DD Sports",
                    platform = BroadcastPlatform.DD_SPORTS,
                    isFree = true
                )
            ),
            startTimeIst = System.currentTimeMillis() + 7_200_000L,
            competition = "FIH Pro League 2025"
        ),

        // 4. Bengaluru FC vs Mohun Bagan – ISL (National / Upcoming)
        SportEvent(
            id = "mock_football_001",
            sport = SportType.FOOTBALL,
            title = "Bengaluru FC vs Mohun Bagan SG",
            homeTeam = "Bengaluru FC",
            awayTeam = "Mohun Bagan SG",
            status = MatchStatus.UPCOMING,
            scoreOrTime = "Tomorrow, 7:30 PM IST",
            venue = "Sree Kanteerava Stadium",
            city = "Bengaluru",
            country = "India",
            hierarchyLevel = HierarchyLevel.NATIONAL,
            broadcasts = listOf(
                Broadcast(
                    channelName = "Sports18",
                    channelNumber = 236,
                    platform = BroadcastPlatform.JIOSTAR,
                    isFree = false,
                    deeplinkUri = "hotstar://sports/football/isl"
                ),
                Broadcast(
                    channelName = "JioStar Premium",
                    platform = BroadcastPlatform.JIOSTAR,
                    isFree = false,
                    streamingUrl = "https://www.hotstar.com/in/sports/football"
                )
            ),
            startTimeIst = System.currentTimeMillis() + 86_400_000L,
            competition = "Indian Super League 2025"
        ),

        // 5. Patna Pirates vs U Mumba – PKL (National / Completed)
        SportEvent(
            id = "mock_kabaddi_001",
            sport = SportType.KABADDI,
            title = "Patna Pirates vs U Mumba",
            homeTeam = "Patna Pirates",
            awayTeam = "U Mumba",
            status = MatchStatus.COMPLETED,
            scoreOrTime = "Patna 38 – 31 U Mumba",
            venue = "Patliputra Indoor Stadium",
            city = "Patna",
            country = "India",
            hierarchyLevel = HierarchyLevel.NATIONAL,
            broadcasts = listOf(
                Broadcast(
                    channelName = "Star Sports 2",
                    channelNumber = 455,
                    platform = BroadcastPlatform.JIOSTAR,
                    isFree = false
                )
            ),
            startTimeIst = System.currentTimeMillis() - 10_800_000L,
            competition = "Pro Kabaddi League Season 11"
        ),

        // 6. PV Sindhu vs Akane Yamaguchi – BWF (International / Upcoming)
        SportEvent(
            id = "mock_badminton_001",
            sport = SportType.BADMINTON,
            title = "PV Sindhu vs Akane Yamaguchi – QF",
            homeTeam = "PV Sindhu (IND)",
            awayTeam = "Akane Yamaguchi (JPN)",
            status = MatchStatus.UPCOMING,
            scoreOrTime = "Today, 3:00 PM IST",
            venue = "Axiata Arena",
            city = "Kuala Lumpur",
            country = "Malaysia",
            hierarchyLevel = HierarchyLevel.INTERNATIONAL,
            broadcasts = listOf(
                Broadcast(
                    channelName = "Star Sports 1",
                    channelNumber = 450,
                    platform = BroadcastPlatform.JIOSTAR,
                    isFree = false,
                    deeplinkUri = "hotstar://sports/badminton"
                ),
                Broadcast(
                    channelName = "FanCode",
                    platform = BroadcastPlatform.FANCODE,
                    isFree = false,
                    deeplinkUri = "fancode://badminton",
                    streamingUrl = "https://fancode.com/sport/badminton"
                )
            ),
            startTimeIst = System.currentTimeMillis() + 3_600_000L,
            competition = "BWF Malaysia Open 2025"
        ),

        // 7. Formula 1 – Bahrain GP (International / LIVE)
        SportEvent(
            id = "mock_f1_001",
            sport = SportType.MOTORSPORTS,
            title = "F1 Bahrain Grand Prix – Race",
            homeTeam = "Red Bull Racing",
            awayTeam = "Ferrari",
            status = MatchStatus.LIVE,
            scoreOrTime = "Lap 42/57 | VER P1, LEC P2",
            venue = "Bahrain International Circuit",
            city = "Sakhir",
            country = "Bahrain",
            hierarchyLevel = HierarchyLevel.INTERNATIONAL,
            broadcasts = listOf(
                Broadcast(
                    channelName = "Star Sports Select 1",
                    channelNumber = 460,
                    platform = BroadcastPlatform.JIOSTAR,
                    isFree = false,
                    deeplinkUri = "hotstar://sports/motorsports/f1"
                )
            ),
            startTimeIst = System.currentTimeMillis() - 2_700_000L,
            competition = "Formula 1 World Championship 2025"
        ),

        // 8. Karnataka vs Maharashtra – Ranji Trophy (Local / Upcoming)
        SportEvent(
            id = "mock_cricket_003",
            sport = SportType.CRICKET,
            title = "Karnataka vs Maharashtra – Ranji Trophy",
            homeTeam = "Karnataka",
            awayTeam = "Maharashtra",
            status = MatchStatus.UPCOMING,
            scoreOrTime = "14 Apr, 9:30 AM IST",
            venue = "M Chinnaswamy Stadium",
            city = "Bengaluru",
            country = "India",
            hierarchyLevel = HierarchyLevel.LOCAL,
            broadcasts = listOf(
                Broadcast(
                    channelName = "FanCode",
                    platform = BroadcastPlatform.FANCODE,
                    isFree = false,
                    deeplinkUri = "fancode://cricket/ranji",
                    streamingUrl = "https://fancode.com/sport/cricket"
                )
            ),
            startTimeIst = System.currentTimeMillis() + 172_800_000L,
            competition = "Ranji Trophy 2024-25"
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Venues
    // ─────────────────────────────────────────────────────────────────────────

    val mockVenues: List<Venue> = listOf(
        Venue(
            placeId = "mock_place_001",
            name = "PlayArena Sports Complex",
            address = "Whitefield, Bengaluru, Karnataka 560066",
            type = VenueType.INDOOR_COMPLEX,
            distanceKm = 3.2,
            travelTimeMinutes = 12,
            rating = 4.5f,
            reviewCount = 1234,
            isOpenNow = true,
            availableSports = listOf(SportType.BADMINTON, SportType.CRICKET, SportType.FOOTBALL),
            lat = 12.9716,
            lng = 77.7499,
            phoneNumber = "+91 80 4567 8901",
            websiteUrl = "https://playarena.in"
        ),
        Venue(
            placeId = "mock_place_002",
            name = "Green Turf Football Ground",
            address = "Koramangala, Bengaluru, Karnataka 560034",
            type = VenueType.TURF,
            distanceKm = 5.8,
            travelTimeMinutes = 22,
            rating = 4.2f,
            reviewCount = 876,
            isOpenNow = true,
            nextOpenTime = null,
            availableSports = listOf(SportType.FOOTBALL, SportType.CRICKET),
            lat = 12.9352,
            lng = 77.6245
        ),
        Venue(
            placeId = "mock_place_003",
            name = "Smash Badminton Academy",
            address = "Indiranagar, Bengaluru, Karnataka 560038",
            type = VenueType.BADMINTON_COURT,
            distanceKm = 7.1,
            travelTimeMinutes = 25,
            rating = 4.7f,
            reviewCount = 542,
            isOpenNow = false,
            nextOpenTime = "Opens at 6:00 AM tomorrow",
            availableSports = listOf(SportType.BADMINTON),
            lat = 12.9784,
            lng = 77.6408
        ),
        Venue(
            placeId = "mock_place_004",
            name = "Kanteerava Outdoor Stadium",
            address = "Kasturba Road, Bengaluru, Karnataka 560001",
            type = VenueType.STADIUM,
            distanceKm = 11.5,
            travelTimeMinutes = 38,
            rating = 4.0f,
            reviewCount = 2100,
            isOpenNow = true,
            availableSports = listOf(SportType.FOOTBALL, SportType.HOCKEY, SportType.CRICKET),
            lat = 12.9786,
            lng = 77.5947
        ),
        Venue(
            placeId = "mock_place_005",
            name = "Aqua Zone Swimming Academy",
            address = "JP Nagar, Bengaluru, Karnataka 560078",
            type = VenueType.SWIMMING_POOL,
            distanceKm = 14.3,
            travelTimeMinutes = 45,
            rating = 4.3f,
            reviewCount = 310,
            isOpenNow = true,
            availableSports = emptyList(),
            lat = 12.9063,
            lng = 77.5857
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Alerts
    // ─────────────────────────────────────────────────────────────────────────

    val mockAlerts: List<MatchAlert> = listOf(
        MatchAlert(
            id = "alert_001",
            eventId = "mock_hockey_001",
            eventTitle = "India vs South Korea – FIH Pro League",
            sport = SportType.HOCKEY,
            eventStartTimeIst = System.currentTimeMillis() + 7_200_000L,
            reminderMinutesBefore = 30,
            isEnabled = true
        ),
        MatchAlert(
            id = "alert_002",
            eventId = "mock_cricket_003",
            eventTitle = "Karnataka vs Maharashtra – Ranji Trophy",
            sport = SportType.CRICKET,
            eventStartTimeIst = System.currentTimeMillis() + 172_800_000L,
            reminderMinutesBefore = 60,
            isEnabled = false
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // User Location
    // ─────────────────────────────────────────────────────────────────────────

    val mockUserLocation = UserLocation(
        latitude = 12.9716,
        longitude = 77.5946,
        areaName = "MG Road",
        cityName = "Bengaluru",
        isManual = false
    )
}
