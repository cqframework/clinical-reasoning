library GlycemicControlHypoglycemicInitialPopulation version '0.0.004'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers
include SupplementalDataElementsFHIR4 version '2.0.000' called SDE
include MATGlobalCommonFunctionsFHIR4 version '6.1.000' called Global
include SharedResourceCreation version '0.0.001' called SharedResource

codesystem "ActCode": 'http://terminology.hl7.org/CodeSystem/v3-ActCode'
codesystem "Diagnosis Role": 'http://terminology.hl7.org/CodeSystem/diagnosis-role'
codesystem "Specimen Type": 'http://terminology.hl7.org/CodeSystem/v2-0487'
codesystem "Observation Category": 'http://terminology.hl7.org/CodeSystem/observation-category'
codesystem "Condition Category": 'http://terminology.hl7.org/CodeSystem/condition-category'

valueset "Blood Glucose Laboratory and Point of Care Tests": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1190.38'
valueset "Inpatient, Emergency, and Observation Locations": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1046.265'
valueset "Discharge Disposition": 'http://terminology.hl7.org/ValueSet/encounter-discharge-disposition'
valueset "Antidiabetic Medications": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1190.58'
valueset "Emergency Department Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292'
valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'
valueset "Observation Services": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1111.143'

code "Chief Complaint": 'CC' from "Diagnosis Role" display 'Chief complaint'
code "emergency": 'EMER' from "ActCode" display 'emergency'
code "inpatient acute": 'ACUTE' from "ActCode" display 'inpatient acute'
code "inpatient encounter": 'IMP' from "ActCode" display 'inpatient encounter'
code "inpatient non-acute": 'NONAC' from "ActCode" display 'inpatient non-acute'
code "observation encounter": 'OBSENC' from "ActCode" display 'observation encounter'
code "short stay": 'SS' from "ActCode" display 'short stay'
code "laboratory": 'laboratory' from "Observation Category" display 'Laboratory'
code "encounter-diagnosis": 'encounter-diagnosis' from "Condition Category" display 'encounter-diagnosis'

parameter "Measurement Period" Interval<DateTime>
 default Interval[@2022-01-01T00:00:00.0, @2022-01-31T00:00:00.0)

context Patient

/*
@author Arvind Jagannathan
*/
define "Initial Population":
  ("Inpatient Encounter"
  union "Patient Hospital Locations") InpatientEncounters
  with "Antidiabetic Drugs Administered or Ordered" ADD
  such that Coalesce(start of Global."Normalize Interval"(ADD.effective), ADD.authoredOn)
      during "HospitalizationWithObservationOrEmergency"(InpatientEncounters)
    and Coalesce(start of Global."Normalize Interval"(ADD.effective), ADD.authoredOn) during "Measurement Period"

define "Inpatient Encounter":
  [Encounter: class in {"inpatient encounter", "inpatient acute", "inpatient non-acute", "short stay"}]
    union [Encounter: "Encounter Inpatient"] Encounters
  where Encounters.status in {'in-progress', 'finished'}
    and Encounters.period overlaps "Measurement Period"

define "Patient Hospital Locations":
  [Encounter] Encounters
  where exists(
    Encounters.location EncounterLocation
    where Global.GetLocation(EncounterLocation.location).type in "Inpatient, Emergency, and Observation Locations"
      and EncounterLocation.period during Encounters.period
  )

define "Antidiabetic Drugs Administered or Ordered":
  ([MedicationAdministration] ADDMedAdmin
    where GetMedicationCode(ADDMedAdmin.medication) in "Antidiabetic Medications"
      and ADDMedAdmin.status ~ 'completed')
  union (
    [MedicationRequest] MedicationRequests
      where GetMedicationCode(MedicationRequests.medication) in "Antidiabetic Medications"
  )

