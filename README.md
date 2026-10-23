# Focus

A lightweight Android timer project for the Focus study workflow.

## First implementation milestone
- Android app using Java, minimum Android 6.0 (API 23).
- Local timer preferences and a foreground service to keep an active timer running when the Activity is closed.
- Editable study, short-break, long-break durations.
- GitHub Actions builds a debug APK and publishes it as the `Focus-debug-APK` artifact.

## Build
Run `gradle assembleDebug` with JDK 17 and Gradle 8.7.

This is an initial implementation. Auto-transition between session types, selectable alarm sounds, the tablet statistics site, and direct local Wi-Fi synchronization are planned for subsequent milestones and are not yet complete.
