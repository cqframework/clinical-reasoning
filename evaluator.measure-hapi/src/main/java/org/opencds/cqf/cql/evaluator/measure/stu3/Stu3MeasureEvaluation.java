package org.opencds.cqf.cql.evaluator.measure.stu3;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvaluation;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureInfo;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureScoring;

/**
 * Implementation of MeasureEvaluation on top of HAPI FHIR STU3 structures.
 */
public class Stu3MeasureEvaluation<ST extends DomainResource> extends
        MeasureEvaluation<IBase, Measure, MeasureGroupComponent, MeasureGroupPopulationComponent, Measure.MeasureSupplementalDataComponent, MeasureReport, MeasureReportGroupComponent, MeasureReport.MeasureReportGroupPopulationComponent, Coding, Extension, Reference, ListResource, ListResource.ListEntryComponent, DomainResource, ST> {

    public Stu3MeasureEvaluation(Context context, Measure measure, Interval measurementPeriod, String packageName,
            Function<DomainResource, String> getId, String patientOrPractitionerId) {
        super(context, measure, measurementPeriod, packageName, getId, patientOrPractitionerId);
    }

    public Stu3MeasureEvaluation(Context context, Measure measure, Interval measurementPeriod, String packageName,
    Function<DomainResource, String> getId) {
        super(context, measure, measurementPeriod, packageName, getId);
    }

    @Override
    protected MeasureScoring getMeasureScoring() {
        return MeasureScoring.fromCode(this.measure.getScoring().getCodingFirstRep().getCode());
    }

    @Override
    protected String getCriteriaExpression(MeasureGroupPopulationComponent mgpc) {
        return mgpc.getCriteria();
    }

    @Override
    protected void setGroupScore(MeasureReportGroupComponent mrgc, Double score) {
        mrgc.setMeasureScore(score);
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
            populationReport.setPatients(new Reference().setReference("#" + SUBJECTLIST.getId()));
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
        report.setType(org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportType.fromCode(type.toCode()));
        report.setMeasure(new Reference(measure.getIdElement().getValue()));
        if (type == MeasureReportType.INDIVIDUAL && !subjects.isEmpty()) {
            report.setPatient(new Reference(this.getId.apply(subjects.get(0))));
        }

        Period period = null;
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
    protected String getSDEExpression(MeasureSupplementalDataComponent sdeItem) {
        String result = null; 
        if( sdeItem.getCriteria() != null ) {
            result = sdeItem.getCriteria();
        }
        return result;
    }

    @Override
    protected Coding getSDECoding(MeasureSupplementalDataComponent sdeItem) {
        Coding result = null;
        if( sdeItem.getIdentifier() != null ) {
            // TODO - Is this correct? There isn't an obvious code in STU3
            result = new Coding().setCode(sdeItem.getIdentifier().getValue());
        }
        return result;
    }

    @Override
    protected boolean isCoding(Object obj) {
        return obj instanceof Coding;
    }

    @Override
    protected Coding createCoding(String text) {
        return new Coding().setCode(text);
    }

    @Override
    protected String getCodingCode(Coding coding) {
        return coding.getCode();
    }

    @Override
    protected DomainResource createPatientObservation(Measure measure, String populationId, Coding coding) {
        Observation obs = createObservation(measure,populationId);
        
        CodeableConcept codeCodeableConcept = new CodeableConcept().setText(populationId);
        obs.setCode( codeCodeableConcept );
        
        CodeableConcept valueCodeableConcept = new CodeableConcept();
        valueCodeableConcept.setCoding(Collections.singletonList(coding));
        obs.setValue(valueCodeableConcept);
        
        return obs;

    }

    @Override
    protected DomainResource createPopulationObservation(Measure measure, String populationId, Coding coding,
            Integer value) {
        
        Observation obs = createObservation(measure,populationId);
        
        CodeableConcept obsCodeableConcept = new CodeableConcept();
        obsCodeableConcept.setCoding(Collections.singletonList(coding));
        
        obs.setCode(obsCodeableConcept);
        obs.setValue(new IntegerType(value));
        
        
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
                .setValue(new StringType(measureInfo.getMeasure()));
        
        Extension extExtPop = new Extension().setUrl(MeasureInfo.POPULATION_ID)
                .setValue(new StringType(measureInfo.getPopulationId()));
        
        Extension obsExtension = new Extension().setUrl(MeasureInfo.EXT_URL);
        obsExtension.addExtension(extExtMeasure);
        obsExtension.addExtension(extExtPop);
        
        return obsExtension;
    }

    @Override
    protected void addEvaluatedResource(MeasureReport report, DomainResource resource) {
        Bundle resources = getEvaluatedResourcesTarget(report);
        resources.addEntry().setResource(resource);
    }

    @Override
    protected void addContained(MeasureReport report, DomainResource resource) {
        report.addContained(resource);
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
    protected void addExtension(Reference resource, Extension extension) {
        resource.addExtension(extension);
    }

    @Override
    protected void setEvaluatedResources(MeasureReport report, Collection<Reference> evaluatedResources) {
        Bundle bundle = getEvaluatedResourcesTarget(report);
        
        //TODO - discuss the expected semantics with the wider group. The existing
        // cqf-ruler implementation noops this function right now. Below is a 
        // proposal that avoids the noop, but may not meet the actual needs of the 
        // consumer. Is it ok?
        ListResource list = (ListResource) bundle.getEntryFirstRep().getResource();
        if( list == null ) {
            list = new ListResource();
            list.setTitle("evaluatedResources");
            list.setId(list.getTitle());
            bundle.getEntryFirstRep().setResource(list);
        }
        
        for( Reference ref : evaluatedResources ) {
            list.addEntry(new ListEntryComponent().setItem(ref));
        }
        
        report.addContained( report.getEvaluatedResourcesTarget() );
        report.setEvaluatedResources(new Reference("#" + bundle.getId()));
        report.setEvaluatedResourcesTarget(null);
    }

    private Bundle getEvaluatedResourcesTarget(MeasureReport report) {
        Bundle bundle = report.getEvaluatedResourcesTarget();
        if( bundle == null ) {
            bundle = new Bundle();
            report.setEvaluatedResourcesTarget(bundle);
        }
        
        if( bundle.getId() == null ) {
            bundle = new Bundle();
            bundle.setId(UUID.randomUUID().toString());
            bundle.setType(Bundle.BundleType.COLLECTION);
            report.setEvaluatedResourcesTarget(bundle);
        }
        return bundle;
    }


}
