# Focus

A lightweight Android timer for the Focus study workflow, laid out for a Samsung Galaxy J2 Prime / Grand Prime class screen (about 540×960, Android 6).

## What this build does
- Java app, minimum Android 6.0 (API 23), portrait.
- Home screen keeps the ring, start button, and the three controls on a short screen, with western digits (`25:00`) so Arabic locales do not scramble the clock.
- Session line reads `0 / 4 جلسات مكتملة في الدورة`.
- Settings durations show Arabic units (دقيقة / دقائق / جلسات). The first themes are Golden Glow, Pink Clouds, Cozy Corner, and Night City.
- The timer runs in a foreground service with a partial wake lock, so Samsung's memory killer is less likely to stop it when the screen is off. Swiping the app away restarts the service if a session is still running.
- Auto-transition follows the three settings switches, including a long break after the chosen number of study sessions.
- Alarm uses the chosen ringtone (short beep when auto-advance is on) and a short vibration.
- Wallpapers are decoded at screen size in RGB_565 so the large theme photos do not run out of memory.

## Build
GitHub Actions builds a debug APK on every push to `main` and uploads it as `Focus-debug-APK`.

Locally: JDK 17 and `gradle assembleDebug`.

The tablet statistics site and direct phone-to-tablet sync are still not connected.
