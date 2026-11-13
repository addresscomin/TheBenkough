# THE BENKOUGH — Product specification

## Product definition
A native Android creative-business workspace that connects content production with small-business operations. Its core loop is:

**Idea → Create → Organize → Deliver → Grow**

It is not a generic AI chatbot. AI is an optional assistant; deterministic tools remain useful when AI is unavailable.

## Primary audiences
- Independent creators who plan and produce short-form content.
- Freelancers and small studios managing clients and deliverables.
- Small businesses that need lightweight marketing and project organization.

## Product pillars

### Creator Studio
- Script Room: rich plain-text/structured script editing, autosave, word and character counts, draft recovery, export.
- Teleprompter: adjustable text size, scroll speed, mirror mode and pause/resume.
- Storyboard: ordered scenes with title, shot notes, duration and reference asset.
- Caption Engine: reusable caption drafts and categorized hashtag collections.
- Content Planner: calendar/list views and draft/published status.
- Thumbnail Planner: layout and copy planning; full visual editor is a later milestone.

### Business Hub
- Client directory and client detail records.
- Lead pipeline with source, stage and notes.
- Projects and tasks with status, priority and due date.
- Invoice records with validated line items and PDF export.
- Activity history and reminders.

### Shared workspace
- Personal and business workspaces.
- Project Vault for files and references.
- Search, tags, notifications and cloud sync.
- Offline-first drafts with explicit sync state.

### Web Forge (later release)
- Shareable business profile and portfolio pages.
- Service listings and contact links.
- Template publishing and public URL management.

### BENKOUGH AI (optional)
- Script and caption suggestions.
- Business copy drafts.
- Project and content ideas.
- All AI requests routed through server-side functions; provider secrets never ship in the APK.

## Monetization
Primary model: Google AdMob, subject to Google Play and AdMob policy compliance.
- Native placements only in appropriate discovery/library surfaces.
- Optional rewarded ads for clearly described bonus content.
- No ads inside active editing, teleprompter, invoice composition or other focused workflows.
- Interstitials are not part of the initial MVP; reconsider only after usability and policy review.
- No deceptive placements, forced ad views or accidental-click layouts.

## MVP acceptance criteria
1. Authentication and account recovery use real backend responses.
2. User can create, edit, archive and delete projects.
3. Script drafts autosave locally and recover after process restart.
4. Tasks and clients support create/read/update/delete with validation.
5. Offline changes remain queued and sync safely when connectivity returns.
6. Workspace data is isolated by RLS; cross-account access tests fail.
7. File uploads use private storage policies.
8. Invoice calculations are validated and PDF output is readable.
9. Ad consent and privacy disclosures are implemented before production ads.
10. Every visible control has a working action or is clearly marked unavailable.

## Out of scope for first MVP
- Full nonlinear video editing.
- Social network feed or public messaging.
- Payment processing and holding customer funds.
- AI-generated image/video production.
- Multi-tenant admin impersonation.
