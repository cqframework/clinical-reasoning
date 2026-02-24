library LibrarySimple

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

define "Denominator Boolean":
    "Initial Population Boolean"

define "Denominator Exclusion Boolean":
  exists "Encounter Cancelled"

define "Denominator Exception Boolean":
  exists "Encounter InProgress"

define "Numerator Exclusion Boolean":
  exists "Encounter Arrived"

define "Numerator Boolean":
  exists "Encounters in Period"

define "Measure Population Exclusions Boolean":
    "Denominator Exclusion Boolean"

define "Measure Population Boolean":
    "Denominator Boolean"

// resource population results
// qty of matching encounters

define "Initial Population Resource":
    "All Encounters"

define "Denominator Resource":
    "Initial Population Resource"

define "Denominator Exclusion Resource":
    "Encounter Cancelled"

define "Denominator Exception Resource":
    "Encounter InProgress"

define "Numerator Exclusion Resource":
    "Encounter Arrived"

define "Numerator Resource":
    "Encounters in Period"

define "Measure Population Exclusions Resource":
    "Denominator Exclusion Resource"

define "Measure Population Resource":
    "Denominator Resource"

// for prospective gap calculations
define "date of compliance":
    "Measurement Period"

// cql to force results
define "always false":
    false

define "always true":
    true

define "list of boolean":
    {true, false, true}

define "list of dates":
  { @2024-01-01, @2024-01-02, @2024-01-03 }

define "PatientRes":
    [Patient] P

define "list of string":
    {'test1', 'test2', 'test3'}

define "interval":
    Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

define "TestIntervalList":
  {
    Interval[@2024-01-01T00:00:00, @2024-01-31T23:59:59],
    Interval[@2024-02-01T00:00:00, @2024-02-29T23:59:59]

  }

define "TestDecimalList":
  { 1.5, 2.75, 1.0 / 3.0 }

define "TestFloatValue":
    1.0 / 3.0

define "list of numbers":
    {31, 88, 11}

define "string":
    'string test'

define "date":
    @2026-01-01

define "datetime":
    @2026-12-30T23:59:59

define "NullExample":
         null

define "EmptyListExample":
  { }

define "number":
         31

define "decimal":
         31.31

define "test tuple":
         {
    number: "number",
    dates: "list of dates",
    birthYear: "string"
  }

define "list test tuple":
         {{
    number: "number",
    dates: "list of dates",
    birthYear: "string"
  },{
    number: "number",
    dates: "list of dates",
    birthYear: "string"
  }}

define "test code":
    "SDE Sex"

// sde single value

define "SDE Sex":
  case
      when Patient.gender = 'male' then Code { code: 'M', system: 'http://hl7.org/fhir/v3/AdministrativeGender', display: 'Male' }
      when Patient.gender = 'female' then Code { code: 'F', system: 'http://hl7.org/fhir/v3/AdministrativeGender', display: 'Female' }
      else null
    end

// sde list of values

 define "SDE Encounters":
    "All Encounters"

// Continuous Variable
// number of hours for encounter

define function "MeasureObservation"(encounter Encounter):
    duration in minutes of encounter.period

// Cont Variable
// boolean basis age in years to end of measurementPeriod

define function "MeasureObservationBoolean"():
         AgeInYearsAt(end of "Measurement Period")
// component stratifier

define "Gender Stratification":
  "SDE Sex"

define function "Age of Period" (i Interval<DateTime>):
  AgeInYearsAt(end of i)
// boolean criteria stratifier

define "boolean strat not finished":
  exists "Encounter Not Finished"

// cql parameter boolean criteria stratifier

define "boolean strat has practitioner":
  exists "Matching General Practitioner"

// resource criteria stratifier

define "resource strat not finished":
    "Encounter Not Finished"

// main criteria logic

define "All Encounters":
     [Encounter] E

define "Encounters in Period":
     [Encounter] E
       where E.period during "Measurement Period" and E.status='finished'

define "Encounter Cancelled":
    [Encounter] E
       where E.status = 'cancelled'

define "Encounter Status":
    [Encounter] E
         return E.status

define "Encounter InProgress":
    [Encounter] E
       where E.status = 'in-progress'

define "Encounter Arrived":
    [Encounter] E
       where E.status = 'arrived'

define "Encounter Not Finished":
    [Encounter] E
        where E.status != 'finished'

define "Matching General Practitioner":
    [Patient] p
       where Last(Split(First(p.generalPractitioner.reference),'/')) = Last(Split(practitionerParam,'/'))

define "Age":
    AgeInYearsAt(start of "Measurement Period")

define "Patient Age Bracket":
  case
    when AgeInYearsAt(start of "Measurement Period") between 0 and 20 then '0--21'
    when AgeInYearsAt(start of "Measurement Period") between 21 and 40 then '21--41'
    when AgeInYearsAt(start of "Measurement Period") > 41 then '>41'
    else 'unknown'
  end

define "Date":
    Interval[@2024-02-01T00:00:00, @2024-10-31T23:59:59]

define "ip date":
    "Date"

define "den date":
    "Date"

define "num date":
    "Date"

define "exc date":
    "Date"

// valueStratifier function for non-subject based measures

define function "Age Range Stratifier"(encounter Encounter):
  case
    when AgeInYearsAt(start of encounter.period) between 0 and 20 then 'P0Y--P21Y'
    when AgeInYearsAt(start of encounter.period) between 21 and 40 then 'P21Y--P41Y'
    when AgeInYearsAt(start of encounter.period) > 41 then 'P41Y--P9999Y'
    else null
  end

define function "Age Function"(encounter Encounter):
  AgeInYearsAt(start of encounter.period)

define function "Encounter Status Stratifier"(encounter Encounter):
  Coalesce(encounter.status.value, 'unknown')

define function "Empty Function"(encounter Encounter):
  {}

define function "Null Function"(encounter Encounter):
  null