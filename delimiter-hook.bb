#!/usr/bin/env bb

(require '[edamame.core :as e]
         '[clojure.java.shell :as shell]
         '[cheshire.core :as json])

;; Load delimiter checking functions
(load-file (str (System/getProperty "user.dir") "/check_delimiters.bb"))

(defn clojure-file? [file-path]
  (some #(clojure.string/ends-with? file-path %)
        [".clj" ".cljs" ".cljc" ".bb" ".edn"]))

(defn process-pre-write
  [hook-input]
  (let [tool-input (:tool_input hook-input)
        file-path (:file_path tool-input)
        content (:content tool-input)]

    (if-not (clojure-file? file-path)
      {:hookSpecificOutput
       {:hookEventName "PreToolUse"
        :permissionDecision "allow"}}

      (if (delimiter-error? content)
        (let [fixed-content (fix-delimiters content)]
          (if fixed-content
            {:hookSpecificOutput
             {:hookEventName "PreToolUse"
              :permissionDecision "allow"
              :permissionDecisionReason "Auto-fixed delimiter errors"
              :updatedInput {:file_path file-path
                             :content fixed-content}}}
            {:hookSpecificOutput
             {:hookEventName "PreToolUse"
              :permissionDecision "deny"
              :permissionDecisionReason "Delimiter errors found and could not be auto-fixed"}}))
        {:hookSpecificOutput
         {:hookEventName "PreToolUse"
          :permissionDecision "allow"}}))))

(defn process-post-edit
  [hook-input]
  (let [tool-input (:tool_input hook-input)
        tool-response (:tool_response hook-input)
        file-path (:file_path tool-input)]

    ;; PostToolUse only fires on success, so if we're here, the edit succeeded
    (when (and (clojure-file? file-path)
               tool-response)
      (let [file-content (slurp file-path)]
        (when (delimiter-error? file-content)
          (when-let [fixed-content (fix-delimiters file-content)]
            (spit file-path fixed-content)
            (binding [*out* *err*]
              (println "Auto-fixed delimiter errors in" file-path))))))

    nil))

(defn process-hook
  [hook-input]
  (let [hook-event (:hook_event_name hook-input)
        tool-name (:tool_name hook-input)]
    (cond
      (and (= hook-event "PreToolUse") (= tool-name "Write"))
      (process-pre-write hook-input)

      (and (= hook-event "PostToolUse") (= tool-name "Edit"))
      (process-post-edit hook-input)

      :else
      (when (= hook-event "PreToolUse")
        {:hookSpecificOutput
         {:hookEventName "PreToolUse"
          :permissionDecision "allow"}}))))

(defn -main []
  (try
    (let [input-json (slurp *in*)
          _ (spit "hook-logs/delimiter-hook-debug.log"
                  (str "INPUT: " input-json "\n")
                  :append true)
          hook-input (json/parse-string input-json true)
          response (process-hook hook-input)
          _ (spit "hook-logs/delimiter-hook-debug.log"
                  (str "OUTPUT: " (json/generate-string response) "\n\n")
                  :append true)]
      (when response
        (println (json/generate-string response)))
      (System/exit 0))
    (catch Exception e
      (binding [*out* *err*]
        (println "Hook error:" (.getMessage e))
        (println "Stack trace:" (with-out-str (.printStackTrace e))))
      (System/exit 2))))

(apply -main *command-line-args*)
