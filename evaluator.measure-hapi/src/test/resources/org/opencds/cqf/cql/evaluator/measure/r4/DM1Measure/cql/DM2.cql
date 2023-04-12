library DM2 version '1.0.1'

/* Diabetes Management Measure 2 - Diabetes with no UACR/urine microalbumin measurement */

using FHIR version '4.0.1'

include DQMDiabetesFHIRHelpers version '1.0.1' called FHIRHelpers
include FHIRCommon version '1.0.1' called FC
include DQMDiabetesCommonLogic version '1.0.1' called Common
include SupplementalDataElements version '1.0.1' called SDE
include AmbulatoryVisits version '1.0.1' called AV
include DMConcepts version '1.0.1' called DC

parameter "Measurement Period" Interval<DateTime>

context Patient

/* SDE */
// SDEs: Provider, Days since last appointment, Last Appointment date, standard urinalysis
define "SDEPractitioner":
  SDE."Most Recent Practitioner"

define "SDEDaysSinceAppointment":
  SDE."Days Since Last Appointment"

define "SDELastAppointmentDate":
  SDE."Last Appointment" 

define "SDEStandardUrinalysis":
  exists "Standard Urinalysis Test"

// Filters:
//  Provider: Chosen from list of providers (CQL Definition to create list of providers -- not exposed as SDE)
//  Last Appointment Date
//  Standard Urinalysis: true/false
define "SDEFilterPractitionersList":
  SDE."Practitioners List"

define "SDEFilterLastAppointment":
  SDE."Last Appointment"
  
define "SDEFilterUrinalysis":
  "Standard Urinalysis Test"


/* Population Criteria */
// Initial Population / Denominator: 
//  - Patients meeting all of the following criteria: 
//      - >= 18 years old with an active diagnosis of type 1 or type 2 diabetes mellitus and had an ambulatory visit in the time period
define "Initial Population":
  AgeInYearsAt(date from start of "Measurement Period") > 18
    and exists (
      AV."Qualifying Encounters" QualifyingEncounter
        where FC.ToInterval(QualifyingEncounter.period) during "Measurement Period")
    and exists (Common.QualifiedConditions([Condition: DC."Diabetes"]))

define "Denominator":
  "Initial Population"

define "Denominator Exclusion":
  exists (Common.QualifiedConditions([Condition: DC."Diabetic Nephropathy"]))
  or exists "Visits within last 6 months"

// Numerator: No urine microalbumin result in the time period
define "Numerator":
  not exists "Urine Microalbumin Test"


/* Definitions - Numerator */
define "Last 6 Month Period":
  Interval[end of "Measurement Period" - 6 months, end of "Measurement Period"]

define "Visits within last 6 months":
  AV."Qualifying Encounters" QualifyingEncounter
    where FC.ToInterval(QualifyingEncounter.period) during "Last 6 Month Period"

 define "Urine Microalbumin Test":
    (Common.QualifiedObservations([Observation: DC."Microalbumin Test"])) UrineMicroalbumin
      where end of FC.ToInterval(UrineMicroalbumin.effective) during "Last 6 Month Period"


/* Supplemental Data Elements */
 define "Standard Urinalysis Test":
    (Common.QualifiedObservations([Observation: DC."Urinalysis"])) StandardUrinalysis
      where end of FC.ToInterval(StandardUrinalysis.effective) during "Last 6 Month Period"


// Tests
define "SDEResourceTest":
  SDE."Resource Test"

define "SDEProviderTest":
  SDE."Provider Test"

define "SDETupleTest":
  SDE."Tuple Test"

define "SDEListTest":
  SDE."List Test"

define "SDEResourceConstructionTest":
  SDE."Resource Construction Test"
