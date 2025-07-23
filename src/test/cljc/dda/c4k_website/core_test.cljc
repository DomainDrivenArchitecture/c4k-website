(ns dda.c4k-website.core-test
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-website.core :as cut]))

(st/instrument `cut/mapize-config)
(st/instrument `cut/generate)

(deftest validate-valid-resources
  (is (s/valid? ::cut/config (yaml/load-as-edn "website-test/valid-config.yaml")))
  (is (s/valid? ::cut/auth (yaml/load-as-edn "website-test/valid-auth.yaml"))))

(def config
  {:issuer "prod"
   :websiteconfigs
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


(def auth
  {:websiteauths
   [{:unique-name "example.io"
     :authtoken "abedjgbasdodj"}
    {:unique-name "test.io"
     :authtoken "abedjgbasdodj"}]
   :mon-auth {:grafana-cloud-user "123"
              :grafana-cloud-password "pwd"}})

(deftest mapize-config
  (is (=
       {"example.io"
        {:issuer "prod"
         :unique-name "example.io",
         :fqdns ["example.org" "www.example.com"],
         :forgejo-host "finegitehost.net",
         :repo-owner "someuser",
         :repo-name "repo",
         :branchname "main"
         :redirects [],
         :average-rate 20,
         :build-cpu-limit "1700m",
         :burst-rate 40,
         :build-memory-limit "1024Mi",
         :namespace "default",
         :volume-size "3"}
        "test.io"
        {:issuer "prod"
         :unique-name "test.io",
         :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
         :forgejo-host "gitlab.de",
         :repo-owner "someuser",
         :repo-name "repo",
         :branchname "main"
         :redirects [],
         :average-rate 20,
         :build-cpu-limit "1700m",
         :burst-rate 40,
         :build-memory-limit "1024Mi",
         :namespace "default",
         :volume-size "3"}}
       (cut/mapize-config config))))

(deftest mapize-auth
  (is (=
       {"example.io"
        {:unique-name "example.io"
         :authtoken "abedjgbasdodj"}
        "test.io"
        {:unique-name "test.io"
         :authtoken "abedjgbasdodj"}}
       (cut/mapize-auth auth))))

(deftest test-config-objects
  (is (= 22
         (count (cut/config-objects [] config)))))

(deftest test-auth-objects
  (is (= 2
         (count (cut/auth-objects [] config auth)))))
