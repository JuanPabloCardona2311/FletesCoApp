# ── Etapa 1: compilación ──
# Node 22 (LTS): cumple el requisito de Vite 8 (^20.19.0 || >=22.12.0).
FROM node:22-alpine AS build
WORKDIR /app

# Primero solo package*.json: las dependencias quedan en una capa que se
# reutiliza mientras no cambien.
COPY package*.json ./
RUN npm ci

COPY . .

# Vite incrusta las variables VITE_* en el código al compilar, por eso se
# reciben como ARG de build (no sirven si se pasan al ejecutar el contenedor).
ARG VITE_AUTH_URL=http://localhost:8081
ARG VITE_PERFIL_URL=http://localhost:8082
ARG VITE_SOLICITUDES_URL=http://localhost:8083
ARG VITE_ADMIN_URL=http://localhost:8084
ENV VITE_AUTH_URL=$VITE_AUTH_URL \
    VITE_PERFIL_URL=$VITE_PERFIL_URL \
    VITE_SOLICITUDES_URL=$VITE_SOLICITUDES_URL \
    VITE_ADMIN_URL=$VITE_ADMIN_URL

RUN npm run build

# ── Etapa 2: servidor estático ──
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
