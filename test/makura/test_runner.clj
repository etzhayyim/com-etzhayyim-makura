(ns makura.test-runner
  (:require [clojure.test :as t]
            [makura.murakumo-test]
            [makura.methods.test-agent]
            [makura.methods.test-charter-gates]))

(def suites
  '[makura.murakumo-test
    makura.methods.test-agent
    makura.methods.test-charter-gates])

(defn -main [& _]
  (let [{:keys [fail error]} (apply t/run-tests suites)]
    (when (pos? (+ fail error))
      (System/exit 1))))
