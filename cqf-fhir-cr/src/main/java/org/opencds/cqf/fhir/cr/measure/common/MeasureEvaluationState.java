package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Holds all mutable evaluation state for a single measure evaluation.
 * <p>
 * {@link MeasureDef} and its children are immutable structure; this class holds the
 * per-evaluation data that was previously mixed into the Def objects. Consumers receive
 * both the {@code MeasureDef} (for structural info) and a {@code MeasureEvaluationState}
 * (for runtime data).
 * <p>
 * State objects are looked up by Def identity (reference equality).
 */
public class MeasureEvaluationState {

    private final List<String> errors = new ArrayList<>();

    private final IdentityHashMap<PopulationDef, PopulationState> populationStates = new IdentityHashMap<>();
    private final IdentityHashMap<GroupDef, GroupState> groupStates = new IdentityHashMap<>();
    private final IdentityHashMap<StratifierDef, StratifierState> stratifierStates = new IdentityHashMap<>();
    private final IdentityHashMap<StratifierComponentDef, StratifierComponentState> componentStates =
            new IdentityHashMap<>();
    private final IdentityHashMap<SdeDef, SdeState> sdeStates = new IdentityHashMap<>();
    private final IdentityHashMap<SupportingEvidenceDef, SupportingEvidenceState> seStates = new IdentityHashMap<>();

    /**
     * Create a state container pre-populated with state objects for every Def in the measure.
     */
    public static MeasureEvaluationState create(MeasureDef measureDef) {
        var state = new MeasureEvaluationState();
        for (GroupDef group : measureDef.groups()) {
            state.groupStates.put(group, new GroupState());
            for (PopulationDef pop : group.populations()) {
                state.populationStates.put(pop, new PopulationState(pop));
                if (pop.getSupportingEvidenceDefs() != null) {
                    for (SupportingEvidenceDef se : pop.getSupportingEvidenceDefs()) {
                        state.seStates.put(se, new SupportingEvidenceState());
                    }
                }
            }
            for (StratifierDef strat : group.stratifiers()) {
                state.stratifierStates.put(strat, new StratifierState());
                for (StratifierComponentDef comp : strat.components()) {
                    state.componentStates.put(comp, new StratifierComponentState());
                }
            }
        }
        for (SdeDef sde : measureDef.sdes()) {
            state.sdeStates.put(sde, new SdeState());
        }
        return state;
    }

    public PopulationState population(PopulationDef def) {
        return Objects.requireNonNull(populationStates.get(def), "No state registered for PopulationDef: " + def.id());
    }

    public GroupState group(GroupDef def) {
        return Objects.requireNonNull(groupStates.get(def), "No state registered for GroupDef: " + def.id());
    }

    public StratifierState stratifier(StratifierDef def) {
        return Objects.requireNonNull(stratifierStates.get(def), "No state registered for StratifierDef: " + def.id());
    }

    public StratifierComponentState component(StratifierComponentDef def) {
        return Objects.requireNonNull(
                componentStates.get(def), "No state registered for StratifierComponentDef: " + def.id());
    }

    public SdeState sde(SdeDef def) {
        return Objects.requireNonNull(sdeStates.get(def), "No state registered for SdeDef: " + def.id());
    }

    public SupportingEvidenceState supportingEvidence(SupportingEvidenceDef def) {
        return Objects.requireNonNull(seStates.get(def), "No state registered for SupportingEvidenceDef");
    }

