(ns dda.c4k-website.core-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.test-helper :as th]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-website.core :as cut]
   [clojure.spec.alpha :as s]))

(def websites
  {:websites
   [{:unique-name "example.io"
     :fqdns ["example.org", "www.example.com"]
     :gitea-host "finegitehost.net"
     :gitea-repo "repo"
     :branchname "main"}
    {:unique-name "test.io"
     :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"]
     :gitea-host "gitlab.de"
     :gitea-repo "repo"
     :branchname "main"}]})

(def auth1
  {:auth
   [{:unique-name "example.io"
     :username "someuser"
     :authtoken "abedjgbasdodj"}
    {:unique-name "test.io"
     :username "someuser"
     :authtoken "abedjgbasdodj"}]})

(def auth2
  {:auth
   [{:unique-name "test.io"
     :username "someuser"
     :authtoken "abedjgbasdodj"}
    {:unique-name "example.io"
     :username "someuser"
     :authtoken "abedjgbasdodj"}]})

(def flattened-and-reduced-config
  {:unique-name "example.io",
   :fqdns ["example.org" "www.example.com"],
   :gitea-host "finegitehost.net",
   :gitea-repo "repo",
   :branchname "main",
   :username "someuser",
   :authtoken "abedjgbasdodj"})

(deftest test-flatten-and-reduce-config
  (is (=
       (cut/flatten-and-reduce-config (merge websites auth1))
       flattened-and-reduced-config))
  (is (=
       (cut/flatten-and-reduce-config (merge websites auth2))
       flattened-and-reduced-config)))