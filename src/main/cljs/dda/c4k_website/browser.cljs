(ns dda.c4k-website.browser
  (:require
   [clojure.string :as st]
   [clojure.tools.reader.edn :as edn]
   [dda.c4k-website.core :as core]
   [dda.c4k-website.website :as website]
   [dda.c4k-common.browser :as br]   
   [dda.c4k-common.common :as cm]))

(defn generate-group
  [name
   content]
  [{:type :element
    :tag :div
    :attrs {:class "rounded border border-3  m-3 p-2"}
    :content [{:type :element
               :tag :b
               :attrs {:style "z-index: 1; position: relative; top: -1.3rem;"}
               :content name}
              {:type :element
               :tag :fieldset
               :content content}]}])

(defn generate-content []
  (cm/concat-vec
   [(assoc
     (br/generate-needs-validation) :content
     (cm/concat-vec
      (generate-group
       "domain"
       (cm/concat-vec
        (br/generate-input-field "fqdn" "Your first fqdn:" "deineWebsite.de")
        (br/generate-input-field "fqdn1" "Your second fqdn:" "deineWebsite.com")
        (br/generate-input-field "fqdn2" "Your third fqdn:" "meineWebsite.org")
        (br/generate-input-field "multi" "Holds fqdns pointing to same ingress" "[\"fqdn\", \"fqdn1\"]")
        (br/generate-input-field "single" "Holds fqdn pointing to another ingress" "fqdn")
        (br/generate-input-field "issuer" "(Optional) Your issuer prod/staging:" "")))      
      (generate-group
       "credentials"
       (br/generate-text-area
        "auth" "Your auth.edn:"
        "{:authtoken \"yourgiteaauthtoken\"        
         :gitrepourl \"https://your.gitea.host/api/v1/repos/<owner>/<repo>/archive/<branchname>.zip\"
         :singlegitrepourl \"https://your.gitea.host/api/v1/repos/<owner>/<otherRepo>/archive/<branchname>.zip\"
         }"
        "3"))
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
  (let [issuer (br/get-content-from-element "issuer" :optional true)]
    (merge
     {:fqdn (br/get-content-from-element "fqdn")}
     (when (not (st/blank? issuer))
       {:issuer issuer}))))

(defn validate-all! []
  (br/validate! "fqdn" ::website/fqdn)
  (br/validate! "fqdn1" ::website/fqdn1)
  (br/validate! "fqdn2" ::website/fqdn2)
  (br/validate! "single" ::website/single)
  (br/validate! "multi" ::website/multi)
  (br/validate! "issuer" ::website/issuer :optional true)
  (br/validate! "auth" core/auth? :deserializer edn/read-string)
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
  (add-validate-listener "fqdn")
  (add-validate-listener "fqdn1")
  (add-validate-listener "fqdn2")
  (add-validate-listener "single")
  (add-validate-listener "multi")
  (add-validate-listener "issuer")
  (add-validate-listener "auth"))
