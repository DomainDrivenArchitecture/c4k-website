(ns dda.c4k-website.website-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.test-helper :as th]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-website.website :as cut]   
   [clojure.spec.alpha :as s]))

(st/instrument `cut/generate-http-ingress)
(st/instrument `cut/generate-https-ingress)
(st/instrument `cut/generate-nginx-configmap)
(st/instrument `cut/generate-nginx-deployment)
(st/instrument `cut/generate-nginx-service)
(st/instrument `cut/generate-website-content-volume)
(st/instrument `cut/generate-website-certificate)
(st/instrument `cut/generate-website-build-cron)
(st/instrument `cut/generate-website-build-deployment)
(st/instrument `cut/generate-website-build-secret)

(deftest should-be-valid-website-auth-spec
  (is (true? (s/valid? cut/auth? {:auth
                                  [{:unique-name "test.io"
                                    :username "someuser"
                                    :authtoken "abedjgbasdodj"}
                                   {:unique-name "example.io"
                                    :username "someuser"
                                    :authtoken "abedjgbasdodj"}]}))))

(deftest should-be-valid-website-conf-spec
  (is (true? (s/valid? cut/config? {:issuer "staging"
                                    :websites
                                    [{:unique-name "test.io" ; 
                                      :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"]
                                      :gitea-host "gitlab.de"
                                      :gitea-repo "repo"
                                      :branchname "main"}
                                     {:unique-name "example.io"
                                      :fqdns ["example.org", "www.example.com"]
                                      :gitea-host "finegitehost.net"
                                      :gitea-repo "repo"
                                      :branchname "main"}]}))))

(deftest should-generate-nginx-configmap
  (is (= {:website.conf-c1 "server {\n  listen 80 default_server;\n  listen [::]:80 default_server;\n  listen 443 ssl;\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n  server_name test.de www.test.de test-it.de www.test-it.de; \n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # add_header Permissions-Policy \"permissions here\";\n  root /var/www/html/website/;\n  index index.html;\n  location / {\n    try_files $uri $uri/ /index.html =404;\n  }\n}\n",
          :website.conf-c2 "server {\n  listen 80 default_server;\n  listen [::]:80 default_server;\n  listen 443 ssl;\n  ssl_certificate /etc/certs/tls.crt;\n  ssl_certificate_key /etc/certs/tls.key;\n  server_name example.de www.example.de example-by.de www.example-by.de; \n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';\n add_header X-XSS-Protection \"1; mode=block\";\n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # add_header Permissions-Policy \"permissions here\";\n  root /var/www/html/website/;\n  index index.html;\n  location / {\n    try_files $uri $uri/ /index.html =404;\n  }\n}\n",
          :name-c1 "test-io-configmap",
          :name-c2 "example-io-configmap"}
         (th/map-diff (cut/generate-nginx-configmap {:unique-name "test.io",
                                                     :gitea-host "gitea.evilorg",
                                                     :gitea-repo "none",
                                                     :branchname "mablain",
                                                     :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})
                      (cut/generate-nginx-configmap {:unique-name "example.io",
                                                     :gitea-host "gitea.evilorg",
                                                     :gitea-repo "none",
                                                     :branchname "mablain",
                                                     :fqdns ["example.de" "www.example.de" "example-by.de" "www.example-by.de"]})))))

(deftest should-generate-nginx-deployment 
  (is (= {:apiVersion "apps/v1",
          :kind "Deployment",
          :metadata {:name "test-io-deployment"},
          :spec
          {:replicas 1,
           :selector {:matchLabels {:app "test-io-nginx"}},
           :template
           {:metadata {:labels {:app "test-io-nginx"}},
            :spec
            {:containers
             [{:name "test-io-nginx",
               :image "nginx:latest",
               :imagePullPolicy "IfNotPresent",
               :ports [{:containerPort 80}],
               :volumeMounts
               [{:mountPath "/etc/nginx", :readOnly true, :name "nginx-config-volume"}
                {:mountPath "/var/log/nginx", :name "log"}
                {:mountPath "/var/www/html/website", :name "website-content-volume", :readOnly true}
                {:mountPath "/etc/certs", :name "website-cert", :readOnly true}]}],
             :volumes
             [{:name "nginx-config-volume",
               :configMap
               {:name "test-io-configmap",
                :items
                [{:key "nginx.conf", :path "nginx.conf"}
                 {:key "website.conf", :path "conf.d/website.conf"}
                 {:key "mime.types", :path "mime.types"}]}}
              {:name "log", :emptyDir {}}
              {:name "website-content-volume", :persistentVolumeClaim {:claimName "test-io-content-volume"}}
              {:name "website-cert",
               :secret
               {:secretName "test-io-cert", :items [{:key "tls.crt", :path "tls.crt"} {:key "tls.key", :path "tls.key"}]}}]}}}}
         (cut/generate-nginx-deployment {:unique-name "test.io",
                                         :gitea-host "gitea.evilorg",
                                         :gitea-repo "none",
                                         :branchname "mablain",
                                         :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}))))

