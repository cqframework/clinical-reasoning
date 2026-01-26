library ContinuousVariableObservationEncounterBasis

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]
// cql parameter that can pass in fhir id of an encounter
parameter practitionerParam String

context Patient

// resource population results
// qty of matching encounters

define "Initial Population Resource":
    "All Encounters"

define "Measure Population Exclusions Resource":
    "Encounter Cancelled"

define "Measure Population Resource":
    "Initial Population Resource"

// Continuous Variable
// number of hours for encounter

define function "MeasureObservation"(encounter Encounter):
    duration in minutes of encounter.period

// main criteria logic

define "All Encounters":
     [Encounter] E

define "Encounter Cancelled":
    [Encounter] E
       where E.status = 'cancelled'

// For stratifier

// subject value stratifier
define "Age":
    AgeInYearsAt(start of "Measurement Period")
// non-subject value stratifier - returns patient age at time of encounter
// This stratifies by the actual patient's age, producing distinct strata for the test data
define function "Age Function" (e Encounter):
    AgeInYearsAt(start of e.period)