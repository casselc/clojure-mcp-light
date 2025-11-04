#!/usr/bin/env bb

(require '[edamame.core :as e]
         '[clojure.java.shell :as shell]
         '[cheshire.core :as json])

(defn delimiter-error?
  "Returns true if the string has a delimiter error specifically.
   Checks both that it's an :edamame/error and has delimiter info."
  [s]
  (try
    (e/parse-string-all s)
    false ; No error = no delimiter error
    (catch clojure.lang.ExceptionInfo ex
      (let [data (ex-data ex)]
        (and (= :edamame/error (:type data))
             (contains? data :edamame/opened-delimiter))))))

(defn get-delimiter-error-info
  "Returns detailed information about a delimiter error, or nil if no error."
  [s]
  (try
    (e/parse-string-all s)
    nil
    (catch clojure.lang.ExceptionInfo ex
      (let [data (ex-data ex)]
        (when (and (= :edamame/error (:type data))
                   (contains? data :edamame/opened-delimiter))
          {:message (.getMessage ex)
           :error-location {:row (:row data) :col (:col data)}
           :opened-delimiter (:edamame/opened-delimiter data)
           :opened-location (:edamame/opened-delimiter-loc data)
           :expected-delimiter (:edamame/expected-delimiter data)})))))

(defn parinfer-repair
  "Attempts to repair delimiter errors using parinfer-rust.
   Returns a map with:
   - :success - boolean indicating if repair was successful
   - :repaired-text - the repaired code (if successful)
   - :error - error message (if unsuccessful)"
  [s]
  (let [result (shell/sh "parinfer-rust"
                         "--mode" "indent"
                         "--language" "clojure"
                         "--output-format" "json"
                         :in s)
        exit-code (:exit result)]
    (if (zero? exit-code)
      (try
        (let [parsed (json/parse-string (:out result) true)
              success (:success parsed)
              text (:text parsed)
              error (:error parsed)]
          (if success
            {:success true
             :repaired-text text
             :error nil}
            {:success false
             :repaired-text nil
             :error (or error "Parinfer repair failed")}))
        (catch Exception e
          {:success false
           :repaired-text nil
           :error (str "Failed to parse parinfer output: " (.getMessage e))}))
      {:success false
       :repaired-text nil
       :error (str "parinfer-rust exited with code " exit-code ": " (:err result))})))

(defn fix-delimiters
  "Takes a Clojure string and attempts to fix delimiter errors.
   Returns the repaired string if successful, false otherwise.
   If no delimiter errors exist, returns the original string."
  [s]
  (if (delimiter-error? s)
    ;; Has delimiter error - try to repair
    (let [repair-result (parinfer-repair s)]
      (if (:success repair-result)
        ;; Parinfer succeeded - verify the repair
        (let [repaired-text (:repaired-text repair-result)]
          (if (delimiter-error? repaired-text)
            ;; Still has delimiter errors after repair
            false
            ;; Successfully repaired!
            repaired-text))
        ;; Parinfer failed
        false))
    ;; No delimiter errors - return original
    s))

#_(defn -main [& args]
    (let [input (if (seq args)
                  (first args)
                  (slurp *in*))]
      (if (delimiter-error? input)
        (do
          (println "❌ Delimiter error found!")
          (when-let [info (get-delimiter-error-info input)]
            (println "\nDetails:")
            (println "  Message:" (:message info))
            (println "  Error at:" (format "line %d, col %d"
                                           (get-in info [:error-location :row])
                                           (get-in info [:error-location :col])))
            (when-let [opened (:opened-delimiter info)]
              (when (seq opened)
                (println "  Opened:" opened "at"
                         (format "line %d, col %d"
                                 (get-in info [:opened-location :row])
                                 (get-in info [:opened-location :col])))))
            (when-let [expected (:expected-delimiter info)]
              (when (seq expected)
                (println "  Expected:" expected))))
          (System/exit 1))
        (do
          (println "✅ No delimiter errors found!")
          (System/exit 0)))))

#_(apply -main *command-line-args*)
