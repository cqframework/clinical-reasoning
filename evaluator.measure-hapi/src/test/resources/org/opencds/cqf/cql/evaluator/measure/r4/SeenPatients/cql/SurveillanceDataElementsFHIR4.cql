/*
This library provides additional data elements to be collected as part of surveillance
reporting events
*/
library SurveillanceDataElementsFHIR4 version '1.0.000'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.001'

include MATGlobalCommonFunctionsFHIR4 version '6.0.000' called Common
include AdultOutpatientEncountersFHIR4 version '2.0.000'called AdultOutpatientEncounters

codesystem "Jurisdictions": 'http://hl7.org/fhir/us/ecr/CodeSystem/ersd-jurisdictions-example'

code "Food insecurity risk [HVS]": '88124-3' from Common.LOINC display 'Food insecurity risk [HVS]'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2019-01-01T00:00:00.0-06:00, @2020-01-01T00:00:00.0-06:00)

context Patient

// Age
define "Age":
  AgeInYearsAt(end of "Measurement Period")

// Jurisdiction of residence (jurisdiction of the patient)
define "Most recent physical home address":
  First(
    Patient.address A
      where A.use = 'home'
        and A.type = 'physical'
      sort by end of period desc
  )

define "State of Residence":
  "Most recent physical home address".state

define "Postal Code of Residence":
  "Most recent physical home address".postalCode

// Jurisdiction of care (jurisdiction of the most recent encounter)
// TODO:
//define "Most recent address of care":

// NOTE: Included in eICR
// Occupational Data for Health - Past or Present Job
// http://hl7.org/fhir/us/odh/StructureDefinition-odh-PastOrPresentJob.html
// Observation: LOINC#11341-5, valueCodeableConcept in https://phinvads.cdc.gov/vads/ViewValueSet.action?oid=2.16.840.1.114222.4.11.7186

// Social Determinants of Health (SDOH)
// http://build.fhir.org/ig/HL7/fhir-sdoh-clinicalcare/StructureDefinition-SDOHCC-ObservationScreeningResponse.html
// Food insecurity risk: http://build.fhir.org/ig/HL7/fhir-sdoh-clinicalcare/Observation-SDOHCC-ObservationResponseHungerVitalSignQuestion3Example.html
// Observation: LOINC#88124-3, valueCodeableConcept in https://loinc.org/LL2510-7/ (At risk, No risk)
define "Food Insecurity Risk Status":
  First(
    [Observation: "Food insecurity risk [HVS]"] O
      sort by issued desc
  ).value as CodeableConcept
