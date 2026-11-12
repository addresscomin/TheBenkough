# THE BENKOUGH

**Create. Build. Grow.** — a native Android creator + business workspace.

## Repository layout
- `android/` — native Android application (Kotlin + Jetpack Compose)
- `supabase/` — database schema, migrations and backend configuration
- `docs/` — product, architecture, security and release documentation
- Existing root landing page remains separate from the Android application.

## Current implementation status
This repository is being converted in stages. The Android source foundation and initial database migration are being added on the `android-foundation` branch. A source scaffold is not yet a signed APK, a deployed backend, or a Play Store release.

## Build
Open the `android/` folder in Android Studio (current stable version with Android SDK installed), allow Gradle sync, then run the `app` configuration on a physical Android device or emulator.

Supabase credentials are intentionally not committed. Configure the project URL and publishable key through local Gradle properties or CI secrets before enabling cloud features. Never place a Supabase service-role key in the Android app.

## Product principles
1. Core creator and business workflows must work without AI.
2. Local drafts must survive app restarts and network loss.
3. Workspace data is private by default and protected by Row Level Security.
4. Ads must be clearly labelled, frequency-limited, and never interrupt active editing.
5. No fake account creation, placeholder success states, or demo-only controls in production flows.

See [Product specification](docs/PRODUCT_SPEC.md), [Architecture](docs/ARCHITECTURE.md), and [Security checklist](docs/SECURITY.md).
