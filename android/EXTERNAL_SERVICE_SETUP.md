# THE BENKOUGH — external service configuration

The Android source is wired for real phone OTP, Firebase Cloud Messaging, Supabase-backed notifications, and AdMob. The following provider-owned values must be supplied by the account owner; they must never be committed to the repository.

## 1. Phone ownership OTP

The app now uses Supabase Auth SMS OTP. It does not generate a local code, accept a guessed code, or mark a phone number verified without Supabase verifying the SMS challenge.

In the Supabase project, enable the Phone provider and configure an authorized SMS delivery provider. Keep phone confirmation enabled. Supabase's phone OTP flow requires a configured SMS provider; the Android app cannot deliver SMS by itself. Until the provider is configured, the app will show the provider error rather than granting a session.

## 2. Firebase Cloud Messaging

Register an Android app in the Firebase project with package name:

`com.thebenkough.app`

Add these values as GitHub Actions repository secrets (or local Gradle properties for local builds):

- `BENKOUGH_FIREBASE_APP_ID`
- `BENKOUGH_FIREBASE_API_KEY`
- `BENKOUGH_FIREBASE_PROJECT_ID`
- `BENKOUGH_FIREBASE_SENDER_ID`

The Android app initializes Firebase from these values, requests an FCM token, and registers it in the user's own `device_tokens` row. The Firebase service is declared in the manifest.

For server delivery, add the Firebase service-account JSON as a Supabase Edge Function secret named:

`FCM_SERVICE_ACCOUNT_JSON`

Do not place the service-account JSON in GitHub source, Android BuildConfig, or the APK. The deployed `send-user-notification` Edge Function validates the caller's Supabase session, creates the user's own inbox row, and sends FCM using the server-side secret.

The function is deployed with JWT verification enabled. It refuses anonymous sessions.

## 3. AdMob

The home screen uses Google's official test banner by default. The application ID and banner unit ID can be overridden through:

- `BENKOUGH_ADMOB_APP_ID`
- `BENKOUGH_ADMOB_BANNER_ID`

Only use production IDs issued for this exact app. Production ad publishing and payment settings must be managed by an account owner who meets Google's age and account requirements; do not bypass those requirements.


## 4. Invoice operations

Invoice creation and management are handled by the deployed JWT-protected Edge Functions:

- `create-invoice` verifies the signed-in non-anonymous user, checks their personal workspace, and calculates subtotal, tax and total on the server.
- `manage-invoice` verifies the same ownership before marking an invoice paid or deleting it.

The Android app must call these functions rather than writing directly to the invoice tables. The invoice tables intentionally do not grant direct client write access.

## 5. Local Gradle properties

For a local build, put the values in your user-level Gradle properties file rather than committing them:

`~/.gradle/gradle.properties`

Use the same property names listed above. GitHub Actions reads the matching repository secrets automatically. Missing Firebase values leave remote push disabled but do not prevent a debug APK from building with test ads.
