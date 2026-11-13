# Architecture

## Android
- Kotlin + Jetpack Compose.
- MVVM with feature-oriented packages and repositories.
- Navigation Compose for navigation.
- Room for durable local drafts and offline queue.
- DataStore for non-sensitive preferences.
- Supabase Auth, Postgres and Storage for cloud services.
- Firebase Cloud Messaging for push notifications.
- AdMob only after consent and policy review.

## Layers
- **Presentation:** Compose screens, view models and UI state.
- **Domain:** use cases and business rules, independent of Android UI.
- **Data:** repositories, local data sources, remote data sources and DTO mapping.
- **Core:** design system, navigation, database, network, logging and security.

## Offline-first write flow
1. Validate user input in the domain layer.
2. Persist the change locally in Room with a UUID and pending sync state.
3. Update the UI from local data immediately.
4. A sync worker submits pending changes when network and auth are available.
5. Server response marks the local mutation synced or records a retryable/permanent error.
6. Conflict policy is explicit per entity; never silently overwrite newer server data.

## Backend boundaries
- Android uses only Supabase URL + publishable key.
- Row Level Security is mandatory on exposed user data.
- Service-role and provider secrets remain in server-side secrets.
- Privileged operations (workspace invitations, AI calls, invoice numbering) belong in Edge Functions or trusted server code.
- Storage objects are private by default; access is granted by workspace membership.

## Data ownership
A user can own a personal workspace and belong to business workspaces. Every tenant-owned record references a workspace. Membership checks use the authenticated user identity and server-enforced RLS, not user-editable profile metadata.

## Reliability
- Structured error states; no swallowed exceptions.
- Retry with bounded backoff for transient network failures.
- Idempotency keys for retryable create operations.
- Crash reporting with privacy-conscious redaction.
- Database migrations are versioned and reviewed.
- Backups and restore procedure are documented before launch.

## Environments
Separate development, staging and production Supabase projects. Never reuse a client project's database. Keep local secrets out of Git.
