package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

data class ReleaseNoteEntry(
    val version: String,
    val notes: List<String>
)

/**
 * The single source of truth for the changelog. Newest version first.
 *
 * To ship a new release: add a new entry at the TOP of this list whose
 * version matches versionName in app/build.gradle. The About screen and
 * the one-time "What's New" pop-up both read from here.
 */
object ReleaseNotes {

    val entries: List<ReleaseNoteEntry> =
        listOf(
            ReleaseNoteEntry(
                version = "1.3",
                notes = listOf(
                    "Material 3 redesign: the whole app now uses Material 3 for a more consistent look.",
                    "Liked Songs: tap the heart on any song to add it to the built-in Liked Songs playlist.",
                    "Importing an M3U playlist named \"Liked Songs\" now fills the built-in Liked Songs playlist.",
                    "Autoplay: when your queue ends, AuralArc keeps playing songs similar to what you were listening to. You can turn it off in Settings.",
                    "CD Shuffle: shuffle your albums while each album still plays from first track to last.",
                    "Grid and list views for Albums, Artists, and Playlists.",
                    "Redesigned Driving Mode with larger controls, shuffle, repeat, and a heart button.",
                    "Choose which Bluetooth device opens Driving Mode from Settings > Driving Mode Bluetooth.",
                    "Fixed turning Shuffle off so your songs return to their original order.",
                    "Added Power Saving Mode."
                )
            ),
            ReleaseNoteEntry(
                version = "1.2.4",
                notes = listOf(
                    "Added support for Duet LRC line-by-line color notation.",
                    "Added shuffle and play buttons to album, artist, and playlist headers.",
                    "Added shuffle and play buttons to Songs page."
                )
            ),
            ReleaseNoteEntry(
                version = "1.2.3",
                notes = listOf(
                    "Fixed the 'smooth' animations and made them more fluid.",
                    "Fixed transitions between app elements.",
                    "Added an initial setup page.",
                    "Added back the import and export M3U capabilities.",
                    "Fixed Navidrome timing out and not loading large music libraries.",
                    "Optimized Navidrome library loading.",
                    "Fixed queue visually jumping to the top when a song is removed or moved around.",
                    "Fixed the app sometimes not saving a song's position when the app is closed.",
                    "Fixed app randomly crashing in the background after a small period of time.",
                    "Optimized library loading.",
                    "Optimized library scanning.",
                    "Optimized and improved lyric scanning to be more reliable.",
                    "Optimized UI and navigation.",
                    "Optimized battery usage.",
                    "Optimized background tasks.",
                    "Optimized app cache size.",
                    "Improved animations, animation speed, and animation jittering.",
                    "Improved app stability.",
                    "Started adding Material3 styling to the app.",
                    "Made AuralArc an externally discoverable music application (to use with KWGT, for example)."
                )
            ),
            ReleaseNoteEntry(
                version = "1.2.2",
                notes = listOf(
                    "Changed app's package name.",
                    "Introduced smooth moving transitions between every page in the app.",
                    "Added a toggle in the Appearance settings to turn animations off.",
                    "Fixed the Today's Picks refreshing every time the tab is changed or reloaded.",
                    "Made the Playlist header smoothly shrink down instead of abruptly shrinking as the playlist is scrolled down.",
                    "Fixed song titles being off-center due to the HD badge. They are now centered."
                )
            ),
            ReleaseNoteEntry(
                version = "1.2.1",
                notes = listOf(
                    "Fixed a bug where some artists' albums would be completely empty.",
                    "Fixed a bug where your Navidrome password would still show up on your keyboard while typing."
                )
            ),
            ReleaseNoteEntry(
                version = "1.2",
                notes = listOf(
                    "This update was a MASSIVE overhaul!",
                    "Fixed playback pausing when Shuffle is toggled or the queue is modified.",
                    "Fixed playback restoration after reopening the app.",
                    "Restored the last played song, queue position, playback position, Shuffle mode, and Repeat mode.",
                    "Restored the last played song in the mini-player while the library is still loading.",
                    "Fixed the playback progress bar starting behind the actual song position.",
                    "Fixed album Play and Shuffle actions. Fixed artist-page Play so music begins playing automatically.",
                    "Improved queue ordering and queue-state restoration. Preserved playback while editing metadata for the currently playing song." ,
                    "Library Organization Fixed albums being separated when songs contain featured artists.",
                    "Albums are now categorized primarily using Album Artist rather than every credited track artist." ,
                    "Replaced the old Library “View” card with a horizontal selector for:",
                    " - Songs",
                    "- Albums",
                    "- Artists",
                    "- Playlists",
                    "The Library now remembers the most recently selected category.",
                    "Removed the fixed “Default Library Tab” setting.",
                    "Improved album and artist browsing.",
                    "Added release-date sorting.",
                    "Improved library startup caching so saved songs appear before a full refresh finishes.",
                    "Only the currently selected library source is refreshed during startup.",
                    "Cleaned up the Search page and search-result layout.",
                    "Added more reliable local and Navidrome library caching.",
                    " ",
                    "The app now remembers whether the user was last viewing Home, Search, or Library.",
                    "AuralArc reopens to the last-used main tab instead of always opening Home.",
                    "Returning from Now Playing restores the previous screen instead of resetting the Library.",
                    "Album, artist, playlist, and Search states are preserved while navigating to other screens.",
                    "Search text and results are preserved when returning from Now Playing.",
                    "Android Back no longer forces Search or Library back to Home before exiting.",
                    "The currently selected Library category is stored between app launches.",
                    "Improve live rotation into landscape and make the foreground system navigation bar transparent without placing controls underneath it.",
                    " ",
                    "Fixed Add Album to Playlist so the complete album is added.",
                    "Removed Add Artist to Playlist.",
                    "Removed inappropriate Add Album to Playlist and Play Next actions from individual-song and Now Playing menus while preserving them where applicable.",
                    "Removed M3U playlist import and export.",
                    "Added automatic playlist artwork.",
                    "Added manually selected playlist artwork.",
                    "Improved playlist artwork display.",
                    "Improved playlist ordering and playlist menus.",
                    "Added an improved collapsing playlist header.",
                    "Replaced the playlist creation “New” button with a centered + button.",
                    " ",
                    "Redesigned and cleaned up the Now Playing interface.",
                    "Improved the Now Playing artwork and information layout.",
                    "Added centered title and HD-badge presentation.",
                    "Added detailed audio-quality information below the progress controls.",
                    "Improved playback control spacing and sizing.",
                    "Improved album Play, Shuffle, and Repeat behavior.",
                    "Improved notification and MediaSession playback synchronization.",
                    "Added smoother interface transitions.",
                    "Improved handling of long song titles with slowly oscillating marquee text.",
                    "Improved portrait and landscape layouts.",
                    " ",
                    "Added synchronized LRC lyrics.",
                    "Added lyric seeking by selecting a synced lyric line.",
                    "Added a compact lyrics preview on the Now Playing screen.",
                    "Added smoother automatic lyric scrolling.",
                    "Improved active-line tracking.",
                    "Added support for picked lyrics folders.",
                    "Added automatic discovery of lyrics from default and user-selected folders.",
                    "Added brand-new filetype Duet LRC support using .dlrc files.",
                    "Added standard and custom LRC metadata parsing.",
                    "Added separate two-column duet lyric rendering.",
                    "Added distinct singer colors and shared-line presentation.",
                    "Added a setting to enable or disable Duet LRC support.",
                    "Increased duet lyric text size.",
                    "Fixed a Duet LRC parser crash caused by regular-expression escaping.",
                    "Improved full-screen and preview lyric layouts.",
                    " ",
                    "Moved Listening Stats access from Settings to the main toolbar.",
                    "Added Weekly, Monthly, Yearly, and All Time filters.",
                    "Added total listening time.",
                    "Added counted-play totals.",
                    "Added completed-play totals.",
                    "Added skip totals.",
                    "Added unique-track totals.",
                    "Added Top 5 Songs.",
                    "Added Top 5 Artists.",
                    "Added Top 5 Albums.",
                    "Added genre-based listening statistics.",
                    "Added genre percentages based on listening duration.",
                    "Added genre metadata support for:",
                    "- Local MediaStore songs",
                    "Preserved existing all-time listening totals.",
                    "Removed the old Recently Played section from Listening Stats.",
                    "Removed the Reset Listening Stats control.",
                    "Track Metadata Editing",
                    "Added a metadata editor to the Track Info screen.",
                    "Added editing support for:",
                    "- Title",
                    "- Artist",
                    "- Album",
                    "- Album Artist",
                    "- Genre",
                    "-Year",
                    "-Track Number",
                    "- Disc Number",
                    " ",
                    "Added Android MediaStore write-approval handling.",
                    "Added write support for picked-folder/SAF tracks.",
                    "Added clear read-only handling for Navidrome tracks.",
                    "Added temporary-copy editing to reduce the risk of damaging the original audio file.",
                    "Added original-file restoration when final write-back fails.",
                    "Updated the Library, queue, Track Info, Now Playing, and cached library after metadata changes.",
                    "Prevented metadata editing from restarting or pausing the currently playing track.",
                    " ",
                    "Added Add Album Artwork to local albums that do not already have artwork.",
                    "Added image selection for album artwork.",
                    "Added batch artwork embedding across every writable song in an album.",
                    "Added MP3, FLAC, Vorbis/OGG, and other supported tag-format handling.",
                    "Fixed Android Jaudiotagger artwork writing failures for FLAC and Vorbis files.",
                    "Added temporary-file validation before replacing original songs.",
                    "Added cached artwork clearing and re-extraction after an edit.",
                    "The artwork option automatically disappears once the album has artwork.",
                    "Added reporting for:",
                    "- Successfully updated tracks",
                    "- Read-only tracks",
                    "- Failed tracks",
                    " ",
                    "Expanded Track Info into separate sections for:",
                    "- Audio Quality",
                    "- File",
                    "- Source",
                    "- Technical Information",
                    "Added more detailed codec, bitrate, sample-rate, bit-depth, channel, file-size, and source information.",
                    "Added HD badges for qualifying high-resolution tracks.",
                    "Improved detailed duration formatting.",
                    "Added Genre to track metadata and library scanning.",
                    " ",
                    "Added expandable release notes.",
                    "Cleaned up the Appearance settings.",
                    "Removed obsolete Library default-tab controls.",
                    "Removed obsolete song-duration row controls.",
                    "Improved toolbar icon placement.",
                    "Improved card sizing, spacing, and visual consistency.",
                    "Improved animation timing throughout the app.",
                    "Improved error and loading messages.",
                    "Display 'Artist • Album' on song rows everywhere, including playlists and queues.",
                    "Fix long-title marquee text so the first character is never initially clipped.",
                )
            ),
            ReleaseNoteEntry(
                version = "1.1",
                notes = listOf(
                    "No release notes."
                )
            ),
            ReleaseNoteEntry(
                version = "1.0",
                notes = listOf(
                    "Initial release."
                )
            )
        )

    fun forVersion(
        version: String
    ): ReleaseNoteEntry? {
        return entries.firstOrNull {
            it.version == version
        }
    }
}

@Composable
fun ReleaseNoteBullets(
    notes: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            6.dp
        )
    ) {
        notes.forEach { note ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.PurpleBright
                )

                Spacer(
                    modifier = Modifier.width(
                        9.dp
                    )
                )

                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AuralArcStyle.TextSecondary,
                    modifier = Modifier.weight(
                        1f
                    )
                )
            }
        }
    }
}