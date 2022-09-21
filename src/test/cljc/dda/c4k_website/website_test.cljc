(ns dda.c4k-website.website-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.test-helper :as th]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-website.website :as cut]))


(st/instrument `cut/generate-certificate)
(st/instrument `cut/generate-ingress)
(st/instrument `cut/generate-nginx-configmap)
(st/instrument `cut/generate-website-content-volume)

(deftest should-generate-certificate
  (is (= {:name-c2 "prod", :name-c1 "staging"}
         (th/map-diff (cut/generate-certificate {:fqdn "test.de"})
                      (cut/generate-certificate {:issuer "prod"                                                 
                                                 :fqdn "test.de"})))))

(deftest should-generate-ingress
  (is (= {:hosts-c1 "test.de",
          :hosts-c2 "test.com",
          :host-c1 "test.de", 
          :host-c2 "test.com"}
         (th/map-diff (cut/generate-ingress {:fqdn "test.de"
                                             })
                      (cut/generate-ingress {:fqdn "test.com"
                                             })))))

(deftest should-generate-nginx-configmap
  (is (= {:website.conf-c1 "server {\n\n  listen 80 default_server;\n  listen [::]:80 default_server;\n\n  listen 443 ssl;\n\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n\n  server_name test.de\n\n  # security headers\n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n  add_header Content-Security-Policy \"default-src 'self'; font-src *;img-src * data:; script-src *; style-src *\";\n  add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # maybe need to add:\n  # add_header Permissions-Policy \"permissions here\";\n\n  root /var/www/html/website/;\n  # root /usr/share/nginx/html/; # testing purposes\n\n  index index.html;\n\n  try_files $uri /index.html;\n\n}",
          :website.conf-c2 "server {\n\n  listen 80 default_server;\n  listen [::]:80 default_server;\n\n  listen 443 ssl;\n\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n\n  server_name test.com\n\n  # security headers\n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n  add_header Content-Security-Policy \"default-src 'self'; font-src *;img-src * data:; script-src *; style-src *\";\n  add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # maybe need to add:\n  # add_header Permissions-Policy \"permissions here\";\n\n  root /var/www/html/website/;\n  # root /usr/share/nginx/html/; # testing purposes\n\n  index index.html;\n\n  try_files $uri /index.html;\n\n}"}
         (th/map-diff (cut/generate-nginx-configmap {:fqdn "test.de"                                                
                                                })
                      (cut/generate-nginx-configmap {:fqdn "test.com"                                                
                                                })))))

(deftest should-generate-website-content-volume
  (is (= {:storage-c1 "2Gi",
          :storage-c2 "10Gi"}
         (th/map-diff (cut/generate-website-content-volume {:volume-total-storage-size 10
                                                            :number-of-websites 5})
                      (cut/generate-website-content-volume {:volume-total-storage-size 50
                                                            :number-of-websites 5})))))