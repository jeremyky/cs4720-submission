# NBAlytics

CS 4720 final project. Android app for browsing live NBA games, predicting winners, and seeing what other users predicted.

Built with Jetpack Compose, Firebase Auth, Cloud Firestore, Retrofit, and DataStore.

## Setup

This repo is public, so the actual API key and Firebase config are NOT committed. Two template files show the shape of what's needed:

- `local.properties.example`
- `app/google-services.json.example`

### What you need to do before building

1. **Odds API key** — copy the template:
   ```
   cp local.properties.example local.properties
   ```
   Then either paste in the key from the Gradescope submission notes, or sign up at <https://the-odds-api.com> for a free tier and put your own key in `local.properties` as `ODDS_API_KEY=...`.

2. **Firebase config** — drop a real `google-services.json` at `app/google-services.json`. If you're grading this submission, the file is provided in the Gradescope submission notes alongside the API key. If you're just exploring the code, you can register your own Firebase project (package name `edu.nd.pmcburne.hwapp.one`) and download a fresh `google-services.json` from the Firebase Console.

The shape of `google-services.json` is in `app/google-services.json.example`.

## Build & run

1. Open the project root in Android Studio (Hedgehog or newer).
2. Let Gradle sync. Android Studio will write `sdk.dir` into `local.properties` on first import — don't add it manually.
3. Run on an emulator (API 27+) or device.

## Test accounts

- `tester1@nbalytics.test` / `Test1234!`
- `tester2@nbalytics.test` / `Test1234!`

Or sign up a fresh account from the login screen.

## Features

- Login / signup with Firebase email + password
- Today's NBA games with live scores and moneyline odds (The Odds API)
- Game details with a "Make a pick" feature: pick a winner, optional note, saved to Firestore
- Community Picks: see all users' predictions for a game (live updates)
- Favorites: save teams, browse your past picks
- Standings: live conference rankings (ESPN)
- Settings: preferred sportsbook, odds format, show-only-favorites toggle (persisted with DataStore)
- Share button on game details (system share sheet)
