package org.opencds.cqf.cql.evaluator.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
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
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.builder.BundleBuilder;
import org.opencds.cqf.cql.evaluator.fhir.builder.CodeableConceptSettings;
import org.opencds.cqf.cql.evaluator.fhir.builder.CompositionBuilder;
import org.opencds.cqf.cql.evaluator.fhir.builder.CompositionSectionComponentBuilder;
import org.opencds.cqf.cql.evaluator.fhir.builder.DetectedIssueBuilder;
import org.opencds.cqf.cql.evaluator.fhir.builder.NarrativeSettings;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.cql.evaluator.fhir.util.Ids;
import org.opencds.cqf.cql.evaluator.fhir.util.Resources;
import org.opencds.cqf.cql.evaluator.measure.CareGapsProperties;
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportType;
import org.opencds.cqf.cql.evaluator.measure.enumeration.CareGapsStatusCode;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Searches;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Map.ofEntries;
import static org.hl7.fhir.r4.model.Factory.newId;
import static org.opencds.cqf.cql.evaluator.fhir.util.Resources.newResource;
import static org.opencds.cqf.cql.evaluator.measure.constant.CareGapsConstants.CARE_GAPS_BUNDLE_PROFILE;
import static org.opencds.cqf.cql.evaluator.measure.constant.CareGapsConstants.CARE_GAPS_COMPOSITION_PROFILE;
import static org.opencds.cqf.cql.evaluator.measure.constant.CareGapsConstants.CARE_GAPS_DETECTED_ISSUE_PROFILE;
import static org.opencds.cqf.cql.evaluator.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_EXTENSION;
import static org.opencds.cqf.cql.evaluator.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_SYSTEM;
import static org.opencds.cqf.cql.evaluator.measure.constant.CareGapsConstants.CARE_GAPS_REPORT_PROFILE;
import static org.opencds.cqf.cql.evaluator.measure.constant.HtmlConstants.HTML_DIV_PARAGRAPH_CONTENT;
import static org.opencds.cqf.cql.evaluator.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.cql.evaluator.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_POPULATION_SYSTEM;
import static org.opencds.cqf.cql.evaluator.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION;

public class R4CareGapsService {

  private static final Logger ourLog = LoggerFactory.getLogger(R4CareGapsService.class);
  public static final Map<String, CodeableConceptSettings> CARE_GAPS_CODES = ImmutableMap.of(
      "http://loinc.org/96315-7", new CodeableConceptSettings().add("http://loinc.org", "96315-7", "Gaps in care report"),
      "http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP", new CodeableConceptSettings().add("http://terminology.hl7.org/CodeSystem/v3-ActCode", "CAREGAP", "Care Gaps")
  );


  private final Repository myRepository;

  private final MeasureEvaluationOptions myMeasureEvaluationOptions;

  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

  private CareGapsProperties myCareGapsProperties;

  private Executor myCqlExecutor;

  private final Map<String, Resource> myConfiguredResources = new HashMap<>();

  public R4CareGapsService(CareGapsProperties theCareGapsProperties,
      Repository theRepository,
      MeasureEvaluationOptions theMeasureEvaluationOptions,
      Executor theExecutor){
    this.myRepository = theRepository;
    this.myCareGapsProperties = theCareGapsProperties;
    this.myMeasureEvaluationOptions = theMeasureEvaluationOptions;
    this.myCqlExecutor = theExecutor;
  }

