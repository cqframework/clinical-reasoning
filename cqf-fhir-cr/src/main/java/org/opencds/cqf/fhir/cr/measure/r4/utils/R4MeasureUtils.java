package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_SCORING_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQFM_AGGREGATE_METHOD_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

/**
 * Utility class for extracting data from R4 FHIR Measure resources.
 *
 * <p>This class provides static helper methods for working with Measure resources, including:
 * <ul>
 *   <li>Extracting measure scoring (Measure-level and Group-level)</li>
 *   <li>Extracting improvement notation</li>
 *   <li>Matching measure groups</li>
 * </ul>
 *
 * <p>All methods are static and designed to be reusable across different measure evaluation contexts.
 */
public class R4MeasureUtils {

    private R4MeasureUtils() {
        // Utility class
    }

    /**
     * Extract MeasureScoring from Measure.scoring.
     *
     * @param measure the Measure resource
     * @return the MeasureScoring enum value
     * @throws InvalidRequestException if scoring code is invalid
     */
    @Nullable
    public static MeasureScoring getMeasureScoring(Measure measure) {
        var scoringCode = measure.getScoring().getCodingFirstRep().getCode();
        return getMeasureScoring(measure.getUrl(), scoringCode);
    }

    /**
     * Parse and validate a measure scoring code string.
     *
     * @param measureUrl the measure URL (for error messages)
     * @param scoringCode the scoring code to parse
     * @return the MeasureScoring enum value, or null if scoringCode is null
     * @throws InvalidRequestException if scoring code is invalid
     */
    @Nullable
    public static MeasureScoring getMeasureScoring(String measureUrl, @Nullable String scoringCode) {
        if (scoringCode != null) {
            var code = MeasureScoring.fromCode(scoringCode);
            if (code == null) {
                throw new InvalidRequestException(
                        "Measure Scoring code: %s, is not a valid Measure Scoring Type for measure: %s."
                                .formatted(scoringCode, measureUrl));
            } else {
                return code;
            }
        }
        return null;
    }

    /**
     * Extract group-level measure scoring from the cqfm-scoring extension.
     *
     * @param measureUrl the measure URL (for error messages)
     * @param measureGroup the MeasureGroupComponent
     * @return the group-level MeasureScoring, or null if no extension present
     * @throws InvalidRequestException if scoring code is invalid
     */
    @Nullable
    public static MeasureScoring getGroupMeasureScoring(String measureUrl, MeasureGroupComponent measureGroup) {
        final Extension ext = measureGroup.getExtensionByUrl(CQFM_SCORING_EXT_URL);

        if (ext != null) {
            final Type extVal = ext.getValue();
            if (extVal instanceof CodeableConcept coding) {
                final String scoringCode = coding.getCodingFirstRep().getCode();
                final MeasureScoring groupMeasureScoring = MeasureScoring.fromCode(scoringCode);

                if (groupMeasureScoring == null) {
                    throw new InvalidRequestException(
                            "Measure Scoring code: %s, is not a valid Measure Scoring Type for measure: %s."
                                    .formatted(scoringCode, measureUrl));
                }

                return groupMeasureScoring;
            }
        }

        return null;
    }

    public static MeasureScoring computeScoring(Measure measure, MeasureGroupComponent measureGroup) {
        return computeScoring(
                measure.getUrl(), getMeasureScoring(measure), getGroupMeasureScoring(measure.getUrl(), measureGroup));
    }

    /**
     * Determines the scoring definition for a measure group.
     * Validates that at least one scoring is specified and returns the group-level scoring if present,
     * otherwise the measure-level scoring.
     */
    public static MeasureScoring computeScoring(
            String measureUrl, MeasureScoring measureScoring, MeasureScoring groupScoring) {
        if (groupScoring == null && measureScoring == null) {
            throw new InvalidRequestException(
                    "MeasureScoring must be specified on Group or Measure for Measure: " + measureUrl);
        }
        if (groupScoring != null) {
            return groupScoring;
        }
        return measureScoring;
    }

