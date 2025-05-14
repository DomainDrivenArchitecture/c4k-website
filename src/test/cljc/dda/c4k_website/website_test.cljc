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

(def test-config {:issuer "staging"
                  :build-cpu-limit "1700m"
                  :build-memory-limit "512Mi"
                  :volume-size "3"
                  :unique-name "test.io",
                  :redirects [["/products.html", "/offer.html"]
                              ["/one-more", "/redirect"]]
                  :forgejo-host "mygit.de",
                  :repo-owner "someuser",
                  :repo-name "repo",
                  :branchname "main",
                  :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})

(deftest should-generate-gitrepourl
  (is (= "https://mygit.de/api/v1/repos/someuser/repo/archive/main.zip"
         (cut/generate-gitrepourl "mygit.de" "someuser" "repo" "main"))))

(deftest should-generate-gitcommiturl
  (is (= "https://mygit.de/api/v1/repos/someuser/repo/git/commits/HEAD"
         (cut/generate-gitcommiturl "mygit.de" "someuser" "repo"))))

(deftest should-generate-redirects
  (is (= "rewrite ^/products.html\\$ /offer.html permanent;\n  rewrite ^/one-more\\$ /redirect permanent;"
         (cut/generate-redirects test-config 2)))
  (is (= ""
         (cut/generate-redirects (merge test-config {:redirects []})
                                 0))))


(deftest should-generate-resource-requests
  (is (= {:limits {:cpu "3000m", :memory "1024Mi"}}
         (-> (cut/generate-nginx-deployment (merge test-config {:build-cpu-limit "3000m"
                                                                :build-memory-limit "1024Mi"}))
             :spec :template :spec :initContainers first :resources)))
  (is (= "test-io"
         (-> (cut/generate-nginx-deployment (merge test-config {:build-cpu-limit "3000m"
                                                                :build-memory-limit "1024Mi"}))
             :metadata :namespace))))

(deftest should-generate-nginx-configmap-website
  (is (str/includes?
       (get-in
        (cut/generate-nginx-configmap test-config)
        [:data :website.conf])
       "/offer.html permanent;\n"))
  (is (str/includes?
       (get-in
        (cut/generate-nginx-configmap test-config)
        [:data :website.conf])
       "server_name test.de www.test.de test-it.de www.test-it.de;"))
  (is (= {:labels {:app.kubernetes.part-of "test-io-website"},
          :namespace "test-io",
          :name "etc-nginx"}
         (get-in
          (cut/generate-nginx-configmap test-config)
          [:metadata]))))

(deftest should-generate-build-configmap
  (is (= {:GITHOST "mygit.de"
          :GITREPOURL "https://mygit.de/api/v1/repos/someuser/repo/archive/main.zip"
          :GITCOMMITURL "https://mygit.de/api/v1/repos/someuser/repo/git/commits/HEAD"}
         (get-in (cut/generate-build-configmap test-config)
                 [:data]))))

(deftest should-generate-build-secret
  (is (= {:apiVersion "v1",
          :kind "Secret",
          :metadata {:name "build-secret",
                     :namespace "test-io",
                     :labels {:app.kubernetes.part-of "test-io-website"}},
          :data
          {:AUTHTOKEN "YWJlZGpnYmFzZG9kag=="}}
         (cut/generate-build-secret test-config
                                    {:authtoken "abedjgbasdodj"}))))

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
         (cut/generate-content-pvc test-config))))

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
         (cut/generate-hash-state-pvc test-config))))

(deftest should-generate-nginx-service
  (is (= {:kind "Service",
          :apiVersion "v1",
          :metadata
          {:name "test-io",
           :namespace "test-io",
           :labels {:app "test-io", :app.kubernetes.part-of "test-io-website"}},
          :spec
          {:selector {:app "nginx"}, :ports [{:name "nginx-http", :port 80}]}}
         (cut/generate-nginx-service test-config))))

(deftest should-generate-nginx-deployment
  (is (= {:name "nginx",
          :namespace "test-io",
          :labels {:app.kubernetes.part-of "test-io-website"}}
         (get-in (cut/generate-nginx-deployment test-config)
                 [:metadata])))
  (is (= {:limits {:cpu "1700m", :memory "512Mi"}}
         (get-in (cut/generate-nginx-deployment test-config)
                 [:spec :template :spec :initContainers 0 :resources]))))

(deftest should-generate-build-cron
  (is (= {:name "build-cron",
          :namespace "test-io",
          :labels {:app.kubernetes.part-of "test-io-website"}},
         (get-in (cut/generate-build-cron test-config)
                 [:metadata])))
  (is (= {:limits {:cpu "1700m", :memory "512Mi"}}
         (get-in (cut/generate-build-cron test-config)
                 [:spec :jobTemplate :spec :template :spec :containers 0 :resources]))))
