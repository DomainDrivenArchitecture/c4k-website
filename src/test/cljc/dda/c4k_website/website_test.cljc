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
  (is (= {:apiVersion "networking.k8s.io/v1",
          :kind "Ingress",
          :metadata
          {:name "test-de-ingress",
           :namespace "default",
           :annotations
           {:ingress.kubernetes.io/ssl-redirect "true",
            :traefik.ingress.kubernetes.io/router.middlewares "default-redirect-https@kubernetescrd"}},
          :spec
          {:tls [{:hosts ["test.de"], :secretName "test-de-cert"}],
           :rules
           [{:host "test.de",
             :http {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "test-de-service", :port {:number 80}}}}]}}]}}
         (cut/generate-ingress {:fqdn "test.de"}))))

(deftest should-generate-nginx-configmap
  (is (= {:website.conf-c1 "server {\n  listen 80 default_server;\n  listen [::]:80 default_server;\n  listen 443 ssl;\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n  server_name test.de;\n  # security headers\n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n  add_header Content-Security-Policy \"default-src 'self'; font-src *;img-src * data:; script-src *; style-src *\";\n  add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # maybe need to add:\n  # add_header Permissions-Policy \"permissions here\";\n  root /var/www/html/website/;\n  # root /usr/share/nginx/html/; # testing purposes\n  index index.html;\n  location / {                              \n    try_files $uri $uri/ /index.html =404;      \n  }\n}",
          :website.conf-c2 "server {\n  listen 80 default_server;\n  listen [::]:80 default_server;\n  listen 443 ssl;\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n  server_name test.com;\n  # security headers\n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n  add_header Content-Security-Policy \"default-src 'self'; font-src *;img-src * data:; script-src *; style-src *\";\n  add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # maybe need to add:\n  # add_header Permissions-Policy \"permissions here\";\n  root /var/www/html/website/;\n  # root /usr/share/nginx/html/; # testing purposes\n  index index.html;\n  location / {                              \n    try_files $uri $uri/ /index.html =404;      \n  }\n}",
          :name-c1 "test-de-configmap",
          :name-c2 "test-com-configmap"}
         (th/map-diff (cut/generate-nginx-configmap {:fqdn "test.de"                                                
                                                })
                      (cut/generate-nginx-configmap {:fqdn "test.com"                                                
                                                })))))

(deftest should-generate-nginx-deployment 
  (is (= {:apiVersion "apps/v1",
          :kind "Deployment",
          :metadata {:name "test-de-deployment"},
          :spec
          {:replicas 1,
           :selector {:matchLabels {:app "test-de-nginx"}},
           :template
           {:metadata {:labels {:app "test-de-nginx"}},
            :spec
            {:containers
             [{:name "test-de-nginx",
               :image "nginx:latest",
               :imagePullPolicy "Always",
               :ports [{:containerPort 80}],
               :volumeMounts
               [{:mountPath "/etc/nginx", :readOnly true, :name "nginx-config-volume"}
                {:mountPath "/var/log/nginx", :name "log"}
                {:mountPath "/var/www/html/website", :name "website-content-volume"}
                {:mountPath "/etc/certs", :name "website-cert", :readOnly true}]}],
             :volumes
             [{:name "nginx-config-volume",
               :configMap
               {:name "test-de-configmap",
                :items
                [{:key "nginx.conf", :path "nginx.conf"}
                 {:key "website.conf", :path "conf.d/website.conf"}
                 {:key "mime.types", :path "mime.types"}]}}
              {:name "log", :emptyDir {}}
              {:name "website-content-volume", :persistentVolumeClaim {:claimName "test-de-content-volume"}}
              {:name "website-cert",
               :secret
               {:secretName "test-de-cert", :items [{:key "tls.crt", :path "tls.crt"} {:key "tls.key", :path "tls.key"}]}}]}}}}
         (cut/generate-nginx-deployment {:fqdn "test.de"}))))