define function "GetMedicationCode"(choice Choice<FHIR.CodeableConcept, FHIR.Reference>):
  case
    when choice is FHIR.CodeableConcept then
      choice as FHIR.CodeableConcept
    when choice is FHIR.Reference then
      GetMedication(choice as FHIR.Reference).code
    else
      null as FHIR.CodeableConcept
  end

define function "GetMedication"(reference Reference ):
  singleton from (
    [Medication: id in {Global.GetId(reference.reference)}]
  )

define function "HospitalizationWithObservationOrEmergency"(TheEncounter FHIR.Encounter ):
  TheEncounter Visit
    let ObsVisit: Last([Encounter: "Observation Services"] LastObs
        where LastObs.class ~ "observation encounter"
          and LastObs.period ends 1 hour or less on or before start of Visit.period
  				sort by end of period
    ),
    VisitStart: Coalesce(start of ObsVisit.period, start of Visit.period),
    EDVisit: Last([Encounter: "Emergency Department Visit"] LastED
        where LastED.class ~ "emergency"
          and LastED.period ends 1 hour or less on or before start of Visit.period
  				sort by end of period
    )
    return Interval[Coalesce(start of EDVisit.period, VisitStart), end of Visit.period]

define function "GetSpecimen"(reference Reference):
  singleton from (
    [Specimen: id in {Global.GetId(reference.reference)}]
  )

define function "GetIdExtensions"(domainResource DomainResource):
  domainResource.extension E
  where E.url = 'https://www.cdc.gov/test-case/fhir/StructureDefinition/original-id'
  return E

define function "GetPatientExtensions"(domainResource DomainResource):
  domainResource.extension E
  where E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-race'
    or E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity'
    or E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex'
    or E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-genderIdentity'
    or E.url = 'http://hl7.org/fhir/StructureDefinition/patient-genderIdentity'
  return E

define function "GetPatientAddress"(addresses List<FHIR.Address>):
  addresses address
  return FHIR.Address{
    postalCode: address.postalCode
  }

define function "GetDosage"(medicationRequest MedicationRequest):
  medicationRequest m
  let dosages: m.dosageInstruction
  return
    dosages d
    return Dosage{
      timing: d.timing,
      doseAndRate: d.doseAndRate,
      route: d.route
    }

define function "getDiagnosis"(encounter Encounter):
  encounter e
  let diagnoses: e.diagnosis
  return
    diagnoses d
    where d.use ~ "Chief Complaint"
    return d

define function "getDischargeDisposition"(encounter Encounter):
  encounter e
  let dischargeDisposition: e.hospitalization.dischargeDisposition
  return
    dischargeDisposition d
    where d in "Discharge Disposition"
    return e.hospitalization

define function "getEncounterLocation"(encounter Encounter):
  encounter e
  let locations: e.location
  return
    locations l
    where l.physicalType in "Inpatient, Emergency, and Observation Locations"
    or Global.GetLocation(l.location).type in "Inpatient, Emergency, and Observation Locations"
    return l

define function "GetMedicationFrom"(choice Choice<FHIR.CodeableConcept, FHIR.Reference>):
  case
    when choice is FHIR.Reference then
      GetMedication(choice as FHIR.Reference)
    else
      null
  end

//Supplemental Data Elements
define "SDE Blood Glucose Observation":
	[Observation: "Blood Glucose Laboratory and Point of Care Tests"] Observations
  with "Initial Population" InitialPopulation
  such that start of Global."Normalize Interval"(Observations.effective) during InitialPopulation.period
    or Global."Normalize Interval"(GetSpecimen(Observations.specimen).collection.collected) during InitialPopulation.period
  return SharedResource.ObservationResource(Observations)

define "SDE Chief Complaint":
  "Initial Population" IP
  with IP.diagnosis InitialPopulationDiagnosis
  such that InitialPopulationDiagnosis.use ~ "Chief Complaint"
  return SharedResource.EncounterResource(IP)

define "SDE Condition":
  [Condition] Conditions
  with "Initial Population" InitialPopulation
  such that Global."Normalize Interval"(Conditions.onset) overlaps InitialPopulation.period
  return SharedResource.ConditionResource(Conditions)

