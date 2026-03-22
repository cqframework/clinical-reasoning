package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.FHIR_ALL_TYPES_SYSTEM_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Static utility methods extracted from the MeasureDefBuilder interface.
 * These are implementation helpers used by version-specific builders (primarily R4),
 * not part of the builder contract.
 */
public final class MeasureDefBuilders {

    private MeasureDefBuilders() {}

    /**
     * Merges a list of populations with an optional additional population definition.
     */
    public static List<PopulationDef> mergePopulations(
            List<PopulationDef> populationsWithCriteriaReference, @Nullable PopulationDef populationDef) {

        final Builder<PopulationDef> immutableListBuilder = ImmutableList.builder();

        immutableListBuilder.addAll(populationsWithCriteriaReference);

        Optional.ofNullable(populationDef).ifPresent(immutableListBuilder::add);

        return immutableListBuilder.build();
    }

    /**
     * Searches for a population with a specific code in a list of population definitions.
     */
    public static PopulationDef checkPopulationForCode(
            List<PopulationDef> populations, MeasurePopulationType measurePopType) {
        return populations.stream()
                .filter(e -> e.code().first().code().equals(measurePopType.toCode()))
                .findAny()
                .orElse(null);
    }

    /**
     * Creates a ConceptDef for a given MeasurePopulationType.
     */
    public static ConceptDef totalConceptDefCreator(MeasurePopulationType measurePopulationType) {
        return new ConceptDef(
                Collections.singletonList(
                        new CodeDef(measurePopulationType.getSystem(), measurePopulationType.toCode())),
                null);
    }

    /**
     * Determines the population basis definition.
     * Returns the group-level basis if present, otherwise the measure-level basis,
     * or defaults to "boolean" if neither is specified.
     */
    public static CodeDef getPopulationBasisDef(@Nullable CodeDef measureBasis, @Nullable CodeDef groupBasis) {
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
    public static CodeDef getImprovementNotation(
            @Nullable CodeDef measureImpNotation, @Nullable CodeDef groupImpNotation) {
        if (measureImpNotation == null && groupImpNotation == null) {
            // default Improvement Notation, if not defined
            return new CodeDef(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_INCREASE);
        }
        return defaultCodeDef(groupImpNotation, measureImpNotation);
    }

    /**
     * Returns the first code if present, otherwise returns the default code.
     */
    public static CodeDef defaultCodeDef(@Nullable CodeDef code, @Nullable CodeDef codeDefault) {
        if (code != null) {
            return code;
        }
        return codeDefault;
    }
}