(deftest should-generate-nginx-service ;todo
  (is (= {:name-c1 "test-de-service",
          :name-c2 "test-com-service",
          :app-c1 "test-de-nginx",
          :app-c2 "test-com-nginx"
          }
         (th/map-diff (cut/generate-nginx-service {:fqdn "test.de"})
                      (cut/generate-nginx-service {:fqdn "test.com"})))))

(deftest should-generate-website-build-cron ;todo
  (is (= {:apiVersion "batch/v1beta1",
          :kind "CronJob",
          :metadata {:name "test-de-build-cron", :labels {:app.kubernetes.part-of "website"}},
          :spec
          {:schedule "1,7,14,21,28,35,42,49,54,59 * * * *",
           :successfulJobsHistoryLimit 1,
           :failedJobsHistoryLimit 1,
           :jobTemplate
           {:spec
            {:template
             {:spec
              {:containers
               [{:image "domaindrivenarchitecture/c4k-website-build",
                 :name "test-de-build-app",
                 :imagePullPolicy "IfNotPresent",
                 :command ["/entrypoint.sh"],
                 :env [{:name "HOSTADRESS", :value "test.de"}],
                 :envFrom [{:secretRef {:name "test-de-secret"}}],
                 :volumeMounts [{:name "content-volume", :mountPath "/var/www/html/website"}]}],
               :volumes [{:name "content-volume", :persistentVolumeClaim {:claimName "test-de-content-volume"}}],
               :restartPolicy "OnFailure"}}}}}}
         (cut/generate-website-build-cron {:fqdn "test.de"}))))

(deftest should-generate-website-build-deployment
  (is (= {:apiVersion "apps/v1",
          :kind "Deployment",
          :metadata {:name "test-de-build-deployment"},
          :spec
          {:replicas 0,
           :selector {:matchLabels {:app "test-de-builder"}},
           :strategy {:type "Recreate"},
           :template
           {:metadata
            {:labels {:app "test-de-builder", :app.kubernetes.io/name "test-de-builder", :app.kubernetes.io/part-of "website"}},
            :spec
            {:containers
             [{:image "domaindrivenarchitecture/c4k-website-build",
               :name "test-de-build-app",
               :imagePullPolicy "IfNotPresent",
               :command ["/entrypoint.sh"],
               :env [{:name "HOSTADRESS", :value "test.de"}],
               :envFrom [{:secretRef {:name "test-de-secret"}}],
               :volumeMounts [{:name "content-volume", :mountPath "/var/www/html/website"}]}],
             :volumes [{:name "content-volume", :persistentVolumeClaim {:claimName "test-de-content-volume"}}]}}}}
         (cut/generate-website-build-deployment {:fqdn "test.de"}))))

(deftest should-generate-website-build-secret
  (is (= {:name-c1 "test-de-secret",
          :name-c2 "test-com-secret",
          :AUTHTOKEN-c1 (b64/encode "token1"), 
          :AUTHTOKEN-c2 (b64/encode "token2"),
          :GITREPOURL-c1 (b64/encode "test.de/user/repo.git"),
          :GITREPOURL-c2 (b64/encode "test.com/user/repo.git")}
         (th/map-diff (cut/generate-website-build-secret {:fqdn "test.de" 
                                                          :authtoken "token1" 
                                                          :gitrepourl "test.de/user/repo.git"})
                      (cut/generate-website-build-secret {:fqdn "test.com"
                                                          :authtoken "token2"
                                                          :gitrepourl "test.com/user/repo.git"})))))


(deftest should-generate-website-content-volume
  (is (= {:storage-c1 "2Gi",
          :storage-c2 "10Gi",
          :name-c1 "test-de-content-volume",
          :name-c2 "test-com-content-volume",
          :app-c1 "test-de-nginx",
          :app-c2 "test-com-nginx"}
         (th/map-diff (cut/generate-website-content-volume {:fqdn "test.de"
                                                            :volume-total-storage-size 10
                                                            :number-of-websites 5})
                      (cut/generate-website-content-volume {:fqdn "test.com"
                                                            :volume-total-storage-size 50
                                                            :number-of-websites 5})))))