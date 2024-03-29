(ns dda.c4k-website.browser
  (:require
   [clojure.string :as st]
   [clojure.tools.reader.edn :as edn]
   [dda.c4k-common.monitoring :as mon]
   [dda.c4k-website.core :as core]
   [dda.c4k-website.website :as website]   
   [dda.c4k-common.common :as cm]   
   [dda.c4k-common.browser :as br]
   ))

(defn generate-content []
  (cm/concat-vec
   [(assoc
     (br/generate-needs-validation) :content
     (cm/concat-vec
      (br/generate-group
       "domain"
       (cm/concat-vec
        (br/generate-input-field "issuer" "(Optional) Your issuer prod/staging:" "staging")
        (br/generate-input-field "mon-cluster-name" "(Optional) monitoring cluster name:" "website")
        (br/generate-input-field "mon-cluster-stage" "(Optional) monitoring cluster stage:" "test")
        (br/generate-input-field "mon-cloud-url" "(Optional) grafana cloud url:" "https://prometheus-prod-01-eu-west-0.grafana.net/api/prom/push")))
      (br/generate-group
       "website-data"
        (br/generate-text-area
         "websiteconfigs" "Contains fqdns, repo infos, an optional sha256sum-output for script execution for each website:"
         "{ :websiteconfigs
          [{:unique-name \"test.io\",
            :fqdns [\"test.de\" \"www.test.de\"],
            :forgejo-host \"githost.de\",
            :repo-owner \"someuser\",
            :repo-name \"repo\",
            :branchname \"main\",
            :sha256sum-output \"123456789ab123cd345de script-file-name.sh\"}
           {:unique-name \"example.io \",
            :fqdns [\"example.org\" \"www.example.org\"],
            :forgejo-host \"githost.org\",
            :repo-owner \"someuser\",
            :repo-name \"repo\",
            :branchname \"main\",
            :build-cpu-request \"1500m\",
            :build-cpu-limit \"3000m\",
            :build-memory-request \"512Mi\",
            :build-memory-limit \"1024Mi\"}] }"
         "16"))
      (br/generate-group
       "credentials"
       (br/generate-text-area
        "auth" "Your authentication data for each website or git repo:"
        "{:mon-auth 
          {:grafana-cloud-user \"your-user-id\"
           :grafana-cloud-password \"your-cloud-password\"}
          :websiteauths
          [{:unique-name \"test.io\",
            :authtoken \"abedjgbasdodj\"}
           {:unique-name \"example.io\",
            :authtoken \"abedjgbasdodj\"}]}"
        "7"))
      [(br/generate-br)]
      (br/generate-button "generate-button" "Generate c4k yaml")))]
   (br/generate-output "c4k-website-output" "Your c4k deployment.yaml:" "15")))

(defn generate-content-div
  []
  {:type :element
   :tag :div
   :content
   (generate-content)})

(defn config-from-document []
  (let [issuer (br/get-content-from-element "issuer" :optional true)
        websiteconfigs (br/get-content-from-element "websiteconfigs" :deserializer edn/read-string)
        mon-cluster-name (br/get-content-from-element "mon-cluster-name" :optional true)
        mon-cluster-stage (br/get-content-from-element "mon-cluster-stage" :optional true)
        mon-cloud-url (br/get-content-from-element "mon-cloud-url" :optional true)]
    (merge
     {:websiteconfigs websiteconfigs}
     (when (not (st/blank? issuer))
       {:issuer issuer})
     (when (some? mon-cluster-name)
       {:mon-cfg {:cluster-name mon-cluster-name
                  :cluster-stage (keyword mon-cluster-stage)
                  :grafana-cloud-url mon-cloud-url}}))))

(defn validate-all! []
  (br/validate! "websiteconfigs" core/websiteconfigs? :deserializer edn/read-string)
  (br/validate! "issuer" ::core/issuer :optional true)
  (br/validate! "mon-cluster-name" ::mon/cluster-name :optional true)
  (br/validate! "mon-cluster-stage" ::mon/cluster-stage :optional true)
  (br/validate! "mon-cloud-url" ::mon/grafana-cloud-url :optional true)
  (br/validate! "auth" core/websiteauths? :deserializer edn/read-string)
  (br/set-form-validated!))

(defn add-validate-listener [name]
  (-> (br/get-element-by-id name)
      (.addEventListener "blur" #(do (validate-all!)))))

(defn init []
  (br/append-hickory (generate-content-div))
  (-> js/document
      (.getElementById "generate-button")
      (.addEventListener "click"
                         #(do (validate-all!)
                              (-> (cm/generate-common
                                   (config-from-document)
                                   (br/get-content-from-element "auth" :deserializer edn/read-string)
                                   core/config-defaults
                                   core/k8s-objects)
                                  (br/set-output!)))))
  (add-validate-listener "websiteconfigs")
  (add-validate-listener "issuer")
  (add-validate-listener "mon-cluster-name")
  (add-validate-listener "mon-cluster-stage")
  (add-validate-listener "mon-cloud-url")
  (add-validate-listener "auth"))
