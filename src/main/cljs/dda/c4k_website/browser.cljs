(ns dda.c4k-website.browser
  (:require
   [clojure.tools.reader.edn :as edn]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.browser :as br]
   [dda.c4k-website.core :as core]
   ))

(defn generate-content []
  (cm/concat-vec
   [(assoc
     (br/generate-needs-validation) :content
     (cm/concat-vec
      (br/generate-group
       "config"
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
       "auth"
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
   (br/generate-output "c4k-website-output" "Your c4k deployment.yaml:" "25")))

(defn generate-content-div
  []
  {:type :element
   :tag :div
   :content
   (generate-content)})

(defn validate-all! []
  (br/validate! "config" core/config? :deserializer edn/read-string)
  (br/validate! "auth" core/auth? :deserializer edn/read-string)
  (br/set-validated!))

(defn add-validate-listener [name]
  (-> (br/get-element-by-id name)
      (.addEventListener "blur" #(do (validate-all!)))))

(defn init []
  (br/append-hickory (generate-content-div))
 (let [config-only false
        auth-only false]
    (-> js/document
        (.getElementById "generate-button")
        (.addEventListener "click"
                           #(do (validate-all!)
                                (-> (cm/generate-cm
                                     (br/get-content-from-element "config" :deserializer edn/read-string)
                                     (br/get-content-from-element "auth" :deserializer edn/read-string)
                                     core/config-defaults
                                     core/config-objects
                                     core/auth-objects
                                     config-only
                                     auth-only)
                                    (br/set-output!))))))
  (add-validate-listener "config")
  (add-validate-listener "auth"))
