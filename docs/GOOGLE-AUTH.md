# Google login setup for Nie Wtop

The Android client is wired for Supabase Auth + Google OAuth using the niewtop://auth callback and PKCE.

## 1. Separate Supabase project

Do not reuse the Bombownia project. This repository is intentionally isolated.

## 2. Enable Google in Supabase

In the new Supabase project:
- Authentication → Providers → Google
- Add the Google OAuth Web Client ID and secret
- Add niewtop://auth to the allowed redirect URLs

## 3. Google Cloud

Create OAuth credentials for this app in Google Cloud / Google Auth Platform and configure the appropriate consent-screen audience.

## 4. Local Android configuration

Create local.properties in the repository root (it is ignored by Git):

SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLISHABLE_KEY

Never put a Supabase service-role or secret key in the APK.

## 5. Database

Run supabase/schema.sql in the new project when cloud history is enabled.

The Android callback registered by the app is niewtop://auth.
