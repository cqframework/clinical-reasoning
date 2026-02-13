package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.FHIR_ALL_TYPES_SYSTEM_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Element;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefBuilder;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

public class Dstu3MeasureDefBuilder implements MeasureDefBuilder<Measure> {
    @Override
    public MeasureDef build(Measure measure) {
        checkId(measure);

        // Validate unique population IDs within each group
        validateUniquePopulationIds(measure);

        // SDES
        List<SdeDef> sdes = new ArrayList<>();
        for (MeasureSupplementalDataComponent s : measure.getSupplementalData()) {
            checkId(s);
            SdeDef sdeDef = new SdeDef(
                    s.getId(),
                    null, // No code on sde in dstu3
                    s.getCriteria());
            sdes.add(sdeDef);
        }

        // scoring
        @Nullable
        final MeasureScoring measureScoring =
                MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode());
        // populationBasis
        var measureBasis = getMeasureBasis(measure);
        // default value is true
        // improvement Notation
        var measureImpNotation = getMeasureImprovementNotation(measure);

        // The group size check here is to ensure that there's parity in the behavior of builder
        // between DSTU3 and R4. In R4, scoring can be on the group level so if we have an
        // empty measure we simply generate an empty MeasureReport.
        // This might not be the best behavior, but we want to ensure that the behavior is the same
        // between versions
        if (measureScoring == null && measure.hasGroup()) {
            throw new InvalidRequestException(
                    "MeasureScoring must be specified on Measure: %s".formatted(measure.getUrl()));
        }
        List<GroupDef> groups = new ArrayList<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            // Populations
            List<PopulationDef> populations = new ArrayList<>();
            var populationBasisDef = getPopulationBasisDef(measureBasis);
            for (MeasureGroupPopulationComponent pop : group.getPopulation()) {
                checkId(pop);
                var populationType = MeasurePopulationType.fromCode(
                        pop.getCode().getCodingFirstRep().getCode());

                populations.add(new PopulationDef(
                        pop.getId(),
                        conceptToConceptDef(pop.getCode()),
                        populationType,
                        pop.getCriteria(),
                        populationBasisDef,
                        null));
            }

            // Stratifiers
            List<StratifierDef> stratifiers = new ArrayList<>();
            for (MeasureGroupStratifierComponent mgsc : group.getStratifier()) {
                checkId(mgsc);
                var stratifierDef = new StratifierDef(
                        mgsc.getId(),
                        null, // No code on stratifier
                        // in dstu3
                        mgsc.getCriteria(),
                        // There's no such thing as a stratifier component in DSTU3, so hard-code to VALUE
                        MeasureStratifierType.VALUE);

                stratifiers.add(stratifierDef);
            }
            var groupDef = new GroupDef(
                    group.getId(),
                    null, // No code on group in dstu3
                    stratifiers,
                    populations,
                    measureScoring,
                    false, // no group scoring
                    getImprovementNotation(measureImpNotation),
                    populationBasisDef);
            groups.add(groupDef);
        }

        return new MeasureDef(
                measure.getIdElement(), measure.getUrl(), measure.getVersion(), measureScoring, groups, sdes);
    }

    private ConceptDef conceptToConceptDef(CodeableConcept codeable) {
        if (codeable == null) {
            return null;
        }

        List<CodeDef> codes = new ArrayList<>();
        for (var c : codeable.getCoding()) {
            codes.add(codeToCodeDef(c));
        }

        return new ConceptDef(codes, codeable.getText());
    }

    private CodeDef codeToCodeDef(Coding coding) {
        return new CodeDef(coding.getSystem(), coding.getVersion(), coding.getCode(), coding.getDisplay());
    }

    private void checkId(Element e) {
        if (e.getId() == null || StringUtils.isBlank(e.getId())) {
            throw new InvalidRequestException("id is required on all Elements of type: " + e.fhirType());
        }
    }

    private void checkId(Resource r) {
        if (r.getId() == null || StringUtils.isBlank(r.getId())) {
            throw new InvalidRequestException("id is required on all Resources of type: " + r.fhirType());
        }
    }

    /**
     * Validates that all population IDs within each group are unique.
     *
     * @param measure the Measure to validate
     * @throws InvalidRequestException if duplicate population IDs exist within a group
     */
    private void validateUniquePopulationIds(Measure measure) {
        String measureIdentifier = measure.hasUrl() ? measure.getUrl() : measure.getId();

        for (int groupIndex = 0; groupIndex < measure.getGroup().size(); groupIndex++) {
            MeasureGroupComponent group = measure.getGroup().get(groupIndex);
            String groupIdentifier = group.hasId() ? group.getId() : "group-" + (groupIndex + 1);

            Set<String> seenPopulationIds = new HashSet<>();

            for (MeasureGroupPopulationComponent population : group.getPopulation()) {
                String populationId = population.getId();

                // Skip null/blank IDs - they will be caught by checkId() validation
                if (populationId == null || populationId.isBlank()) {
                    continue;
                }

                if (!seenPopulationIds.add(populationId)) {
                    throw new InvalidRequestException("Duplicate population ID '%s' found in %s of Measure: %s"
                            .formatted(populationId, groupIdentifier, measureIdentifier));
                }
            }
        }
    }

    private void validateImprovementNotationCode(Measure measure, CodeDef improvementNotation) {
        if (improvementNotation != null) {
            var code = improvementNotation.code();
            boolean hasValidCode = IMPROVEMENT_NOTATION_SYSTEM_INCREASE.equals(code)
                    || IMPROVEMENT_NOTATION_SYSTEM_DECREASE.equals(code);
            if (!hasValidCode) {
                throw new IllegalArgumentException(
                        "ImprovementNotation Coding has invalid code: %s, combination for Measure: %s"
                                .formatted(code, measure.getUrl()));
            }
        }
    }

    public CodeDef getMeasureBasis(Measure measure) {

        var ext = measure.getExtensionByUrl(MeasureConstants.POPULATION_BASIS_URL);
        // check for population-basis Extension, assume boolean if no Extension is found
        if (ext != null) {
            return new CodeDef(
                    MeasureConstants.FHIR_ALL_TYPES_SYSTEM_URL, ext.getValue().toString());
        }
        return null;
    }

    public CodeDef getMeasureImprovementNotation(Measure measure) {
        if (measure.hasImprovementNotation()) {
            var impNot = new CodeDef(null, measure.getImprovementNotation());
            validateImprovementNotationCode(measure, impNot);
            // Dstu3 only has a string defined for improvementNotation
            return impNot;
        }
        return null;
    }

    private CodeDef getPopulationBasisDef(CodeDef measureBasis) {
        // default basis, if not defined
        if (measureBasis != null) {
            return measureBasis;
        } else {
            return new CodeDef(FHIR_ALL_TYPES_SYSTEM_URL, "boolean");
        }
    }

    private CodeDef getImprovementNotation(CodeDef measureImpNotation) {
        return Objects.requireNonNullElseGet(
                measureImpNotation, () -> new CodeDef(null, IMPROVEMENT_NOTATION_SYSTEM_INCREASE));
    }
}
