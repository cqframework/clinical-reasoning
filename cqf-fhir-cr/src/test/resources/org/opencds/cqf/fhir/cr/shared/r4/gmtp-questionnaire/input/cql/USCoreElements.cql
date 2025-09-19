library USCoreElements

using FHIR version '4.0.1'
using USCore version '7.0.0'

include hl7.fhir.uv.cql.FHIRHelpers version '4.0.1'
include hl7.fhir.uv.cql.FHIRCommon version '2.0.0'
include USCoreCommon called UC

codesystem "LOINC": 'http://loinc.org'
codesystem "Identifier Type": 'http://terminology.hl7.org/CodeSystem/v2-0203'
codesystem "Verification Status": 'http://terminology.hl7.org/CodeSystem/condition-ver-status'
codesystem "Condition Clinical": 'http://terminology.hl7.org/CodeSystem/condition-clinical'

valueset "Common allergy substances": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1186.8'

code "No known allergy (situation)": '716186003' from FHIRCommon.SNOMEDCT
code "Member Number": 'MB' from "Identifier Type"
code "Subscriber Number": 'SN' from "Identifier Type"
code "Body surface area": '8277-6' from "LOINC" display 'Body surface area'
code "Confirmed": 'confirmed' from "Verification Status"
code "Active": 'active' from "Condition Clinical"

context Patient

// Patient Patterns

define fluent function ageInDaysAt(patient Patient, asOf Date):
  CalculateAgeInDaysAt(Patient.birthDate, asOf)

define fluent function ageInDays(patient Patient):
  CalculateAgeInDaysAt(Patient.birthDate, Today())

define fluent function ageInMonthsAt(patient Patient, asOf Date):
  CalculateAgeInMonthsAt(Patient.birthDate, asOf)

define fluent function ageInMonths(patient Patient):
  CalculateAgeInMonthsAt(Patient.birthDate, Today())

define fluent function ageInYearsAt(patient Patient, asOf Date):
  CalculateAgeInYearsAt(Patient.birthDate, asOf)

define fluent function ageInYears(patient Patient):
  CalculateAgeInYearsAt(Patient.birthDate, Today())

define fluent function usualName(patient Patient):
  patient.name name
    where name.use ~ 'usual'
      and name.period is not null implies name.period includes Today()
    sort by start of period desc

define fluent function officialName(patient Patient):
  patient.name name
    where name.use ~ 'official'
      and name.period is not null implies name.period includes Today()
    sort by start of period desc

define fluent function firstNonOfficialNonUsualName(patient Patient):
  First(
    patient.name name
      where not(name.use ~ 'official') and not(name.use ~ 'usual')
        and name.period is not null implies name.period includes Today()
      sort by start of period desc
  )

define fluent function memberID(coverage FHIR.Coverage):
  coverage.identifier.where(type ~ "Member Number").single().value

define fluent function policyNumber(coverage FHIR.Coverage):
  singleton from (
    coverage.identifier I
      where I.type ~ "Subscriber Number"
  ).value

define fluent function medicalRecordNumber(patient Patient):
  patient.identifier.where(type ~ UC.MedicalRecordNumber).single().value

define fluent function name(practitioner Practitioner):
  Coalesce(practitioner.officialName().first(), practitioner.usualName().first(), practitioner.firstNonOfficialNonUsualName())

define fluent function usualName(practitioner Practitioner):
  practitioner.name name
    where name.use ~ 'usual'
      and name.period is not null implies name.period includes Today()
    sort by start of period desc

define fluent function officialName(practitioner Practitioner):
  practitioner.name name
    where name.use ~ 'official'
      and name.period is not null implies name.period includes Today()
    sort by start of period desc

define fluent function firstNonOfficialNonUsualName(practitioner Practitioner):
  First(
    practitioner.name name
      where not(name.use ~ 'official') and not(name.use ~ 'usual')
        and name.period is not null implies name.period includes Today()
      sort by start of period desc
  )

define fluent function name(practitioner Practitioner):
  Coalesce(practitioner.officialName().first(), practitioner.usualName().first(), practitioner.firstNonOfficialNonUsualName())

define fluent function firstName(name HumanName):
  name.given.first()

define fluent function middleNames(name HumanName):
  Combine(Skip(name.given, 1), ' ')

define fluent function lastName(name HumanName):
  name.family

define fluent function firstMiddleLast(name HumanName):
  Combine(name.given, ' ') + ' ' + name.family

