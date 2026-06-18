# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# VITE_* duoc nhung vao bundle luc BUILD (khong phai runtime).
# URL Keycloak chung origin voi app qua duong dan /kc (HTTPS).
ARG VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_URL=${VITE_KEYCLOAK_URL}
RUN npm run build

# ---- Run stage ----
FROM nginx:1.27-alpine
# Tao self-signed cert cho HTTPS (secure context -> crypto.subtle hoat dong).
# SAN gan voi IP cua VM de trinh duyet chap nhan dung dia chi.
ARG HOST_IP=localhost
RUN apk add --no-cache openssl && \
    mkdir -p /etc/nginx/certs && \
    openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
      -keyout /etc/nginx/certs/key.pem -out /etc/nginx/certs/cert.pem \
      -subj "/CN=${HOST_IP}" \
      -addext "subjectAltName=IP:${HOST_IP},DNS:localhost"
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80 443
