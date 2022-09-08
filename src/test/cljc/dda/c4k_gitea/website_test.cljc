(ns dda.c4k-website.website-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.test-helper :as th]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-website.website :as cut]))

(st/instrument `cut/generate-appini-env)
(st/instrument `cut/generate-ingress)
(st/instrument `cut/generate-secrets)

(deftest should-generate-appini-env
  (is (= {:APP_NAME-c1 "",
          :APP_NAME-c2 "test website",
          :website__mailer__FROM-c1 "",
          :website__mailer__FROM-c2 "test@test.com",
          :website__mailer__HOST-c1 "m.t.de:123",
          :website__mailer__HOST-c2 "mail.test.com:123",
          :website__server__DOMAIN-c1 "test.de",
          :website__server__DOMAIN-c2 "test.com",
          :website__server__ROOT_URL-c1 "https://test.de",
          :website__server__ROOT_URL-c2 "https://test.com",
          :website__server__SSH_DOMAIN-c1 "test.de",
          :website__server__SSH_DOMAIN-c2 "test.com",
          :website__service__EMAIL_DOMAIN_WHITELIST-c1 "adb.de",
          :website__service__EMAIL_DOMAIN_WHITELIST-c2 "test.com,test.net",
          :website__service__NO_REPLY_ADDRESS-c1 "",
          :website__service__NO_REPLY_ADDRESS-c2 "noreply@test.com"}
         (th/map-diff (cut/generate-appini-env {:default-app-name ""
                                                :fqdn "test.de"                                                
                                                :mailer-from ""
                                                :mailer-host-port "m.t.de:123"                                                
                                                :service-domain-whitelist "adb.de"
                                                :service-noreply-address ""
                                                })
                      (cut/generate-appini-env {:default-app-name "test website"
                                                :fqdn "test.com"                                                 
                                                :mailer-from "test@test.com"
                                                :mailer-host-port "mail.test.com:123"                                                
                                                :service-domain-whitelist "test.com,test.net"
                                                :service-noreply-address "noreply@test.com"
                                                })))))

(deftest should-generate-certificate
  (is (= {:name-c2 "prod", :name-c1 "staging"}
         (th/map-diff (cut/generate-certificate {}) 
                      (cut/generate-certificate {:issuer "prod"})))))

(deftest should-generate-secret
  (is (= {:website__database__USER-c1 "",
          :website__database__USER-c2 (b64/encode "pg-user"),
          :website__database__PASSWD-c1 "",
          :website__database__PASSWD-c2 (b64/encode "pg-pw"),
          :website__mailer__USER-c1 "",
          :website__mailer__USER-c2 (b64/encode "maileruser"),
          :website__mailer__PASSWD-c1 "",
          :website__mailer__PASSWD-c2 (b64/encode "mailerpw")}
         (th/map-diff (cut/generate-secrets {:postgres-db-user ""
                                             :postgres-db-password ""
                                             :mailer-user ""
                                             :mailer-pw ""})
                      (cut/generate-secrets {:postgres-db-user "pg-user"
                                             :postgres-db-password "pg-pw"
                                             :mailer-user "maileruser"
                                             :mailer-pw "mailerpw"})))))

(deftest should-generate-data-volume
  (is (= {:storage-c1 "1Gi",
          :storage-c2 "15Gi"}
         (th/map-diff (cut/generate-data-volume {:volume-total-storage-size 1})
                      (cut/generate-data-volume {:volume-total-storage-size 15})))))