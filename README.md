# Mp3os

A Home-screen launcher that is only a music player. Set it as the default Home app and the phone boots straight into it.

**Look**: Material 3 Expressive (spring motion, morphing shapes, cookie placeholder), with directional motion blur on tab slides, track changes and fast scrolling (Android 12+). The whole app takes its colours from the current song's cover; songs without a cover get a placeholder and the default orange palette.

**Tabs**
- **Library**: every song Android has indexed, with covers, search, rescan and shuffle all. The playing row dances
- **Now playing**: cover that dances to the bass (measured inside the player, no mic permission), wavy seek bar, play/pause, next/previous (previous restarts the song after 3 s), back/forward 10 s, shuffle, repeat, **speed** and **pitch** (0.5x to 2.0x in **0.01** steps: slider, or tap / hold the - and + buttons) with a **Sync** button that locks them together
- **Apps**: grid of your installed apps (streaming apps, Settings). Pressing Home from any app returns to the player

Music keeps playing while you're in other apps (foreground media service with a notification and Bluetooth/headset button support).

## Build

1. Install Android Studio (Ladybug or newer) with JDK 17.
2. File > Open > select this `Mp3os` folder and let Gradle sync.
   - If Studio says the Gradle wrapper is missing, accept its offer to use Gradle 8.11.1.
3. Plug in the phone (USB debugging on) and press Run, or Build > Build APK(s) and install it.

## Make it the boot screen

1. Open the app once, allow music access and notifications.
2. Press Home. Android asks which Home app to use: pick **Mp3os** > **Always**.
   Or via adb: `adb shell cmd package set-home-activity com.mp3os.player/.MainActivity`
3. Skip the lockscreen: Settings > Lock screen > Screen lock type > **None** (a PIN/pattern/fingerprint can't be skipped).
4. Optional: Settings > Battery > no restrictions for Mp3os so the system never kills playback.

Don't uninstall Settings or the stock launcher. Keep them as a fallback; the Apps tab opens Settings.

## Notes

- Not compiled or tested on a device yet: expect to fix a small thing or two on first build.
- Library uses MediaStore with `IS_MUSIC != 0`; files in Ringtones/Notifications/Alarms folders are skipped.
- Streaming apps (Spotify, etc.) have their own audio engine, so speed/pitch only apply to the local library.