  /**
   * Calculate measures describing gaps in care
   * @param thePeriodStart
   * @param thePeriodEnd
   * @param theTopic
   * @param theSubject
   * @param thePractitioner
   * @param theOrganization
   * @param theStatuses
   * @param theMeasureIds
   * @param theMeasureIdentifiers
   * @param theMeasureUrls
   * @param thePrograms
   * @return Parameters that includes zero to many document bundles that
   * include Care Gap Measure Reports will be returned.
   */
  public Parameters getCareGapsReport(IPrimitiveType<Date> thePeriodStart,
      IPrimitiveType<Date> thePeriodEnd,
      List<String> theTopic,
      String theSubject,
      String thePractitioner,
      String theOrganization,
      List<String> theStatuses,
      List<String> theMeasureIds,
      List<String> theMeasureIdentifiers,
      List<CanonicalType> theMeasureUrls,
      List<String> thePrograms) {

    validateConfiguration();

    List<Measure> measures = ensureMeasures(getMeasures(theMeasureIds, theMeasureIdentifiers, theMeasureUrls));

    List<Patient> patients;
    if (!Strings.isNullOrEmpty(theSubject)) {
      patients = getPatientListFromSubject(theSubject);
    } else {
      throw new NotImplementedOperationException(Msg.code(2275) + "Only the subject parameter has been implemented.");
    }

    List<CompletableFuture<ParametersParameterComponent>> futures = new ArrayList<>();
    Parameters result = initializeResult();
    if (myCareGapsProperties.getThreadedCareGapsEnabled()) {
      patients
          .forEach(
              patient -> {
                Parameters.ParametersParameterComponent patientReports = patientReports(
                    thePeriodStart.getValueAsString(), thePeriodEnd.getValueAsString(), patient, theStatuses, measures,
                    theOrganization);
                futures.add(CompletableFuture.supplyAsync(() -> patientReports, myCqlExecutor));
              });

      futures.forEach(x -> result.addParameter(x.join()));
    } else {
      patients.forEach(
          patient -> {
            Parameters.ParametersParameterComponent patientReports = patientReports(
                thePeriodStart.getValueAsString(), thePeriodEnd.getValueAsString(), patient, theStatuses, measures,
                theOrganization);
            if (patientReports != null) {
              result.addParameter(patientReports);
            }
          });
    }
    return result;
  }

  public void validateConfiguration() {
    checkNotNull(myCareGapsProperties,
        "Setting care-gaps properties are required for the $care-gaps operation.");
    checkArgument(!Strings.isNullOrEmpty(myCareGapsProperties.getCareGapsReporter()),
        "Setting care-gaps properties.care_gaps_reporter setting is required for the $care-gaps operation.");
    checkArgument(!Strings.isNullOrEmpty(myCareGapsProperties.getCareGapsCompositionSectionAuthor()),
        "Setting care-gaps properties.care_gaps_composition_section_author is required for the $care-gaps operation.");
    checkNotNull(!Strings.isNullOrEmpty(myCareGapsProperties.getMyFhirBaseUrl()),
        "The fhirBaseUrl setting is required for the $care-gaps operation.");
    Resource configuredReporter = addConfiguredResource(Organization.class,
        myCareGapsProperties.getCareGapsReporter(), "care_gaps_reporter");
    Resource configuredAuthor = addConfiguredResource(Organization.class,
        myCareGapsProperties.getCareGapsCompositionSectionAuthor(),
        "care_gaps_composition_section_author");

    checkNotNull(configuredReporter, String.format(
        "The %s Resource is configured as the CareGapsProperties.care_gaps_reporter but the Resource could not be read.",
        myCareGapsProperties.getCareGapsReporter()));
    checkNotNull(configuredAuthor, String.format(
        "The %s Resource is configured as the CareGapsProperties.care_gaps_composition_section_author but the Resource could not be read.",
        myCareGapsProperties.getCareGapsCompositionSectionAuthor()));
  }
  List<Patient> getPatientListFromSubject(String theSubject) {
    if (theSubject.startsWith("Patient/")) {
      return Collections.singletonList(validatePatientExists(theSubject));
    } else if (theSubject.startsWith("Group/")) {
      return getPatientListFromGroup(theSubject);
    }

    ourLog.info("Subject member was not a Patient or a Group, so skipping. \n{}", theSubject);
    return Collections.emptyList();
  }

