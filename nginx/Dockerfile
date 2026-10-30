# Nginx reverse proxy image: the config is baked in (not bind-mounted) so the stack can be
# deployed by a Jenkins agent running in a container, where host bind-mount paths differ.
FROM nginxinc/nginx-unprivileged:stable-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/nginx-health || exit 1
