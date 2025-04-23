library USCoreElements

using USCore version '3.1.1'
using FHIR version '4.0.1'

include FHIRHelpers version '4.1.0'
include USCoreCommon called UC

codesystem "LOINC": 'http://loinc.org'
codesystem "Identifier Type": 'http://terminology.hl7.org/CodeSystem/v2-0203'
codesystem "Verification Status": 'http://terminology.hl7.org/CodeSystem/condition-ver-status'
codesystem "Condition Clinical": 'http://terminology.hl7.org/CodeSystem/condition-clinical'

valueset "Common allergy substances": 'http://hl7.org/fhir/us/core/ValueSet/us-core-allergy-substance'

code "No known allergy (situation)": '716186003' from UC.SNOMEDCT
code "Member Number": 'MB' from "Identifier Type"
code "Subscriber Number": 'SN' from "Identifier Type"
code "Body surface area": '8277-6' from "LOINC" display 'Body surface area'
code "Confirmed": 'confirmed' from "Verification Status"
code "Active": 'active' from "Condition Clinical"

context Patient

// Patient Patterns

define fluent function ageInYearsAt(patient Patient, asOf Date):
  CalculateAgeInYearsAt(date from Patient.birthDate, asOf)

define fluent function ageInYears(patient Patient):
  CalculateAgeInYearsAt(date from Patient.birthDate, Today())

define "Usual Name":
  First(
    Patient.name name
      where name.use ~ 'usual'
  )

define "Official Name":
  First(
    Patient.name name
      where name.use ~ 'official'
  )

define "First Occurrence of Non-Official, Non-Usual Name":
  First(
    Patient.name name
      where not(name.use ~ 'official') and not(name.use ~ 'usual')
  )

define "Name":
  Coalesce("Official Name", "Usual Name", "First Occurrence of Non-Official, Non-Usual Name")

define "First Name":
  "Name".given[0]

define "Middle Names":
  Combine(Skip("Name".given, 1), ' ')

define "Last Name":
  "Name".family

define "Name - First Middle(s) Last":
  Combine("Name".given, ' ') + ' ' + "Name".family

define "Name - Last, First Middle(s)":
  "Name".family + ', ' + Combine("Name".given, ' ')

// Allergy/Intolerance

/*
@description: Returns all allergies and intolerances
@comment: This definition returns all allergies and intolerances conforming to the US Core 3.1.1
[AllergyIntolerance](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-allergyintolerance.html) profile.
Allergies and intolerances returned by this definition include records with any clinical status (including none)
and any verification status (including none).
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-allergyintolerance
*/
define "All Allergies and Intolerances":
  [USCore."AllergyIntolerance"]

define "Active Confirmed Allergies and Intolerances":
  "All Allergies and Intolerances".allergyActive().allergyConfirmed()

define "Common Allergies and Intolerances":
  "All Allergies and Intolerances" A
    where A.code in "Common allergy substances"

define "Active Confirmed Common Allergies and Intolerances":
  "Common Allergies and Intolerances".allergyActive().allergyConfirmed()

// No Known Allergies (Not Asked)
define "No Known Allergies (Not Asked)":
  "All Allergies and Intolerances" A
    where A.code ~ "No known allergy (situation)"
      and A.isAllergyActive()
      and A.isAllergyUnconfirmed()

// No Known Allergies (Confirmed)
define "No Known Allergies (Confirmed)":
  "All Allergies and Intolerances" A
    where A.code ~ "No known allergy (situation)"
      and A.isAllergyActive()
      and A.isAllergyConfirmed()

// Condition

