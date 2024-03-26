library MinimalProportionResourceBasisSingleGroup

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Initial Population":
    ["Encounter"] InpatientEncounter

define "Denominator":
    "Initial Population"

define "Denominator Exclusion":
    ["Encounter"] InpatientEncounter
        with ["Patient"] BirthDate
            such that "CalendarAgeInYearsAt"(FHIRHelpers.ToDate(BirthDate.birthDate), start of InpatientEncounter.period) between 75 and 90

define "Denominator Exception":
    ["Encounter"] InpatientEncounter
        where "LengthInDays"(FHIRHelpers.ToInterval(InpatientEncounter.period)) > 5

define "Numerator Exclusion":
    ["Encounter"] InpatientEncounter
        with ["Patient"] BirthDate
            such that "CalendarAgeInYearsAt"(FHIRHelpers.ToDate(BirthDate.birthDate), start of InpatientEncounter.period)> 90

define "Numerator":
    ["Encounter"] InpatientEncounter
        with ["Patient"] BirthDate
            such that "CalendarAgeInYearsAt"(FHIRHelpers.ToDate(BirthDate.birthDate), start of InpatientEncounter.period) > 2
                where "LengthInDays"(FHIRHelpers.ToInterval(InpatientEncounter.period)) < 10

define function "MeasureObservation"(Encounter Encounter):
    Encounter e
        where (difference in days between start of e.period and end of e.period)>0

define "Measure Population Exclusions":
    "Denominator Exclusion"

define "Measure Population":
    "Denominator"

define function "CalendarAgeInYearsAt"(BirthDateTime DateTime, AsOf DateTime):
	years between ToDate(BirthDateTime)and ToDate(AsOf)

define function "LengthInDays"(Value Interval<DateTime>):
	difference in days between start of Value and end of Value