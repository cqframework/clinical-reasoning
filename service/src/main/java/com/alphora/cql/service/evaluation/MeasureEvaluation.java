// package com.alphora.cql.service.evaluation;

// import org.hl7.fhir.dstu3.model.MeasureReport;
// import org.hl7.fhir.instance.model.api.IBase;
// import org.hl7.fhir.instance.model.api.IBaseResource;
// import org.opencds.cqf.cql.data.DataProvider;
// import org.opencds.cqf.cql.execution.Context;
// import org.opencds.cqf.cql.runtime.Interval;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import java.util.*;
// import java.util.function.Function;

// public class MeasureEvaluation<MeasureType extends IBase, MeasureReportType extends IBase, ResourceType, PatientType extends ResourceType> {

//     private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluation.class);

//     private DataProvider provider;
//     private Interval measurementPeriod;
//     private Function<ResourceType, String> getId;

//     public MeasureEvaluation(DataProvider provider, Interval measurementPeriod) {
//         this.provider = provider;
//         this.measurementPeriod = measurementPeriod;
//     }

//     public MeasureReportType evaluatePatientMeasure(MeasureType measure, Context context, String patientId) {
//         logger.info("Generating individual report");

//         if (patientId == null) {
//             return evaluatePopulationMeasure(measure, context);
//         }

//         Iterable<Object> patientRetrieve = provider.retrieve("Patient", "id", patientId, "Patient", null, null, null, null, null, null, null, null);
//         PatientType patient = null;
//         if (patientRetrieve.iterator().hasNext()) {
//             patient = (PatientType)patientRetrieve.iterator().next();
//         }

//         return evaluate(measure, context, patient == null ? Collections.emptyList() : Collections.singletonList(patient), MeasureReportType.MeasureReportType.INDIVIDUAL);
//     }

//     public MeasureReportType evaluateSubjectListMeasure(MeasureType measure, Context context, String practitionerRef)
//     {
//         logger.info("Generating patient-list report");

//         List<PatientType> patients = practitionerRef == null ? getAllPatients() : getPractitionerPatients(practitionerRef);
//         return evaluate(measure, context, patients, MeasureReportType.MeasureReportType.SUBJECTLIST);
//     }

//     private List<PatientType> getPractitionerPatients(String practitionerRef) {
//         List<PatientType> patients = new ArrayList<>();
//         Iterable<Object> patientRetrieve = provider.retrieve("Practitioner", "generalPractitioner", practitionerRef, "Patient", null, null, null, null, null, null, null, null);
//         patientRetrieve.forEach(x -> patients.add((PatientType) x));
//         return patients;
//     }

//     private List<PatientType> getAllPatients() {
//         List<PatientType> patients = new ArrayList<>();
//         Iterable<Object> patientRetrieve = provider.retrieve(null, null, null, "Patient", null, null, null, null, null, null, null, null);
//         patientRetrieve.forEach(x -> patients.add((PatientType) x));
//         return patients;
//     }

//     public MeasureReportType evaluatePopulationMeasure(MeasureType measure, Context context) {
//         logger.info("Generating summary report");

//         return evaluate(measure, context, getAllPatients(), MeasureReportType.MeasureReportType.SUMMARY);
//     }

//     private Iterable<ResourceType> evaluateCriteria(Context context, PatientType patient, MeasureType.MeasureGroupPopulationComponent pop) {
//         if (!pop.hasCriteria()) {
//             return Collections.emptyList();
//         }

//         context.setContextValue("Patient", this.getId.apply(patient));
//         Object result = context.resolveExpressionRef(pop.getCriteria().getExpression()).evaluate(context);
//         if (result == null) {
//             Collections.emptyList();
//         }
        
//         if (result instanceof Boolean) {
//             if (((Boolean)result)) {
//                 return Collections.singletonList(patient);
//             }
//             else {
//                 return Collections.emptyList();
//             }
//         }

//         return (Iterable)result;
//     }