    /**
     * Extract group-level measure scoring, with convenience overload.
     *
     * @param measure the Measure resource (for URL)
     * @param measureGroup the MeasureGroupComponent
     * @return the group-level MeasureScoring, or null if no extension present
     */
    @Nullable
    public static MeasureScoring getGroupMeasureScoring(Measure measure, MeasureGroupComponent measureGroup) {
        return getGroupMeasureScoring(measure.getUrl(), measureGroup);
    }

    /**
     * Extract group-level improvement notation from extension.
     *
     * @param measure the Measure resource (for URL/validation)
     * @param measureGroup the MeasureGroupComponent
     * @return the CodeDef representing the improvement notation, or null if not present
     * @throws InvalidRequestException if the improvement notation code is invalid
     */
    @Nullable
    public static CodeDef getGroupImprovementNotation(Measure measure, MeasureGroupComponent measureGroup) {
        var ext = measureGroup.getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION);
        if (ext != null) {
            var value = ext.getValue();
            if (value instanceof CodeableConcept coding) {
                var codeDef = new CodeDef(
                        coding.getCodingFirstRep().getSystem(),
                        coding.getCodingFirstRep().getCode());
                validateImprovementNotationCode(measure.getUrl(), codeDef);
                return codeDef;
            }
        }
        return null;
    }

    /**
     * Check if the improvement notation represents "increase".
     *
     * @param improvementNotation the CodeDef to check (may be null)
     * @return true if the code is "increase", or true if improvementNotation is null (default)
     */
    public static boolean isIncreaseImprovementNotation(@Nullable CodeDef improvementNotation) {
        if (improvementNotation != null) {
            return improvementNotation.code().equals("increase");
        } else {
            // default response if null
            return true;
        }
    }

    /**
     * Validate that an improvement notation code has a valid system and code.
     *
     * @param measureUrl the measure URL (for error messages)
     * @param improvementNotation the CodeDef to validate
     * @throws InvalidRequestException if the system or code is invalid
     */
    public static void validateImprovementNotationCode(String measureUrl, CodeDef improvementNotation) {
        var code = improvementNotation.code();
        var system = improvementNotation.system();
        boolean hasValidSystem = system.equals(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM);
        boolean hasValidCode =
                IMPROVEMENT_NOTATION_SYSTEM_INCREASE.equals(code) || IMPROVEMENT_NOTATION_SYSTEM_DECREASE.equals(code);
        if (!hasValidCode || !hasValidSystem) {
            throw new InvalidRequestException(
                    "ImprovementNotation Coding has invalid System: %s, code: %s, combination for Measure: %s"
                            .formatted(system, code, measureUrl));
        }
    }

    /**
     * Find a MeasureGroupComponent by matching with a MeasureReportGroupComponent.
     * Matches by ID if present, otherwise returns null.
     *
     * @param measure the Measure resource
     * @param reportGroup the MeasureReportGroupComponent to match
     * @return the matching MeasureGroupComponent, or null if not found
     */
    @Nullable
    public static MeasureGroupComponent getMeasureGroup(Measure measure, MeasureReportGroupComponent reportGroup) {
        if (reportGroup.getId() == null) {
            return null;
        }
        return measure.getGroup().stream()
                .filter(measureGroup -> Objects.nonNull(measureGroup.getId()))
                .filter(measureGroup -> measureGroup.getId().equals(reportGroup.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find a MeasureGroupComponent by ID.
     *
     * @param measure the Measure resource
     * @param groupId the group ID to match
     * @return the matching MeasureGroupComponent, or null if not found
     */
    @Nullable
    public static MeasureGroupComponent getMeasureGroup(Measure measure, String groupId) {
        if (groupId == null) {
            return null;
        }
        return measure.getGroup().stream()
                .filter(measureGroup -> Objects.nonNull(measureGroup.getId()))
                .filter(measureGroup -> measureGroup.getId().equals(groupId))
                .findFirst()
                .orElse(null);
    }

    public static ContinuousVariableObservationAggregateMethod getAggregateMethod(
            String measureUrl, @Nullable MeasureGroupPopulationComponent groupPopulation) {

        if (groupPopulation == null) {
            return ContinuousVariableObservationAggregateMethod.N_A;
        }

        var aggMethodExt = groupPopulation.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        if (aggMethodExt != null) {
            // this method is only required if scoringType = continuous-variable or Ratio Continuous variable
            var aggregateMethodString = aggMethodExt.getValue().toString();

            var aggregateMethod = ContinuousVariableObservationAggregateMethod.fromString(aggregateMethodString);

            // check that method is accepted
            if (aggregateMethod == null) {
                throw new InvalidRequestException("Aggregation method: %s is not a valid value for Measure: %s"
                        .formatted(aggregateMethodString, measureUrl));
            }

            return aggregateMethod;
        }

        return ContinuousVariableObservationAggregateMethod.N_A;
    }

    /**
     * Determine if a measure group is ratio continuous variable scoring.
     *
     * @param scoring the MeasureScoring for the group
     * @param measureGroup the MeasureGroupComponent
     * @return true if the group is ratio continuous variable
     */
    public static boolean isRatioContinuousVariable(MeasureScoring scoring, MeasureGroupComponent measureGroup) {
        if (scoring != MeasureScoring.RATIO) {
            return false;
        }

        // Check if any measure observation populations have an aggregate method extension
        return getMeasureObservationPopulations(measureGroup).stream()
                .anyMatch(pop -> pop.hasExtension(EXT_CQFM_AGGREGATE_METHOD_URL));
    }

    /**
     * Get all MEASURE_OBSERVATION populations from a measure group.
     *
     * @param measureGroup the MeasureGroupComponent
     * @return list of MEASURE_OBSERVATION populations
     */
    public static List<MeasureGroupPopulationComponent> getMeasureObservationPopulations(
            MeasureGroupComponent measureGroup) {
        return getPopulationsByType(measureGroup, MeasurePopulationType.MEASUREOBSERVATION);
    }

    /**
     * Get populations of a specific type from a measure group.
     *
     * @param measureGroup the MeasureGroupComponent
     * @param populationType the MeasurePopulationType to filter by
     * @return list of populations of the specified type
     */
    public static List<MeasureGroupPopulationComponent> getPopulationsByType(
            MeasureGroupComponent measureGroup, MeasurePopulationType populationType) {
        return measureGroup.getPopulation().stream()
                .filter(pop -> populationType
                        .toCode()
                        .equals(pop.getCode().getCodingFirstRep().getCode()))
                .toList();
    }

    /**
     * Extract the criteria reference from a population.
     *
     * @param population the MeasureGroupPopulationComponent
     * @return the criteria reference value, or null if not present
     */
    @Nullable
    public static String getCriteriaReferenceFromPopulation(MeasureGroupPopulationComponent population) {
        var ext = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        if (ext != null && ext.getValue() instanceof org.hl7.fhir.r4.model.StringType stringType) {
            return stringType.getValue();
        }
        return null;
    }

    /**
     * Check if a criteria reference matches a specific population type.
     *
     * @param criteriaReference the criteria reference string
     * @param populationType the MeasurePopulationType to check against
     * @return true if the criteria reference matches the population type ID
     */
    public static boolean criteriaReferenceMatches(String criteriaReference, MeasurePopulationType populationType) {
        if (criteriaReference == null || populationType == null) {
            return false;
        }
        // Common pattern: criteria references use lowercase IDs like "numerator", "denominator"
        return criteriaReference.equalsIgnoreCase(populationType.toCode());
    }

    /**
     * Determines whether the population basis is BOOLEAN.
     *
     * <p>A BOOLEAN population basis indicates subject-level (boolean) evaluation
     * rather than resource-based evaluation.
     *
     * @param populationBasisDef the population basis definition (must not be null)
     * @return true if the population basis code is BOOLEAN, false otherwise
     */
    public static boolean isBooleanPopulationBasis(CodeDef populationBasisDef) {
        return populationBasisDef != null && FHIRAllTypes.BOOLEAN.toCode().equals(populationBasisDef.code());
    }
}
