#!/bin/sh
set -e
mkdir -p /var/log/supervisor /tmp
exec /usr/bin/supervisord -c /app/supervisord.conf
