(ns dda.c4k-website.core-test
  (:require
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-website.core :as cut]))

(st/instrument `cut/sort-config)
(st/instrument `cut/flattened-and-reduced-config)
(st/instrument `cut/flatten-and-reduce-auth)
(st/instrument `cut/generate-ingress)
(st/instrument `cut/generate)

#?(:cljs
   (defmethod yaml/load-resource :website-test [resource-name]
     (case resource-name
       "website-test/valid-auth.yaml"   (rc/inline "website-test/valid-auth.yaml")
       "website-test/valid-config.yaml" (rc/inline "website-test/valid-config.yaml")
       (throw (js/Error. "Undefined Resource!")))))

(deftest validate-valid-resources
  (is (s/valid? cut/config? (yaml/load-as-edn "website-test/valid-config.yaml")))
  (is (s/valid? cut/auth? (yaml/load-as-edn "website-test/valid-auth.yaml"))))

(def websites1
  {:websiteconfigs
   [{:unique-name "example.io"
     :fqdns ["example.org", "www.example.com"]
     :forgejo-host "finegitehost.net"
     :repo-owner "someuser"
     :repo-name "repo"
     :branchname "main"}
    {:unique-name "test.io"
     :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"]
     :forgejo-host "gitlab.de"
     :repo-owner "someuser"
     :repo-name "repo"
     :branchname "main"}]})

(def websites2
  {:websiteconfigs
   [{:unique-name "test.io"
     :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"]
     :forgejo-host "gitlab.de"
     :repo-owner "someuser"
     :repo-name "repo"
     :branchname "main"}
    {:unique-name "example.io"
     :fqdns ["example.org", "www.example.com"]
     :forgejo-host "finegitehost.net"
     :repo-owner "someuser"
     :repo-name "repo"
     :branchname "main"}]})

(def auth1
  {:websiteauths
   [{:unique-name "example.io"
     :authtoken "abedjgbasdodj"}
    {:unique-name "test.io"
     :authtoken "abedjgbasdodj"}]})

(def auth2
  {:websiteauths
   [{:unique-name "test.io"
     :authtoken "abedjgbasdodj"}
    {:unique-name "example.io"
     :authtoken "abedjgbasdodj"}]})

(def flattened-and-reduced-config
  {:unique-name "example.io",
   :fqdns ["example.org" "www.example.com"],
   :forgejo-host "finegitehost.net",
   :repo-owner "someuser",
   :repo-name "repo",
   :branchname "main"})

(def flattened-and-reduced-auth
  {:unique-name "example.io",
   :authtoken "abedjgbasdodj"})

(deftest sorts-config
  (is (= {:issuer "staging",
          :websiteconfigs
          [{:unique-name "example.io",
            :fqdns ["example.org" "www.example.com"],
            :forgejo-host "finegitehost.net",
            :repo-owner "someuser",
            :repo-name "repo",
            :branchname "main"},
           {:unique-name "test.io",
            :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
            :forgejo-host "gitlab.de",
            :repo-owner "someuser",
            :repo-name "repo",
            :branchname "main",
            :sha256sum-output "123456789ab123cd345de script-file-name.sh"}],
          :mon-cfg {:grafana-cloud-url "url-for-your-prom-remote-write-endpoint", :cluster-name "jitsi", :cluster-stage "test"}}
         (cut/sort-config
          {:issuer "staging",
           :websiteconfigs
           [{:unique-name "test.io",
             :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
             :forgejo-host "gitlab.de",
             :repo-owner "someuser",
             :repo-name "repo",
             :branchname "main",
             :sha256sum-output "123456789ab123cd345de script-file-name.sh"}
            {:unique-name "example.io",
             :fqdns ["example.org" "www.example.com"],
             :forgejo-host "finegitehost.net",
             :repo-owner "someuser",
             :repo-name "repo",
             :branchname "main"}],
           :mon-cfg {:grafana-cloud-url "url-for-your-prom-remote-write-endpoint", :cluster-name "jitsi", :cluster-stage "test"}}))))

(deftest test-flatten-and-reduce-config
  (is (=
       flattened-and-reduced-config
       (cut/flatten-and-reduce-config (cut/sort-config websites1))))
  (is (=
       flattened-and-reduced-config
       (cut/flatten-and-reduce-config (cut/sort-config websites2)))))

(deftest test-flatten-and-reduce-auth
  (is (= flattened-and-reduced-auth
         (cut/flatten-and-reduce-auth (cut/sort-auth auth1))))
  (is (= flattened-and-reduced-auth
         (cut/flatten-and-reduce-auth (cut/sort-auth auth2)))))

(deftest test-generate
  (is (= 24
         (count (cut/generate
                 (yaml/load-as-edn "website-test/valid-config.yaml")
                 (yaml/load-as-edn "website-test/valid-auth.yaml"))))))

(deftest should-generate-ingress
  (is (= [{:host "test.de",
           :http
           {:paths
            [{:pathType "Prefix",
              :path "/",
              :backend {:service {:name "test-io", :port {:number 80}}}}]}}
          {:host "test.org",
           :http
           {:paths
            [{:pathType "Prefix",
              :path "/",
              :backend {:service {:name "test-io", :port {:number 80}}}}]}}
          {:host "www.test.de",
           :http
           {:paths
            [{:pathType "Prefix",
              :path "/",
              :backend {:service {:name "test-io", :port {:number 80}}}}]}}
          {:host "www.test.org",
           :http
           {:paths
            [{:pathType "Prefix",
              :path "/",
              :backend {:service {:name "test-io", :port {:number 80}}}}]}}]
         (get-in
          (cut/generate-ingress {:unique-name "test.io",
                                 :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                 :forgejo-host "gitlab.de",
                                 :repo-owner "someuser",
                                 :repo-name "repo",
                                 :sha256sum-output "123456789ab123cd345de script-file-name.sh",
                                 :issuer "staging",
                                 :branchname "main",
                                 :build-cpu-request "500m"
                                 :build-cpu-limit "1700m"
                                 :build-memory-request "256Mi"
                                 :build-memory-limit "512Mi"
                                 :volume-size "3"
                                 :redirects []})
          [2 :spec :rules])))
  (is (= "test-io"
         (get-in
          (cut/generate-ingress {:unique-name "test.io",
                                 :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                 :forgejo-host "gitlab.de",
                                 :repo-owner "someuser",
                                 :repo-name "repo",
                                 :sha256sum-output "123456789ab123cd345de script-file-name.sh",
                                 :issuer "staging",
                                 :branchname "main",
                                 :build-cpu-request "500m"
                                 :build-cpu-limit "1700m"
                                 :build-memory-request "256Mi"
                                 :build-memory-limit "512Mi"
                                 :volume-size "3"
                                 :redirects []})
          [2 :metadata :namespace]))))