(ns makura.murakumo-test
  (:require [clojure.test :refer [deftest is testing]]
            [makura.murakumo :as makura]))

(def full-attestations
  (into {}
        (map (fn [gate] [gate (str "attested-" (name gate))]))
        (distinct (mapcat :required-gates (vals makura/cell-specs)))))

(deftest maps-all-legacy-pillow-cells
  (is (= #{"pillow_fabric_attestation"
           "pillow_filling_close"
           "pillow_foam_blowing"
           "pillow_foam_shredding"
           "pillow_isocyanate_dispensing"
           "pillow_packaging"
           "pillow_polyol_attestation"
           "pillow_qc"
           "pillow_shell_sewing"}
         (set (map :legacy-cell (vals makura/cell-specs))))))

(deftest r0-gates-block-effects
  (let [plan (makura/cell-plan :polyol-attestation
                               {:batch-id "batch-001"
                                :computed-at "2026-06-29T00:00:00Z"})]
    (is (= :blocked (:status plan)))
    (is (= [:council-charter-attestation
            :silen-comfort-review
            :r1-activation-adr
            :pu-foam-chemist-registry
            :industrial-hygienist-registry
            :open-foam-recipe-baseline
            :bio-content-disclosure-baseline]
           (:missing-gates plan)))
    (is (empty? (:effects plan)))))

(deftest attested-isocyanate-emits-batch-and-exposure-effects
  (let [plan (makura/cell-plan :isocyanate-dispensing
                               {:attestations full-attestations
                                :batch-id "batch-001"
                                :computed-at "2026-06-29T00:00:00Z"
                                :records {"com.etzhayyim.makura.workerExposureRecord"
                                          {:tid "exp-001"
                                           :mdiTwaPpb 2.1}}})
        effects (:effects plan)]
    (is (= :ready (:status plan)))
    (is (= ["com.etzhayyim.makura.foamBatchAttestation"
            "com.etzhayyim.makura.workerExposureRecord"]
           (mapv :collection effects)))
    (is (= :mst/put-record (:op (first effects))))
    (is (= makura/actor-did (:actor (first effects))))
    (is (= "exp-001" (:rkey (second effects))))
    (is (= 2.1 (get-in effects [1 :record :mdiTwaPpb])))))

(deftest special-gates-remain-cell-specific
  (testing "foam blowing keeps VOC and FR chemistry gates"
    (let [attestations (dissoc full-attestations :fr-chemistry-exclusion-baseline)
          plan (makura/cell-plan :foam-blowing {:attestations attestations})]
      (is (= [:fr-chemistry-exclusion-baseline] (:missing-gates plan)))
      (is (empty? (:effects plan)))))
  (testing "packaging keeps take-back chain gate"
    (let [attestations (dissoc full-attestations :take-back-chain-ipfs-baseline)
          plan (makura/cell-plan :packaging {:attestations attestations})]
      (is (= [:take-back-chain-ipfs-baseline] (:missing-gates plan)))
      (is (empty? (:effects plan))))))

(deftest all-cell-plans-ready-when-attested
  (let [plans (makura/all-cell-plans {:attestations full-attestations
                                      :batch-id "batch-001"
                                      :lot-id "lot-001"
                                      :pillow-serial "MK-001"
                                      :computed-at "2026-06-29T00:00:00Z"})]
    (is (= (set (keys makura/cell-specs)) (set (keys plans))))
    (is (every? #(= :ready (:status %)) (vals plans)))
    (is (= 11 (count (mapcat :effects (vals plans)))))))