define fluent function lastFirstMiddle(name HumanName):
  name.family + ', ' + Combine(name.given, ' ')

// Allergy/Intolerance

/*
@description: Returns all allergies and intolerances
@comment: This definition returns all allergies and intolerances conforming to the US Core 7.0.0
[AllergyIntolerance](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-allergyintolerance.html) profile.
Allergies and intolerances returned by this definition include records with any clinical status (including none)
and any verification status (including none).
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-allergyintolerance
*/
define "All Allergies and Intolerances":
  [USCore."AllergyIntolerance"]

define "Active Confirmed Allergies and Intolerances":
  "All Allergies and Intolerances".active().confirmed()

define "Common Allergies and Intolerances":
  "All Allergies and Intolerances" A
    where A.code in "Common allergy substances"

define "Active Confirmed Common Allergies and Intolerances":
  "Common Allergies and Intolerances".active().confirmed()

// No Known Allergies (Not Asked)
define "No Known Allergies (Not Asked)":
  "All Allergies and Intolerances" A
    where A.code ~ "No known allergy (situation)"
      and A.isActive()
      and A.isUnconfirmed()

// No Known Allergies (Confirmed)
define "No Known Allergies (Confirmed)":
  "All Allergies and Intolerances" A
    where A.code ~ "No known allergy (situation)"
      and A.isActive()
      and A.isConfirmed()

// Condition

/*
@description: Returns all problem list items, encounter diagnoses, and health concerns
@comment: This definition returns all conditions of any category.
[Condition Encounter Diagnosis](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-condition-encounter-diagnosis.html)
[Condition Problems and Health Concerns](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-condition-problems-health-concerns.html)
Conditions returned by this definition include records with any clinical status (including none) and
any verification status (including none).
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition
*/
define "All Conditions":
  [FHIR."Condition"]

define "All Problem List Items":
  "All Conditions" C
    where C.isProblemListItem()

define "Active Confirmed Problem List Items":
  "All Problem List Items".active().confirmed()

define "All Encounter Diagnoses":
  "All Conditions" C
    where C.isEncounterDiagnosis()

define "All Health Concerns":
  "All Conditions" C
    where C.isHealthConcern()

// Laboratory Diagnostic Report

/*
@comment: [Diagnostic Report for Laboratory Results](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-diagnosticreport-lab.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab
*/
define "All Diagnostic Reports for Laboratory Results":
  ["DiagnosticReportProfileLaboratoryReporting"]

// Diagnostic Report

/*
@comment: [Diagnostic Report Note](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-diagnosticreport-note.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-note
*/
define "All Diagnostic Report Notes":
  ["DiagnosticReportProfileNoteExchange"]

// Encounter

/*
@comment: [Encounter](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-encounter.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter
*/
define "All Encounters":
  ["EncounterProfile"]

define "All Performed Encounters":
  "All Encounters" E
    where E.status = 'finished'

// Immunization

/*
@comment: [Immunization](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-immunization.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-immunization
*/
define "All Immunizations":
  ["ImmunizationProfile"]

define "All Completed Immunizations":
  "All Immunizations" I
    where I.status = 'completed'

// Implantable Device

/*
@comment: [Implantable Device](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-implantable-device.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-implantable-device
*/
define "All Implantable Devices":
  ["ImplantableDeviceProfile"]

// Laboratory Result

/*
@description: Returns all laboratory results
@comment: [Laboratory Result](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-observation-lab.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab
*/
define "All Laboratory Results":
  ["LaboratoryResultObservationProfile"]

define "Resulted Laboratory Results":
  "All Laboratory Results" L
    where L.status in { 'preliminary', 'final', 'amended', 'corrected' }

// Medication Request

/*
@description: Returns all medication requests
@comment: [Medication Request](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-medicationrequest.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationrequest
*/
define "Most Recent Medication Request":
  "All Medication Requests".mostRecent()

define "All Medication Requests":
  ["MedicationRequestProfile"]

define "Active Medication Orders":
  "All Medication Requests" M
    where M.status = 'active'
      and M.intent in { 'order', 'original-order', 'reflex-order', 'filler-order', 'instance-order' }

define "All Medications":
  ["MedicationProfile"]

// Pediatric BMI for Age

