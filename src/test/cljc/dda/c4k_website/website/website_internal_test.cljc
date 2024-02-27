(ns dda.c4k-website.website.website-internal-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-website.website.website-internal :as cut]))

(st/instrument `cut/replace-dots-by-minus)
(st/instrument `cut/generate-gitrepourl)
(st/instrument `cut/generate-gitcommiturl)
(st/instrument `cut/replace-all-matching-substrings-beginning-with)
(st/instrument `cut/generate-redirects)
(st/instrument `cut/generate-nginx-configmap)
(st/instrument `cut/generate-website-build-secret)
(st/instrument `cut/generate-website-content-volume)
(st/instrument `cut/generate-hashfile-volume)
(st/instrument `cut/generate-website-build-cron)
(st/instrument `cut/generate-nginx-service)

(deftest should-generate-redirects
  (is (= "rewrite ^/products.html\\$ /offer.html permanent;\n  rewrite ^/one-more\\$ /redirect permanent;"
         (cut/generate-redirects {:issuer "staging"
                                  :build-cpu-request "500m"
                                  :build-cpu-limit "1700m"
                                  :build-memory-request "256Mi"
                                  :build-memory-limit "512Mi"
                                  :volume-size "3"
                                  :unique-name "test.io",
                                  :redirects [["/products.html", "/offer.html"]
                                              ["/one-more", "/redirect"]]
                                  :forgejo-host "gitea.evilorg",
                                  :forgejo-repo "none",
                                  :branchname "mablain",
                                  :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}
                                 2)))
  (is (= ""
         (cut/generate-redirects {:issuer "staging"
                                  :build-cpu-request "500m"
                                  :build-cpu-limit "1700m"
                                  :build-memory-request "256Mi"
                                  :build-memory-limit "512Mi"
                                  :volume-size "3"
                                  :unique-name "test.io",
                                  :redirects []
                                  :forgejo-host "gitea.evilorg",
                                  :forgejo-repo "none",
                                  :branchname "mablain",
                                  :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}
                                 0))))


(deftest should-generate-resource-requests
  (is (= {:requests {:cpu "1500m", :memory "512Mi"}, :limits {:cpu "3000m", :memory "1024Mi"}}
         (-> (cut/generate-nginx-deployment {:forgejo-host "gitlab.de",
                                             :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                             :forgejo-repo "repo",
                                             :issuer "staging",
                                             :branchname "main",
                                             :unique-name "test.io",
                                             :redirects [],
                                             :build-cpu-request "1500m"
                                             :build-cpu-limit "3000m"
                                             :build-memory-request "512Mi"
                                             :build-memory-limit "1024Mi"
                                             :volume-size 3})
             :spec :template :spec :initContainers first :resources)))
  (is (= "test-io"
         (-> (cut/generate-nginx-deployment {:forgejo-host "gitlab.de",
                                             :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                             :forgejo-repo "repo",
                                             :issuer "staging",
                                             :branchname "main",
                                             :unique-name "test.io",
                                             :redirects [],
                                             :build-cpu-request "1500m"
                                             :build-cpu-limit "3000m"
                                             :build-memory-request "512Mi"
                                             :build-memory-limit "1024Mi"
                                             :volume-size 3})
             :metadata :namespace))))

(deftest should-generate-nginx-configmap-website
  (is (= "server {\n  listen 80 default_server;\n  listen [::]:80 default_server;\n  server_name test.de www.test.de test-it.de www.test-it.de;\n  add_header Strict-Transport-Security 'max-age=31536000; includeSubDomains; preload';      \n  add_header X-Frame-Options \"SAMEORIGIN\";\n  add_header X-Content-Type-Options nosniff;\n  add_header Referrer-Policy \"strict-origin\";\n  # add_header Permissions-Policy \"permissions here\";\n  root /var/www/html/website/;\n  index index.html;\n  location / {\n    try_files $uri $uri/ /index.html =404;\n  }\n  # redirects\n  rewrite ^/products.html$ /offer.html permanent;\n  rewrite ^/one-more$ /redirect permanent;\n}\n"
         (:website.conf (:data (cut/generate-nginx-configmap {:issuer "staging"
                                                              :build-cpu-request "500m"
                                                              :build-cpu-limit "1700m"
                                                              :build-memory-request "256Mi"
                                                              :build-memory-limit "512Mi"
                                                              :volume-size "3"
                                                              :unique-name "test.io",
                                                              :redirects [["/products.html", "/offer.html"]
                                                                          ["/one-more", "/redirect"]]
                                                              :forgejo-host "gitea.evilorg",
                                                              :forgejo-repo "none",
                                                              :branchname "mablain",
                                                              :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))))
  (is (= "types {\n  text/html                             html htm shtml;\n  text/css                              css;\n  text/xml                              xml rss;\n  image/gif                             gif;\n  image/jpeg                            jpeg jpg;\n  application/x-javascript              js;\n  text/plain                            txt;\n  text/x-component                      htc;\n  text/mathml                           mml;\n  image/svg+xml                         svg svgz;\n  image/png                             png;\n  image/x-icon                          ico;\n  image/x-jng                           jng;\n  image/vnd.wap.wbmp                    wbmp;\n  application/java-archive              jar war ear;\n  application/mac-binhex40              hqx;\n  application/pdf                       pdf;\n  application/x-cocoa                   cco;\n  application/x-java-archive-diff       jardiff;\n  application/x-java-jnlp-file          jnlp;\n  application/x-makeself                run;\n  application/x-perl                    pl pm;\n  application/x-pilot                   prc pdb;\n  application/x-rar-compressed          rar;\n  application/x-redhat-package-manager  rpm;\n  application/x-sea                     sea;\n  application/x-shockwave-flash         swf;\n  application/x-stuffit                 sit;\n  application/x-tcl                     tcl tk;\n  application/x-x509-ca-cert            der pem crt;\n  application/x-xpinstall               xpi;\n  application/zip                       zip;\n  application/octet-stream              deb;\n  application/octet-stream              bin exe dll;\n  application/octet-stream              dmg;\n  application/octet-stream              eot;\n  application/octet-stream              iso img;\n  application/octet-stream              msi msp msm;\n  audio/mpeg                            mp3;\n  audio/x-realaudio                     ra;\n  video/mpeg                            mpeg mpg;\n  video/quicktime                       mov;\n  video/x-flv                           flv;\n  video/x-msvideo                       avi;\n  video/x-ms-wmv                        wmv;\n  video/x-ms-asf                        asx asf;\n  video/x-mng                           mng;\n}\n"
         (:mime.types (:data (cut/generate-nginx-configmap {:issuer "staging"
                                                            :build-cpu-request "500m"
                                                            :build-cpu-limit "1700m"
                                                            :build-memory-request "256Mi"
                                                            :build-memory-limit "512Mi"
                                                            :volume-size "3"
                                                            :unique-name "test.io",
                                                            :redirects [],
                                                            :forgejo-host "gitea.evilorg",
                                                            :forgejo-repo "none",
                                                            :branchname "mablain",
                                                            :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))))
  (is (= "user nginx;\nworker_processes 3;\nerror_log stdout info;\npid /var/log/nginx/nginx.pid;\nworker_rlimit_nofile 8192;\nevents {\n  worker_connections 4096;\n}\nhttp {\n  include /etc/nginx/mime.types;\n  default_type application/octet-stream;\n  log_format   main '$remote_addr - $remote_user [$time_local] $status'\n  '\"$request\" $body_bytes_sent \"$http_referer\"'\n  '\"$http_user_agent\" \"$http_x_forwarded_for\"';\n  access_log stdout main;\n  sendfile on;\n  tcp_nopush on;\n  keepalive_timeout 65;\n  server_names_hash_bucket_size 128;\n  include /etc/nginx/conf.d/website.conf;\n}\n"
         (:nginx.conf (:data (cut/generate-nginx-configmap {:issuer "staging"
                                                            :build-cpu-request "500m"
                                                            :build-cpu-limit "1700m"
                                                            :build-memory-request "256Mi"
                                                            :build-memory-limit "512Mi"
                                                            :volume-size "3"
                                                            :unique-name "test.io",
                                                            :redirects [],
                                                            :forgejo-host "gitea.evilorg",
                                                            :forgejo-repo "none",
                                                            :branchname "mablain",
                                                            :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))))
  (is (= {:apiVersion "v1",
          :kind "ConfigMap",
          :metadata {:labels {:app.kubernetes.part-of "test-io-website"},
                     :namespace "test-io",
                     :name "etc-nginx"}}
         (dissoc (cut/generate-nginx-configmap {:issuer "staging"
                                                :build-cpu-request "500m"
                                                :build-cpu-limit "1700m"
                                                :build-memory-request "256Mi"
                                                :build-memory-limit "512Mi"
                                                :volume-size "3"
                                                :unique-name "test.io",
                                                :redirects [],
                                                :forgejo-host "gitea.evilorg",
                                                :forgejo-repo "none",
                                                :branchname "mablain",
                                                :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}) :data))))

(deftest should-generate-nginx-service
  (is (= {:kind "Service",
          :apiVersion "v1",
          :metadata
          {:name "test-io",
           :namespace "test-io",
           :labels {:app "test-io", :app.kubernetes.part-of "test-io-website"}},
          :spec
          {:selector {:app "nginx"}, :ports [{:name "nginx-http", :port 80}]}}
         (cut/generate-nginx-service {:issuer "staging"
                                      :build-cpu-request "500m"
                                      :build-cpu-limit "1700m"
                                      :build-memory-request "256Mi"
                                      :build-memory-limit "512Mi"
                                      :volume-size "3"
                                      :unique-name "test.io",
                                      :redirects [],
                                      :forgejo-host "gitea.evilorg",
                                      :forgejo-repo "none",
                                      :branchname "mablain",
                                      :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})
)))


(deftest should-generate-website-build-cron
  (is (= {:apiVersion "batch/v1",
          :kind "CronJob",
          :metadata {:name "build-cron",
                     :namespace "test-io",
                     :labels {:app.kubernetes.part-of "test-io-website"}},
          :spec
          {:schedule "0/7 * * * *",
           :successfulJobsHistoryLimit 1,
           :failedJobsHistoryLimit 1,
           :jobTemplate
           {:spec
            {:template
             {:metadata
              {:namespace "test-io",
               :labels
               {:app "build-cron", :app.kubernetes.part-of "test-io-website"}}
              :spec 
              {:containers
               [{:image "domaindrivenarchitecture/c4k-website-build",
                 :name "build-cron-container",
                 :imagePullPolicy "IfNotPresent",
                 :resources {:requests {:cpu "500m", :memory "256Mi"}, :limits {:cpu "1700m", :memory "512Mi"}},
                 :command ["/entrypoint.sh"],
                 :envFrom [{:secretRef {:name "build-secret"}}],
                 :volumeMounts [{:name "content-volume", :mountPath "/var/www/html/website"}
                                {:name "hash-state-volume", :mountPath "/var/hashfile.d"}]}],
               :volumes [{:name "content-volume", :persistentVolumeClaim {:claimName "content-volume"}}
                         {:name "hash-state-volume", :persistentVolumeClaim {:claimName "hash-state-volume"}}],
               :restartPolicy "OnFailure"}}}}}}
         (cut/generate-website-build-cron {:issuer "staging"
                                           :build-cpu-request "500m"
                                           :build-cpu-limit "1700m"
                                           :build-memory-request "256Mi"
                                           :build-memory-limit "512Mi"
                                           :volume-size "3"
                                           :forgejo-host "gitlab.de",
                                           :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                           :forgejo-repo "repo",
                                           :branchname "main",
                                           :unique-name "test.io",
                                           :redirects [],}))))

(deftest should-generate-website-build-secret
  (is (= {:apiVersion "v1",
          :kind "Secret",
          :metadata {:name "build-secret", 
                     :namespace "test-io",
                     :labels {:app.kubernetes.part-of "test-io-website"}},
          :data
          {:AUTHTOKEN "YWJlZGpnYmFzZG9kag==",
           :GITREPOURL "aHR0cHM6Ly9naXRsYWIuZGUvYXBpL3YxL3JlcG9zL3NvbWV1c2VyL3JlcG8vYXJjaGl2ZS9tYWluLnppcA==",
           :GITCOMMITURL "aHR0cHM6Ly9naXRsYWIuZGUvYXBpL3YxL3JlcG9zL3NvbWV1c2VyL3JlcG8vZ2l0L2NvbW1pdHMvSEVBRA=="}}
         (cut/generate-website-build-secret {:fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                             :forgejo-repo "repo",
                                             :issuer "staging",
                                             :branchname "main",
                                             :unique-name "test.io",
                                             :redirects [],
                                             :forgejo-host "gitlab.de"
                                             :build-cpu-request "500m"
                                             :build-cpu-limit "1700m"
                                             :build-memory-request "256Mi"
                                             :build-memory-limit "512Mi"
                                             :volume-size "3"}
                                            {:unique-name "test.io",
                                             :authtoken "abedjgbasdodj",
                                             :username "someuser"}))))

(deftest should-generate-website-content-volume
  (is (= {:apiVersion "v1",
          :kind "PersistentVolumeClaim",
          :metadata
          {:name "content-volume",
           :namespace "test-io",
           :labels {:app.kubernetes.part-of "test-io-website"}},
          :spec
          {:storageClassName "local-path",
           :accessModes ["ReadWriteOnce"],
           :resources {:requests {:storage "3Gi"}}}}
         (cut/generate-website-content-volume {:issuer "staging"
                                               :build-cpu-request "500m"
                                               :build-cpu-limit "1700m"
                                               :build-memory-request "256Mi"
                                               :build-memory-limit "512Mi"
                                               :volume-size "3"
                                               :unique-name "test.io",
                                               :redirects [],
                                               :forgejo-host "gitea.evilorg",
                                               :forgejo-repo "none",
                                               :branchname "mablain",
                                               :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}))))


(deftest should-generate-hashfile-volume
  (is (= {:apiVersion "v1",
          :kind "PersistentVolumeClaim",
          :metadata
          {:name "hash-state-volume",
           :namespace "test-io",
           :labels {:app.kubernetes.part-of "test-io-website"}},
          :spec {:storageClassName "local-path", 
                 :accessModes ["ReadWriteOnce"], 
                 :resources {:requests {:storage "16Mi"}}}}
         (cut/generate-hashfile-volume {:issuer "staging"
                                        :build-cpu-request "500m"
                                        :build-cpu-limit "1700m"
                                        :build-memory-request "256Mi"
                                        :build-memory-limit "512Mi"
                                        :volume-size "3"
                                        :unique-name "test.io",
                                        :redirects [],
                                        :forgejo-host "gitea.evilorg",
                                        :forgejo-repo "none",
                                        :branchname "mablain",
                                        :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}))))