define "SDE Condition during Encounter":
  [Condition] ConditionEncounterDiagnosis
  with ConditionEncounterDiagnosis.category category
  such that category ~ "encounter-diagnosis"
  return SharedResource.ConditionResource(ConditionEncounterDiagnosis)

define "SDE Coverage":
	[Coverage] Coverages
  with "Initial Population" InitialPopulation
  such that start of Coverages.period before day of end of InitialPopulation.period
  and (end of Coverages.period is null
    or (end of Coverages.period is not null
      and end of Coverages.period on or after day of end of InitialPopulation.period))
  return SharedResource.CoverageResource(Coverages)

define "SDE Encounter Discharge Dispositions":
	"Initial Population" DischargeDispositions
  where DischargeDispositions.hospitalization.dischargeDisposition in "Discharge Disposition"
  return SharedResource.EncounterResource(DischargeDispositions)

define "SDE Encounter Locations":
  "Initial Population" InitialPopulation
  where exists (
    InitialPopulation.location InitialPopulationLocation
    where (Global.GetLocation(InitialPopulationLocation.location).type in "Inpatient, Emergency, and Observation Locations"
      and InitialPopulationLocation.period during InitialPopulation.period)
  )
  return SharedResource.EncounterResource(InitialPopulation)

define "SDE Initial Population Encounters":
  "Initial Population" IP
  return SharedResource.EncounterResource(IP)

define "SDE Medication":
  ("SDE Medication Request"
  union "SDE Medication Administration") MedReqOrAdmin
  where MedReqOrAdmin.medication is FHIR.Reference
  return SharedResource.MedicationResource(GetMedicationFrom(MedReqOrAdmin.medication))

define "SDE Medication Administration":
	[MedicationAdministration] MedicationAdministrations
  with "Initial Population" InitialPopulation
  such that start of Global."Normalize Interval"(MedicationAdministrations.effective) during "HospitalizationWithObservationOrEmergency"(InitialPopulation)
    and start of Global."Normalize Interval"(MedicationAdministrations.effective) during "Measurement Period"
  return SharedResource.MedicationAdministrationResource(MedicationAdministrations)

define "SDE Medication Request":
	[MedicationRequest] MedicationRequests
  with "Initial Population" InitialPopulation
	such that MedicationRequests.authoredOn during "HospitalizationWithObservationOrEmergency"(InitialPopulation)
    and MedicationRequests.authoredOn during "Measurement Period"
  return SharedResource.MedicationRequestResource(MedicationRequests)

define "SDE Minimal Patient":
  Patient p
  return SharedResource.PatientResource(p)

define "SDE Service Request":
  [ServiceRequest] ServiceRequests
  with "Initial Population" InitialPopulation
  such that ServiceRequests.authoredOn during InitialPopulation.period
  return SharedResource.ServiceRequestResource(ServiceRequests)

define "SDE Specimen":
  [Specimen] Specimens
  with "Initial Population" InitialPopulation
  such that Global."Normalize Interval"(Specimens.collection.collected) during InitialPopulation.period
  return SharedResource.SpecimenResource(Specimens)

//Validation
/*define "Invalid MedicationRequest":
  [MedicationRequest] MedicationRequests
  return "MedicationRequest Not Valid"(MedicationRequests)

define function "MedicationRequest Not Valid"(medicationRequest MedicationRequest):
  medicationRequest m
  return "MedicationRequest Requester Not Valid"(m)

define function "MedicationRequest Requester Not Valid"(medicationRequest MedicationRequest):
  medicationRequest m
  where m.requester is null
  return SharedResource.OperationOutcomeResource('LCR-MedicationRequestRequesterNotValid-' + m.id, m.id, 'MedicationRequest does not have a requester')*/





//Note, when creating custom resources, you need to specify the exact type of each element created if it's new and not pulled from an existing resource