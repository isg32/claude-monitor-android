# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Android app that monitors Claude Code API usage and costs. It tracks token consumption, calculates spending per model, and surfaces this data through a dashboard, CLI log, Playground tab, and a home screen widget.

## Build & Run

Requires Android Studio with JDK 21 and Gradle 9.3.1.

**Prerequisites:**
- Copy `.env.example` to `.env` and set `GEMINI_API_KEY` (loaded via Secrets Gradle Plugin)
- Firebase requires `app/google-services.json` — CI auto-generates a dummy one; for local dev you can do the same

```bash
# Assemble debug APK
./gradlew assembleDebug

# Run unit tests (Robolectric, runs on JVM)
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.ExampleUnitTest"

# Generate Roborazzi screenshot (writes to app/src/test/screenshots/)
./gradlew test --tests "com.example.GreetingScreenshotTest"

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

## Architecture

Single-module, single-`Activity` app following MVVM + Repository pattern.

**Data layer** (`com.example.data`):
- `AppDatabase` — Room singleton with `fallbackToDestructiveMigration` (version bumps are destructive)
- `UsageDao` — DAO for `UsageLog` and `ApiConfig` entities, both exposed as `Flow`
- `UsageRepository` — thin wrapper; `allUsageLogs` and `apiConfig` are the two live streams

**Network layer** (`com.example.network`):
- `AnthropicClient` — Retrofit singleton hitting `https://api.anthropic.com/`
- `AnthropicApiService` — single `createMessage` endpoint
- `ModelPricing` — source of truth for model IDs, per-token USD rates, display names, and chart colors. **Update here when adding new models.**

**UI layer** (`com.example.ui`):
- `MonitorViewModel` — owns all state via `MutableStateFlow<MonitorUiState>`; handles login/logout, demo mode, playground queries, and log refresh
- `MonitorScreen` — single full-screen Composable with a 4-tab layout: Dashboard, CLI Log, Playground, Settings
- Theme in `com.example.ui.theme`

**Widget** (`com.example.widget`):
- `MonitorWidgetProvider` — `AppWidgetProvider` that reads the Room DB directly and renders cost/budget data onto `monitor_widget_layout.xml`
- `BudgetNotificationHelper` — posts a notification when spending exceeds the configured budget threshold

**Demo mode:** When no API key is set, `MonitorViewModel` pre-loads mock `UsageLog` entries and simulates refresh/playground responses without hitting the network.

## Key Conventions

- `ApiConfig` is a single-row entity (always `id = 1`); use `upsert`-style `saveApiConfig` to update it
- Costs are calculated client-side in `ModelPricing.calculateCost`; token counts come from the Anthropic API response `usage` field
- The `isDemoMode` flag on `ApiConfig` gates all live network calls in the ViewModel
- Room schema version is `1` with destructive migration — bump version and add a proper `Migration` object before shipping to production users

## CI/CD

`.github/workflows/release.yml` builds a debug APK on every push to `main` and on PRs. A GitHub Release with the APK is created automatically when a `v*` tag is pushed.
