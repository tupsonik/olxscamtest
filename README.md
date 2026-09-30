# Pantest Lab

Autonomiczne laboratorium bezpieczeństwa dla własnych/autoryzowanych hostów.

## Co robi
- DNS i podstawowe informacje o hoście
- HTTP headers, redirects, cookies i TLS
- bezpieczny skan usług przez Nmap
- sprawdzenie typowych publicznych plików
- OWASP ZAP Baseline
- zapis dowodów jako workflow artifacts

Domyślny cel: `pasieka.hstn.me`.

Nie wykonuje destrukcyjnych exploitów, brute-force ani zmian na celu.

## Uruchomienie
GitHub → Actions → Pantest Lab → Run workflow.

Używaj wyłącznie wobec systemów, do których masz zgodę na testowanie.
