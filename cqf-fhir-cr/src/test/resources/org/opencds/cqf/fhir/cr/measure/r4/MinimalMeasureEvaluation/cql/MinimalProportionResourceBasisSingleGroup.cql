library MinimalProportionResourceBasisSingleGroup

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers
include MATGlobalCommonFunctions version '5.0.000' called Global

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Initial Population":
    Global."Inpatient Encounter" InpatientEncounter

define "Denominator":
    "Initial Population"

define "Denominator Exclusion":
    Global."Inpatient Encounter" InpatientEncounter
        with ["Patient"] BirthDate
            such that Global."CalendarAgeInYearsAt"(FHIRHelpers.ToDate(BirthDate.birthDate), start of InpatientEncounter.period) between 75 and 90

define "Denominator Exception":
    Global."Inpatient Encounter" InpatientEncounter
        where Global."LengthInDays"(FHIRHelpers.ToInterval(InpatientEncounter.period)) > 5

define "Numerator Exclusion":
    Global."Inpatient Encounter" InpatientEncounter
        with ["Patient"] BirthDate
            such that Global."CalendarAgeInYearsAt"(FHIRHelpers.ToDate(BirthDate.birthDate), start of InpatientEncounter.period)> 90

define "Numerator":
    Global."Inpatient Encounter" InpatientEncounter
        with ["Patient"] BirthDate
            such that Global."CalendarAgeInYearsAt"(FHIRHelpers.ToDate(BirthDate.birthDate), start of InpatientEncounter.period) > 2
                where Global."LengthInDays"(FHIRHelpers.ToInterval(InpatientEncounter.period)) < 10

define function "MeasureObservation"(Encounter Encounter):
    Encounter e
        where (difference in days between start of e.period and end of e.period)>0

define "Measure Population Exclusions":
    "Denominator Exclusion"

define "Measure Population":
    "Denominator"