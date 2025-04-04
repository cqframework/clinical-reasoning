package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_BUNDLE_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_COMPOSITION_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_DETECTED_ISSUE_MR_GROUP_ID;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_DETECTED_ISSUE_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_REPORT_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.HtmlConstants.HTML_DIV_PARAGRAPH_CONTENT;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_ORGANIZATION;
import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.enumeration.CareGapsStatusCode;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.builder.BundleBuilder;
import org.opencds.cqf.fhir.utility.builder.CodeableConceptSettings;
import org.opencds.cqf.fhir.utility.builder.CompositionBuilder;
import org.opencds.cqf.fhir.utility.builder.CompositionSectionComponentBuilder;
import org.opencds.cqf.fhir.utility.builder.DetectedIssueBuilder;
import org.opencds.cqf.fhir.utility.builder.NarrativeSettings;

/**
 * Care Gaps Bundle Builder houses the logic for constructing a Care-Gaps Document Bundle for a Patient per Measures requested
 */
public class R4CareGapsBundleBuilder {
    private static final Map<String, CodeableConceptSettings> CARE_GAPS_CODES = ImmutableMap.of(
            "http://loinc.org/96315-7",
            new CodeableConceptSettings().add("http://loinc.org", "96315-7", "Gaps in care report"),
            "http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP",
            new CodeableConceptSettings()
                    .add("http://terminology.hl7.org/CodeSystem/v3-ActCode", "CAREGAP", "Care Gaps"));
    private final Repository repository;
    private final Map<String, Resource> configuredResources;
    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private final CareGapsProperties careGapsProperties;
    private final String serverBase;
    private final R4MeasureServiceUtils r4MeasureServiceUtils;
    private final R4MultiMeasureService r4MultiMeasureService;

    public R4CareGapsBundleBuilder(
            CareGapsProperties careGapsProperties,
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase,
            Map<String, Resource> configuredResources,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.careGapsProperties = careGapsProperties;
        this.serverBase = serverBase;
        this.configuredResources = configuredResources;

        r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        r4MultiMeasureService =
                new R4MultiMeasureService(repository, measureEvaluationOptions, serverBase, measurePeriodValidator);
    }

    public List<Parameters.ParametersParameterComponent> makePatientBundles(
            List<String> subjects, R4CareGapsParameters r4CareGapsParameters, List<IdType> measureId) {

        // retrieve reporter from configuration
        String reporter = RESOURCE_TYPE_ORGANIZATION.concat("/" + careGapsProperties.getCareGapsReporter());

        List<ParametersParameterComponent> paramResults = new ArrayList<>();

        for (String subject : subjects) {
            // Measure Reports
            Bundle result = r4MultiMeasureService.evaluate(R4MeasureEvaluatorMultipleRequest.builder()
                    .setMeasureId(measureId)
                    .setPeriodStart(r4CareGapsParameters.getPeriodStart())
                    .setPeriodEnd(r4CareGapsParameters.getPeriodEnd())
                    .setReportType(MeasureEvalType.SUBJECT.toCode())
                    .setSubject(subject)
                    .setReporter(reporter)
                    .build());

            // Patient, subject comes in as format "ResourceType/[id]", no resourceType required to be specified.
            var patient = repository.read(Patient.class, new IdType(subject));

            Bundle bundle;
            // finalize patient Bundle results
            bundle = makePatientBundle(
                    result, r4CareGapsParameters.getStatus(), patient, r4CareGapsParameters.isNotDocument());

            // add parameter with results
            if (bundle != null && bundle.hasEntry()) {
                paramResults.add(initializePatientParameter(patient).setResource(bundle));
            }
        }
        return paramResults;
    }
    /**
     * method to use for creating Care-Gaps Bundle per Patient. IsDocumentMode will control which
     * resources are added or excluded from the final bundle
     */
    @Nullable
    public Bundle makePatientBundle(Bundle bundle, List<String> statuses, Patient patient, boolean notDocument) {
        Map<String, Resource> evalPlusSDE = new HashMap<>();
        List<DetectedIssue> detectedIssues = new ArrayList<>();
        List<MeasureReport> measureReports = new ArrayList<>();
        var gapEvaluator = new R4CareGapStatusEvaluator();
        var composition = getComposition(patient, notDocument);

        // get Evaluation Bundle Results
        for (BundleEntryComponent entry : bundle.getEntry()) {
            MeasureReport mr = (MeasureReport) entry.getResource();
            addProfile(mr);
            addResourceId(mr);
            Measure measure = r4MeasureServiceUtils.resolveByUrl(mr.getMeasure());
            // Applicable Reports per Gap-Status
            var gapStatus = gapEvaluator.getGroupGapStatus(measure, mr);
            var filteredGapStatus = filteredGapStatus(gapStatus, statuses);
            if (!filteredGapStatus.isEmpty()) {
                if (!notDocument) {
                    // add document mode required elements to final Care-gap report
                    measureReports.add(mr);
                    populateEvaluatedResources(mr, evalPlusSDE);
                    populateSDEResources(mr, evalPlusSDE);
                }
                // Issue(s) Detected from MeasureReport
                for (Map.Entry<String, CareGapsStatusCode> item : filteredGapStatus.entrySet()) {
                    String groupId = item.getKey();
                    CareGapsStatusCode careGapsStatusCode = item.getValue();
                    // create DetectedIssue per gap-status and MeasureReport.groupId
                    DetectedIssue issue =
                            getDetectedIssue(patient, mr, groupId, careGapsStatusCode, measure, notDocument);
                    // add DetectedIssue list to set on Bundle
                    detectedIssues.add(issue);
                    // add sections for DetectedIssues created
                    if (!notDocument) {
                        composition.addSection(getSection(measure, mr, issue, careGapsStatusCode));
                    }
                }
            }
        }

        if (!detectedIssues.isEmpty()) {
            // only add if a DetectedIssue is found and has MeasureReports
            return addBundleEntries(serverBase, composition, detectedIssues, measureReports, evalPlusSDE, notDocument);
        } else {
            // return nothing if not-applicable
            return null;
        }
    }