//     private boolean evaluatePopulationCriteria(Context context, PatientType patient,
//                                                MeasureType.MeasureGroupPopulationComponent criteria, HashMap<String, ResourceType> population, HashMap<String, PatientType> populationPatients,
//                                                MeasureType.MeasureGroupPopulationComponent exclusionCriteria, HashMap<String, ResourceType> exclusionPopulation, HashMap<String, PatientType> exclusionPatients
//     ) {
//         boolean inPopulation = false;
//         if (criteria != null) {
//             for (ResourceType resource : evaluateCriteria(context, patient, criteria)) {
//                 inPopulation = true;
//                 population.put(this.getId.apply(resource), resource);
//             }
//         }

//         if (inPopulation) {
//             // Are they in the exclusion?
//             if (exclusionCriteria != null) {
//                 for (ResourceType resource: evaluateCriteria(context, patient, exclusionCriteria)) {
//                     inPopulation = false;
//                     exclusionPopulation.put(this.getId.apply(resource), resource);
//                     population.remove(this.getId.apply(resource));
//                 }
//             }
//         }

//         if (inPopulation && populationPatients != null) {
//             populationPatients.put(this.getId.apply(patient), patient);
//         }
//         if (!inPopulation && exclusionPatients != null) {
//             exclusionPatients.put(this.getId.apply(patient), patient);
//         }

//         return inPopulation;
//     }

//     private void addPopulationCriteriaReport(MeasureReportType report, MeasureReportType.MeasureReportGroupComponent reportGroup, MeasureType.MeasureGroupPopulationComponent populationCriteria, int populationCount, Iterable<PatientType> patientPopulation) {
//         if (populationCriteria != null) {
//             MeasureReportType.MeasureReportGroupPopulationComponent populationReport = new MeasureReportType.MeasureReportGroupPopulationComponent();
//             populationReport.setCode(populationCriteria.getCode());
//             if (report.getType() == MeasureReportType.MeasureReportType.SUBJECTLIST && patientPopulation != null) {
//                 ListResourceType SUBJECTLIST = new ListResource();
//                 SUBJECTLIST.setId(UUID.randomUUID().toString());
//                 populationReport.setSubjectResults(new Reference().setReference("#" + SUBJECTLIST.getId()));
//                 for (PatientType patient : patientPopulation) {
//                     ListResource.ListEntryComponent entry = new ListResource.ListEntryComponent()
//                             .setItem(new Reference().setReference(
//                                     this.getId.apply(patient).startsWith("Patient/") ?
//                                             this.getId.apply(patient) :
//                                             String.format("Patient/%s", this.getId.apply(patient)))
//                                     .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));
//                     SUBJECTLIST.addEntry(entry);
//                 }
//                 report.addContained(SUBJECTLIST);
//             }
// 			populationReport.setCount(populationCount);
//             reportGroup.addPopulation(populationReport);
//         }
//     }

//     private MeasureReportType evaluate(MeasureType measure, Context context, List<PatientType> patients, MeasureReportType.MeasureReportType type)
//     {
//         MeasureReportType report = new MeasureReportType();
//         report.setStatus(MeasureReport.MeasureReportStatus.fromCode("complete"));
//         report.setType(type);
//         report.setMeasure(new Reference(measure.getIdElement().getValue()));
//         if (type == MeasureReport.MeasureReportType.INDIVIDUAL && !patients.isEmpty()) {
//             report.setPatient(new Reference(patients.get(0).getId().getValue()));
//         }

//         report.setPeriod(
//             new Period()
//                     .setStart((Date) measurementPeriod.getStart())
//                     .setEnd((Date) measurementPeriod.getEnd()));

//         HashMap<String,ResourceType> resources = new HashMap<>();
//         HashMap<String,HashSet<String>> codeToResourceMap = new HashMap<>();

//         MeasureScoring measureScoring = MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode());
//         if (measureScoring == null) {
//             throw new RuntimeException("MeasureType scoring is required in order to calculate.");
//         }

//         for (MeasureType.MeasureGroupComponent group : measure.getGroup()) {
// 			MeasureReportType.MeasureReportGroupComponent reportGroup = new MeasureReportType.MeasureReportGroupComponent();
// 			reportGroup.setId(group.getId());
//             report.getGroup().add(reportGroup);

