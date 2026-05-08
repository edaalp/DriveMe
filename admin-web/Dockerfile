FROM node:20-alpine AS build
WORKDIR /app

ARG VITE_API_ORIGIN
ENV VITE_API_ORIGIN=$VITE_API_ORIGIN

COPY package*.json ./
RUN npm ci

COPY index.html vite.config.js ./
COPY public public
COPY src src
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
