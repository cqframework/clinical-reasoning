package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOREXCEPTION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOREXCLUSION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.INITIALPOPULATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATIONEXCLUSION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.NUMERATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.NUMERATOREXCLUSION;

/**
 * Applies set-algebra rules that enforce population subset and exclusion constraints
 * during measure evaluation. These operations implement the HQMF / QM IG rules:
 * <ul>
 *   <li>Proportion/Ratio: D ⊆ IP, N ⊆ D, plus DX/NX/DE exclusion/exception removal</li>
 *   <li>Continuous Variable: MP ⊆ IP, MPE ⊆ MP</li>
 * </ul>
 */
public final class PopulationSetAlgebra {
    private PopulationSetAlgebra() {}

    /**
     * Applies the subset enforcement and exclusion/exception rules for
     * Proportion and Ratio scoring types.
     *
     * <p>Precondition: population membership evaluation has already been performed
     * for all relevant populations (IP, D, N, DX, NX, DE).
     */
    public static void applyProportionRules(
            String subjectId, GroupDef groupDef, boolean applyScoring, MeasureEvaluationState state) {

        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        PopulationDef numerator = groupDef.getSingle(NUMERATOR);
        PopulationDef denominator = groupDef.getSingle(DENOMINATOR);
        PopulationDef denominatorExclusion = groupDef.getSingle(DENOMINATOREXCLUSION);
        PopulationDef denominatorException = groupDef.getSingle(DENOMINATOREXCEPTION);
        PopulationDef numeratorExclusion = groupDef.getSingle(NUMERATOREXCLUSION);

        if (applyScoring) {
            // remove denominator values not in IP
            state.population(denominator).retainAllResources(subjectId, state.population(initialPopulation));
            state.population(denominator).retainAllSubjects(state.population(initialPopulation));
            // remove numerator values if not in Denominator
            state.population(numerator).retainAllSubjects(state.population(denominator));
            state.population(numerator).retainAllResources(subjectId, state.population(denominator));
        }

        // Apply Exclusions and Exceptions
        if (groupDef.isBooleanBasis()) {
            // Remove Subject and Resource Exclusions
            if (denominatorExclusion != null && applyScoring) {
                // numerator should not include den-exclusions
                state.population(numerator).removeAllSubjects(state.population(denominatorExclusion));

                // verify exclusion results are found in denominator
                state.population(denominatorExclusion).retainAllResources(subjectId, state.population(denominator));
                state.population(denominatorExclusion).retainAllSubjects(state.population(denominator));
            }
            if (numeratorExclusion != null && applyScoring) {
                // verify results are in Numerator
                state.population(numeratorExclusion).retainAllResources(subjectId, state.population(numerator));
                state.population(numeratorExclusion).retainAllSubjects(state.population(numerator));
            }
            if (denominatorException != null && applyScoring) {
                // Remove Subjects Exceptions that are present in Numerator
                state.population(denominatorException).removeAllSubjects(state.population(numerator));
                state.population(denominatorException).removeAllResources(subjectId, state.population(numerator));

                // verify exception results are found in denominator
                state.population(denominatorException).retainAllResources(subjectId, state.population(denominator));
                state.population(denominatorException).retainAllSubjects(state.population(denominator));
            }
        } else {
            // Remove Only Resource Exclusions
            // * Multiple resources can be from one subject and represented in multiple populations
            // * This is why we only remove resources and not subjects too for `Resource Basis`.
            if (denominatorExclusion != null && applyScoring) {
                // remove any denominator-exclusion subjects/resources found in Numerator
                state.population(numerator).removeAllResources(subjectId, state.population(denominatorExclusion));
                // verify exclusion results are found in denominator
                state.population(denominatorExclusion).retainAllResources(subjectId, state.population(denominator));
            }
            if (numeratorExclusion != null && applyScoring) {
                // verify exclusion results are found in numerator results, otherwise remove
                state.population(numeratorExclusion).retainAllResources(subjectId, state.population(numerator));
            }
            if (denominatorException != null && applyScoring) {
                // Remove Resource Exceptions that are present in Numerator
                state.population(denominatorException).removeAllResources(subjectId, state.population(numerator));
                // verify exception results are found in denominator
                state.population(denominatorException).retainAllResources(subjectId, state.population(denominator));
            }
        }
    }

    /**
     * Applies the subset enforcement rules for Continuous Variable scoring type.
     *
     * <p>Precondition: population membership evaluation has already been performed
     * for IP, MP, and optionally MPE.
     */
    public static void applyContinuousVariableRules(
            String subjectId, GroupDef groupDef, boolean applyScoring, MeasureEvaluationState state) {

        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        PopulationDef measurePopulation = groupDef.getSingle(MEASUREPOPULATION);
        PopulationDef measurePopulationExclusion = groupDef.getSingle(MEASUREPOPULATIONEXCLUSION);

        if (measurePopulation != null && initialPopulation != null && applyScoring) {
            // verify initial-population are in measure-population
            state.population(measurePopulation).retainAllResources(subjectId, state.population(initialPopulation));
            state.population(measurePopulation).retainAllSubjects(state.population(initialPopulation));
        }

        if (measurePopulationExclusion != null && applyScoring && measurePopulation != null) {
            // verify exclusions are in measure-population
            state.population(measurePopulationExclusion)
                    .retainAllResources(subjectId, state.population(measurePopulation));
            state.population(measurePopulationExclusion).retainAllSubjects(state.population(measurePopulation));
        }
    }
}
