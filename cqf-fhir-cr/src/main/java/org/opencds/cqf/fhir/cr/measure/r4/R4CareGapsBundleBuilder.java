package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasureConstants.EXT_SDE_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_BUNDLE_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_COMPOSITION_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_DETECTED_ISSUE_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_REPORT_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.HtmlConstants.HTML_DIV_PARAGRAPH_CONTENT;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_ORGANIZATION;
import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
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
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
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
    private static final Map<String, CodeableConceptSettings> CARE_GAPS_CODES = Map.of(
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
            Map<String, Resource> configuredResources) {
        this.repository = repository;
        this.careGapsProperties = careGapsProperties;
        this.serverBase = serverBase;
        this.configuredResources = configuredResources;

        r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        r4MultiMeasureService = new R4MultiMeasureService(repository, measureEvaluationOptions, serverBase);
    }

    public List<Parameters.ParametersParameterComponent> makePatientBundles(
            IPrimitiveType<Date> periodStart,
            IPrimitiveType<Date> periodEnd,
            List<String> subjects,
            List<String> statuses,
            List<IdType> measureIds) {

        // retrieve reporter from configuration
        String reporter = RESOURCE_TYPE_ORGANIZATION.concat("/" + careGapsProperties.getCareGapsReporter());

        List<ParametersParameterComponent> paramResults = new ArrayList<>();

        for (String subject : subjects) {
            // Measure Reports
            Bundle result = r4MultiMeasureService.evaluate(
                    measureIds,
                    null,
                    null,
                    periodStart.getValueAsString(),
                    periodEnd.getValueAsString(),
                    MeasureEvalType.SUBJECT.toCode(),
                    subject,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    reporter);

            // Patient, subject comes in as format "ResourceType/[id]", no resourceType required to be specified.
            var patient = repository.read(Patient.class, new IdType(subject));

            // finalize patient Bundle results
            var bundle = makePatientBundle(result, statuses, patient);

            // add parameter with results
            if (bundle != null && bundle.hasEntry()) {
                paramResults.add(initializePatientParameter(patient).setResource(bundle));
            }
        }
        return paramResults;
    }

    @Nullable
    public Bundle makePatientBundle(Bundle bundle, List<String> statuses, Patient patient) {
        Map<String, Resource> evalPlusSDE = new HashMap<>();
        List<DetectedIssue> detectedIssues = new ArrayList<>();
        List<MeasureReport> measureReports = new ArrayList<>();
        var gapEvaluator = new R4CareGapStatusEvaluator();
        Composition composition = getComposition(patient);
        // get Evaluation Bundle Results
        for (BundleEntryComponent entry : bundle.getEntry()) {
            MeasureReport mr = (MeasureReport) entry.getResource();
            addProfile(mr);
            addResourceId(mr);
            Measure measure = r4MeasureServiceUtils.resolveByUrl(mr.getMeasure());
            // Applicable Reports per Gap-Status
            var gapStatus = gapEvaluator.getGapStatus(measure, mr);
            boolean keepResult = statuses.contains(gapStatus.toString());
            if (keepResult) {
                // add Report to final Care-gap report
                measureReports.add(mr);
                // Issue Detected for Report
                DetectedIssue detectedIssue = getDetectedIssue(patient, mr, gapStatus);
                detectedIssues.add(getDetectedIssue(patient, mr, gapStatus));
                composition.addSection(getSection(measure, mr, detectedIssue, gapStatus));
                // Track evaluated Resources
                populateEvaluatedResources(mr, evalPlusSDE);
                populateSDEResources(mr, evalPlusSDE);
            }
        }

        if (!measureReports.isEmpty()) {
            // only add if a DetectedIssue is found and has MeasureReports
            return addBundleEntries(serverBase, composition, detectedIssues, measureReports, evalPlusSDE);
        } else {
            // return nothing if not-applicable
            return null;
        }
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

    private Composition getComposition(Patient patient) {
        return new CompositionBuilder<>(Composition.class)
                .withProfile(CARE_GAPS_COMPOSITION_PROFILE)
                .withType(CARE_GAPS_CODES.get("http://loinc.org/96315-7"))
                .withStatus(Composition.CompositionStatus.FINAL.toString())
                .withTitle("Care Gap Report for " + Ids.simplePart(patient))
                .withSubject(Ids.simple(patient))
                .withAuthor(Ids.simple(configuredResources.get("care_gaps_composition_section_author")))
                // .withCustodian(organization) // TODO: Optional: identifies the organization
                // who is responsible for ongoing maintenance of and accessing to this gaps in
                // care report. Add as a setting and optionally read if it's there.
                .build();
    }

    private DetectedIssue getDetectedIssue(
            Patient patient, MeasureReport measureReport, CareGapsStatusCode careGapStatusCode) {
        return new DetectedIssueBuilder<>(DetectedIssue.class)
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
                                        careGapStatusCode.toString(),
                                        careGapStatusCode.toDisplayString())))
                .build();
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
            Map<String, Resource> evalPlusSDEs) {
        Bundle reportBundle = makeNewBundle();
        reportBundle.addEntry(getBundleEntry(serverBase, composition));
        measureReports.forEach(report -> reportBundle.addEntry(getBundleEntry(serverBase, report)));
        detectedIssues.forEach(detectedIssue -> reportBundle.addEntry(getBundleEntry(serverBase, detectedIssue)));
        configuredResources.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
        evalPlusSDEs.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
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
