library ContinuousVariableObservationBooleanBasis

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]
// cql parameter that can pass in fhir id of an encounter
parameter practitionerParam String

context Patient

// boolean population results
// has matching encounter

define "Initial Population Boolean":
  exists "All Encounters"

define "Measure Population Exclusion Boolean":
  exists "Encounter Cancelled"

define "Measure Population Boolean":
    "Initial Population Boolean"

// Continuous Variable
// age of patient in years from Measurement Period start

define function "MeasureObservation"():
	CalendarAgeInYearsAt(Patient.birthDate, "Measurement Period".low)

define function "CalendarAgeInYearsAt"(BirthDate Date, AsOf DateTime):
	years between BirthDate and ToDate(AsOf)

// main criteria logic

define "All Encounters":
     [Encounter] E

define "Encounter Cancelled":
    [Encounter] E
       where E.status = 'cancelled'

define "Gender":
    Patient.gender
