#!/bin/sh
set -eu

# Render runtime config
if [ -f /config.template.js ]; then
  # shellcheck disable=SC2016
  API_BASE_URL_ESCAPED=$(printf '%s' "${API_BASE_URL:-/api}" | sed 's/"/\\"/g')
  export API_BASE_URL="$API_BASE_URL_ESCAPED"
  envsubst < /config.template.js > /usr/share/nginx/html/config.js
fi

exec nginx -g 'daemon off;'
