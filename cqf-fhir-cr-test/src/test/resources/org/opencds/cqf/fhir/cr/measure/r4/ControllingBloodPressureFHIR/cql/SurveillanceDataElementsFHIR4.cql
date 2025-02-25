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
codesystem "ISO-8601-Derived Periods": 'http://ohie.org/CodeSystem/iso-8601-derived-periods' // { 'P0Y--P1Y', 'P1Y--P5Y', ... }

code "Food insecurity risk [HVS]": '88124-3' from Common.LOINC display 'Food insecurity risk [HVS]'

// Age Groups
code "P0Y--P0Y": 'P0Y--P0Y' from "ISO-8601-Derived Periods" display '< 1 year'
code "P1Y--P4Y": 'P1Y--P4Y' from "ISO-8601-Derived Periods" display '1-4 years'
code "P5Y--P9Y": 'P5Y--P9Y' from "ISO-8601-Derived Periods" display '5-9 year'
code "P10Y--P14Y": 'P10Y--P14Y' from "ISO-8601-Derived Periods" display '10-14 year'
code "P15Y--P19Y": 'P15Y--P19Y' from "ISO-8601-Derived Periods" display '15-19 year'
code "P20Y--P24Y": 'P20Y--P24Y' from "ISO-8601-Derived Periods" display '20-24 year'
code "P25Y--P29Y": 'P25Y--P29Y' from "ISO-8601-Derived Periods" display '25-29 year'
code "P30Y--P34Y": 'P30Y--P34Y' from "ISO-8601-Derived Periods" display '30-34 year'
code "P35Y--P39Y": 'P35Y--P39Y' from "ISO-8601-Derived Periods" display '35-39 year'
code "P40Y--P49Y": 'P40Y--P49Y' from "ISO-8601-Derived Periods" display '40-49 year'
code "P50Y--P9999Y": 'P50Y--P9999Y' from "ISO-8601-Derived Periods" display '50+ years'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2019-01-01T00:00:00.0-06:00, @2020-01-01T00:00:00.0-06:00)

context Patient

// Age
define "Age":
  AgeInYearsAt(end of "Measurement Period")

// Age Group
define "Age Group":
  case
    when AgeInYearsAt(start of "Measurement Period") in Interval[0, 1) then "P0Y--P0Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[1, 5) then "P1Y--P4Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[5, 10) then "P5Y--P9Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[10, 15) then "P10Y--P14Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[15, 20) then "P15Y--P19Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[20, 25) then "P20Y--P24Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[25, 30) then "P25Y--P29Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[30, 35) then "P30Y--P34Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[35, 40) then "P35Y--P39Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[40, 50) then "P40Y--P49Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[50, null] then "P50Y--P9999Y"
    else null
  end

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