/*
@comment: [Pediatric BMI for Age](https://hl7.org/fhir/us/core/STU7/StructureDefinition-pediatric-bmi-for-age.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/pediatric-bmi-for-age
*/
define "Pediatric BMI for Age":
  ((["PediatricBMIforAgeObservationProfile"]).resulted()) O
    return O as "PediatricBMIforAgeObservationProfile"

define "Pediatric BMI for Age All Statuses":
  ["PediatricBMIforAgeObservationProfile"]

// Pediatric Head Circumference Percentile

/*
@comment: [Pediatric Head Circumference Percentile](https://hl7.org/fhir/us/core/STU7/StructureDefinition-head-occipital-frontal-circumference-percentile.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/head-occipital-frontal-circumference-percentile
*/
define "Pediatric Head Circumference Percentile":
  ((["PediatricHeadOccipitalFrontalCircumferencePercentileProfile"]).resulted()) O
    return O as "PediatricHeadOccipitalFrontalCircumferencePercentileProfile"

// Pediatric Weight for Height

/*
@comment: [Pediatric Weight for Height](https://hl7.org/fhir/us/core/STU7/StructureDefinition-pediatric-weight-for-height.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/pediatric-weight-for-height
*/
define "Pediatric Weight for Height":
  ((["PediatricWeightForHeightObservationProfile"]).resulted()) O
    return O as "PediatricWeightForHeightObservationProfile"

define "Pediatric Weight for Height All Statuses":
  ["PediatricWeightForHeightObservationProfile"]

// Procedure

/*
@comment: [Procedure](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-procedure.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-procedure
*/
define "All Procedures":
  ["ProcedureProfile"] P
    where P.status in { 'preparation', 'in-progress', 'on-hold', 'completed' }

define "All Performed Procedures":
  "All Procedures" P
    where P.status = 'completed'

// Pulse Oximetry

/*
@comment: [Pulse Oximetry](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-pulse-oximetry.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-pulse-oximetry
*/
define "Pulse Oximetry":
  ((["PulseOximetryProfile"]).resulted()) O
    return O as "PulseOximetryProfile"

define "Pulse Oximetry All Statuses":
  ["PulseOximetryProfile"]

// Smoking Status

/*
@description: Returns all smoking status observations
@comment: This definition returns all smoking status observations conforming to the US Core 3.1.1
[Smoking Status](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-smokingstatus.html)
profile.
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-smokingstatus
*/
define "Smoking Status":
  ((["SmokingStatusProfile"]).resulted()) O
    return O as "SmokingStatusProfile"

define "Smoking Status All Statuses":
  ["SmokingStatusProfile"]

/*
@description: Returns the most recent smoking status
@comment: This definition returns the most recent (by issued time) smoking status observation conforming to the
US Core [Smoking Status](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-smokingstatus.html)
profile.
*/
define "Most Recent Smoking Status":
  Last(
    "Smoking Status" SS
        where SS.status = 'final'
        sort by issued
  )

// Vital Signs

/*
@comment: [Vital Signs Panel](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-vital-signs.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-vital-signs
*/
define "All Vital Signs":
  ((["Vital Signs Profile"]).resulted()) O
    return O as "Vital Signs Profile"

define "All Vital Signs All Statuses":
  ["Vital Signs Profile"]

// Respiratory Rate

/*
@comment: [Respiratory Rate](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-respiratory-rate.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-respiratory-rate
*/
define "All Respiratory Rate Measurements":
  ((["Respiratory Rate Profile"]).resulted()) O
    return O as "Respiratory Rate Profile"

define "All Respiratory Rate Measurements All Statuses":
  ["Respiratory Rate Profile"]

// Heart Rate

/*
@comment: [Heart Rate](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-heart-rate.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-heart-rate
*/
define "All Heart Rate Measurements":
  ((["Heart Rate Profile"]).resulted()) O
    return O as "Heart Rate Profile"

define "All Heart Rate Measurements All Statuses":
  ["Heart Rate Profile"]

// Body Temperature

/*
@comment: [Body Temperature](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-body-temperature.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-body-temperature
*/
define "All Body Temperature Measurements":
  ((["Body Temperature Profile"]).resulted()) O
    return O as "Body Temperature Profile"

define "All Body Temperature Measurements All Statuses":
  ["Body Temperature Profile"]

// Body Height

/*
@comment: [Body Height](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-body-height.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-body-height
*/
define "All Body Height Measurements":
   ((["Body Height Profile"]).resulted()) O
     return O as "Body Height Profile"

define "All Body Height Measurements All Statuses":
  ["Body Height Profile"]

