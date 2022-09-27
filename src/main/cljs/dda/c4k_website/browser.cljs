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
        (br/generate-input-field "fqdn" "Your fqdn:" "deineWebsite.de")
        (br/generate-input-field "issuer" "(Optional) Your issuer prod/staging:" "")))
      (generate-group
       "provider"
       (cm/concat-vec
        (br/generate-input-field "volume-total-storage-size" "Your website volume-total-storage-size:" "20")
        (br/generate-input-field "number-of-websites" "The Number of websites running on your cluster" "5")))
      [(br/generate-br)]
      (br/generate-button "generate-button" "Generate c4k yaml")))]
   (br/generate-output "c4k-website-output" "Your c4k deployment.yaml:" "25")))

(defn generate-content-div
  []
  {:type :element
   :tag :div
   :content
   (generate-content)})

(defn config-from-document []
  (let [issuer (br/get-content-from-element "issuer" :optional true)]
    (merge
     {:fqdn (br/get-content-from-element "fqdn")      
      :volume-total-storage-size (br/get-content-from-element "volume-total-storage-size" :deserializer js/parseInt)
      :number-of-websites (br/get-content-from-element "number-of-websites" :deserializer js/parseInt)}     
     (when (not (st/blank? issuer))
       {:issuer issuer})          
     )))

(defn validate-all! [] ; ToDo: Add all necessary inputs and auth
  (br/validate! "fqdn" ::website/fqdn)  
  (br/validate! "issuer" ::website/issuer :optional true)
  (br/validate! "volume-total-storage-size" ::website/volume-total-storage-size :deserializer js/parseInt)
  (br/validate! "number-of-websites" ::website/number-of-websites :deserializer js/parseInt)  
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
                                   website/config-defaults
                                   core/k8s-objects)
                               (br/set-output!)))))
  (add-validate-listener "fqdn")  
  (add-validate-listener "volume-total-storage-size")
  (add-validate-listener "issuer")
  (add-validate-listener "number-of-websites"))