//             // Declare variables to avoid a hash lookup on every patient
//             // TODO: Isn't quite right, there may be multiple initial populations for a ratio MeasureType...
//             MeasureType.MeasureGroupPopulationComponent initialPopulationCriteria = null;
//             MeasureType.MeasureGroupPopulationComponent numeratorCriteria = null;
//             MeasureType.MeasureGroupPopulationComponent numeratorExclusionCriteria = null;
//             MeasureType.MeasureGroupPopulationComponent denominatorCriteria = null;
//             MeasureType.MeasureGroupPopulationComponent denominatorExclusionCriteria = null;
//             MeasureType.MeasureGroupPopulationComponent denominatorExceptionCriteria = null;
//             MeasureType.MeasureGroupPopulationComponent measurePopulationCriteria = null;
//             MeasureType.MeasureGroupPopulationComponent measurePopulationExclusionCriteria = null;
//             // TODO: Isn't quite right, there may be multiple MeasureType observations...
//             MeasureType.MeasureGroupPopulationComponent measureObservationCriteria = null;

//             HashMap<String, ResourceType> initialPopulation = null;
//             HashMap<String, ResourceType> numerator = null;
//             HashMap<String, ResourceType> numeratorExclusion = null;
//             HashMap<String, ResourceType> denominator = null;
//             HashMap<String, ResourceType> denominatorExclusion = null;
//             HashMap<String, ResourceType> denominatorException = null;
//             HashMap<String, ResourceType> measurePopulation = null;
//             HashMap<String, ResourceType> measurePopulationExclusion = null;
//             HashMap<String, ResourceType> measureObservation = null;

//             HashMap<String, PatientType> initialPopulationPatients = null;
//             HashMap<String, PatientType> numeratorPatients = null;
//             HashMap<String, PatientType> numeratorExclusionPatients = null;
//             HashMap<String, PatientType> denominatorPatients = null;
//             HashMap<String, PatientType> denominatorExclusionPatients = null;
//             HashMap<String, PatientType> denominatorExceptionPatients = null;
//             HashMap<String, PatientType> measurePopulationPatients = null;
//             HashMap<String, PatientType> measurePopulationExclusionPatients = null;

//             for (MeasureType.MeasureGroupPopulationComponent pop : group.getPopulation()) {
//                 MeasurePopulationType populationType = MeasurePopulationType.fromCode(pop.getCode().getCodingFirstRep().getCode());
//                 MeasureReportType
//                 if (populationType != null) {
//                     switch (populationType) {
//                         case INITIALPOPULATION:
//                             initialPopulationCriteria = pop;
//                             initialPopulation = new HashMap<>();
//                             if (type == MeasureReportType.MeasureReportType.SUBJECTLIST) {
//                                 initialPopulationPatients = new HashMap<>();
//                             }
//                             break;
//                         case NUMERATOR:
//                             numeratorCriteria = pop;
//                             numerator = new HashMap<>();
//                             if (type == MeasureReportType.MeasureReportType.SUBJECTLIST) {
//                                 numeratorPatients = new HashMap<>();
//                             }
//                             break;
//                         case NUMERATOREXCLUSION:
//                             numeratorExclusionCriteria = pop;
//                             numeratorExclusion = new HashMap<>();
//                             if (type == MeasureReportType.MeasureReportType.SUBJECTLIST) {
//                                 numeratorExclusionPatients = new HashMap<>();
//                             }
//                             break;
//                         case DENOMINATOR:
//                             denominatorCriteria = pop;
//                             denominator = new HashMap<>();
//                             if (type == MeasureReportType.MeasureReportType.SUBJECTLIST) {
//                                 denominatorPatients = new HashMap<>();
//                             }
//                             break;
//                         case DENOMINATOREXCLUSION:
//                             denominatorExclusionCriteria = pop;
//                             denominatorExclusion = new HashMap<>();
//                             if (type == MeasureReportType.MeasureReportType.SUBJECTLIST) {
//                                 denominatorExclusionPatients = new HashMap<>();
//                             }
//                             break;
//                         case DENOMINATOREXCEPTION:
//                             denominatorExceptionCriteria = pop;
//                             denominatorException = new HashMap<>();
//                             if (type == MeasureReportType.MeasureReportType.SUBJECTLIST) {
//                                 denominatorExceptionPatients = new HashMap<>();
//                             }
//                             break;
//                         case MEASUREPOPULATION:
//                             measurePopulationCriteria = pop;
//                             measurePopulation = new HashMap<>();
//                             if (type == MeasureReportType.MeasureReportType.SUBJECTLIST) {
//                                 measurePopulationPatients = new HashMap<>();
//                             }
//                             break;
//                         case MEASUREPOPULATIONEXCLUSION:
//                             measurePopulationExclusionCriteria = pop;
//                             measurePopulationExclusion = new HashMap<>();
//                             if (type == MeasureReportType.MeasureReportType.SUBJECTLIST) {
//                                 measurePopulationExclusionPatients = new HashMap<>();
//                             }
//                             break;
//                         case MEASUREOBSERVATION:
//                             break;
//                     }
//                 }
//             }

