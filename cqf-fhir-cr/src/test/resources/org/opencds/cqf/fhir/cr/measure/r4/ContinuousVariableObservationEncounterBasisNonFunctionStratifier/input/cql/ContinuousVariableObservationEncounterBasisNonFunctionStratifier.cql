library ContinuousVariableObservationEncounterBasisNonFunctionStratifier

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

context Patient

define "Initial Population Resource":
    "All Encounters"

define "Measure Population Resource":
    "Initial Population Resource"

define "Measure Population Exclusions Resource":
    "Encounter Cancelled"

define function "MeasureObservation"(encounter Encounter):
    duration in minutes of encounter.period

define "All Encounters":
    [Encounter] E

define "Encounter Cancelled":
    [Encounter] E
        where E.status = 'cancelled'

define "Age":
    AgeInYearsAt(date from end of "Measurement Period")
