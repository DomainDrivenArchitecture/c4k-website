(ns dda.c4k-website.ingress-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.test-helper :as th]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-website.ingress :as cut]
   [clojure.spec.alpha :as s]))

(st/instrument `cut/generate-rule)
(st/instrument `cut/generate-http-ingress)

(deftest should-genereate-rule
  (is (= {:host "test.com",
          :http
          {:paths
           [{:pathType "Prefix",
             :path "/",
             :backend
             {:service {:name "myservice", :port {:number 3000}}}}]}}

         (cut/generate-rule "myservice" 3000 "test.com"))))


(deftest should-generate-http-ingress
  (is (= {:apiVersion "networking.k8s.io/v1",
          :kind "Ingress",
          :metadata
          {:name "myservice-http-ingress",
           :namespace "default",
           :annotations
           #:traefik.ingress.kubernetes.io{:router.entrypoints "web",
                                           :router.middlewares "default-redirect-https@kubernetescrd"}}}
         (dissoc (cut/generate-http-ingress
                  {:issuer "prod"
                   :service-name "myservice"
                   :service-port 3000
                   :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}) :spec)))
  (is (= {:rules
          [{:host "test.de",
            :http
            {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "myservice", :port {:number 3000}}}}]}}
           {:host "www.test.de",
            :http
            {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "myservice", :port {:number 3000}}}}]}}
           {:host "test-it.de",
            :http
            {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "myservice", :port {:number 3000}}}}]}}
           {:host "www.test-it.de",
            :http
            {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "myservice", :port {:number 3000}}}}]}}]}
         (:spec (cut/generate-http-ingress
                 {:issuer "prod"
                  :service-name "myservice"
                  :service-port 3000
                  :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))))

;; (deftest should-generate-https-ingress
;;   (is (= {:apiVersion "networking.k8s.io/v1",
;;           :kind "Ingress",
;;           :metadata
;;           {:name "test-io-https-ingress",
;;            :namespace "default",
;;            :annotations #:traefik.ingress.kubernetes.io{:router.entrypoints "websecure", :router.tls "true"}},
;;           :spec
;;           {:tls [{:hosts ["test.de" "www.test.de" "test-it.de" "www.test-it.de"], :secretName "test-io-cert"}],
;;            :rules
;;            [{:host "test.de",
;;              :http
;;              {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "test-io-service", :port {:number 80}}}}]}}
;;             {:host "www.test.de",
;;              :http
;;              {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "test-io-service", :port {:number 80}}}}]}}
;;             {:host "test-it.de",
;;              :http
;;              {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "test-io-service", :port {:number 80}}}}]}}
;;             {:host "www.test-it.de",
;;              :http
;;              {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "test-io-service", :port {:number 80}}}}]}}]}}
;;          (cut/generate-https-ingress {:unique-name "test.io"
;;                                       :gitea-host "gitea.evilorg"
;;                                       :gitea-repo "none"
;;                                       :branchname "mablain"
;;                                       :issuer "prod"
;;                                       :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}))))