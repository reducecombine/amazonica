(ns amazonica.test.lambda
  (:use [clojure.test]
        [amazonica.aws.lambda]
        [clojure.java.shell])
  (:require [amazonica.aws.identitymanagement :as iam]
            [clojure.string :as string])
  (:import [com.amazonaws.auth
            AWSCredentialsProvider
            DefaultAWSCredentialsProviderChain]))

(def cred
  (let [file  (str (System/getProperty "user.home") "/.aws/credentials")
        lines (string/split (slurp file) #"\n")
        creds (into {} (filter second (map #(string/split % #"\s*=\s*") lines)))]
    {:access-key (get creds "aws_access_key_id")
     :secret-key (get creds "aws_secret_access_key")}))

(def role (delay (-> #(.contains (:role-name %) "lambda")
            (filter (:roles (iam/list-roles)))
            first
                     :arn)))

(deftest aws-lambda []

  (def handler "exports.helloWorld = function(event, context) {
                  console.log('value1 = ' + event.key1)
                  console.log('value2 = ' + event.key2)
                  console.log('value3 = ' + event.key3)
                  context.done(null, 'Hello World')
                }")

  (create-function :role @role :function handler)
  ; (create-function {:role role :function handler})
  ; (create-function cred :role role :function handler)
  ; (create-function cred {:role role :function handler})
  ; (create-function (DefaultAWSCredentialsProviderChain.) :role role :function handler)
  ; (create-function (DefaultAWSCredentialsProviderChain.) {:role role :function handler})

  (let [f (-> (list-functions) :functions first)]
    (is (= "uploaded via amazonica" (:description f)))
    (is (= "helloWorld" (:function-name f)))
    (is (= "helloWorld.helloWorld" (:handler f)))
    (is (= 10 (:timeout f)))
    (is (= 256 (:memory-size f))))

  ; (create-function cred
  ;                  :timeout 30
  ;                  :memory-size 512
  ;                  :description "helloWorld - amazonica test"
  ;                  :role role
  ;                  :function-name "helloWorld"
  ;                  :function handler
  ;                  :handler "helloWorld.helloWorld")

  ; (let [f (-> (list-functions) :functions first)]
  ;   (is (= "helloWorld - amazonica test" (:description f)))
  ;   (is (= 30 (:timeout f)))
  ;   (is (= 512 (:memory-size f))))

  ;(add-event-source :function-name "helloWorld")

  (list-event-source-mappings :function-name "helloWorld")

  (invoke :function-name "helloWorld"
          :invocation-type "Event"
          :payload "{\"key1\": 1, \"key2\": 2, \"key3\": 3}")

  (delete-function :function-name "helloWorld")

)
