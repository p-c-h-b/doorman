(ns doorman.core
  (:require
   [me.raynes.conch.low-level :as sh]
   [taoensso.timbre :as timbre :refer [info]])
  (:gen-class))

(defonce ^:dynamic *modem* nil)

(defn- make-modem [m]
  (let [modem (sh/proc "socat" "-" (str m ",crlf"))] ; "/dev/cu.usbmodem24680241,crlf"
    (assoc
     modem
     :reader
     (clojure.java.io/reader (:out modem))
     :writer
     (clojure.java.io/writer (:in modem)))))

(defn- at [command]
  (let [w (:writer *modem*)]
    (.write w (str command "\n"))
    (.flush w)))

(defn- read-loop []
  (with-open [rdr (:reader *modem*)]
    (let [ls (line-seq rdr)]
      (loop [[line & r] ls]
        (case line
          "RING"
          (do
            (info "Door Opened")
            (at "ath1")
            (Thread/sleep 1000)
            (at "atdt 6")
            (Thread/sleep 1000)
            (at "ath0"))
          nil)
        (recur r)))))

(defn -main [& args]
  (binding [*modem* (make-modem (first args))]
    (at "ats11=250")
    (read-loop)))