  List<Patient> getPatientListFromGroup(String theSubjectGroupId) {
    List<Patient> patientList = new ArrayList<>();
    Group group = myRepository.read(Group.class, newId(theSubjectGroupId));
    if (group == null) {
      throw new IllegalArgumentException(Msg.code(2276) + "Could not find Group: " + theSubjectGroupId);
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

  Patient validatePatientExists(String thePatientRef) {
    Patient patient = myRepository.read(Patient.class, new IdType(thePatientRef));
    if (patient == null) {
      throw new IllegalArgumentException(Msg.code(2277) + "Could not find Patient: " + thePatientRef);
    }

    return patient;
  }

  List<Measure> getMeasures(List<String> theMeasureIds, List<String> theMeasureIdentifiers,
      List<CanonicalType> theMeasureCanonicals) {
    boolean hasMeasureIds = theMeasureIds != null && !theMeasureIds.isEmpty();
    boolean hasMeasureIdentifiers =
        theMeasureIdentifiers != null && !theMeasureIdentifiers.isEmpty();
    boolean hasMeasureUrls = theMeasureCanonicals != null && !theMeasureCanonicals.isEmpty();
    if (!hasMeasureIds && !hasMeasureIdentifiers && !hasMeasureUrls) {
      return Collections.emptyList();
    }

    List<Measure> measureList = new ArrayList<>();

    if (hasMeasureIds) {
      for (int i = 0; i < theMeasureIds.size(); i++) {
        Measure measureById = resolveById(new IdType("Measure", theMeasureIds.get(i)));
        measureList.add(measureById);
      }
    }

    if (hasMeasureUrls) {
      for (int i = 0; i < theMeasureCanonicals.size(); i++) {
        Measure measureByUrl = resolveByUrl(theMeasureCanonicals.get(i));
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
    Bundle result = this.myRepository.search(Bundle.class, Measure.class, Searches.byNameAndVersion(
        parts.idPart(),
        parts.version()));
    return (Measure) result.getEntryFirstRep().getResource();
  }

  protected Measure resolveById(IdType id) {
    return this.myRepository.read(Measure.class, id);
  }

  private <T extends Resource> T addConfiguredResource(Class<T> theResourceClass, String theId, String theKey) {
    T resource = null;
    // read resource from repository
    resource = myRepository.read(theResourceClass,new IdType(theId));
    // add resource to configured resources
    myConfiguredResources.put(theKey, resource);
    return resource;
  }

  private List<Measure> ensureMeasures(List<Measure> theMeasures) {
    theMeasures.forEach(measure -> {
      if (!measure.hasScoring()) {
        ourLog.info("Measure does not specify a scoring so skipping: {}.", measure.getId());
        theMeasures.remove(measure);
      }
      if (!measure.hasImprovementNotation()) {
        ourLog.info("Measure does not specify an improvement notation so skipping: {}.", measure.getId());
        theMeasures.remove(measure);
      }
    });
    return theMeasures;
  }

  private Parameters.ParametersParameterComponent patientReports( String thePeriodStart,
      String thePeriodEnd, Patient thePatient, List<String> theStatuses, List<Measure> theMeasures, String theOrganization) {
    // TODO: add organization to report, if it exists.
    Composition composition = getComposition(thePatient);
    List<DetectedIssue> detectedIssues = new ArrayList<>();
    Map<String, Resource> evalPlusSDE = new HashMap<>();
    List<MeasureReport> reports = getReports(thePeriodStart, thePeriodEnd, thePatient, theStatuses, theMeasures,
        composition, detectedIssues, evalPlusSDE);

    if (reports.isEmpty()) {
      return null;
    }

    return initializePatientParameter(thePatient).setResource(

        addBundleEntries(myCareGapsProperties.getMyFhirBaseUrl(), composition, detectedIssues, reports, evalPlusSDE));
  }

  private List<MeasureReport> getReports( String thePeriodStart, String thePeriodEnd,
      Patient thePatient, List<String> theStatuses, List<Measure> theMeasures, Composition theComposition,
      List<DetectedIssue> theDetectedIssues, Map<String, Resource> theEvalPlusSDEs) {

    List<MeasureReport> reports = new ArrayList<>();
    MeasureReport report;

    String theReportType = MeasureReportType.INDIVIDUAL.toString();
    R4MeasureProcessor r4MeasureProcessor = new R4MeasureProcessor(myRepository, myMeasureEvaluationOptions, new R4RepositorySubjectProvider(myRepository));

    for (Measure measure : theMeasures) {
      
      List<String> subjects = Collections.singletonList(Ids.simple(thePatient));

      report = r4MeasureProcessor.evaluateMeasure(Eithers.forMiddle3(measure.getIdElement()), thePeriodStart, thePeriodEnd, theReportType, subjects, null);


      if (!report.hasGroup()) {
        ourLog.info("Report does not include a group so skipping.\nSubject: {}\nMeasure: {}",
            Ids.simple(thePatient),
            Ids.simplePart(measure));
        continue;
      }

      initializeReport(report);

      CareGapsStatusCode gapStatus = getGapStatus(measure, report);
      if (!theStatuses.contains(gapStatus.toString())) {
        continue;
      }

      DetectedIssue detectedIssue = getDetectedIssue(thePatient, report, gapStatus);
      theDetectedIssues.add(detectedIssue);
      theComposition.addSection(getSection(measure, report, detectedIssue, gapStatus));
      populateEvaluatedResources(report, theEvalPlusSDEs);
      populateSDEResources(report, theEvalPlusSDEs);
      reports.add(report);
    }

    return reports;
  }

  private void initializeReport(MeasureReport theMeasureReport) {
    if (Strings.isNullOrEmpty(theMeasureReport.getId())) {
      IIdType id = Ids.newId(MeasureReport.class, UUID.randomUUID().toString());
      theMeasureReport.setId(id);
    }
    Reference reporter = new Reference().setReference(myCareGapsProperties.getCareGapsReporter());
    // TODO: figure out what this extension is for
    // reporter.addExtension(new
    // Extension().setUrl(CARE_GAPS_MEASUREREPORT_REPORTER_EXTENSION));
    theMeasureReport.setReporter(reporter);
    if (theMeasureReport.hasMeta()) {
      theMeasureReport.getMeta().addProfile(CARE_GAPS_REPORT_PROFILE);
    } else {
      theMeasureReport.setMeta(new Meta().addProfile(CARE_GAPS_REPORT_PROFILE));
    }
  }

  private Parameters.ParametersParameterComponent initializePatientParameter(Patient thePatient) {
    Parameters.ParametersParameterComponent patientParameter = Resources
        .newBackboneElement(Parameters.ParametersParameterComponent.class)
        .setName("return");
    patientParameter.setId("subject-" + Ids.simplePart(thePatient));
    return patientParameter;
  }

  private Bundle addBundleEntries(String theServerBase, Composition theComposition, List<DetectedIssue> theDetectedIssues,
      List<MeasureReport> theMeasureReports, Map<String, Resource> theEvalPlusSDEs) {
    Bundle reportBundle = getBundle();
    reportBundle.addEntry(getBundleEntry(theServerBase, theComposition));
    theMeasureReports.forEach(report -> reportBundle.addEntry(getBundleEntry(theServerBase, report)));
    theDetectedIssues.forEach(detectedIssue -> reportBundle.addEntry(getBundleEntry(theServerBase, detectedIssue)));
    myConfiguredResources.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(theServerBase, resource)));
    theEvalPlusSDEs.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(theServerBase, resource)));
    return reportBundle;
  }