    private Map<String, CareGapsStatusCode> filteredGapStatus(
            Map<String, CareGapsStatusCode> careGapStatusPerGroupId, List<String> statuses) {
        Map<String, CareGapsStatusCode> filtered = new HashMap<>();
        for (Map.Entry<String, CareGapsStatusCode> entry : careGapStatusPerGroupId.entrySet()) {
            String groupId = entry.getKey();
            CareGapsStatusCode careGapsStatusCode = entry.getValue();
            // check resulting status for report groups is in operation request 'statuses'
            if (statuses.contains(careGapsStatusCode.toString())) {
                filtered.put(groupId, careGapsStatusCode);
            }
        }
        return filtered;
    }

    private Bundle.BundleEntryComponent getBundleEntry(String serverBase, Resource resource) {
        return new Bundle.BundleEntryComponent().setResource(resource).setFullUrl(getFullUrl(serverBase, resource));
    }

    private Composition.SectionComponent getSection(
            Measure measure, MeasureReport measureReport, DetectedIssue detectedIssue, CareGapsStatusCode gapStatus) {
        String narrative = String.format(
                HTML_DIV_PARAGRAPH_CONTENT,
                gapStatus == CareGapsStatusCode.CLOSED_GAP
                        ? "No detected issues."
                        : String.format("Issues detected.  See %s for details.", Ids.simple(detectedIssue)));
        return new CompositionSectionComponentBuilder<>(Composition.SectionComponent.class)
                .withTitle(measure.hasTitle() ? measure.getTitle() : measure.getUrl())
                .withFocus(Ids.simple(measureReport))
                .withText(new NarrativeSettings(narrative))
                .withEntry(Ids.simple(detectedIssue))
                .build();
    }

    @Nullable
    private Composition getComposition(Patient patient, boolean notDocument) {
        Composition composition = null;
        if (!notDocument) {
            composition = new CompositionBuilder<>(Composition.class)
                    .withProfile(CARE_GAPS_COMPOSITION_PROFILE)
                    .withType(CARE_GAPS_CODES.get("http://loinc.org/96315-7"))
                    .withStatus(Composition.CompositionStatus.FINAL.toString())
                    .withTitle("Care Gap Report for " + Ids.simplePart(patient))
                    .withSubject(Ids.simple(patient))
                    .withAuthor(Ids.simple(configuredResources.get("care_gaps_composition_section_author")))
                    .build();
        }
        return composition;
    }

    private boolean isMultiRateMeasure(MeasureReport measureReport) {
        return measureReport.getGroup().size() > 1;
    }

    private DetectedIssue getDetectedIssue(
            Patient patient,
            MeasureReport measureReport,
            String measureReportGroupId,
            CareGapsStatusCode careGapsStatusCode,
            Measure measure,
            boolean notDocument) {

        var detectedIssue = new DetectedIssueBuilder<>(DetectedIssue.class)
                .withProfile(CARE_GAPS_DETECTED_ISSUE_PROFILE)
                .withStatus(DetectedIssue.DetectedIssueStatus.FINAL.toString())
                .withCode(CARE_GAPS_CODES.get("http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP"))
                .withPatient(Ids.simple(patient))
                .withEvidenceDetail(Ids.simple(measureReport))
                .withModifierExtension(new ImmutablePair<>(
                        CARE_GAPS_GAP_STATUS_EXTENSION,
                        new CodeableConceptSettings()
                                .add(
                                        CARE_GAPS_GAP_STATUS_SYSTEM,
                                        careGapsStatusCode.toString(),
                                        careGapsStatusCode.toDisplayString())))
                .build();

        // add period from MeasureReport for Identified period of Issue
        detectedIssue.setIdentified(measureReport.getPeriod());
        // add Measure reference as Implicated reference for Issue
        detectedIssue.setImplicated(Collections.singletonList(new Reference(Ids.simple(measure))));

        if (measureReportGroupId != null && isMultiRateMeasure(measureReport)) {
            // MeasureReportGroupComponent.id value set here to differentiate between DetectedIssue resources for the
            // same MeasureReport
            Extension groupIdExt = new Extension();
            groupIdExt.setUrl(CARE_GAPS_DETECTED_ISSUE_MR_GROUP_ID);
            groupIdExt.setValue(new StringType(measureReportGroupId));
            detectedIssue.setExtension(Collections.singletonList(groupIdExt));
        }
        if (notDocument) {
            // add Report as contained resource
            detectedIssue.setContained(Collections.singletonList(measureReport));
            // update evidence reference to '#' prefixed reference to indicate it is contained.
            detectedIssue
                    .getEvidenceFirstRep()
                    .setDetail(Collections.singletonList(new Reference("#" + measureReport.getId())));
        }
        return detectedIssue;
    }

