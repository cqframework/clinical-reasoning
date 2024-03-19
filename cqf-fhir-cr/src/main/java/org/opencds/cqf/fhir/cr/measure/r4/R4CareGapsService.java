package org.opencds.cqf.fhir.cr.measure.r4;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.hl7.fhir.r4.model.Factory.newId;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_BUNDLE_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_COMPOSITION_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_DETECTED_ISSUE_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_REPORT_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.HtmlConstants.HTML_DIV_PARAGRAPH_CONTENT;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_POPULATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION;
import static org.opencds.cqf.fhir.utility.Resources.newResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.enumeration.CareGapsStatusCode;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.builder.BundleBuilder;
import org.opencds.cqf.fhir.utility.builder.CodeableConceptSettings;
import org.opencds.cqf.fhir.utility.builder.CompositionBuilder;
import org.opencds.cqf.fhir.utility.builder.CompositionSectionComponentBuilder;
import org.opencds.cqf.fhir.utility.builder.DetectedIssueBuilder;
import org.opencds.cqf.fhir.utility.builder.NarrativeSettings;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4CareGapsService {

    private static final Logger ourLog = LoggerFactory.getLogger(R4CareGapsService.class);
    public static final Map<String, CodeableConceptSettings> CARE_GAPS_CODES = ImmutableMap.of(
            "http://loinc.org/96315-7",
            new CodeableConceptSettings().add("http://loinc.org", "96315-7", "Gaps in care report"),
            "http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP",
            new CodeableConceptSettings()
                    .add("http://terminology.hl7.org/CodeSystem/v3-ActCode", "CAREGAP", "Care Gaps"));

    private final Repository repository;

    private final MeasureEvaluationOptions measureEvaluationOptions;

    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

    private CareGapsProperties careGapsProperties;

    private String serverBase;

    private final Map<String, Resource> configuredResources = new HashMap<>();

    public R4CareGapsService(
            CareGapsProperties careGapsProperties,
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase) {
        this.repository = repository;
        this.careGapsProperties = careGapsProperties;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.serverBase = serverBase;
    }

    /**
     * Calculate measures describing gaps in care
     *
     * @param periodStart
     * @param periodEnd
     * @param topic
     * @param subject
     * @param practitioner
     * @param organization
     * @param statuses
     * @param measureIds
     * @param measureIdentifiers
     * @param measureUrls
     * @param programs
     * @return Parameters that includes zero to many document bundles that include Care Gap Measure
     *         Reports will be returned.
     */
    public Parameters getCareGapsReport(
            IPrimitiveType<Date> periodStart,
            IPrimitiveType<Date> periodEnd,
            List<String> topic,
            String subject,
            String practitioner,
            String organization,
            List<String> statuses,
            List<String> measureIds,
            List<String> measureIdentifiers,
            List<CanonicalType> measureUrls,
            List<String> programs) {

        validateConfiguration();

        List<Measure> measures = ensureMeasures(getMeasures(measureIds, measureIdentifiers, measureUrls));

        List<Patient> patients;
        if (!Strings.isNullOrEmpty(subject)) {
            patients = getPatientListFromSubject(subject);
        } else {
            throw new NotImplementedOperationException(
                    Msg.code(2275) + "Only the subject parameter has been implemented.");
        }

        if (statuses.isEmpty()) {
            throw new RuntimeException("CareGap 'statuses' parameter is empty");
        }

        checkValidStatusCode(statuses);

        Parameters result = initializeResult();
        patients.forEach(patient -> {
            Parameters.ParametersParameterComponent patientReports = patientReports(
                    periodStart.getValueAsString(),
                    periodEnd.getValueAsString(),
                    patient,
                    statuses,
                    measures,
                    organization);
            if (patientReports != null) {
                result.addParameter(patientReports);
            }
        });
        return result;
    }

    public void validateConfiguration() {
        checkNotNull(careGapsProperties, "Setting care-gaps properties are required for the $care-gaps operation.");
        checkArgument(
                !Strings.isNullOrEmpty(careGapsProperties.getCareGapsReporter()),
                "Setting care-gaps properties.care_gaps_reporter setting is required for the $care-gaps operation.");
        checkArgument(
                !Strings.isNullOrEmpty(careGapsProperties.getCareGapsCompositionSectionAuthor()),
                "Setting care-gaps properties.care_gaps_composition_section_author is required for the $care-gaps operation.");
        checkArgument(
                !Strings.isNullOrEmpty(serverBase),
                "The fhirBaseUrl setting is required for the $care-gaps operation.");
        Resource configuredReporter = addConfiguredResource(
                Organization.class, careGapsProperties.getCareGapsReporter(), "care_gaps_reporter");
        Resource configuredAuthor = addConfiguredResource(
                Organization.class,
                careGapsProperties.getCareGapsCompositionSectionAuthor(),
                "care_gaps_composition_section_author");

        checkNotNull(
                configuredReporter,
                String.format(
                        "The %s Resource is configured as the CareGapsProperties.care_gaps_reporter but the Resource could not be read.",
                        careGapsProperties.getCareGapsReporter()));
        checkNotNull(
                configuredAuthor,
                String.format(
                        "The %s Resource is configured as the CareGapsProperties.care_gaps_composition_section_author but the Resource could not be read.",
                        careGapsProperties.getCareGapsCompositionSectionAuthor()));
    }

    List<Patient> getPatientListFromSubject(String subject) {
        if (subject.startsWith("Patient/")) {
            return Collections.singletonList(validatePatientExists(subject));
        } else if (subject.startsWith("Group/")) {
            return getPatientListFromGroup(subject);
        }

        ourLog.info("Subject member was not a Patient or a Group, so skipping. \n{}", subject);
        return Collections.emptyList();
    }

    List<Patient> getPatientListFromGroup(String subjectGroupId) {
        List<Patient> patientList = new ArrayList<>();
        Group group = repository.read(Group.class, newId(subjectGroupId));
        if (group == null) {
            throw new IllegalArgumentException(Msg.code(2276) + "Could not find Group: " + subjectGroupId);
        }

        group.getMember().forEach(member -> {
            Reference reference = member.getEntity();
            if (reference.getReferenceElement().getResourceType().equals("Patient")) {
                Patient patient = validatePatientExists(reference.getReference());
                patientList.add(patient);
            } else if (reference.getReferenceElement().getResourceType().equals("Group")) {
                patientList.addAll(getPatientListFromGroup(reference.getReference()));
            } else {
                ourLog.info("Group member was not a Patient or a Group, so skipping. \n{}", reference.getReference());
            }
        });

        return patientList;
    }

    Patient validatePatientExists(String patientRef) {
        Patient patient = repository.read(Patient.class, new IdType(patientRef));
        if (patient == null) {
            throw new IllegalArgumentException(Msg.code(2277) + "Could not find Patient: " + patientRef);
        }

        return patient;
    }

    List<Measure> getMeasures(
            List<String> measureIds, List<String> measureIdentifiers, List<CanonicalType> measureCanonicals) {
        boolean hasMeasureIds = measureIds != null && !measureIds.isEmpty();
        boolean hasMeasureIdentifiers = measureIdentifiers != null && !measureIdentifiers.isEmpty();
        boolean hasMeasureUrls = measureCanonicals != null && !measureCanonicals.isEmpty();
        if (!hasMeasureIds && !hasMeasureIdentifiers && !hasMeasureUrls) {
            return Collections.emptyList();
        }

        List<Measure> measureList = new ArrayList<>();

        if (hasMeasureIds) {
            for (int i = 0; i < measureIds.size(); i++) {
                Measure measureById = resolveById(new IdType("Measure", measureIds.get(i)));
                measureList.add(measureById);
            }
        }

        if (hasMeasureUrls) {
            for (int i = 0; i < measureCanonicals.size(); i++) {
                Measure measureByUrl = resolveByUrl(measureCanonicals.get(i));
                measureList.add(measureByUrl);
            }
        }

        // TODO: implement searching by measure identifiers
        if (hasMeasureIdentifiers) {
            throw new NotImplementedOperationException(
                    Msg.code(2278) + "Measure identifiers have not yet been implemented.");
        }

        Map<String, Measure> result = new HashMap<>();
        measureList.forEach(measure -> result.putIfAbsent(measure.getUrl(), measure));

        return new ArrayList<>(result.values());
    }

    protected Measure resolveByUrl(CanonicalType url) {
        Canonicals.CanonicalParts parts = Canonicals.getParts(url);
        Bundle result = this.repository.search(
                Bundle.class, Measure.class, Searches.byNameAndVersion(parts.idPart(), parts.version()));
        return (Measure) result.getEntryFirstRep().getResource();
    }

    protected Measure resolveById(IdType id) {
        return this.repository.read(Measure.class, id);
    }

    private <T extends Resource> T addConfiguredResource(Class<T> resourceClass, String id, String key) {
        T resource = null;
        // read resource from repository
        resource = repository.read(resourceClass, new IdType(id));
        // add resource to configured resources
        configuredResources.put(key, resource);
        return resource;
    }

    private List<Measure> ensureMeasures(List<Measure> measures) {
        measures.forEach(measure -> {
            if (!measure.hasScoring()) {
                ourLog.info("Measure does not specify a scoring so skipping: {}.", measure.getId());
                measures.remove(measure);
            }
            if (!measure.hasImprovementNotation()) {
                ourLog.info("Measure does not specify an improvement notation so skipping: {}.", measure.getId());
                measures.remove(measure);
            }
        });
        return measures;
    }

    private Parameters.ParametersParameterComponent patientReports(
            String periodStart,
            String periodEnd,
            Patient patient,
            List<String> statuses,
            List<Measure> measures,
            String organization) {
        // TODO: add organization to report, if it exists.
        Composition composition = getComposition(patient);
        List<DetectedIssue> detectedIssues = new ArrayList<>();
        Map<String, Resource> evalPlusSDE = new HashMap<>();
        List<MeasureReport> reports = getReports(
                periodStart, periodEnd, patient, statuses, measures, composition, detectedIssues, evalPlusSDE);

        if (reports.isEmpty()) {
            return null;
        }

        return initializePatientParameter(patient)
                .setResource(addBundleEntries(serverBase, composition, detectedIssues, reports, evalPlusSDE));
    }

    private List<MeasureReport> getReports(
            String periodStart,
            String periodEnd,
            Patient patient,
            List<String> statuses,
            List<Measure> measures,
            Composition composition,
            List<DetectedIssue> detectedIssues,
            Map<String, Resource> evalPlusSDEs) {

        List<MeasureReport> reports = new ArrayList<>();
        MeasureReport report;

        String reportType = MeasureReportType.INDIVIDUAL.toString();
        R4MeasureProcessor r4MeasureProcessor =
                new R4MeasureProcessor(repository, measureEvaluationOptions, new R4RepositorySubjectProvider());

        for (Measure measure : measures) {

            List<String> subjects = Collections.singletonList(Ids.simple(patient));

            report = r4MeasureProcessor.evaluateMeasure(
                    Eithers.forMiddle3(measure.getIdElement()),
                    periodStart,
                    periodEnd,
                    reportType,
                    subjects,
                    null,
                    null);

            if (!report.hasGroup()) {
                ourLog.info(
                        "Report does not include a group so skipping.\nSubject: {}\nMeasure: {}",
                        Ids.simple(patient),
                        Ids.simplePart(measure));
                continue;
            }

            initializeReport(report);

            CareGapsStatusCode gapStatus = getGapStatus(measure, report);
            if (!statuses.contains(gapStatus.toString())) {
                continue;
            }

            DetectedIssue detectedIssue = getDetectedIssue(patient, report, gapStatus);
            detectedIssues.add(detectedIssue);
            composition.addSection(getSection(measure, report, detectedIssue, gapStatus));
            populateEvaluatedResources(report, evalPlusSDEs);
            populateSDEResources(report, evalPlusSDEs);
            reports.add(report);
        }

        return reports;
    }

    private void initializeReport(MeasureReport measureReport) {
        if (Strings.isNullOrEmpty(measureReport.getId())) {
            IIdType id = Ids.newId(MeasureReport.class, UUID.randomUUID().toString());
            measureReport.setId(id);
        }
        Reference reporter = new Reference().setReference(careGapsProperties.getCareGapsReporter());
        // TODO: figure out what this extension is for
        // reporter.addExtension(new
        // Extension().setUrl(CARE_GAPS_MEASUREREPORT_REPORTER_EXTENSION));
        measureReport.setReporter(reporter);
        if (measureReport.hasMeta()) {
            measureReport.getMeta().addProfile(CARE_GAPS_REPORT_PROFILE);
        } else {
            measureReport.setMeta(new Meta().addProfile(CARE_GAPS_REPORT_PROFILE));
        }
    }

    private Parameters.ParametersParameterComponent initializePatientParameter(Patient patient) {
        Parameters.ParametersParameterComponent patientParameter = Resources.newBackboneElement(
                        Parameters.ParametersParameterComponent.class)
                .setName("return");
        patientParameter.setId("subject-" + Ids.simplePart(patient));
        return patientParameter;
    }

    private Bundle addBundleEntries(
            String serverBase,
            Composition composition,
            List<DetectedIssue> detectedIssues,
            List<MeasureReport> measureReports,
            Map<String, Resource> evalPlusSDEs) {
        Bundle reportBundle = getBundle();
        reportBundle.addEntry(getBundleEntry(serverBase, composition));
        measureReports.forEach(report -> reportBundle.addEntry(getBundleEntry(serverBase, report)));
        detectedIssues.forEach(detectedIssue -> reportBundle.addEntry(getBundleEntry(serverBase, detectedIssue)));
        configuredResources.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
        evalPlusSDEs.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
        return reportBundle;
    }

    private CareGapsStatusCode getGapStatus(Measure measure, MeasureReport measureReport) {
        Pair<String, Boolean> inNumerator = new MutablePair<>("numerator", false);
        Pair<String, Boolean> inDenominator = new MutablePair<>("denominator", false);
        measureReport.getGroup().forEach(group -> group.getPopulation().forEach(population -> {
            if (population.hasCode()
                    && population.getCode().hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inNumerator.getKey())
                    && population.getCount() == 1) {
                inNumerator.setValue(true);
            }
            if (population.hasCode()
                    && population.getCode().hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inDenominator.getKey())
                    && population.getCount() == 1) {
                inDenominator.setValue(true);
            }
        }));

        boolean isPositive =
                measure.getImprovementNotation().hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, "increase");

        if (!inDenominator.getValue()) {
            // patient is not in eligible population
            return CareGapsStatusCode.NOT_APPLICABLE;
        }

        if (inDenominator.getValue()
                && ((isPositive && !inNumerator.getValue()) || (!isPositive && inNumerator.getValue()))) {
            return CareGapsStatusCode.OPEN_GAP;
        }

        return CareGapsStatusCode.CLOSED_GAP;
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

    private Bundle getBundle() {
        return new BundleBuilder<>(Bundle.class)
                .withProfile(CARE_GAPS_BUNDLE_PROFILE)
                .withType(Bundle.BundleType.DOCUMENT.toString())
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

    protected void populateEvaluatedResources(MeasureReport measureReport, Map<String, Resource> resources) {
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

    protected void populateSDEResources(MeasureReport measureReport, Map<String, Resource> resources) {
        if (measureReport.hasExtension()) {
            for (Extension extension : measureReport.getExtension()) {
                if (extension.hasUrl() && extension.getUrl().equals(MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION)) {
                    Reference sdeRef = extension.hasValue() && extension.getValue() instanceof Reference
                            ? (Reference) extension.getValue()
                            : null;
                    if (sdeRef != null
                            && sdeRef.hasReference()
                            && !sdeRef.getReference().startsWith("#")) {
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

    private Parameters initializeResult() {
        return newResource(Parameters.class, "care-gaps-report-" + UUID.randomUUID());
    }

    public static String getFullUrl(String serverAddress, IBaseResource resource) {
        checkArgument(
                resource.getIdElement().hasIdPart(),
                "Cannot generate a fullUrl because the resource does not have an id.");
        return getFullUrl(serverAddress, resource.fhirType(), Ids.simplePart(resource));
    }

    public static String getFullUrl(String serverAddress, String fhirType, String elementId) {
        return String.format("%s%s/%s", serverAddress + (serverAddress.endsWith("/") ? "" : "/"), fhirType, elementId);
    }

    public CareGapsProperties getCareGapsProperties() {
        return careGapsProperties;
    }

    public void checkValidStatusCode(List<String> statuses) {
        for (int x = 0; x < statuses.size(); x++) {
            var status = statuses.get(x);
            if (!status.equals(CareGapsStatusCode.CLOSED_GAP.toString())
                    && !status.equals(CareGapsStatusCode.OPEN_GAP.toString())
                    && !status.equals(CareGapsStatusCode.NOT_APPLICABLE.toString())) {
                throw new RuntimeException("CareGap status parameter: " + status + " is not an accepted value");
            }
        }
    }
}
