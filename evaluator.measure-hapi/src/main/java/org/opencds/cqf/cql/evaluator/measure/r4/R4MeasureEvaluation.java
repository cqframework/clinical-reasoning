package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvaluation;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureScoring;
import org.opencds.cqf.cql.engine.runtime.Interval;

/**
 * Implementation of MeasureEvaluation on top of HAPI FHIR R4 structures.
 */
public class R4MeasureEvaluation<RT, ST extends RT> extends
        MeasureEvaluation<IBase, Measure, MeasureGroupComponent, MeasureGroupPopulationComponent, MeasureReport, MeasureReportGroupComponent, MeasureReport.MeasureReportGroupPopulationComponent, RT, ST> {

    public R4MeasureEvaluation(Context context, Measure measure, Interval measurementPeriod, String packageName,
            Function<RT, String> getId, String patientOrPractitionerId) {
        super(context, measure, measurementPeriod, packageName, getId, patientOrPractitionerId);
    }

    public R4MeasureEvaluation(Context context, Measure measure, Interval measurementPeriod, String packageName,
    Function<RT, String> getId) {
        super(context, measure, measurementPeriod, packageName, getId);
    }

    @Override
    protected MeasureScoring getMeasureScoring() {
        return MeasureScoring.fromCode(this.measure.getScoring().getCodingFirstRep().getCode());
    }

    @Override
    protected String getCriteriaExpression(MeasureGroupPopulationComponent mgpc) {
        return mgpc.hasCriteria() ? mgpc.getCriteria().getExpression() : null;
    }

    @Override
    protected void setGroupScore(MeasureReportGroupComponent mrgc, Double score) {
        mrgc.setMeasureScore(new Quantity(score));
    }

    @Override
    protected MeasurePopulationType getPopulationType(MeasureGroupPopulationComponent mgpc) {
        return MeasurePopulationType.fromCode(mgpc.getCode().getCodingFirstRep().getCode());
    }

    @Override
    protected Iterable<MeasureGroupComponent> getGroup() {
        return this.measure.getGroup();
    }

    @Override
    protected Iterable<MeasureGroupPopulationComponent> getPopulation(MeasureGroupComponent mgc) {
        return mgc.getPopulation();
    }

    @Override
    protected void addPopulationReport(MeasureReport report, MeasureReportGroupComponent reportGroup,
            MeasureGroupPopulationComponent populationCriteria, int populationCount, Iterable<ST> subjectPopulation) {
        MeasureReport.MeasureReportGroupPopulationComponent populationReport = new MeasureReport.MeasureReportGroupPopulationComponent();
        populationReport.setCount(populationCount);
        populationReport.setCode(populationCriteria.getCode());
        MeasureReportType type = MeasureReportType.fromCode(report.getType().toCode());
        if ((type == MeasureReportType.SUBJECTLIST || type == MeasureReportType.PATIENTLIST) && subjectPopulation != null) {
            ListResource SUBJECTLIST = new ListResource();
            SUBJECTLIST.setId(UUID.randomUUID().toString());
            populationReport.setSubjectResults(new Reference().setReference("#" + SUBJECTLIST.getId()));
            for (ST patient : subjectPopulation) {
                ListResource.ListEntryComponent entry = new ListResource.ListEntryComponent()
                        .setItem(new Reference().setReference(
                                this.getId.apply(patient).startsWith("Patient/") ?
                                        this.getId.apply(patient) :
                                        String.format("Patient/%s", this.getId.apply(patient))));
                                // TODO: patient name;
                                // .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));
                SUBJECTLIST.addEntry(entry);
            }
            report.addContained(SUBJECTLIST);
        }

        reportGroup.addPopulation(populationReport);
    }

    @Override
    protected MeasureReport createMeasureReport(String status, MeasureReportType type, Interval measurementPeriod, List<ST> subjects) {
        MeasureReport report = new MeasureReport();
        report.setStatus(MeasureReport.MeasureReportStatus.fromCode("complete"));
        report.setType(org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.fromCode(type.toCode()));
        report.setMeasure(measure.getIdElement().getIdPart());
        if (type == MeasureReportType.INDIVIDUAL && !subjects.isEmpty()) {
            report.setSubject(new Reference(this.getId.apply(subjects.get(0))));
        }

        report.setPeriod(
            new Period()
                    .setStart((Date) measurementPeriod.getStart())
                    .setEnd((Date) measurementPeriod.getEnd()));

        return report;
    }

    @Override
    protected MeasureReportGroupComponent createReportGroup(String id) {
        MeasureReport.MeasureReportGroupComponent mrgc = new MeasureReport.MeasureReportGroupComponent();
        mrgc.setId(id);

        return mrgc;
    }

    @Override
    protected String getGroupId(MeasureGroupComponent group) {
        return group.getId();
    }

    @Override
    protected void addReportGroup(MeasureReport report, MeasureReportGroupComponent group) {
        report.addGroup(group);
    }
}
