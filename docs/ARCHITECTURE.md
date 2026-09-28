# Nie Wtop — Architecture

## Principle
The client never contains provider secrets. Android sends normalized user input to our backend. The backend orchestrates deterministic checks, external reputation sources and AI explanation.

## Pipeline
Input
→ normalize URL / OCR text
→ platform detection
→ deterministic checks
→ reputation checks
→ price/context checks
→ evidence aggregation
→ AI explanation
→ Result with confidence + evidence

## Evidence classes
- Domain / URL
- Platform safety rules
- Reputation / blacklist
- Offer price/context
- Seller-provided claims
- User-provided screenshot/text

## Result model
A result has:
- risk level: LOW / CAUTION / HIGH / UNKNOWN
- confidence: LOW / MEDIUM / HIGH
- reasons[]
- evidence[]
- recommended_next_steps[]
- checked_at

Risk and confidence are deliberately separate. A high-confidence observation can still be inconclusive overall.

## Planned backend
Supabase:
- auth
- Postgres
- Edge Functions
- storage for explicitly uploaded screenshots
- rate limiting / usage accounting

## External providers
Possible adapters:
- CERT Polska warning list
- VirusTotal URL intelligence
- urlscan.io
- domain registration / DNS / TLS signals
- platform-specific public safety guidance

Provider adapters stay behind our own interface so the app does not depend on one vendor.