(deftest should-generate-nginx-service
  (is (= {:name-c1 "test-io-service",
          :name-c2 "test-org-service",
          :app-c1 "test-io-nginx",
          :app-c2 "test-org-nginx"}
         (th/map-diff (cut/generate-nginx-service {:unique-name "test.io",
                                                   :gitea-host "gitea.evilorg",
                                                   :gitea-repo "none",
                                                   :branchname "mablain",
                                                   :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})
                      (cut/generate-nginx-service {:unique-name "test.org",
                                                   :gitea-host "gitea.evilorg",
                                                   :gitea-repo "none",
                                                   :branchname "mablain",
                                                   :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))))

(deftest should-generate-website-build-cron
  (is (= {:apiVersion "batch/v1beta1",
          :kind "CronJob",
          :metadata {:name "test-io-build-cron", :labels {:app.kubernetes.part-of "website"}},
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
                 :name "test-io-build-app",
                 :imagePullPolicy "IfNotPresent",
                 :command ["/entrypoint.sh"],
                 :envFrom [{:secretRef {:name "test-io-secret"}}],
                 :volumeMounts [{:name "content-volume", :mountPath "/var/www/html/website"}]}],
               :volumes [{:name "content-volume", :persistentVolumeClaim {:claimName "test-io-content-volume"}}],
               :restartPolicy "OnFailure"}}}}}}
         (cut/generate-website-build-cron {:unique-name "test.io",
                                           :gitea-host "gitea.evilorg",
                                           :gitea-repo "none",
                                           :branchname "mablain",
                                           :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}))))

(deftest should-generate-website-build-deployment
  (is (= {:apiVersion "apps/v1",
          :kind "Deployment",
          :metadata {:name "test-io-build-deployment"},
          :spec
          {:replicas 0,
           :selector {:matchLabels {:app "test-io-builder"}},
           :strategy {:type "Recreate"},
           :template
           {:metadata
            {:labels {:app "test-io-builder", :app.kubernetes.io/name "test-io-builder", :app.kubernetes.io/part-of "website"}},
            :spec
            {:containers
             [{:image "domaindrivenarchitecture/c4k-website-build",
               :name "test-io-build-app",
               :imagePullPolicy "IfNotPresent",
               :command ["/entrypoint.sh"],
               :envFrom [{:secretRef {:name "test-io-secret"}}],
               :volumeMounts [{:name "content-volume", :mountPath "/var/www/html/website"}]}],
             :volumes [{:name "content-volume", :persistentVolumeClaim {:claimName "test-io-content-volume"}}]}}}}
         (cut/generate-website-build-deployment {:unique-name "test.io",
                                                 :gitea-host "gitea.evilorg",
                                                 :gitea-repo "none",
                                                 :branchname "mablain",
                                                 :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}))))

(deftest should-generate-website-build-secret
  (is (= {:name-c1 "test-io-secret",
          :name-c2 "test-org-secret",
          :AUTHTOKEN-c1 (b64/encode "token1"),
          :AUTHTOKEN-c2 (b64/encode "token2"),
          :GITREPOURL-c1 (b64/encode "https://gitlab.org/api/v1/repos/dumpty/websitebau/archive/testname.zip"),
          :GITREPOURL-c2 (b64/encode "https://github.com/api/v1/repos/humpty/websitedachs/archive/testname.zip")}
         (th/map-diff (cut/generate-website-build-secret {:unique-name "test.io",
                                                          :authtoken "token1",
                                                          :gitea-host "gitlab.org",
                                                          :gitea-repo "websitebau",
                                                          :username "dumpty",
                                                          :branchname "testname"})
                      (cut/generate-website-build-secret {:unique-name "test.org",
                                                          :authtoken "token2",
                                                          :gitea-host "github.com",
                                                          :gitea-repo "websitedachs",
                                                          :username "humpty",
                                                          :branchname "testname"})))))

(deftest should-generate-website-content-volume
  (is (= {:name-c1 "test-io-content-volume",
          :name-c2 "test-org-content-volume",
          :app-c1 "test-io-nginx",
          :app-c2 "test-org-nginx"}
         (th/map-diff (cut/generate-website-content-volume {:unique-name "test.io",
                                                            :gitea-host "gitea.evilorg",
                                                            :gitea-repo "none",
                                                            :branchname "mablain",
                                                            :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})
                      (cut/generate-website-content-volume {:unique-name "test.org",
                                                            :gitea-host "gitea.evilorg",
                                                            :gitea-repo "none",
                                                            :branchname "mablain",
                                                            :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))))
