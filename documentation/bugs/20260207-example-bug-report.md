This is an example bug report.
---
## Title: "Example Bug Report"
## Description
This is an example bug report. It is meant to show the format and structure of a bug report, and to provide a template for future bug reports. It is not meant to be a real bug report, and should not be taken seriously.

## Steps to Reproduce
1. Open the app.
2. Click on the "Example Bug" button.
3. Observe the error message that appears.

## Expected Behavior
When the "Example Bug" button is clicked, the app should display a message that says "Example Bug Button Clicked!" instead of an error message.

## Actual Behavior
When the "Example Bug" button is clicked, the app displays an error message that says "An error has occurred. Please try again later."

## Additional Information
- Device: Pixel 4a
- Android Version: 11
- App Version: 1.0.0
### Logcat Output:
```
2026-02-07 12:34:56.789 E/ExampleBug: An error has occurred. Please try again later.
java.lang.NullPointerException: Attempt to invoke virtual method 'void com.kingjoshdavid.funfection.ExampleBugButton.onClick()' on a null object reference
    at com.kingjoshdavid.funfection.MainActivity.onCreate(MainActivity.java:42)
    at android.app.Activity.performCreate(Activity.java:8000)
    at android.app.Activity.performCreate(Activity.java:7984)
    at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1309)
(truncated)
```

## Thoughts where problem might be:
The error message and stack trace suggest that there is a null pointer exception occurring when the "Example Bug" button is clicked. This could be due to the button not being properly initialized or referenced in the code. It may be worth checking the `ExampleActivity` class, specifically around line 42, to see if the button is being set up correctly and if there are any potential issues with the way it is being accessed.