    private void populateEvaluatedResources(MeasureReport measureReport, Map<String, Resource> resources) {
        measureReport.getEvaluatedResource().forEach(evaluatedResource -> {
            IIdType resourceId = evaluatedResource.getReferenceElement();
            if (resourceId.getResourceType() == null || resources.containsKey(Ids.simple(resourceId))) {
                return;
            }

            Class<? extends IBaseResource> resourceType = fhirContext
                    .getResourceDefinition(resourceId.getResourceType())
                    .newInstance()
                    .getClass();
            IBaseResource resource = repository.read(resourceType, resourceId);

            if (resource instanceof Resource) {
                Resource resourceBase = (Resource) resource;
                resources.put(Ids.simple(resourceId), resourceBase);
            }
        });
    }

    private void populateSDEResources(MeasureReport measureReport, Map<String, Resource> resources) {
        if (measureReport.hasExtension()) {
            for (Extension extension : measureReport.getExtension()) {
                if (extension.hasUrl() && extension.getUrl().equals(EXT_SDE_REFERENCE_URL)) {
                    Reference sdeRef = extension.hasValue() && extension.getValue() instanceof Reference
                            ? (Reference) extension.getValue()
                            : null;
                    if (sdeRef != null
                            && sdeRef.hasReference()
                            && !sdeRef.getReference().startsWith("#")) {
                        // sde reference comes in format [ResourceType]/{id}
                        IdType sdeId = new IdType(sdeRef.getReference());
                        if (!resources.containsKey(Ids.simple(sdeId))) {
                            Class<? extends IBaseResource> resourceType = fhirContext
                                    .getResourceDefinition(sdeId.getResourceType())
                                    .newInstance()
                                    .getClass();
                            IBaseResource resource = repository.read(resourceType, sdeId);
                            if (resource instanceof Resource) {
                                Resource resourceBase = (Resource) resource;
                                resources.put(Ids.simple(sdeId), resourceBase);
                            }
                        }
                    }
                }
            }
        }
    }

    private Bundle makeNewBundle() {
        return new BundleBuilder<>(Bundle.class)
                .withProfile(CARE_GAPS_BUNDLE_PROFILE)
                .withType(Bundle.BundleType.DOCUMENT.toString())
                .build();
    }

    private Bundle addBundleEntries(
            String serverBase,
            Composition composition,
            List<DetectedIssue> detectedIssues,
            List<MeasureReport> measureReports,
            Map<String, Resource> evalPlusSDEs,
            boolean notDocument) {
        Bundle reportBundle = makeNewBundle();
        if (notDocument) {
            detectedIssues.forEach(detectedIssue -> reportBundle.addEntry(getBundleEntry(serverBase, detectedIssue)));
        } else {
            reportBundle.addEntry(getBundleEntry(serverBase, composition));
            measureReports.forEach(report -> reportBundle.addEntry(getBundleEntry(serverBase, report)));
            detectedIssues.forEach(detectedIssue -> reportBundle.addEntry(getBundleEntry(serverBase, detectedIssue)));
            configuredResources
                    .values()
                    .forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
            evalPlusSDEs.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
        }
        return reportBundle;
    }

    private void addProfile(MeasureReport measureReport) {
        if (measureReport.hasMeta()) {
            measureReport.getMeta().addProfile(CARE_GAPS_REPORT_PROFILE);
        } else {
            measureReport.setMeta(new Meta().addProfile(CARE_GAPS_REPORT_PROFILE));
        }
    }

    private void addResourceId(MeasureReport measureReport) {
        if (Strings.isNullOrEmpty(measureReport.getId())) {
            IIdType id = Ids.newId(MeasureReport.class, UUID.randomUUID().toString());
            measureReport.setId(id);
        }
    }

    private Parameters.ParametersParameterComponent initializePatientParameter(Patient patient) {
        Parameters.ParametersParameterComponent patientParameter = Resources.newBackboneElement(
                        Parameters.ParametersParameterComponent.class)
                .setName("return");
        patientParameter.setId("subject-" + Ids.simplePart(patient));
        return patientParameter;
    }
}
