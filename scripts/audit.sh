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
{
  echo "=== DATE ==="
  date -u
  echo "=== TARGET ==="
  echo "$TARGET"
  echo "=== DNS ==="
  getent hosts "$HOST" || true
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
for path in robots.txt sitemap.xml .well-known/security.txt favicon.ico; do
  echo "=== /$path ===" >> "$OUT/common-files.txt"
  curl -k -sS -L --max-time 15 -o /dev/null -w '%{http_code} %{content_type} %{size_download} %{url_effective}\n' "http://$HOST/$path" >> "$OUT/common-files.txt" || true
done
nmap -Pn -sV --version-light --script "default,safe" --top-ports 100 "$HOST" -oA "$OUT/nmap" || true
echo "[+] audit complete"
