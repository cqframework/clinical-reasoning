package org.opencds.cqf.cql.evaluator.measure.dstu3;

import java.util.ArrayList;
import java.util.List;

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
import org.opencds.cqf.cql.evaluator.measure.common.CodeDef;
import org.opencds.cqf.cql.evaluator.measure.common.ConceptDef;
import org.opencds.cqf.cql.evaluator.measure.common.GroupDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDefBuilder;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureScoring;
import org.opencds.cqf.cql.evaluator.measure.common.PopulationDef;
import org.opencds.cqf.cql.evaluator.measure.common.SdeDef;
import org.opencds.cqf.cql.evaluator.measure.common.StratifierDef;

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
            SdeDef sdeDef = new SdeDef(s.getId(), null, // No code on sde in dstu3
                    s.getCriteria());
            sdes.add(sdeDef);

        }

        // Groups
        List<GroupDef> groups = new ArrayList<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            checkId(group);

            // Populations
            List<PopulationDef> populations = new ArrayList<>();
            for (MeasureGroupPopulationComponent pop : group.getPopulation()) {
                checkId(pop);
                var populationType =
                        MeasurePopulationType.fromCode(pop.getCode().getCodingFirstRep().getCode());

                populations.add(new PopulationDef(pop.getId(), conceptToConceptDef(pop.getCode()),
                        populationType, pop.getCriteria()));
            }

            // Stratifiers
            List<StratifierDef> stratifiers = new ArrayList<>();
            for (MeasureGroupStratifierComponent mgsc : group.getStratifier()) {
                checkId(mgsc);
                var stratifierDef = new StratifierDef(mgsc.getId(), null, // No code on stratifier
                                                                          // in dstu3
                        mgsc.getCriteria());

                stratifiers.add(stratifierDef);
            }

            groups.add(new GroupDef(group.getId(), null, // No code on group in dstu3
                    stratifiers, populations));
        }

        return new MeasureDef(measure.getId(), measure.getUrl(), measure.getVersion(),
                MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode()), groups,
                sdes);
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
        return new CodeDef(coding.getSystem(), coding.getVersion(), coding.getCode(),
                coding.getDisplay());
    }

    private void checkId(Element e) {
        if (enforceIds && (e.getId() == null || StringUtils.isBlank(e.getId()))) {
            throw new NullPointerException(
                    "id is required on all Elements of type: " + e.fhirType());
        }
    }

    private void checkId(Resource r) {
        if (enforceIds && (r.getId() == null || StringUtils.isBlank(r.getId()))) {
            throw new NullPointerException(
                    "id is required on all Resources of type: " + r.fhirType());
        }
    }
}
