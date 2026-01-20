# build
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

# runtime
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY public/config.template.js /config.template.js
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENV API_BASE_URL=/api
ENTRYPOINT ["/entrypoint.sh"]
