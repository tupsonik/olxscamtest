#!/usr/bin/env bash
set -Eeuo pipefail
TARGET="$1"
OUT="$2"
mkdir -p "$OUT"
HOST="$(python3 - "$TARGET" <<'PY'
import sys, urllib.parse
print(urllib.parse.urlparse(sys.argv[1]).hostname or "")
PY
)"
BASE="http://$HOST"
if [[ "$TARGET" == https://* ]]; then BASE="https://$HOST"; fi

{
  echo "=== DATE ==="; date -u
  echo "=== TARGET ==="; echo "$TARGET"
  echo "=== DNS ==="; getent hosts "$HOST" || true
  echo "=== CURL HEADERS ==="
  curl -k -sS -D - -o /dev/null --max-time 20 "$TARGET" || true
  curl -k -sS -L --max-time 30 "$TARGET" -o "$OUT/response.body" || true
  curl -k -sS -L -D "$OUT/headers.txt" -o /dev/null --max-time 30 "$TARGET" || true
} > "$OUT/summary.txt" 2>&1

echo "=== DISCOVERED LINKS ===" > "$OUT/links.txt"
python3 - "$OUT/response.body" >> "$OUT/links.txt" <<'PY'
import re, sys
try:
    s=open(sys.argv[1], errors="replace").read()
    for x in sorted(set(re.findall(r'''(?:href|src|action)\s*=\s*["']([^"']+)["']''', s, re.I))):
        print(x)
except Exception as e:
    print("extract_error:", e)
PY

# Passive/safe application checks. No state-changing requests.
{
  echo "=== METHOD/HEADER BEHAVIOR ==="
  for method in GET HEAD OPTIONS; do
    echo "--- $method ---"
    curl -k -sS -i -X "$method" --max-time 15 "$TARGET" | head -80 || true
  done

  echo "=== PARAMETER PROBES ==="
  for suffix in     "?i=1"     "?i=0"     "?i=-1"     "?i=%27"     "?i=%22"     "?i=%3Cscript%3E"     "?i=%2e%2e%2f"     "?i=%00"; do
    echo "--- $suffix ---"
    curl -k -sS -L -o /dev/null -w 'code=%{http_code} type=%{content_type} size=%{size_download} redirects=%{num_redirects} url=%{url_effective}\n' --max-time 20 "$BASE/pasieka.php$suffix" || true
  done

  echo "=== COMMON ENDPOINTS ==="
  for path in     /pasieka.php     /index.php     /login.php     /admin/     /.git/HEAD     /.env     /config.php     /phpinfo.php     /server-status     /server-info; do
    curl -k -sS -L -o /dev/null -w "$path code=%{http_code} type=%{content_type} size=%{size_download} final=%{url_effective}\n" --max-time 15 "$BASE$path" || true
  done
} > "$OUT/app-probes.txt" 2>&1

for path in robots.txt sitemap.xml .well-known/security.txt favicon.ico; do
  echo "=== /$path ===" >> "$OUT/common-files.txt"
  curl -k -sS -L --max-time 15 -o /dev/null -w '%{http_code} %{content_type} %{size_download} %{url_effective}\n' "$BASE/$path" >> "$OUT/common-files.txt" || true
done

nmap -Pn -sV --version-light --script "default,safe" --top-ports 100 "$HOST" -oA "$OUT/nmap" || true
echo "[+] audit complete"
