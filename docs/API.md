# API Contract — v1

## POST /v1/analyze
Request:
{
  "input_type": "url | screenshot",
  "url": "optional",
  "text": "optional",
  "locale": "pl-PL",
  "platform_hint": "optional"
}

Response:
{
  "analysis_id": "uuid",
  "status": "queued | analyzing | complete | failed",
  "result": {
    "risk_level": "LOW | CAUTION | HIGH | UNKNOWN",
    "confidence": "LOW | MEDIUM | HIGH",
    "summary": "...",
    "reasons": [],
    "evidence": [],
    "next_steps": []
  }
}

## Evidence object
- type
- title
- observation
- source
- timestamp
- reliability

The AI layer may summarize evidence but must not invent evidence.

## POST /v1/report
Allows a user to report a suspicious listing/domain. Reports are queued for moderation and are never treated as proof by themselves.
