FROM nginx:alpine

# Конфиг nginx с проксированием /api/ на gateway
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Статика
COPY index.html /usr/share/nginx/html/
COPY login.html /usr/share/nginx/html/
COPY register.html /usr/share/nginx/html/
COPY documents.html /usr/share/nginx/html/
COPY css /usr/share/nginx/html/css
COPY js /usr/share/nginx/html/js

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]