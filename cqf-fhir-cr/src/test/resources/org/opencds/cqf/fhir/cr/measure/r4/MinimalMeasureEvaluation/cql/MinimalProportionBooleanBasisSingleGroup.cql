library MinimalProportionBooleanBasisSingleGroup

using FHIR version '4.0.1'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Initial Population":
    AgeInYearsAt(start of "Measurement Period") > 0

define "Denominator":
    "Initial Population"

define "Denominator Exclusion":
    AgeInYearsAt(start of "Measurement Period") between 75 and 90

define "Denominator Exception":
    AgeInYearsAt(start of "Measurement Period") <=3

define "Numerator Exclusion":
    AgeInYearsAt(start of "Measurement Period") > 90

define "Numerator":
    AgeInYearsAt(start of "Measurement Period") > 2


define "Measure Population Exclusions":
    "Denominator Exclusion"

define "Measure Population":
    "Denominator"

define "date of compliance":
    "Measurement Period"