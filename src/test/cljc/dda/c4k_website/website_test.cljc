(ns dda.c4k-website.website-test
  (:require
   [clojure.string :as str]
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-website.website :as cut]))

(st/instrument `cut/replace-dots-by-minus)
(st/instrument `cut/generate-gitrepourl)
(st/instrument `cut/generate-gitcommiturl)
(st/instrument `cut/replace-all-matching-prefixes)
(st/instrument `cut/generate-redirects)
(st/instrument `cut/generate-nginx-configmap)
(st/instrument `cut/generate-build-secret)
(st/instrument `cut/generate-content-pvc)
(st/instrument `cut/generate-hash-state-pvc)
(st/instrument `cut/generate-build-cron)
(st/instrument `cut/generate-nginx-service)


(deftest should-generate-gitrepourl
  (is (= "https://mygit.de/api/v1/repos/someuser/repo/archive/main.zip"
         (cut/generate-gitrepourl "mygit.de" "someuser" "repo" "main"))))

(deftest should-generate-gitcommiturl
  (is (= "https://mygit.de/api/v1/repos/someuser/repo/git/commits/HEAD"
         (cut/generate-gitcommiturl "mygit.de" "someuser" "repo"))))

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
                                  :repo-user "someuser",
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
                                  :repo-user "someuser",
                                  :forgejo-repo "none",
                                  :branchname "mablain",
                                  :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}
                                 0))))


(deftest should-generate-resource-requests
  (is (= {:requests {:cpu "1500m", :memory "512Mi"}, :limits {:cpu "3000m", :memory "1024Mi"}}
         (-> (cut/generate-nginx-deployment {:forgejo-host "gitlab.de",
                                             :repo-user "someuser",
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
                                             :repo-user "someuser",
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
  (is (str/includes?
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
                                                            :repo-user "someuser",
                                                            :forgejo-repo "none",
                                                            :branchname "mablain",
                                                            :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))
       " /offer.html permanent;\n"))
  (is (str/includes?
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
                                                            :repo-user "someuser",
                                                            :forgejo-repo "none",
                                                            :branchname "mablain",
                                                            :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))
       " /redirect permanent;\n"))
  (is (str/includes?
       (:website.conf (:data (cut/generate-nginx-configmap {:issuer "staging"
                                                            :build-cpu-request "500m"
                                                            :build-cpu-limit "1700m"
                                                            :build-memory-request "256Mi"
                                                            :build-memory-limit "512Mi"
                                                            :volume-size "3"
                                                            :unique-name "test.io",
                                                            :redirects [],
                                                            :forgejo-host "gitea.evilorg",
                                                            :repo-user "someuser",
                                                            :forgejo-repo "none",
                                                            :branchname "mablain",
                                                            :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))
       "server_name test.de www.test.de test-it.de www.test-it.de;"))
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
                                                :repo-user "someuser",
                                                :forgejo-repo "none",
                                                :branchname "mablain",
                                                :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})
                 :data))))

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
                                      :repo-user "someuser",
                                      :forgejo-repo "none",
                                      :branchname "mablain",
                                      :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}))))


(deftest should-generate-build-cron
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
         (cut/generate-build-cron {:issuer "staging"
                                   :build-cpu-request "500m"
                                   :build-cpu-limit "1700m"
                                   :build-memory-request "256Mi"
                                   :build-memory-limit "512Mi"
                                   :volume-size "3"
                                   :forgejo-host "gitlab.de",
                                   :repo-user "someuser",
                                   :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                   :forgejo-repo "repo",
                                   :branchname "main",
                                   :unique-name "test.io",
                                   :redirects []}))))

(deftest should-generate-build-configmap
  (is (= {:apiVersion "v1",
          :kind "ConfigMap",
          :metadata {:name "build-configmap",
                     :namespace "test-io",
                     :labels {:app.kubernetes.part-of "test-io-website"}},
          :data
          {:GITREPOURL "https://mygit.de/api/v1/repos/someuser/repo/archive/main.zip"
           :GITCOMMITURL "https://mygit.de/api/v1/repos/someuser/repo/git/commits/HEAD"}}
         (cut/generate-build-configmap {:issuer "staging"
                                        :build-cpu-request "500m"
                                        :build-cpu-limit "1700m"
                                        :build-memory-request "256Mi"
                                        :build-memory-limit "512Mi"
                                        :volume-size "3"
                                        :forgejo-host "mygit.de",
                                        :repo-user "someuser",
                                        :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                        :forgejo-repo "repo",
                                        :branchname "main",
                                        :unique-name "test.io",
                                        :redirects []}))))

(deftest should-generate-build-secret
  (is (= {:apiVersion "v1",
          :kind "Secret",
          :metadata {:name "build-secret",
                     :namespace "test-io",
                     :labels {:app.kubernetes.part-of "test-io-website"}},
          :data
          {:AUTHTOKEN "YWJlZGpnYmFzZG9kag=="}}
         (cut/generate-build-secret {:unique-name "test.io",
                                     :authtoken "abedjgbasdodj"}))))

(deftest should-generate-content-pvc
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
         (cut/generate-content-pvc {:issuer "staging"
                                    :build-cpu-request "500m"
                                    :build-cpu-limit "1700m"
                                    :build-memory-request "256Mi"
                                    :build-memory-limit "512Mi"
                                    :volume-size "3"
                                    :unique-name "test.io",
                                    :redirects [],
                                    :forgejo-host "gitea.evilorg",
                                    :repo-user "someuser",
                                    :forgejo-repo "none",
                                    :branchname "mablain",
                                    :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}))))


(deftest should-generate-hash-state-pvc
  (is (= {:apiVersion "v1",
          :kind "PersistentVolumeClaim",
          :metadata
          {:name "hash-state-volume",
           :namespace "test-io",
           :labels {:app.kubernetes.part-of "test-io-website"}},
          :spec {:storageClassName "local-path",
                 :accessModes ["ReadWriteOnce"],
                 :resources {:requests {:storage "16Mi"}}}}
         (cut/generate-hash-state-pvc {:issuer "staging"
                                       :build-cpu-request "500m"
                                       :build-cpu-limit "1700m"
                                       :build-memory-request "256Mi"
                                       :build-memory-limit "512Mi"
                                       :volume-size "3"
                                       :unique-name "test.io",
                                       :redirects [],
                                       :forgejo-host "gitea.evilorg",
                                       :repo-user "someuser",
                                       :forgejo-repo "none",
                                       :branchname "mablain",
                                       :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}))))