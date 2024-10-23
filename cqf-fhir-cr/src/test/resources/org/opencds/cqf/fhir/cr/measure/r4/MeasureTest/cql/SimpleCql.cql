library SimpleCql

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]
//# cql parameter that can pass in fhir id of an encounter
parameter practitionerParam String

context Patient

//# boolean population results
//# has matching encounter

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

//# resource population results
//# qty of matching encounters

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

//# for prospective gap calculations
define "date of compliance":
    "Measurement Period"

//# cql to force results
define "always false":
    false

define "always true":
    true

//# sde

define "SDE Sex":
  case
      when Patient.gender = 'male' then Code { code: 'M', system: 'http://hl7.org/fhir/v3/AdministrativeGender', display: 'Male' }
      when Patient.gender = 'female' then Code { code: 'F', system: 'http://hl7.org/fhir/v3/AdministrativeGender', display: 'Female' }
      else null
    end

//# Continuous Variable
//# number of hours for encounter

define function "MeasureObservation"(Encounter Encounter):
    Encounter e
        where (difference in hours between start of e.period and end of e.period)>0

//# component stratifier

define "Gender Stratification":
  "SDE Sex"

//# boolean criteria stratifier

define "boolean strat not finished":
  exists "Encounter Not Finished"

//# cql parameter boolean criteria stratifier

define "boolean strat has practitioner":
  exists "Matching General Practitioner"

//# resource criteria stratifier

define "resource strat not finished":
    "Encounter Not Finished"

//# main criteria logic

define "All Encounters":
     [Encounter] E

define "Encounters in Period":
     [Encounter] E
       where E.period during "Measurement Period"

define "Encounter Cancelled":
    [Encounter] E
       where E.status = 'cancelled'

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
