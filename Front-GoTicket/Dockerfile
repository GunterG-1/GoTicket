FROM node:20-alpine AS build
ARG VITE_API_MANAGER_URL=
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ENV VITE_API_MANAGER_URL=${VITE_API_MANAGER_URL}
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 80
