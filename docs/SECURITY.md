# Security and privacy checklist

- [ ] Dedicated Supabase project for THE BENKOUGH; do not reuse other client projects.
- [ ] RLS enabled on every exposed application table.
- [ ] Test cross-user and cross-workspace access with at least two accounts.
- [ ] Publishable key only in Android; no service-role or secret key in APK.
- [ ] Private storage buckets for workspace files and exports.
- [ ] Validate file size, MIME type and allowed extensions.
- [ ] Server-side authorization for invitations and privileged actions.
- [ ] No passwords, access tokens, invoice PII or private file URLs in logs.
- [ ] Account deletion and data deletion workflow.
- [ ] Privacy policy, Data Safety form, consent and ad disclosures.
- [ ] Dependency and SDK review before each release.
- [ ] Guardian-managed Play Console/AdMob setup where required by age and platform rules.