// Head Circumference

/*
@comment: [Head Circumference](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-head-circumference.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-head-circumference
*/
define "All Head Circumference Measurements":
  ((["Head Circumference Profile"]).resulted()) O
    return O as "Head Circumference Profile"

define "All Head Circumference Measurements All Statuses":
  ["Head Circumference Profile"]

// Body Weight

/*
@comment: [Body Weight](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-body-weight.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-body-weight
*/
define "All Body Weight Measurements":
  ((["Body Weight Profile"]).resulted()) O
    return O as "Body Weight Profile"

define "All Body Weight Measurements All Statuses":
  ["Body Weight Profile"]

// Body Mass Index

/*
@comment: [Body Mass Index](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-bmi.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-bmi
*/
define "All Body Mass Index Measurements":
  ((["BMI Profile"]).resulted()) O
    return O as "BMI Profile"

define "All Body Mass Index Measurements All Statuses":
  ["BMI Profile"]

// Blood Pressure

/*
@comment: [Blood Pressure](https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-blood-pressure.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-blood-pressure
*/
define "All Blood Pressure Measurements":
  ((["Blood Pressure Profile"]).resulted()) O
    return O as "Blood Pressure Profile"

define "All Blood Pressure Measurements All Statuses":
  ["Blood Pressure Profile"]

// Systolic Blood Pressure
define "Systolic Blood Pressure":
  "All Blood Pressure Measurements".mostRecent().systolic().value

// Diastolic Blood Pressure
define "Diastolic Blood Pressure":
  "All Blood Pressure Measurements".mostRecent().diastolic().value

define "Most Recent BSA":
  convert (([FHIR.Observation: "Body surface area"]).resulted().mostRecent() as FHIR.Observation).value to 'm2'

define function CalculateBSA(alg System.String, height System.Quantity, weight System.Quantity):
  System.Quantity { value:
    // Mosteller formula using lbs and inches
    if (alg = 'Mosteller') then
      ((((convert(height) to '[in_i]') * (convert(weight) to '[lb_av]'))/3131).value)^0.5

    // DuBois and DuBois formula using cm and kg
    // NOTE: never to be used for newborn calculation
    else if (alg = 'DuBois and DuBois') then
      0.007184 * ((convert(height) to 'cm').value^0.725) * ((convert(weight) to 'kg').value^0.425)

    // No matching algorithm found
    else null,
    unit: 'm2'
  }

define "All ServiceRequests":
  [FHIR.ServiceRequest]

define function MostRecentSR(serviceRequests List<"FHIR.ServiceRequest">):
  Last(
    serviceRequests SR
      // sort by authoredOn
  )

define fluent function mostRecentSR(serviceRequests List<"FHIR.ServiceRequest">):
  Last(
    serviceRequests SR
      // sort by authoredOn
  )

define function RequestingProvider(serviceRequest FHIR.ServiceRequest):
  [PractitionerProfile] P
    where EndsWith(serviceRequest.requester.reference.value, P.id)

define function ServicingProvider(serviceRequest FHIR.ServiceRequest):
  [PractitionerProfile] P
    where EndsWith(First(serviceRequest.performer.reference.value), P.id)

define function BillingProvider(coverage FHIR.Coverage):
  singleton from([OrganizationProfile] O
    where EndsWith(First(coverage.payor.reference.value), O.id))

define function RelatedCondition(serviceRequest FHIR.ServiceRequest):
  [FHIR.Condition] C
    where EndsWith(First(serviceRequest.reasonReference.reference.value), C.id)

define "Research Subject":
  [FHIR.ResearchSubject] R
    where  EndsWith(R.individual.reference, Patient.id)

define "All Clinical Trials":
  [FHIR.ResearchStudy]

define "Clinical Trial Organization":
  [OrganizationProfile] O

define function "GetServiceRequestReasonCondition"(serviceRequest ServiceRequest):
  [FHIR.Condition] C
    where EndsWith(First(serviceRequest.insurance.reference), C.id)
  return C

define function Requester(medicationRequest MedicationRequestProfile):
  singleton from (
    [USCore.PractitionerProfile] P
      where EndsWith(medicationRequest.requester.reference, P.id)
  )

define function RequesterRole(medicationRequest MedicationRequestProfile):
  singleton from (
    [USCore.PractitionerRoleProfile] R
      where EndsWith(R.practitioner.reference, Requester(medicationRequest).id)
  )

