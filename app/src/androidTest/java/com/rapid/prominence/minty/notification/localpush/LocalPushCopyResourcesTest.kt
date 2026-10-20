package com.rapid.prominence.minty.notification.localpush

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rapid.prominence.minty.R
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalPushCopyResourcesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun allDefaultScenesExposeApprovedEnglishCopyAndGoAction() {
        expectedTitles.forEach { (scene, expected) ->
            assertArrayEquals(scene, expected.toTypedArray(), stringArray("notif_event_${scene}_titles"))
            assertEquals(expected.first(), string("notif_event_${scene}_title"))
        }
        assertArrayEquals(
            expectedBackgroundTitles.toTypedArray(),
            stringArray("notif_event_app_background_titles"),
        )
        assertArrayEquals(
            expectedBackgroundContents.toTypedArray(),
            stringArray("notif_event_app_background_contents"),
        )
        assertEquals(expectedBackgroundTitles.first(), string("notif_event_app_background_title"))
        assertEquals(expectedBackgroundContents.first(), string("notif_event_app_background_content"))
        assertEquals("GO", context.getString(R.string.notif_event_action_open_home))
    }

    @Test
    fun allDefaultScenesRotateInOrderAndWrap() {
        val resolveTitle = LocalEventPushHelper::class.java.getDeclaredMethod(
            "resolveTitle",
            android.content.Context::class.java,
            LocalEventPushHelper.EventType::class.java,
        ).apply { isAccessible = true }

        expectedTitles.forEach { (scene, expected) ->
            val type = LocalEventPushHelper.EventType.valueOf(scene.uppercase(Locale.US))
            val actual = List(expected.size + 1) {
                resolveTitle.invoke(LocalEventPushHelper, context, type) as String
            }
            assertCyclicRotation(scene, expected, actual)
        }

        val expectedBackgroundPairs = expectedBackgroundTitles.zip(expectedBackgroundContents) { title, body ->
            "$title\n$body"
        }
        val actualBackgroundPairs = List(expectedBackgroundPairs.size + 1) {
            resolveTitle.invoke(
                LocalEventPushHelper,
                context,
                LocalEventPushHelper.EventType.APP_BACKGROUND,
            ) as String
        }
        assertCyclicRotation("app_background", expectedBackgroundPairs, actualBackgroundPairs)
    }

    @Test
    fun pendingTitleSelectionDoesNotAdvanceUntilDisplayed() {
        val selectTitle = runCatching {
            LocalEventPushHelper::class.java.getDeclaredMethod(
                "selectTitle",
                android.content.Context::class.java,
                LocalEventPushHelper.EventType::class.java,
            ).apply { isAccessible = true }
        }.getOrElse {
            org.junit.Assert.fail("Local push copy must support deferred title commits")
            return
        }
        val type = LocalEventPushHelper.EventType.PLAYBACK_PAUSED
        val expected = expectedTitles.getValue("playback_paused")

        val firstSelection = requireNotNull(selectTitle.invoke(LocalEventPushHelper, context, type))
        val secondSelection = requireNotNull(selectTitle.invoke(LocalEventPushHelper, context, type))
        val firstTitle = selectionTitle(firstSelection)
        assertEquals(firstTitle, selectionTitle(secondSelection))

        firstSelection.javaClass.getDeclaredMethod("commit").apply { isAccessible = true }
            .invoke(firstSelection)
        val nextSelection = requireNotNull(selectTitle.invoke(LocalEventPushHelper, context, type))
        val start = expected.indexOf(firstTitle)
        assertNotEquals("playback_paused returned copy outside its approved pool", -1, start)
        assertEquals(expected[(start + 1) % expected.size], selectionTitle(nextSelection))

        nextSelection.javaClass.getDeclaredMethod("commit").apply { isAccessible = true }
            .invoke(nextSelection)
        secondSelection.javaClass.getDeclaredMethod("commit").apply { isAccessible = true }
            .invoke(secondSelection)
        val afterStaleCommit = requireNotNull(selectTitle.invoke(LocalEventPushHelper, context, type))
        assertEquals(
            "A stale selection must not roll the persisted index backward",
            expected[(start + 2) % expected.size],
            selectionTitle(afterStaleCommit),
        )
    }

    private fun stringArray(name: String): Array<String> {
        val id = context.resources.getIdentifier(name, "array", context.packageName)
        assertNotEquals("Missing string-array $name", 0, id)
        return context.resources.getStringArray(id)
    }

    private fun string(name: String): String {
        val id = context.resources.getIdentifier(name, "string", context.packageName)
        assertNotEquals("Missing string $name", 0, id)
        return context.getString(id)
    }

    private fun selectionTitle(selection: Any): String =
        selection.javaClass.getDeclaredMethod("getDisplayTitle").apply { isAccessible = true }
            .invoke(selection) as String

    private fun assertCyclicRotation(scene: String, expected: List<String>, actual: List<String>) {
        val start = expected.indexOf(actual.first())
        assertNotEquals("$scene returned copy outside its approved pool", -1, start)
        val expectedCycle = List(expected.size + 1) { offset ->
            expected[(start + offset) % expected.size]
        }
        assertEquals("$scene should rotate in order and wrap to its first item", expectedCycle, actual)
    }

    private val expectedTitles = linkedMapOf(
        "battery_below_20" to listOf(
            "Battery under 20% — don't let the music die mid-song. Plug in and keep listening.",
            "Low battery alert! Your playlist is waiting — hum along while you charge.",
            "The vibe's peaking but the battery isn't. Plug in — the mood goes on.",
            "Low battery, no intermission. Plug in — the music will be right back.",
            "20% battery, 100% mood — plug in and let the music fill up too.",
        ),
        "battery_below_50" to listOf(
            "50% left — just enough for this mixtape. Press play.",
            "50% battery report: not quite done with this playlist yet. Continue?",
            "Halfway there — perfect time to slow down with a mellow track.",
            "Half the battery, half the quiet — leave the rest to the music.",
            "50% is enough for the music to walk this stretch with you.",
        ),
        "charging_to_80" to listOf(
            "80% battery, 100% vibe — the music stays on.",
            "Nearly charged — stay for tonight's headliner track.",
            "80% battery ping: it's been a while since you pressed play.",
            "Nearly full charge, nearly full heart — music will top it off.",
            "Battery filling up — tonight's BGM should clock in too.",
        ),
        "network_change" to listOf(
            "WiFi's back and so is your music. Tap to tune in.",
            "No network? No problem. Your offline playlist never leaves.",
            "Back online — treat your ears to lossless quality again.",
            "WiFi connected — your radio mode is one tap away.",
            "The blackout was a pause, the music's the plot. Resume.",
        ),
        "charging" to listOf(
            "Charging time is music time — let the songs keep you company.",
            "Phone's charging. Are you? Recharge with some music too.",
            "Phone's recharging — what's your recharging anthem?",
            "Plugged in? Now cue yourself a song.",
            "Don't just wait while charging — finally play those saved songs.",
        ),
        "charging_disconnect" to listOf(
            "Unplugged — don't forget to bring the music along.",
            "Full battery deserves a full-energy song.",
            "Charged up — now tune your mood. Press play.",
            "Battery at 100%, soundtrack standing by.",
            "The moment the cable's out, let the music lead.",
        ),
        "screen_on_after_lock" to listOf(
            "Screen's lit — light up your ears too.",
            "The screen woke up — wake your ears with a track.",
            "Screen on: a song is waiting for you.",
            "Screen's on — stay for one line of the song.",
            "Screen on? Perfect — new tracks just landed.",
        ),
        "screen_lock" to listOf(
            "Screen dark, listening bright.",
            "Screen off — just sound and you.",
            "The moment it goes dark, music takes over.",
            "Screen off, deep-listen mode on.",
            "After the blackout: good headphones and a whole album.",
        ),
        "user_unlock" to listOf(
            "Unlocked. The play button awaits.",
            "You unlocked your phone — let music unlock your mood.",
            "Just unlocked? Music fits this moment.",
            "Unlock done — your queue is standing by.",
            "Phone's open — don't skip today's new songs.",
        ),
        "boot_completed" to listOf(
            "Boot done, and your playlist's already on deck.",
            "A new day starts at boot with a song.",
            "Boot success — a song to settle the fresh start.",
            "Just booted, don't swipe yet — a song first.",
            "Boot ping: a song's waiting for you to open up.",
        ),
        "wired_headset_connect" to listOf(
            "Headphones in — the world mutes, the music maxes.",
            "Welcome back to your private concert.",
            "Plugged in, your personal DJ is on the decks.",
            "Headphones detected — so is the mood for music.",
            "Connected. Your repeat-one era starts now.",
        ),
        "wired_headset_disconnect" to listOf(
            "Unplugged — remember to save what you were playing.",
            "Cable's out, playlist's still waiting.",
            "That last track was good, right? Save it.",
            "Unplugged — don't forget the song that got you.",
            "Disconnected — the playlist's still hot.",
        ),
        "bluetooth_connect" to listOf(
            "BT device detected — party mode can switch on anytime.",
            "Bluetooth in place, the music can roll.",
            "Device ready, and your playlist's synced into place.",
            "New device linked — an old favorite is the best way to test it.",
            "Speaker linked — tonight's BGM is yours to decide.",
        ),
        "bluetooth_disconnect" to listOf(
            "Disconnected, progress all saved — resume whenever.",
            "Disconnected? A good excuse to switch listening styles.",
            "No encore lost — continue the show right on your phone.",
            "Connection lost, but that song of yours isn't over.",
            "Disconnected — don't leave this good song halfway.",
        ),
        "audio_focus_lost" to listOf(
            "The song got interrupted? It's still right there waiting.",
            "Someone hijacked your BGM? Time to take it back.",
            "All handled? Your song's been waiting.",
            "Come back — this song's hitting its peak right now.",
            "Right where it paused, the best part is hiding.",
        ),
        "volume_above_50" to listOf(
            "High-volume warning — this chorus genuinely earns it.",
            "Volume up, and the joy should follow.",
            "One note: volume's high — protect those ears even for a great song.",
            "Volume up, chorus incoming — are you ready?",
            "That volume — even the neighbors want your playlist.",
        ),
        "volume_below_20" to listOf(
            "Volume under 20%, but the feeling's not gone.",
            "Those who listen softly understand music best.",
            "In quiet mode, this track hits different — surprisingly good.",
            "A near-silent night deserves a slow serenade.",
            "Under 20%, just right for some alone time.",
        ),
        "app_inactive" to listOf(
            "It's been a while, and your playlist kind of misses you.",
            "A whole day without music — even your headphones miss you.",
            "Today's BGM slot is still empty, waiting for you.",
            "Days away? A fresh playlist's already packed.",
            "That paused song has been waiting for you to continue.",
        ),
        "scheduled_push_1" to listOf(
            "New music found on your device — guess what turned up.",
            "Scan surprise: a few hidden gems from the bottom of the box.",
            "Library refreshed — come claim the songs that are yours.",
            "Messy playlist? A quick tidy fixes it.",
            "Still \"Untitled\"? This playlist deserves a real name.",
            "The songs you play often deserve a playlist of their own.",
            "An album you saved just updated — worth a look.",
            "While sorting, found one you haven't played in ages.",
            "That album you saved — 3 tracks still unheard.",
            "That genre you love just got new arrivals.",
            "Rock, pop or folk — which flavor tonight?",
            "Your playlists lean one genre — ready to branch out?",
            "AI just made something new, tuned exactly to your taste.",
            "AI music isn't cold — it remembers what you love.",
            "AI continued a track from what you played last week.",
            "Those saved songs — how long since you last played them?",
            "Favorites overflowing — folders make them easier to find.",
            "Cleanup plan: drop the meh, keep the love.",
        ),
        "scheduled_push_2" to listOf(
            "That hour's here — a soft tune to drift off with.",
            "The sleep playlist's ready — just close your eyes.",
            "Before goodnight, one last gentle song.",
            "Today's worries end here — the music takes over to soothe.",
            "Night mode on — music fades as the dreams deepen.",
        ),
        "scheduled_push_3" to listOf(
            "Put music on your home screen, and it plays in one press.",
            "Add the widget, and great songs are within reach.",
            "Place the widget, and the play button lives on your screen.",
            "Widget in hand, an instant shuffle is always available.",
            "There's still an empty music slot on your home screen.",
        ),
        "playback_paused" to listOf(
            "A song paused right here, waiting on your \"continue.\"",
            "The music paused, the story isn't over — come back.",
            "That melody from last time still waits to pick back up.",
            "The second half of that song is even better — don't miss it.",
            "Come finish this one — it's almost done.",
        ),
        "playback_background" to listOf(
            "Your personal radio is live on air right now.",
            "It's playing now, and the chorus is almost here.",
            "This sound fits the you of this exact moment.",
            "Now playing: a song that really fits you.",
            "BGM in progress — protect the rhythm.",
        ),
        "audio_download" to listOf(
            "Download complete, and the new track's in your offline list.",
            "Fresh download in — come give it a listening check.",
            "Downloaded — repeat-one is now an option.",
            "100% downloaded — this song is yours for good.",
            "Offline stash +1, and your no-network playlist grows.",
        ),
    )

    private val expectedBackgroundTitles = listOf(
        "Don't ditch your song",
        "Where'd you go?",
        "5s absence alert",
        "Music's calling",
        "Don't miss this second",
        "Music still open",
        "Wait, song's not done",
        "Come back, the good part's here",
    )

    private val expectedBackgroundContents = listOf(
        "5 seconds out, and the melody's already missing you.",
        "The music's waiting backstage; come back.",
        "Your BGM's still rolling; come take over.",
        "Still sounding back there; come take a look.",
        "5s backstage, and the song's peaking.",
        "5s out, and it resumes in a single move.",
        "5s backstage; don't miss this part.",
        "5s backstage, and this bit's worth it.",
    )
}
