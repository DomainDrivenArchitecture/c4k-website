(ns dda.c4k-website.website-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.test-helper :as th]
   [dda.c4k-website.website :as cut]))

(deftest should-generate-nginx-deployment
  (is (= {:apiVersion "apps/v1",
          :kind "Deployment",
          :metadata {:name "test-io-deployment",
                     :namespace "test-io"
                     :labels {:app.kubernetes.part-of "test-io"}},
          :spec
          {:replicas 1,
           :selector {:matchLabels {:app "test-io-nginx"}},
           :template
           {:metadata {:namespace "webserver"
                       :labels {:app "test-io-nginx"}},
            :spec
            {:containers
             [{:name "test-io-nginx",
               :image "nginx:latest",
               :imagePullPolicy "IfNotPresent",
               :ports [{:containerPort 80}],
               :volumeMounts
               [{:mountPath "/etc/nginx", :readOnly true, :name "etc-ngingx"}
                {:mountPath "/var/log/nginx", :name "log"}
                {:mountPath "/var/www/html/website", :name "content-volume", :readOnly true}]}],
             :initContainers
             [{:image "domaindrivenarchitecture/c4k-website-build",
               :name "test-io-init-build-container",
               :imagePullPolicy "IfNotPresent",
               :resources {:requests {:cpu "500m", :memory "256Mi"}, :limits {:cpu "1700m", :memory "512Mi"}},
               :command ["/entrypoint.sh"],
               :envFrom [{:secretRef {:name "test-io-secret"}}],
               :volumeMounts [{:name "content-volume", :mountPath "/var/www/html/website"}
                              {:name "hashfile-volume", :mountPath "/var/hashfile.d"}]}],
             :volumes
             [{:name "etc-ngingx",
               :configMap
               {:name "etc-ngingx",
                :items
                [{:key "nginx.conf", :path "nginx.conf"}
                 {:key "website.conf", :path "conf.d/website.conf"}
                 {:key "mime.types", :path "mime.types"}]}}
              {:name "log", :emptyDir {}}
              {:name "content-volume", :persistentVolumeClaim {:claimName "test-io-content-volume"}}
              {:name "hashfile-volume", :persistentVolumeClaim {:claimName "test-io-hashfile-volume"}}]}}}}
         (cut/generate-nginx-deployment {:forgejo-host "gitlab.de",
                                         :fqdns ["test.de" "test.org" "www.test.de" "www.test.org"],
                                         :forgejo-repo "repo",
                                         :sha256sum-output "123456789ab123cd345de script-file-name.sh",
                                         :issuer "staging",
                                         :branchname "main",
                                         :unique-name "test.io"}))))