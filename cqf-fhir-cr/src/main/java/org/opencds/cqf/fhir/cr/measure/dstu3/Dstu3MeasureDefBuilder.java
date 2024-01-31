package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.TOTALDENOMINATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.TOTALNUMERATOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class Dstu3MeasureDefBuilder implements MeasureDefBuilder<Measure> {

    private final boolean enforceIds;

    public Dstu3MeasureDefBuilder() {
        this(false);
    }

    public Dstu3MeasureDefBuilder(boolean enforceIds) {
        this.enforceIds = enforceIds;
    }

    @Override
    public MeasureDef build(Measure measure) {
        checkId(measure);

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

        // Groups
        MeasureScoring groupMeasureScoringCode = getMeasureScoring(measure);
        if (groupMeasureScoringCode == null) {
            throw new IllegalStateException("MeasureScoring must be specified on Measure");
        }
        List<GroupDef> groups = new ArrayList<>();
        Map<GroupDef, MeasureScoring> groupMeasureScoring = new HashMap<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            checkId(group);

            // Populations
            List<PopulationDef> populations = new ArrayList<>();
            for (MeasureGroupPopulationComponent pop : group.getPopulation()) {
                checkId(pop);
                var populationType = MeasurePopulationType.fromCode(
                        pop.getCode().getCodingFirstRep().getCode());

                populations.add(new PopulationDef(
                        pop.getId(), conceptToConceptDef(pop.getCode()), populationType, pop.getCriteria()));
            }
            // total Denominator/Numerator Def Builder
            // validate population is not in Def
            if (checkPopulationForCode(populations, TOTALDENOMINATOR) == null) {
                // add to definition
                populations.add(new PopulationDef(
                        "totalDenominator", totalConceptDefCreator(TOTALDENOMINATOR), TOTALDENOMINATOR, null));
            }
            if (checkPopulationForCode(populations, TOTALNUMERATOR) == null) {
                // add to definition
                populations.add(new PopulationDef(
                        "totalNumerator", totalConceptDefCreator(TOTALNUMERATOR), TOTALNUMERATOR, null));
            }

            // Stratifiers
            List<StratifierDef> stratifiers = new ArrayList<>();
            for (MeasureGroupStratifierComponent mgsc : group.getStratifier()) {
                checkId(mgsc);
                var stratifierDef = new StratifierDef(
                        mgsc.getId(),
                        null, // No code on stratifier
                        // in dstu3
                        mgsc.getCriteria());

                stratifiers.add(stratifierDef);
            }
            var groupDef = new GroupDef(
                    group.getId(),
                    null, // No code on group in dstu3
                    stratifiers,
                    populations);
            groups.add(groupDef);
            groupMeasureScoring.put(groupDef, groupMeasureScoringCode);
        }

        return new MeasureDef(
                measure.getId(), measure.getUrl(), measure.getVersion(), groupMeasureScoring, groups, sdes);
    }

    private PopulationDef checkPopulationForCode(
            List<PopulationDef> populations, MeasurePopulationType measurePopType) {
        return populations.stream()
                .filter(e -> e.code().first().code().equals(measurePopType.toCode()))
                .findAny()
                .orElse(null);
    }

    private ConceptDef totalConceptDefCreator(MeasurePopulationType measurePopulationType) {
        return new ConceptDef(
                Collections.singletonList(
                        new CodeDef(measurePopulationType.getSystem(), measurePopulationType.toCode())),
                null);
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
        if (enforceIds && (e.getId() == null || StringUtils.isBlank(e.getId()))) {
            throw new NullPointerException("id is required on all Elements of type: " + e.fhirType());
        }
    }

    private void checkId(Resource r) {
        if (enforceIds && (r.getId() == null || StringUtils.isBlank(r.getId()))) {
            throw new NullPointerException("id is required on all Resources of type: " + r.fhirType());
        }
    }

    private MeasureScoring getMeasureScoring(Measure measure) {
        return MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode());
    }
}
