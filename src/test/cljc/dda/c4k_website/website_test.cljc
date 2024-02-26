(ns dda.c4k-website.website-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.test-helper :as th]
   [dda.c4k-website.website :as cut]))

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
          (cut/generate-ingress {:forgejo-host "gitlab.de",
                                 :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                 :forgejo-repo "repo",
                                 :sha256sum-output "123456789ab123cd345de script-file-name.sh",
                                 :issuer "staging",
                                 :branchname "main",
                                 :unique-name "test.io"})
          [2 :spec :rules])))
  (is (= "test-io"
         (get-in
          (cut/generate-ingress {:forgejo-host "gitlab.de",
                                 :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                 :forgejo-repo "repo",
                                 :sha256sum-output "123456789ab123cd345de script-file-name.sh",
                                 :issuer "staging",
                                 :branchname "main",
                                 :unique-name "test.io"})
          [2 :metadata :namespace]))))