/*
@description: Returns all problem list items, encounter diagnoses, and health concerns
@comment: This definition returns all conditions of any category conforming to the US Core 3.1.1
[Condition](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-condition.html) profile.
Conditions returned by this definition include records with any clinical status (including none) and
any verification status (including none).
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition
*/
define "All Conditions":
  [USCore."Condition"]

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
@comment: [Diagnostic Report for Laboratory Results](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-diagnosticreport-lab.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab
*/
define "All Diagnostic Reports for Laboratory Results":
  ["DiagnosticReportProfileLaboratoryReporting"]

// Diagnostic Report

/*
@comment: [Diagnostic Report Note](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-diagnosticreport-note.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-note
*/
define "All Diagnostic Report Notes":
  ["DiagnosticReportProfileNoteExchange"]

// Encounter

/*
@comment: [Encounter](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-encounter.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter
*/
define "All Encounters":
  ["EncounterProfile"]

define "All Performed Encounters":
  "All Encounters" E
    where E.status = 'finished'

// Immunization

/*
@comment: [Immunization](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-immunization.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-immunization
*/
define "All Immunizations":
  ["ImmunizationProfile"]

define "All Completed Immunizations":
  "All Immunizations" I
    where I.status = 'completed'

// Implantable Device

/*
@comment: [Implantable Device](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-implantable-device.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-implantable-device
*/
define "All Implantable Devices":
  ["ImplantableDeviceProfile"]

// Laboratory Result

/*
@description: Returns all laboratory results
@comment: [Laboratory Result](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-observation-lab.html)
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
@comment: [Medication Request](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-medicationrequest.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationrequest
*/
define "Most Recent Medication Request":
  UC.MostRecent("All Medication Requests")

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
@comment: [Pediatric BMI for Age](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-pediatric-bmi-for-age.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/pediatric-bmi-for-age
*/
define "Pediatric BMI for Age":
  ((["PediatricBMIforAgeObservationProfile"]).resulted()) O
    return O as "PediatricBMIforAgeObservationProfile"

define "Pediatric BMI for Age All Statuses":
  ["PediatricBMIforAgeObservationProfile"]

// Pediatric Head Circumference Percentile

/*
@comment: [Pediatric Head Circumference Percentile](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-head-occipital-frontal-circumference-percentile.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/head-occipital-frontal-circumference-percentile
*/
//define "All Pediatric Head Circumference Percentile Measurements":
  //TODO - profile not in modelinfo
  //UsCorePediatricHeadOccipitalFrontalCircumferencePercentileProfile


// Pediatric Weight for Height

/*
@comment: [Pediatric Weight for Height](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-pediatric-weight-for-height.html)
@profile: http://hl7.org/fhir/us/core/StructureDefinition/pediatric-weight-for-height
*/
define "Pediatric Weight for Height":
  ((["PediatricWeightForHeightObservationProfile"]).resulted()) O
    return O as "PediatricWeightForHeightObservationProfile"

define "Pediatric Weight for Height All Statuses":
  ["PediatricWeightForHeightObservationProfile"]

// Procedure

/*
@comment: [Procedure](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-procedure.html)
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
@comment: [Pulse Oximetry](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-pulse-oximetry.html)
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
[Smoking Status](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-smokingstatus.html)
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
US Core 3.1.1 [Smoking Status](https://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-smokingstatus.html)
profile.
*/
define "Most Recent Smoking Status":
  Last(
    "Smoking Status" SS
        where SS.status = 'final'
        sort by issued
  )

// Vital Signs Panel

/*
@comment: [Vital Signs Panel](http://hl7.org/fhir/R4/vitalspanel.html)
@profile: http://hl7.org/fhir/StructureDefinition/vitalspanel
*/
define "All Vital Signs Panels":
  ((["observation-vitalspanel"]).resulted()) O
    return O as "observation-vitalspanel"

define "All Vital Signs Panels All Statuses":
  ["observation-vitalspanel"]

// Respiratory Rate

/*
@comment: [Respiratory Rate](http://hl7.org/fhir/R4/resprate.html)
@profile: http://hl7.org/fhir/StructureDefinition/resprate
*/
define "All Respiratory Rate Measurements":
  ((["observation-resprate"]).resulted()) O
    return O as "observation-resprate"

define "All Respiratory Rate Measurements All Statuses":
  ["observation-resprate"]

// Heart Rate

/*
@comment: [Heart Rate](http://hl7.org/fhir/R4/heartrate.html)
@profile:http://hl7.org/fhir/StructureDefinition/heartrate
*/
define "All Heart Rate Measurements":
  ((["observation-heartrate"]).resulted()) O
    return O as "observation-heartrate"

define "All Heart Rate Measurements All Statuses":
  ["observation-heartrate"]

// Oxygen Saturation

/*
@comment: [Oxygen Saturation](http://hl7.org/fhir/R4/oxygensat.html)
@profile: http://hl7.org/fhir/StructureDefinition/oxygensat
*/
define "All Oxygen Saturation Measurements":
  ((["observation-oxygensat"]).resulted()) O
    return O as "observation-oxygensat"

define "All Oxygen Saturation Measurements All Statuses":
  ["observation-oxygensat"]

// Body Temperature

/*
@comment: [Body Temperature](http://hl7.org/fhir/R4/bodytemp.html)
@profile: http://hl7.org/fhir/StructureDefinition/bodytemp
*/
define "All Body Temperature Measurements":
  ((["observation-bodytemp"]).resulted()) O
    return O as "observation-bodytemp"

define "All Body Temperature Measurements All Statuses":
  ["observation-bodytemp"]

// Body Height

/*
@comment: [Body Height](http://hl7.org/fhir/R4/bodyheight.html)
@profile: http://hl7.org/fhir/StructureDefinition/bodyheight
*/
define "All Body Height Measurements":
   ((["observation-bodyheight"]).resulted()) O
     return O as "observation-bodyheight"

define "All Body Height Measurements All Statuses":
  ["observation-bodyheight"]

// Head Circumference

/*
@comment: [Head Circumference](http://hl7.org/fhir/R4/headcircum.html)
@profile: http://hl7.org/fhir/StructureDefinition/headcircum
*/
define "All Head Circumference Measurements":
  ((["observation-headcircum"]).resulted()) O
    return O as "observation-headcircum"

define "All Head Circumference Measurements All Statuses":
  ["observation-headcircum"]

// Body Weight

/*
@comment: [Body Weight](http://hl7.org/fhir/R4/bodyweight.html)
@profile: http://hl7.org/fhir/StructureDefinition/bodyweight
*/
define "All Body Weight Measurements":
  ((["observation-bodyweight"]).resulted()) O
    return O as "observation-bodyweight"

define "All Body Weight Measurements All Statuses":
  ["observation-bodyweight"]

// Body Mass Index

/*
@comment: [Body Mass Index](http://hl7.org/fhir/R4/bmi.html)
@profile: http://hl7.org/fhir/StructureDefinition/bmi
*/
define "All Body Mass Index Measurements":
  ((["observation-bmi"]).resulted()) O
    return O as "observation-bmi"

define "All Body Mass Index Measurements All Statuses":
  ["observation-bmi"]

// Blood Pressure

/*
@comment: [Blood Pressure](http://hl7.org/fhir/R4/bp.html)
@profile: http://hl7.org/fhir/StructureDefinition/bp
*/
define "All Blood Pressure Measurements":
  ((["observation-bp"]).resulted()) O
    return O as "observation-bp"

define "All Blood Pressure Measurements All Statuses":
  ["observation-bp"]

// Systolic Blood Pressure
define "Systolic Blood Pressure":
  "All Blood Pressure Measurements".SystolicBP.value

// Diastolic Blood Pressure
define "Diastolic Blood Pressure":
  "All Blood Pressure Measurements".DiastolicBP.value

/* MBODA InitialExpressions */

// TODO: Need to address how to identify which coverage is in use
// define "Coverage":
//   First(
//     [FHIR.Coverage] C
//       where EndsWith(C.beneficiary.reference, Patient.id)
//         and C.period includes Today()
//       sort by Coalesce(period.end, period.start) desc
//   )

// define "Member ID":
//   Coverage.identifier.where(type ~ "Member Number").single().value

define function MemberID(coverage FHIR.Coverage):
  coverage.identifier.where(type ~ "Member Number").single().value

define function PolicyNumber(coverage FHIR.Coverage):
  singleton from (
    coverage.identifier I
      where I.type ~ "Subscriber Number"
  ).value

define "Medical Record Number":
  Patient.identifier.where(type ~ UC.MedicalRecordNumber).single().value

define function MostRecentObservation(observations List<"FHIR.Observation">):
  Last(
    observations O
      sort by issued
  )

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

// define "Most Recent ServiceRequest":
//   UC.MostRecent("All ServiceRequests")

// define "Requesting Provider Practitioner"://assuming the requester is a practitioner
//   [PractitionerProfile] P
//     where EndsWith("Most Recent ServiceRequest".requester.reference.value, P.id)

define function RequestingProvider(serviceRequest FHIR.ServiceRequest):
  [PractitionerProfile] P
    where EndsWith(serviceRequest.requester.reference.value, P.id)

/*
define "Requesting Provider Organization":
  [OrganizationProfile] O
    where EndsWith("Most Recent ServiceRequest".requester.reference.value, O.id)
*/

// define "Requesting Provider":
//   "Requesting Provider Practitioner"

// define "Servicing Provider"://assuming the servicing provider is a lab - (molecular/genetic testing)
//   singleton from ([OrganizationProfile] O
//     where EndsWith(First("Most Recent ServiceRequest".performer.reference.value), O.id))

define function ServicingProvider(serviceRequest FHIR.ServiceRequest):
  [PractitionerProfile] P
    where EndsWith(First(serviceRequest.performer.reference.value), P.id)

// define "Test Coverage":
//   [FHIR.Coverage] C
//     with "Most Recent ServiceRequest" SR such that EndsWith(First(SR.insurance.reference), C.id)

// define "Billing Provider":
//   singleton from([OrganizationProfile] O
//     where EndsWith(First("Test Coverage".payor.reference.value), O.id))

define function BillingProvider(coverage FHIR.Coverage):
  singleton from([OrganizationProfile] O
    where EndsWith(First(coverage.payor.reference.value), O.id))

// define "Test Related Condition":
//   [FHIR.Condition] C
//     where EndsWith(First("Most Recent ServiceRequest".reasonReference.reference.value), C.id)
//   //.reasonReference changed to .reason in FHIR R5

define function RelatedCondition(serviceRequest FHIR.ServiceRequest):
  [FHIR.Condition] C
    where EndsWith(First(serviceRequest.reasonReference.reference.value), C.id)
  //.reasonReference changed to .reason in FHIR R5

define "Research Subject":
  [FHIR.ResearchSubject] R
    where  EndsWith(R.individual.reference, Patient.id)
    //ResearchSubject.individual is from FHIR version v4.0.1 and has been replaced by R.subject in FHIR R5

define "All Clinical Trials":
  [FHIR.ResearchStudy]

//bug req (does not work if full statement is in USCoreElements and referenced in GMTPInitialExpressions)
/*define "Clinical Trial":
  [FHIR.ResearchStudy] R
    with "Research Subject" S such that EndsWith(S.study.reference, R.id)
    with  "Test Related Condition" C such that C.code in R.condition */

define "Clinical Trial Organization":
  [OrganizationProfile] O

define function "GetServiceRequestReasonCondition"(serviceRequest ServiceRequest):
  [FHIR.Condition] C
    where EndsWith(First(serviceRequest.insurance.reference), C.id)
  return C

// define "Policy Number":
//   singleton from (
//     Coverage.identifier I
//       where I.type ~ "Subscriber Number"
//   )

define function Requester(medicationRequest MedicationRequestProfile):
  singleton from (
    [USCore.PractitionerProfile] P
      where EndsWith(medicationRequest.requester.reference, P.id)
  )

// May not be appropriate for the Elements library, this is based on identifying a medication request...
// define "Requester":
//   singleton from (
//     [USCore.PractitionerProfile] P
//       where EndsWith("Most Recent Medication Request".requester.reference, P.id)
//   )

define function RequesterRole(medicationRequest MedicationRequestProfile):
  singleton from (
    [USCore.PractitionerRoleProfile] R
      where EndsWith(R.practitioner.reference, Requester(medicationRequest).id)
  )

// define "RequesterRole":
//   singleton from (
//     [USCore.PractitionerRoleProfile] R
//       where EndsWith(R.practitioner.reference, "Requester".id)
//   )