  private CareGapsStatusCode getGapStatus(Measure theMeasure, MeasureReport theMeasureReport) {
    Pair<String, Boolean> inNumerator = new MutablePair<>("numerator", false);
    theMeasureReport.getGroup().forEach(group -> group.getPopulation().forEach(population -> {
      if (population.hasCode()
          && population.getCode().hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inNumerator.getKey())
          && population.getCount() == 1) {
        inNumerator.setValue(true);
      }
    }));

    boolean isPositive = theMeasure.getImprovementNotation().hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
        "increase");

    if ((isPositive && !inNumerator.getValue()) || (!isPositive && inNumerator.getValue())) {
      return CareGapsStatusCode.OPEN_GAP;
    }

    return CareGapsStatusCode.CLOSED_GAP;
  }

  private Bundle.BundleEntryComponent getBundleEntry(String theServerBase, Resource theResource) {
    return new Bundle.BundleEntryComponent().setResource(theResource)
        .setFullUrl(getFullUrl(theServerBase, theResource));
  }

  private Composition.SectionComponent getSection(Measure theMeasure, MeasureReport theMeasureReport, DetectedIssue theDetectedIssue,
      CareGapsStatusCode theGapStatus) {
    String narrative = String.format(HTML_DIV_PARAGRAPH_CONTENT,
        theGapStatus == CareGapsStatusCode.CLOSED_GAP ? "No detected issues."
            : String.format("Issues detected.  See %s for details.", Ids.simple(theDetectedIssue)));
    return new CompositionSectionComponentBuilder<>(Composition.SectionComponent.class)
        .withTitle(theMeasure.hasTitle() ? theMeasure.getTitle() : theMeasure.getUrl())
        .withFocus(Ids.simple(theMeasureReport))
        .withText(new NarrativeSettings(narrative))
        .withEntry(Ids.simple(theDetectedIssue))
        .build();
  }

  private Bundle getBundle() {
    return new BundleBuilder<>(Bundle.class)
        .withProfile(CARE_GAPS_BUNDLE_PROFILE)
        .withType(Bundle.BundleType.DOCUMENT.toString())
        .build();
  }

  private Composition getComposition(Patient thePatient) {
    return new CompositionBuilder<>(Composition.class)
        .withProfile(CARE_GAPS_COMPOSITION_PROFILE)
        .withType(CARE_GAPS_CODES.get("http://loinc.org/96315-7"))
        .withStatus(Composition.CompositionStatus.FINAL.toString())
        .withTitle("Care Gap Report for " + Ids.simplePart(thePatient))
        .withSubject(Ids.simple(thePatient))
        .withAuthor(Ids.simple(myConfiguredResources.get("care_gaps_composition_section_author")))
        // .withCustodian(organization) // TODO: Optional: identifies the organization
        // who is responsible for ongoing maintenance of and accessing to this gaps in
        // care report. Add as a setting and optionally read if it's there.
        .build();
  }

  private DetectedIssue getDetectedIssue(Patient thePatient, MeasureReport theMeasureReport, CareGapsStatusCode theCareGapStatusCode) {
    return new DetectedIssueBuilder<>(DetectedIssue.class)
        .withProfile(CARE_GAPS_DETECTED_ISSUE_PROFILE)
        .withStatus(DetectedIssue.DetectedIssueStatus.FINAL.toString())
        .withCode(CARE_GAPS_CODES.get("http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP"))
        .withPatient(Ids.simple(thePatient))
        .withEvidenceDetail(Ids.simple(theMeasureReport))
        .withModifierExtension(new ImmutablePair<>(
            CARE_GAPS_GAP_STATUS_EXTENSION,
            new CodeableConceptSettings().add(CARE_GAPS_GAP_STATUS_SYSTEM, theCareGapStatusCode.toString(),
                theCareGapStatusCode.toDisplayString())))
        .build();
  }

  protected void populateEvaluatedResources(MeasureReport theMeasureReport, Map<String, Resource> theResources) {
    theMeasureReport.getEvaluatedResource().forEach(evaluatedResource -> {
      IIdType resourceId = evaluatedResource.getReferenceElement();
      if (resourceId.getResourceType() == null || theResources.containsKey(Ids.simple(resourceId))) {
        return;
      }

      Class<? extends IBaseResource> resourceType = fhirContext.getResourceDefinition(resourceId.getResourceType()).newInstance().getClass();
      IBaseResource resource = myRepository.read(resourceType, resourceId);

      if (resource instanceof Resource) {
        Resource resourceBase = (Resource) resource;
        theResources.put(Ids.simple(resourceId), resourceBase);
      }
    });
  }

  protected void populateSDEResources(MeasureReport theMeasureReport, Map<String, Resource> theResources) {
    if (theMeasureReport.hasExtension()) {
      for (Extension extension : theMeasureReport.getExtension()) {
        if (extension.hasUrl() && extension.getUrl().equals(MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION)) {
          Reference sdeRef = extension.hasValue() && extension.getValue() instanceof Reference
              ? (Reference) extension.getValue()
              : null;
          if (sdeRef != null && sdeRef.hasReference() && !sdeRef.getReference().startsWith("#")) {
            IdType sdeId = new IdType(sdeRef.getReference());
            if (!theResources.containsKey(Ids.simple(sdeId))) {
              Class<? extends IBaseResource> resourceType = fhirContext.getResourceDefinition(sdeId.getResourceType()).newInstance().getClass();
              IBaseResource resource = myRepository.read(resourceType, sdeId);
              if (resource instanceof Resource) {
                Resource resourceBase = (Resource) resource;
                theResources.put(Ids.simple(sdeId), resourceBase);
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

  public static String getFullUrl(String theServerAddress, IBaseResource theResource) {
    checkArgument(theResource.getIdElement().hasIdPart(),
        "Cannot generate a fullUrl because the resource does not have an id.");
    return getFullUrl(theServerAddress, theResource.fhirType(), Ids.simplePart(theResource));
  }

  public static String getFullUrl(String theServerAddress, String theFhirType, String theElementId) {
    return String.format("%s%s/%s", theServerAddress + (theServerAddress.endsWith("/") ? "" : "/"), theFhirType,
        theElementId);
  }

  public CareGapsProperties getCareGapsProperties() {
    return myCareGapsProperties;
  }


}
