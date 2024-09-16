library DM1 version '1.0.1'

/* Diabetes Management Measure 1 - Diabetes Poor Control */

using FHIR version '4.0.1'

include DQMDiabetesFHIRHelpers version '1.0.1' called FHIRHelpers
include FHIRCommon version '1.0.1' called FC
include DQMDiabetesCommonLogic version '1.0.1' called Common
include SupplementalDataElements version '1.0.1' called SDE
include AmbulatoryVisits version '1.0.1' called AV
include DMConcepts version '1.0.1' called DC

parameter "Measurement Period" Interval<DateTime> default Interval[@2019-01-01T00:00:00.0, @2019-12-31T11:59:59.9)

context Patient

/* SDE */
// SDEs: Provider, Days since last appointment, Last Appointment date, A1c
define "SDEPractitioner":
  SDE."Most Recent Practitioner"

define "SDEDaysSinceAppointment":
  SDE."Days Since Last Appointment"

define "SDELastAppointmentDate":
  SDE."Last Appointment"

define "SDEA1C":
  "Most Recent HbA1c"

// Filters: 
//  Provider: Chosen from list of providers (CQL Definition to create list of providers -- not exposed as SDE)
//  Last Appointment Date
//  A1c: Whole Number filters from 1 - 15
define "SDEFilterPractitionersList":
  SDE."Practitioners List"

define "SDEFilterLastAppointment":
  SDE."Last Appointment"

define "SDEFilterA1C":
  "Most Recent HbA1c"


/* Population Criteria */
// Initial Population / Denominator: 
//  - >= 18 years old, with an active diagnosis of type 1 or type 2 diabetes mellitus, and has an ambulatory visit in the time period
define "Initial Population":
  AgeInYearsAt(date from start of "Measurement Period") > 18
    and exists (
      AV."Qualifying Encounters" QualifyingEncounter
        where FC.ToInterval(QualifyingEncounter.period) during "Measurement Period")
    and exists (Common.QualifiedConditions([Condition: DC."Diabetes"]))

define "Denominator":
  "Initial Population"

define "Denominator Exclusion":
  exists "Visits within last 6 months"

// Numerator: Patient with A1C > 9
define "Numerator":
  "Has Most Recent Elevated HbA1c" is true


/* Definitions - Numerator */
define "Last 6 Month Period":
  Interval[end of "Measurement Period" - 6 months, end of "Measurement Period"]

define "Visits within last 6 months":
  AV."Qualifying Encounters" QualifyingEncounter
    where FC.ToInterval(QualifyingEncounter.period) during "Last 6 Month Period"

define "Most Recent HbA1c":
  Common.MostRecentObservation(
    (Common.QualifiedObservations([Observation: DC."HbA1c Laboratory Test"])) RecentHbA1c
      where end of FC.ToInterval(RecentHbA1c.effective) during "Measurement Period"
  )

define "Has Most Recent Elevated HbA1c":
  ("Most Recent HbA1c".value as Quantity) > 9 '%'


// Tests
define "SDEResourceTest":
  SDE."Resource Test"

define "SDEProviderTest":
  SDE."Provider Test"

define "SDETupleTest":
  SDE."Tuple Test"

define "SDEListTest":
  SDE."List Test"

define "Last Encounter":
  (Common.MostRecentEncounter(AV."Qualifying Encounters"))

define "Resource Construction Test Inlib":
  FHIR.Observation {
    id: FHIR.id { id: 'observation-sdetestinlib-' + ToString(Now()) },
    status: FHIR.ObservationStatus { value: 'final' },
    code: FHIR.CodeableConcept { coding: { FHIR.Coding { code: FHIR.code { value: 'days-since-appointment' } } } },
    subject: FHIR.Reference { reference: FHIR.string { value: Patient.id.value }},
    value: FHIR.integer { value: Abs(difference in days between end of SDE."Last Appointment".period and ToDateTime(Today())) }
  }

define "SDEResourceConstructionTest":
  SDE."Resource Construction Test"
  
define "SDEResourceConstructionTestInlib":
  "Resource Construction Test Inlib"  
