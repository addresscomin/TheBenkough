# THE BENKOUGH — external service configuration

## 1. Username and password authentication

The Android app signs users up and signs them in through Supabase Auth password sessions. The user enters a username and password; the app maps the username to an internal, non-deliverable Auth email identifier under `users.thebenkough.app`. This is an implementation detail and is not a contact email.

Before enabling signup on the hosted Supabase project:
- Enable the Email provider.
- Turn **Confirm email** off, because the internal username identifier cannot receive confirmation mail.
- Keep anonymous sign-ins disabled.
- Do not describe these accounts as having verified email or phone ownership.

Usernames are stored in `public.profiles.username`, normalized to lowercase, and protected by a unique index. Passwords are handled by Supabase Auth; the app does not store plaintext passwords.

**Password recovery is not available for username-only accounts.** Add and verify a real recovery email to each account before implementing self-service password reset. Do not send reset links to the internal identifier.

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

## 6. Retired phone access endpoint

The old `register-phone-access` endpoint remains disabled with HTTP 410. It must not be re-enabled because it created accounts with phone-confirmed status before proving phone ownership. The Android app no longer uses phone OTP.