//             switch (measureScoring) {
//                 case PROPORTION:
//                 case RATIO: {

//                     // For each patient in the initial population
//                     for (PatientType patient : patients) {

//                         // Are they in the initial population?
//                         boolean inInitialPopulation = evaluatePopulationCriteria(context, patient, initialPopulationCriteria,
//                                 initialPopulation, initialPopulationPatients, null, null, null);
//                         populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);

//                         if (inInitialPopulation) {
//                             // Are they in the denominator?
//                             boolean inDenominator = evaluatePopulationCriteria(context, patient,
//                                     denominatorCriteria, denominator, denominatorPatients,
//                                     denominatorExclusionCriteria, denominatorExclusion, denominatorExclusionPatients);
//                             populateResourceMap(context, MeasurePopulationType.DENOMINATOR, resources, codeToResourceMap);

//                             if (inDenominator) {
//                                 // Are they in the numerator?
//                                 boolean inNumerator = evaluatePopulationCriteria(context, patient,
//                                         numeratorCriteria, numerator, numeratorPatients,
//                                         numeratorExclusionCriteria, numeratorExclusion, numeratorExclusionPatients);
//                                 populateResourceMap(context, MeasurePopulationType.NUMERATOR, resources, codeToResourceMap);

//                                 if (!inNumerator && inDenominator && (denominatorExceptionCriteria != null)) {
//                                     // Are they in the denominator exception?
//                                     boolean inException = false;
//                                     for (ResourceType resource : evaluateCriteria(context, patient, denominatorExceptionCriteria)) {
//                                         inException = true;
//                                         denominatorException.put(this.getId.apply(resource), resource);
//                                         denominator.remove(this.getId.apply(resource));
//                                         populateResourceMap(context, MeasurePopulationType.DENOMINATOREXCEPTION, resources, codeToResourceMap);
//                                     }
//                                     if (inException) {
//                                         if (denominatorExceptionPatients != null) {
//                                             denominatorExceptionPatients.put(this.getId.apply(patient), patient);
//                                         }
//                                         if (denominatorPatients != null) {
//                                             denominatorPatients.remove(this.getId.apply(patient));
//                                         }
//                                     }
//                                 }
//                             }
//                         }
//                     }

//                     // Calculate actual MeasureType score, Count(numerator) / Count(denominator)
//                     if (denominator != null && numerator != null && denominator.size() > 0) {
//                         reportGroup.setMeasureScore(new Quantity(numerator.size() / (double)denominator.size()));
//                     }

//                     break;
//                 }
//                 case CONTINUOUSVARIABLE: {

//                     // For each patient in the PatientType list
//                     for (PatientType patient : patients) {

//                         // Are they in the initial population?
//                         boolean inInitialPopulation = evaluatePopulationCriteria(context, patient, initialPopulationCriteria,
//                                 initialPopulation, initialPopulationPatients, null, null, null);
//                         populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);

//                         if (inInitialPopulation) {
//                             // Are they in the MeasureType population?
//                             boolean inMeasurePopulation = evaluatePopulationCriteria(context, patient,
//                                     measurePopulationCriteria, measurePopulation, measurePopulationPatients,
//                                     measurePopulationExclusionCriteria, measurePopulationExclusion, measurePopulationExclusionPatients);

//                             if (inMeasurePopulation) {
//                                 // TODO: Evaluate MeasureType observations
//                                 for (ResourceType resource: evaluateCriteria(context, patient, measureObservationCriteria)) {
//                                     measureObservation.put(this.getId.apply(resource), resource);
//                                 }
//                             }
//                         }
//                     }

//                     break;
//                 }
                
//                 case COHORT: {

