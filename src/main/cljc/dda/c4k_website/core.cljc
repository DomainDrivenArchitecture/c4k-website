(ns dda.c4k-website.core
 (:require
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]
  [dda.c4k-website.website :as website]))

(defn k8s-objects [config]  
    (cm/concat-vec
     (map yaml/to-string
          [(website/generate-nginx-deployment config)
           (website/generate-nginx-configmap config)
           (website/generate-nginx-service config)
           (website/generate-website-content-volume config)
           (website/generate-ingress config)
           (website/generate-certificate config)
           (website/generate-website-build-cron config)           
           (website/generate-website-build-secret config)])))

(not
 (=
  {:website.conf-c1 
   "server {\n  listen 80 default_server;\n  listen [::]:80 default_server;\n  listen 443 ssl;\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n  server_name test.de;\n  # security headers\n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n  add_header Content-Security-Policy \"default-src 'self'; font-src *;img-src * data:; script-src *; style-src *\";\n  add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # maybe need to add:\n  # add_header Permissions-Policy \"permissions here\";\n  root /var/www/html/website/;\n  # root /usr/share/nginx/html/; # testing purposes\n  index index.html;\n  location / {                              \n    try_files $uri $uri/ /index.html =404;      \n  }\n}", 
   :website.conf-c2 "server {\n  listen 80 default_server;\n  listen [::]:80 default_server;\n  listen 443 ssl;\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n  server_name test.com;\n  # security headers\n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n  add_header Content-Security-Policy \"default-src 'self'; font-src *;img-src * data:; script-src *; style-src *\";\n  add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # maybe need to add:\n  # add_header Permissions-Policy \"permissions here\";\n  root /var/www/html/website/;\n  # root /usr/share/nginx/html/; # testing purposes\n  index index.html;\n  location / {                              \n    try_files $uri $uri/ /index.html =404;      \n  }\n}", 
   :name-c1 "test-de-configmap", 
   :name-c2 "test-com-configmap"}
  {:website.conf-c1
   "server {\n  listen 80 default_server;\n  listen [::]:80 default_server;\n  listen 443 ssl;\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n  server_name test.de;\n  # security headers\n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n  add_header Content-Security-Policy \"default-src 'self'; font-src *;img-src * data:; script-src *; style-src *\";\n  add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # maybe need to add:\n  # add_header Permissions-Policy \"permissions here\";\n  root /var/www/html/website/;\n  # root /usr/share/nginx/html/; # testing purposes\n  index index.html;\n  location / {                              \n    try_files $uri $uri/ /index.html =404;      \n  }\n}\n",
   :website.conf-c2 "server {\n  listen 80 default_server;\n  listen [::]:80 default_server;\n  listen 443 ssl;\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n  server_name test.com;\n  # security headers\n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n  add_header Content-Security-Policy \"default-src 'self'; font-src *;img-src * data:; script-src *; style-src *\";\n  add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # maybe need to add:\n  # add_header Permissions-Policy \"permissions here\";\n  root /var/www/html/website/;\n  # root /usr/share/nginx/html/; # testing purposes\n  index index.html;\n  location / {                              \n    try_files $uri $uri/ /index.html =404;      \n  }\n}\n",
   :name-c1 "test-de-configmap",
   :name-c2 "test-com-configmap"}))
