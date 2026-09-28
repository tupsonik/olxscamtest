# Supabase Data Model

## analyses
- id uuid primary key
- user_id uuid nullable
- input_type text
- normalized_url text nullable
- platform text nullable
- risk_level text nullable
- confidence text nullable
- summary text nullable
- created_at timestamptz

## evidence
- id uuid primary key
- analysis_id uuid
- type text
- title text
- observation text
- source_url text nullable
- reliability text
- observed_at timestamptz

## reports
- id uuid primary key
- user_id uuid nullable
- target text
- category text
- details text
- status text
- created_at timestamptz

## usage
- user_id uuid
- period_start date
- analyses_count int

## Privacy
Keep raw screenshots only when needed and only for the shortest practical retention period. Prefer extracted structured data for long-term history.