    public List<String> errors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
    }

    // ── Population State ──────────────────────────────────────────────

    /**
     * Per-population mutable evaluation state. Holds subject membership, evaluated resources,
     * and aggregation results that were previously on {@link PopulationDef}.
     */
    public static class PopulationState {

        private final PopulationDef def;
        private final Map<String, Set<Object>> subjectResources = new HashMap<>();
        private Set<Object> evaluatedResources;

        @Nullable
        private Double aggregationResult;

        PopulationState(PopulationDef def) {
            this.def = def;
        }

        /** Add a resource for a subject. Creates the subject entry if absent. */
        public void addResource(String key, Object value) {
            subjectResources
                    .computeIfAbsent(key, k -> new HashSetForFhirResourcesAndCqlTypes<>())
                    .add(value);
        }

        public Map<String, Set<Object>> getSubjectResources() {
            return subjectResources;
        }

        public Set<String> getSubjects() {
            return subjectResources.keySet();
        }

        public Set<Object> getResourcesForSubject(String subjectId) {
            return subjectResources.getOrDefault(subjectId, new HashSetForFhirResourcesAndCqlTypes<>());
        }

        public Set<Object> getEvaluatedResources() {
            if (evaluatedResources == null) {
                evaluatedResources = new HashSetForFhirResourcesAndCqlTypes<>();
            }
            return evaluatedResources;
        }

        /**
         * All resources across all subjects, preserving duplicates per-subject.
         */
        public List<Object> getAllSubjectResources() {
            return subjectResources.values().stream()
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .toList();
        }

        /** Count observation resources (nested maps). */
        public int countObservations() {
            return getAllSubjectResources().stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .mapToInt(Map::size)
                    .sum();
        }

        /**
         * Compute the count for this population, taking population basis and type into account.
         */
        public int getCount() {
            if (def.isBooleanBasis()) {
                return getSubjects().size();
            } else {
                if (def.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    return countObservations();
                }
                return getAllSubjectResources().size();
            }
        }

        // ── Set algebra operations ────────────────────────────────

        public void retainAllResources(String subjectId, PopulationState other) {
            getResourcesForSubject(subjectId).retainAll(other.getResourcesForSubject(subjectId));
        }

        public void retainAllSubjects(PopulationState other) {
            getSubjects().retainAll(other.getSubjects());
        }

        public void removeAllResources(String subjectId, PopulationState other) {
            getResourcesForSubject(subjectId).removeAll(other.getResourcesForSubject(subjectId));
        }

        public void removeAllSubjects(PopulationState other) {
            getSubjects().removeAll(other.getSubjects());
        }

        /**
         * Removes a measure observation resource key from all inner maps for a subject.
         * After removal, any empty inner maps are removed. If the subject has no remaining
         * observations, the subject is also removed.
         */
        public void removeExcludedMeasureObservationResource(String subjectId, Object measureObservationResourceKey) {
            if (!def.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                return;
            }
            final Set<Object> resourcesForSubject = subjectResources.get(subjectId);
            if (resourcesForSubject == null) {
                return;
            }
            resourcesForSubject.forEach(element -> {
                if (element instanceof Map<?, ?> innerMap) {
                    innerMap.remove(measureObservationResourceKey);
                }
            });
            resourcesForSubject.removeIf(element -> element instanceof Map<?, ?> m && m.isEmpty());
            if (resourcesForSubject.isEmpty()) {
                subjectResources.remove(subjectId);
            }
        }

        // ── Aggregation ───────────────────────────────────────────

        @Nullable
        public Double getAggregationResult() {
            return aggregationResult;
        }

        public void setAggregationResult(@Nullable QuantityDef quantityDefResult) {
            setAggregationResult(Optional.ofNullable(quantityDefResult)
                    .map(QuantityDef::value)
                    .orElse(null));
        }

        public void setAggregationResult(@Nullable Double aggregationResult) {
            this.aggregationResult = aggregationResult;
        }

        @Nullable
        public ContinuousVariableObservationAggregateMethod getAggregateMethod() {
            return def.getAggregateMethod();
        }
    }

    // ── Group State ───────────────────────────────────────────────────

    /** Per-group mutable state. Holds the computed score. */
    public static class GroupState {

        @Nullable
        private Double score;

        @Nullable
        public Double getScore() {
            return score;
        }

        public void setScore(@Nullable Double score) {
            this.score = score;
        }
    }

    // ── Stratifier State ──────────────────────────────────────────────

    /** Per-stratifier mutable state. Holds evaluation results and computed strata. */
    public static class StratifierState {

        private final Map<String, CriteriaResult> results = new HashMap<>();
        private final List<StratumDef> strata = new ArrayList<>();

        public Map<String, CriteriaResult> getResults() {
            return results;
        }

        public void putResult(String subject, Object value, Set<Object> evaluatedResources) {
            results.put(
                    subject, new CriteriaResult(value, new HashSetForFhirResourcesAndCqlTypes<>(evaluatedResources)));
        }

        public List<StratumDef> getStrata() {
            return strata;
        }

        public void addAllStrata(List<StratumDef> stratumDefs) {
            strata.addAll(stratumDefs);
        }
    }

    // ── Stratifier Component State ────────────────────────────────────

    /** Per-component mutable state. Holds evaluation results. */
    public static class StratifierComponentState {

        private final Map<String, CriteriaResult> results = new HashMap<>();

        public Map<String, CriteriaResult> getResults() {
            return results;
        }

        public void putResult(String subject, Object value, Set<Object> evaluatedResources) {
            results.put(
                    subject, new CriteriaResult(value, new HashSetForFhirResourcesAndCqlTypes<>(evaluatedResources)));
        }
    }

    // ── SDE State ─────────────────────────────────────────────────────

    /** Per-SDE mutable state. Holds evaluation results. */
    public static class SdeState {

        private final Map<String, CriteriaResult> results = new HashMap<>();

        public Map<String, CriteriaResult> getResults() {
            return results;
        }

        public void putResult(String subject, Object value, Set<Object> evaluatedResources) {
            results.put(subject, new CriteriaResult(value, evaluatedResources));
        }
    }

    // ── Supporting Evidence State ─────────────────────────────────────

    /** Per-supporting-evidence mutable state. Holds subject resources. */
    public static class SupportingEvidenceState {

        private final Map<String, Set<Object>> subjectResources = new HashMap<>();

        public Map<String, Set<Object>> getSubjectResources() {
            return subjectResources;
        }

        public Set<Object> getResourcesForSubject(String subjectId) {
            return subjectResources.getOrDefault(subjectId, new HashSetForFhirResourcesAndCqlTypes<>());
        }

        public void addResource(String key, Object value) {
            subjectResources
                    .computeIfAbsent(key, k -> new HashSetForFhirResourcesAndCqlTypes<>())
                    .add(value);
        }
    }
}
