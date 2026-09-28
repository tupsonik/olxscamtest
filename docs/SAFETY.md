# Safety, Privacy & Trust

## Never claim certainty
The product must not label a person "scammer" based only on an automated score.

Use language such as:
- "Found warning signs"
- "Needs verification"
- "We could not verify this information"
- "This is not proof of fraud"

## Sensitive data
Do not ask for:
- passwords
- card numbers
- CVV
- banking login
- one-time authentication codes

If a screenshot appears to contain sensitive payment/authentication data, the app should warn the user before upload and encourage redaction.

## Third-party scanning
Some URL-analysis services can retain submitted indicators in their datasets. VirusTotal explicitly warns that submitted or queried indicators can enter its dataset. Therefore URLs should be sent to external providers only after the user understands the implications, and private scanning should be preferred where contractually and technically appropriate.

## Platform compliance
The app must not impersonate OLX, Vinted, banks, CERT Polska or government services. It is an independent safety assistant.

## False positives
Every automated warning needs an explanation and, where possible, a source. Users must be able to see what was actually observed versus what the model inferred.
