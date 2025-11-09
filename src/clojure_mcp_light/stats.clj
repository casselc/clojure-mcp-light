(ns clojure-mcp-light.stats
  "Statistics tracking for delimiter repair events"
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [taoensso.timbre :as timbre])
  (:import [java.time Instant]))

;; ============================================================================
;; Configuration
;; ============================================================================

(def ^:dynamic *enable-stats* false)

(def stats-file-path
  "Global stats log file location"
  (let [home (System/getProperty "user.home")]
    (str home "/.clojure-mcp-light/stats.log")))

;; ============================================================================
;; Event Logging
;; ============================================================================

(defn edn-output-fn
  "Timbre output function that produces pure EDN from first varg.
  This is used by the stats appender to write EDN entries directly."
  [{:keys [vargs]}]
  (when-let [data (first vargs)]
    (pr-str data)))

(defn timestamp-iso8601
  "Generate ISO-8601 timestamp string"
  []
  (.toString (Instant/now)))

(defn ensure-parent-dir
  "Ensure parent directory exists for the given file path"
  [file-path]
  (let [parent-dir (.getParentFile (io/file file-path))]
    (when parent-dir
      (.mkdirs parent-dir))))

(defn log-event!
  "Log a delimiter event to the stats file.

  Parameters:
  - event-type: keyword like :delimiter-error, :delimiter-fixed, :delimiter-fix-failed, :delimiter-ok
  - hook-event: string like \"PreToolUse\" or \"PostToolUse\"
  - file-path: string path to the file being processed

  Uses binding with timbre/*config* to set up a minimal logging config with file locking."
  [event-type hook-event file-path]
  (when *enable-stats*
    (try
      (ensure-parent-dir stats-file-path)
      (let [entry {:event-type event-type
                   :hook-event hook-event
                   :timestamp (timestamp-iso8601)
                   :file-path file-path}
            stats-config {:min-level :trace
                          :appenders {:stats (assoc
                                              (timbre/spit-appender {:fname stats-file-path})
                                              :enabled? true
                                              :output-fn edn-output-fn)}}]
        (binding [timbre/*config* stats-config]
          (timbre/trace entry)))
      (catch Exception e
        ;; Use parent config for error logging
        (timbre/error "Failed to log stats event:" (.getMessage e))))))
