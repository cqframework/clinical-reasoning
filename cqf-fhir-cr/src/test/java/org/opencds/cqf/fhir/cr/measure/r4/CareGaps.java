package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_DETECTED_ISSUE_MR_GROUP_ID;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_SYSTEM;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class CareGaps {

    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";

    @FunctionalInterface
    interface Validator<T> {
        void validate(T value);
    }

    @FunctionalInterface
    interface Selector<T, S> {
        T select(S from);
    }

    interface ChildOf<T> {
        T up();
    }

    interface SelectedOf<T> {
        T value();
    }

    protected static class Selected<T, P> implements CareGaps.SelectedOf<T>, CareGaps.ChildOf<P> {
        private final P parent;
        private final T value;

        public Selected(T value, P parent) {
            this.parent = parent;
            this.value = value;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public P up() {
            return parent;
        }
    }

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private IRepository repository;
        private MeasureEvaluationOptions evaluationOptions;
        private CareGapsProperties careGapsProperties;
        private final String serverBase;
        private final MeasurePeriodValidator measurePeriodEvaluator;

        public Given() {
            this.evaluationOptions = MeasureEvaluationOptions.defaultOptions();
            this.evaluationOptions
                    .getEvaluationSettings()
                    .getRetrieveSettings()
                    .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                    .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

            this.evaluationOptions
                    .getEvaluationSettings()
                    .getTerminologySettings()
                    .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);

            this.careGapsProperties = new CareGapsProperties();

            this.careGapsProperties.setCareGapsReporter("alphora");
            this.careGapsProperties.setCareGapsCompositionSectionAuthor("alphora-author");
            this.serverBase = "http://localhost";
            this.measurePeriodEvaluator = new MeasurePeriodValidator();
        }

        public CareGaps.Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        public CareGaps.Given repositoryFor(String repositoryPath) {
            this.repository = new IgRepository(
                    FhirContext.forR4Cached(),
                    Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public CareGaps.Given evaluationOptions(MeasureEvaluationOptions evaluationOptions) {
            this.evaluationOptions = evaluationOptions;
            return this;
        }

        public CareGaps.Given careGapsProperties(CareGapsProperties careGapsProperties) {
            this.careGapsProperties = careGapsProperties;
            return this;
        }

        private R4CareGapsService buildCareGapsService() {
            return new R4CareGapsService(
                    careGapsProperties, repository, evaluationOptions, serverBase, measurePeriodEvaluator);
        }

        public When when() {
            return new When(buildCareGapsService());
        }
    }

    public static class When {

        private final R4CareGapsService service;

        When(R4CareGapsService service) {
            this.service = service;
        }

        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        private String subject;
        private List<String> status = new ArrayList<>();
        private List<IdType> measureId = new ArrayList<>();
        private List<String> measureIdentifier = new ArrayList<>();
        private List<CanonicalType> measureUrl = new ArrayList<>();
        private Supplier<Parameters> operation;

        private boolean notDocument;

        public CareGaps.When periodEnd(ZonedDateTime periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public CareGaps.When periodStart(ZonedDateTime periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public CareGaps.When subject(String subject) {
            this.subject = subject;
            return this;
        }

        public CareGaps.When status(String status) {
            this.status.add(status);
            return this;
        }

        public CareGaps.When measureId(String measureId) {
            this.measureId.add(new IdType("Measure", measureId));
            return this;
        }

        public CareGaps.When measureUrl(String measureUrl) {
            this.measureUrl.add(new CanonicalType(measureUrl));
            return this;
        }

        public CareGaps.When measureIdentifier(String measureIdentifier) {
            this.measureIdentifier.add(measureIdentifier);
            return this;
        }

        public CareGaps.When notDocument(boolean notDocument) {
            this.notDocument = notDocument;
            return this;
        }

        public CareGaps.When getCareGapsReport() {
            this.operation = () -> service.getCareGapsReport(
                    periodStart, periodEnd, subject, status, measureId, measureIdentifier, measureUrl, notDocument);
            return this;
        }

        public SelectedReport then() {
            if (this.operation == null) {
                throw new IllegalStateException(
                        "No operation was selected as part of 'when'. Choose an operation to invoke by adding one, such as 'evaluate' to the method chain.");
            }

            return new SelectedReport(this.operation.get());
        }
    }

    public static class SelectedReport extends CareGaps.Selected<Parameters, Void> {
        public SelectedReport(Parameters report) {
            super(report, null);
        }

        public Parameters report() {
            return this.value();
        }

        public SelectedBundle firstParameter() {
            return this.parameter(x -> resourceToBundle(x.getParameter().get(0).getResource()));
        }
        // select subjects bundle from parameter results
        public SelectedBundle parameter(String id) {
            return this.parameter(g -> resourceToBundle(g.getParameter().stream()
                    .filter(x -> x.getId().contains(id))
                    .findFirst()
                    .get()
                    .getResource()));
        }

        public SelectedBundle parameter(CareGaps.Selector<Bundle, Parameters> parameterSelector) {
            var g = parameterSelector.select(value());
            return new SelectedBundle(g, this);
        }

        public SelectedReport hasBundleCount(int count) {
            assertEquals(count, report().getParameter().size());
            return this;
        }

        public Bundle resourceToBundle(Resource theResource) {
            IParser parser = FhirContext.forR4Cached().newJsonParser();
            return (Bundle) parser.parseResource(parser.encodeResourceToString(theResource));
        }
    }

    static class SelectedBundle extends Selected<Bundle, SelectedReport> {

        public SelectedBundle(Bundle value, SelectedReport parent) {
            super(value, parent);
        }

        public Bundle bundleReport() {
            return this.value();
        }
        // DetectedIssue getters
        public SelectedDetectedIssue detectedIssue() {
            return this.detectedIssue(g -> resourceToDetectedIssue(g.getEntry().stream()
                    .filter(x -> x.getResource().getResourceType().toString().equals("DetectedIssue"))
                    .findFirst()
                    .get()
                    .getResource()));
        }

        public SelectedDetectedIssue detectedIssue(Selector<DetectedIssue, Bundle> bundleSelector) {
            var p = bundleSelector.select(value());
            return new SelectedDetectedIssue(p, this);
        }

        public SelectedBundle detectedIssueCount(int detectedIssueCount) {
            assertEquals(
                    detectedIssueCount,
                    bundleReport().getEntry().stream()
                            .filter(x ->
                                    x.getResource().getResourceType().toString().equals("DetectedIssue"))
                            .toList()
                            .size());
            return this;
        }

        public SelectedBundle bundleEntryCount(int count) {
            assertEquals(count, bundleReport().getEntry().size());
            return this;
        }

        public DetectedIssue resourceToDetectedIssue(Resource theResource) {
            IParser parser = FhirContext.forR4Cached().newJsonParser();
            return (DetectedIssue) parser.parseResource(parser.encodeResourceToString(theResource));
        }

        // Composition getters
        public SelectedComposition composition() {
            return this.composition(g -> resourceToComposition(g.getEntry().stream()
                    .filter(x -> x.getResource().getResourceType().toString().equals("Composition"))
                    .findFirst()
                    .get()
                    .getResource()));
        }

        public SelectedComposition composition(Selector<Composition, Bundle> bundleSelector) {
            var p = bundleSelector.select(value());
            return new SelectedComposition(p, this);
        }

        public Composition resourceToComposition(Resource theResource) {
            IParser parser = FhirContext.forR4Cached().newJsonParser();
            return (Composition) parser.parseResource(parser.encodeResourceToString(theResource));
        }
        // MeasureReport getters

        public SelectedBundle measureReportCount(int measureReportCount) {
            assertEquals(
                    measureReportCount,
                    bundleReport().getEntry().stream()
                            .filter(x ->
                                    x.getResource().getResourceType().toString().equals("MeasureReport"))
                            .toList()
                            .size());
            return this;
        }

        public SelectedMeasureReport measureReport(Selector<MeasureReport, Bundle> bundleSelector) {
            var p = bundleSelector.select(value());
            return new SelectedMeasureReport(p, this);
        }

        public SelectedMeasureReport measureReport() {

            return this.measureReport(g -> resourceToMeasureReport(g.getEntry().stream()
                    .filter(x -> x.getResource().getResourceType().toString().equals("MeasureReport"))
                    .findFirst()
                    .get()
                    .getResource()));
        }

        public SelectedBundle measureReportEvaluatedResourcesFound() {
            // get resource References from the Patient Bundle
            List<String> bundleResourceReferences = bundleReport().getEntry().stream()
                    .map(BundleEntryComponent::getResource)
                    .map(x -> x.getResourceType().toString().concat("/" + x.getIdPart()))
                    .toList();

            // get resource References from evaluatedResources on Measure Report
            List<MeasureReport> measureReports = bundleReport().getEntry().stream()
                    .filter(x -> x.getResource() instanceof MeasureReport)
                    .map(g -> (MeasureReport) g.getResource())
                    .toList();

            // check all references are found in patient bundle
            for (MeasureReport report : measureReports) {
                var reportReferences = report.getEvaluatedResource();
                if (!reportReferences.isEmpty()) {
                    for (Reference reference : reportReferences) {
                        assertTrue(bundleResourceReferences.contains(reference.getReference()));
                    }
                }
            }
            return this;
        }

        public MeasureReport resourceToMeasureReport(Resource theResource) {
            IParser parser = FhirContext.forR4Cached().newJsonParser();
            return (MeasureReport) parser.parseResource(parser.encodeResourceToString(theResource));
        }
        // Organization getters

        public SelectedOrganization organization() {
            return this.organization(g -> resourceToOrganization(g.getEntry().stream()
                    .filter(x -> x.getResource().getResourceType().toString().equals("Organization"))
                    .findFirst()
                    .get()
                    .getResource()));
        }

        public SelectedOrganization organization(Selector<Organization, Bundle> bundleSelector) {
            var p = bundleSelector.select(value());
            return new SelectedOrganization(p, this);
        }

        public Organization resourceToOrganization(Resource theResource) {
            IParser parser = FhirContext.forR4Cached().newJsonParser();
            return (Organization) parser.parseResource(parser.encodeResourceToString(theResource));
        }
    }

    static class SelectedDetectedIssue extends Selected<DetectedIssue, SelectedBundle> {

        public SelectedDetectedIssue(DetectedIssue value, SelectedBundle parent) {
            super(value, parent);
        }

        public DetectedIssue detectedIssueReport() {
            return this.value();
        }

        public SelectedDetectedIssue hasCareGapStatus(String gapStatus) {
            var statusExt = detectedIssueReport().getModifierExtensionsByUrl(CARE_GAPS_GAP_STATUS_EXTENSION).stream()
                    .findFirst();
            CodeableConcept cc = (CodeableConcept) statusExt.get().getValue();
            assertEquals(CARE_GAPS_GAP_STATUS_SYSTEM, cc.getCodingFirstRep().getSystem());
            assertEquals(gapStatus, cc.getCodingFirstRep().getCode());
            return this;
        }

        public SelectedDetectedIssue hasIdentifiedPeriod() {
            assertTrue(detectedIssueReport().hasIdentifiedPeriod());
            return this;
        }

        public SelectedDetectedIssue hasImplicatedMeasureReference() {
            assertTrue(
                    detectedIssueReport().getImplicatedFirstRep().getReference().startsWith("Measure"));
            return this;
        }

        public SelectedDetectedIssue hasContainedMeasureReport() {
            var containedResource = detectedIssueReport().getContained().get(0);
            assertInstanceOf(MeasureReport.class, containedResource);
            return this;
        }

        public SelectedDetectedIssue hasContainedEvidenceReference() {
            assertTrue(detectedIssueReport()
                    .getEvidenceFirstRep()
                    .getDetailFirstRep()
                    .getReference()
                    .startsWith("#"));
            return this;
        }

        public SelectedDetectedIssue hasPatientReference(String patientRef) {
            assertEquals(patientRef, detectedIssueReport().getPatient().getReference());
            return this;
        }

        public SelectedDetectedIssue hasMeasureReportEvidence() {
            var evidenceDetail = detectedIssueReport().getEvidence().get(0).getDetail();
            assertNotNull(evidenceDetail.stream()
                    .filter(x -> x.getReference().contains("MeasureReport"))
                    .findFirst()
                    .get());
            return this;
        }

        public SelectedDetectedIssue hasGroupIdReportExtension(String groupId) {
            var groupIdExt = detectedIssueReport().getExtensionByUrl(CARE_GAPS_DETECTED_ISSUE_MR_GROUP_ID);
            assertEquals(groupIdExt.getValue().toString(), groupId);
            return this;
        }
    }

    static class SelectedComposition extends Selected<Composition, SelectedBundle> {

        public SelectedComposition(Composition value, SelectedBundle parent) {
            super(value, parent);
        }

        public Composition compositionReport() {
            return this.value();
        }
        // has Subject
        public SelectedComposition hasSubjectReference(String subjectRef) {
            assertEquals(subjectRef, compositionReport().getSubject().getReference());
            return this;
        }
        // author
        public SelectedComposition hasAuthor(String orgReference) {
            // author not empty
            assertNotNull(compositionReport().getAuthor().get(0));
            // author was found
            assertNotNull(compositionReport().getAuthor().stream()
                    .filter(x -> x.getReference().contains(orgReference))
                    .findFirst()
                    .get());
            return this;
        }

        public SelectedComposition sectionCount(int sectionCount) {
            // should match number of measures executed
            assertEquals(sectionCount, compositionReport().getSection().size());
            return this;
        }
    }

    static class SelectedOrganization extends Selected<Organization, SelectedBundle> {

        public SelectedOrganization(Organization value, SelectedBundle parent) {
            super(value, parent);
        }

        public Organization organizationReport() {
            return this.value();
        }

        // org resource found
        public SelectedOrganization orgResourceMatches(String theOrgId) {
            assertEquals(theOrgId, organizationReport().getId());
            return this;
        }
    }

    static class SelectedMeasureReport extends Selected<MeasureReport, SelectedBundle> {

        public SelectedMeasureReport(MeasureReport value, SelectedBundle parent) {
            super(value, parent);
        }

        public MeasureReport measureReport() {
            return this.value();
        }
        // measure url found
        public SelectedMeasureReport measureReportMatches(String measureReportUrl) {
            assertEquals(measureReportUrl, measureReport().getMeasure());
            return this;
        }
        // subject ref found
        public SelectedMeasureReport measureReportSubjectMatches(String subjectReference) {
            assertEquals(subjectReference, measureReport().getSubject().getReference());
            return this;
        }
        // reportType individual
        public SelectedMeasureReport measureReportTypeIndividual() {
            assertEquals(MeasureReportType.INDIVIDUAL, measureReport().getType());
            return this;
        }

        public SelectedMeasureReport hasContainedOperationOutcome() {
            assertTrue(measureReport().hasContained()
                    && measureReport().getContained().stream()
                            .anyMatch(t -> t.getResourceType().equals(ResourceType.OperationOutcome)));
            return this;
        }

        public SelectedMeasureReport hasContainedOperationOutcomeMsg(String msg) {
            assertTrue(measureReport().getContained().stream()
                    .filter(t -> t.getResourceType().equals(ResourceType.OperationOutcome))
                    .map(y -> (OperationOutcome) y)
                    .anyMatch(x -> x.getIssueFirstRep().getDiagnostics().contains(msg)));
            return this;
        }

        public SelectedMeasureReport hasStatus(MeasureReportStatus status) {
            assertEquals(status, measureReport().getStatus());
            return this;
        }
    }
}
