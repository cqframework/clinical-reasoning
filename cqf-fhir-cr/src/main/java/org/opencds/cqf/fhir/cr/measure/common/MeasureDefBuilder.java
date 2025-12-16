package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.FHIR_ALL_TYPES_SYSTEM_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.opencds.cqf.fhir.cr.measure.common.def.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.def.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.def.measure.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.def.measure.PopulationDef;

public interface MeasureDefBuilder<MeasureT> {
    MeasureDef build(MeasureT measure);

    /**
     * Merges a list of populations with an optional additional population definition.
     * This is version-agnostic as it only operates on PopulationDef objects.
     */
    // Moved from R4MeasureDefBuilder by Claude Sonnet 4.5 - version-agnostic utility
    default List<PopulationDef> mergePopulations(
            List<PopulationDef> populationsWithCriteriaReference, @Nullable PopulationDef populationDef) {

        final Builder<PopulationDef> immutableListBuilder = ImmutableList.builder();

        immutableListBuilder.addAll(populationsWithCriteriaReference);

        Optional.ofNullable(populationDef).ifPresent(immutableListBuilder::add);

        return immutableListBuilder.build();
    }

    /**
     * Searches for a population with a specific code in a list of population definitions.
     * This is version-agnostic as it only operates on PopulationDef and MeasurePopulationType.
     */
    // Moved from R4MeasureDefBuilder by Claude Sonnet 4.5 - version-agnostic helper
    default PopulationDef checkPopulationForCode(
            List<PopulationDef> populations, MeasurePopulationType measurePopType) {
        return populations.stream()
                .filter(e -> e.code().first().code().equals(measurePopType.toCode()))
                .findAny()
                .orElse(null);
    }

    /**
     * Creates a ConceptDef for a given MeasurePopulationType.
     * This is version-agnostic as it only operates on common classes.
     */
    // Moved from R4MeasureDefBuilder by Claude Sonnet 4.5 - version-agnostic helper
    default ConceptDef totalConceptDefCreator(MeasurePopulationType measurePopulationType) {
        return new ConceptDef(
                Collections.singletonList(
                        new CodeDef(measurePopulationType.getSystem(), measurePopulationType.toCode())),
                null);
    }

    /**
     * Determines the scoring definition for a measure group.
     * Validates that at least one scoring is specified and returns the group-level scoring if present,
     * otherwise the measure-level scoring.
     */
    // Moved from R4MeasureDefBuilder by Claude Sonnet 4.5 - version-agnostic validation
    default MeasureScoring getScoringDef(
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
     * Determines the population basis definition.
     * Returns the group-level basis if present, otherwise the measure-level basis,
     * or defaults to "boolean" if neither is specified.
     */
    // Moved from R4MeasureDefBuilder by Claude Sonnet 4.5 - version-agnostic helper
    default CodeDef getPopulationBasisDef(@Nullable CodeDef measureBasis, @Nullable CodeDef groupBasis) {
        if (measureBasis == null && groupBasis == null) {
            // default basis, if not defined
            return new CodeDef(FHIR_ALL_TYPES_SYSTEM_URL, "boolean");
        }
        return defaultCodeDef(groupBasis, measureBasis);
    }

    /**
     * Determines the improvement notation for a measure group.
     * Returns the group-level notation if present, otherwise the measure-level notation,
     * or defaults to "increase" if neither is specified.
     */
    // Moved from R4MeasureDefBuilder by Claude Sonnet 4.5 - version-agnostic helper
    default CodeDef getImprovementNotation(@Nullable CodeDef measureImpNotation, @Nullable CodeDef groupImpNotation) {
        if (measureImpNotation == null && groupImpNotation == null) {
            // default Improvement Notation, if not defined
            return new CodeDef(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_INCREASE);
        }
        return defaultCodeDef(groupImpNotation, measureImpNotation);
    }

    /**
     * Returns the first code if present, otherwise returns the default code.
     * This is a pure utility method that is version-agnostic.
     */
    // Moved from R4MeasureDefBuilder by Claude Sonnet 4.5 - version-agnostic utility
    default CodeDef defaultCodeDef(@Nullable CodeDef code, @Nullable CodeDef codeDefault) {
        if (code != null) {
            return code;
        }
        return codeDefault;
    }

    /**
     * Validates a measure scoring code and returns the corresponding MeasureScoring enum.
     * This is version-agnostic validation logic.
     */
    // Moved from R4MeasureDefBuilder by Claude Sonnet 4.5 - version-agnostic validation
    default MeasureScoring getMeasureScoring(String measureUrl, @Nullable String scoringCode) {
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
     * Validates an improvement notation code against the expected system and valid codes.
     * This is version-agnostic validation logic.
     */
    // Moved from R4MeasureDefBuilder by Claude Sonnet 4.5 - version-agnostic validation
    default void validateImprovementNotationCode(String measureUrl, CodeDef improvementNotation) {
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
}
