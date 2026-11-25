FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --include=dev --no-audit --no-fund
COPY . .
ARG REACT_APP_AUTH_URL=http://localhost:9090
ARG REACT_APP_CATALOGO_URL=http://localhost:9092
ARG REACT_APP_SUBASTAS_URL=http://localhost:9094
ARG REACT_APP_NOTIFICACIONES_URL=http://localhost:9093
ARG REACT_APP_REQUEST_TIMEOUT_MS=15000
ENV REACT_APP_AUTH_URL=$REACT_APP_AUTH_URL \
    REACT_APP_CATALOGO_URL=$REACT_APP_CATALOGO_URL \
    REACT_APP_SUBASTAS_URL=$REACT_APP_SUBASTAS_URL \
    REACT_APP_NOTIFICACIONES_URL=$REACT_APP_NOTIFICACIONES_URL \
    REACT_APP_REQUEST_TIMEOUT_MS=$REACT_APP_REQUEST_TIMEOUT_MS
RUN npm run build

FROM nginxinc/nginx-unprivileged:stable-alpine
COPY nginx.conf /etc/nginx/techstore.conf.template
COPY --from=build /app/build /usr/share/nginx/html
USER 101
ENV PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD wget -q --spider "http://127.0.0.1:$PORT/health" || exit 1
CMD ["/bin/sh", "-ec", "envsubst '$PORT' < /etc/nginx/techstore.conf.template > /tmp/techstore.conf; exec nginx -c /tmp/techstore.conf -g 'daemon off;'"]
