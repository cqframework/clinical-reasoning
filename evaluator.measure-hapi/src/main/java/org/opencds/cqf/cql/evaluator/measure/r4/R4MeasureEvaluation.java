package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvaluation;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureInfo;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureScoring;

/**
 * Implementation of MeasureEvaluation on top of HAPI FHIR R4 structures.
 */
public class R4MeasureEvaluation<ST extends DomainResource> extends
        MeasureEvaluation<IBase, Measure, MeasureGroupComponent, MeasureGroupPopulationComponent, MeasureSupplementalDataComponent,
        MeasureReport, MeasureReportGroupComponent, MeasureReport.MeasureReportGroupPopulationComponent,
        Coding, Extension, Reference, ListResource, ListResource.ListEntryComponent, DomainResource, ST> {

    public R4MeasureEvaluation(Context context, Measure measure) {
        super(context, measure, x -> x.getIdElement().getIdPart());
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
    protected MeasureReport createMeasureReport(String status, MeasureReportType type, List<ST> subjects) {
        MeasureReport report = new MeasureReport();
        report.setStatus(MeasureReport.MeasureReportStatus.fromCode("complete"));
        report.setType(org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.fromCode(type.toCode()));
        report.setMeasure(measure.getIdElement().getIdPart());
        if (type == MeasureReportType.INDIVIDUAL && !subjects.isEmpty()) {
            report.setSubject(new Reference(this.getId.apply(subjects.get(0))));
        }

        Period period = null;
        Interval measurementPeriod = this.getMeasurementPeriod();
        if( measurementPeriod.getStart() instanceof DateTime ) {
            DateTime dtStart = (DateTime)measurementPeriod.getStart();
            DateTime dtEnd = (DateTime)measurementPeriod.getEnd();
            
            period = new Period()
                    .setStart(dtStart.toJavaDate())
                    .setEnd(dtEnd.toJavaDate());
            
        } else if( measurementPeriod.getStart() instanceof Date ) {
            period = new Period()
                    .setStart((Date) measurementPeriod.getStart())
                    .setEnd((Date)measurementPeriod.getEnd());
        } else { 
            throw new IllegalArgumentException("Measurement period should be an interval of CQL DateTime or Java Date objects");
        }
        
        report.setPeriod(period);

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
    
    @Override
    protected List<MeasureSupplementalDataComponent> getSupplementalData(Measure measure) {
        return measure.getSupplementalData();
    }

    @Override
    protected String getSdeExpression(MeasureSupplementalDataComponent sdeItem) {
        String result = null; 
        if( sdeItem.getCriteria() != null ) {
            result = sdeItem.getCriteria().getExpression();
        }
        return result;
    }

    @Override
    protected Coding getSdeCoding(MeasureSupplementalDataComponent sdeItem) {
        Coding result = null;
        if( sdeItem.getCode() != null && sdeItem.getCode().getCodingFirstRep() != null ) {
            result = sdeItem.getCode().getCodingFirstRep();
        }
        return result;
    }
    
    @Override
    protected String getCodingCode(Coding coding) { 
        String result = null;
        if( coding != null ) {
            result = coding.getCode();
        }
        return result;
    }

    @Override
    protected boolean isCoding(Object obj) {
        return obj instanceof Coding;
    }

    @Override
    protected DomainResource createPopulationObservation(Measure measure, String populationId, Coding valueCoding, Integer sdeAccumulatorValue) {

        Observation obs = createObservation(measure,populationId);
        
        CodeableConcept obsCodeableConcept = new CodeableConcept();
        obsCodeableConcept.setCoding(Collections.singletonList(valueCoding));
        
        obs.setCode(obsCodeableConcept);
        obs.setValue(new IntegerType(sdeAccumulatorValue));
        
        
        return obs;
    }
    
    @Override
    protected DomainResource createPatientObservation(Measure measure, String populationId, Coding valueCoding) {

        Observation obs = createObservation(measure,populationId);
        
        CodeableConcept codeCodeableConcept = new CodeableConcept().setText(populationId);
        obs.setCode( codeCodeableConcept );
        
        CodeableConcept valueCodeableConcept = new CodeableConcept();
        valueCodeableConcept.setCoding(Collections.singletonList(valueCoding));
        obs.setValue(valueCodeableConcept);
        
        return obs;
    }
    
    protected Observation createObservation(Measure measure, String populationId) {
        MeasureInfo measureInfo = new MeasureInfo()
                .withMeasure(MeasureInfo.MEASURE_PREFIX + measure.getIdElement().getIdPart())
                .withPopulationId(populationId);
        
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(UUID.randomUUID().toString());
        obs.addExtension( createMeasureInfoExtension(measureInfo) );
        
        return obs;
    }
    
    protected Extension createMeasureInfoExtension(MeasureInfo measureInfo) {
        
        Extension extExtMeasure = new Extension().setUrl(MeasureInfo.MEASURE)
                .setValue(new CanonicalType(measureInfo.getMeasure()));
        
        Extension extExtPop = new Extension().setUrl(MeasureInfo.POPULATION_ID)
                .setValue(new StringType(measureInfo.getPopulationId()));
        
        Extension obsExtension = new Extension().setUrl(MeasureInfo.EXT_URL);
        obsExtension.addExtension(extExtMeasure);
        obsExtension.addExtension(extExtPop);
        
        return obsExtension;
    }

    @Override
    protected void addEvaluatedResource(MeasureReport report, DomainResource resource) {
        report.addEvaluatedResource( new Reference("#" + this.getId.apply(resource)) );
    }

    @Override
    protected void addContained(MeasureReport report, DomainResource resource) {
        report.addContained((Resource)resource);
    }

    @Override
    protected Coding createCoding(String text) {
        return new Coding().setCode(text);
    }

    @Override
    protected Coding getExtensionCoding(ST patient, String coreCategory, String sdeCode) {
        Coding valueCoding = new Coding();
        
        patient.getExtension().forEach((ptExt) -> {
            if (ptExt.getUrl().contains(coreCategory)) {
                Coding extCoding = (Coding) ptExt.getExtension().get(0).getValue();
                String extCode = getCodingCode(extCoding);
                if (extCode.equalsIgnoreCase(sdeCode)) {
                    valueCoding.setSystem(extCoding.getSystem());
                    valueCoding.setCode(extCoding.getCode());
                    valueCoding.setDisplay(extCoding.getDisplay());
                }
            }
        });
        
        return valueCoding;
    }

    @Override
    protected Extension createCodingExtension(String url, String codeSystem, String code) {
        Extension ext = new Extension().setUrl(url);
        Coding coding = new Coding().setSystem(codeSystem).setCode(code);
        ext.setValue(coding);
        return ext;
    }

    @Override
    protected Reference createReference(String resourceId) {
        return new Reference(resourceId);
    }

    @Override
    protected ListResource createListResource(Collection<ListEntryComponent> entries) {
        ListResource resource = new ListResource();
        resource.setId(UUID.randomUUID().toString());
        resource.getEntry().addAll(entries);
        return resource;
    }

    @Override
    protected ListEntryComponent createListEntry(Reference reference) {
        return new ListEntryComponent().setItem(reference);
    }

    @Override
    protected void addExtension(Reference reference, Extension extension) {
        reference.addExtension(extension);
    }

    @Override
    protected void setEvaluatedResources(MeasureReport report, Collection<Reference> evaluatedResources) {
        report.getEvaluatedResource().addAll(evaluatedResources);
    }
}