//                     // For each patient in the PatientType list
//                     for (PatientType patient : patients) {
//                         // Are they in the initial population?
//                         boolean inInitialPopulation = evaluatePopulationCriteria(context, patient, initialPopulationCriteria,
//                                 initialPopulation, initialPopulationPatients, null, null, null);
//                         populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);
//                     }

//                     break;
//                 }
//             }

//             // Add population reports for each group
//             addPopulationCriteriaReport(report, reportGroup, initialPopulationCriteria, initialPopulation != null ? initialPopulation.size() : 0, initialPopulationPatients != null ? initialPopulationPatients.values() : null);
//             addPopulationCriteriaReport(report, reportGroup, numeratorCriteria, numerator != null ? numerator.size() : 0, numeratorPatients != null ? numeratorPatients.values() : null);
//             addPopulationCriteriaReport(report, reportGroup, numeratorExclusionCriteria, numeratorExclusion != null ? numeratorExclusion.size() : 0, numeratorExclusionPatients != null ? numeratorExclusionPatients.values() : null);
//             addPopulationCriteriaReport(report, reportGroup, denominatorCriteria, denominator != null ? denominator.size() : 0, denominatorPatients != null ? denominatorPatients.values() : null);
//             addPopulationCriteriaReport(report, reportGroup, denominatorExclusionCriteria, denominatorExclusion != null ? denominatorExclusion.size() : 0, denominatorExclusionPatients != null ? denominatorExclusionPatients.values() : null);
//             addPopulationCriteriaReport(report, reportGroup, denominatorExceptionCriteria, denominatorException != null ? denominatorException.size() : 0, denominatorExceptionPatients != null ? denominatorExceptionPatients.values() : null);
//             addPopulationCriteriaReport(report, reportGroup, measurePopulationCriteria,  measurePopulation != null ? measurePopulation.size() : 0, measurePopulationPatients != null ? measurePopulationPatients.values() : null);
//             addPopulationCriteriaReport(report, reportGroup, measurePopulationExclusionCriteria,  measurePopulationExclusion != null ? measurePopulationExclusion.size() : 0, measurePopulationExclusionPatients != null ? measurePopulationExclusionPatients.values() : null);
//             // TODO: MeasureType Observations...
//         }

//         // for (String key : codeToResourceMap.keySet()) {
//         //     list = new ListResource();
//         //     for (String element : codeToResourceMap.get(key)) {
//         //         ListResource.ListEntryComponent comp = new ListEntryComponent();
//         //         comp.setItem(new Reference('#' + element));
//         //         list.addEntry(comp);
//         //     }

//         //     if (!list.isEmpty()) {
//         //         list.setId(UUID.randomUUID().toString());
//         //         list.setTitle(key);
//         //         resources.put(list.getId(), list);
//         //     }
//         // }

//         // if (!resources.isEmpty()) {
//         //     FhirMeasureBundler bundler = new FhirMeasureBundler();
//         //     Bundle evaluatedResources = bundler.bundle(resources.values());
//         //     evaluatedResources.setId(UUID.randomUUID().toString());
//         //     report.setEvaluatedResource(Collections.singletonList(new Reference('#' + evaluatedResources.getId())));
//         //     report.addContained(evaluatedResources);
//         // }

//         return report;
//     }

//     private void populateResourceMap(
//             Context context, MeasurePopulationType type, HashMap<String, ResourceType> resources,
//             HashMap<String,HashSet<String>> codeToResourceMap)
//     {
//         if (context.getEvaluatedResources().isEmpty()) {
//             return;
//         }

//         if (!codeToResourceMap.containsKey(type.toCode())) {
//             codeToResourceMap.put(type.toCode(), new HashSet<>());
//         }

//         HashSet<String> codeHashSet = codeToResourceMap.get((type.toCode()));

//         for (Object o : context.getEvaluatedResources()) {
//             try {
//                 ResourceType r = (ResourceType)o;
//                 String id = this.getId.apply(r);
//                 if (!codeHashSet.contains(id)) {
//                     codeHashSet.add(id);
//                 }

//                 if (!resources.containsKey(id)) {
//                     resources.put(id, r);
//                 }
//             }
//             catch(Exception e) {}
//         }

//         context.clearEvaluatedResources();
//     }
// }