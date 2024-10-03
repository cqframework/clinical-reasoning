library UberSimple

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

// Below makes the numerator assertion fail:
//parameter "Measurement Period" Interval<DateTime> default Interval[@2020-01-16T20:00:00, @2020-01-16T21:00:00)
// Below makes the numerator assertion pass:
parameter "Measurement Period" Interval<DateTime> default Interval[@2020-01-16T19:59:00, @2020-01-16T21:01:00)

context Patient

define "Initial Population":
    exists ("All Encounters")

define "Denominator":
    "Initial Population"

define "Numerator":
    exists ("Encounters in Period")


define "All Encounters":
     [Encounter] E

define "Encounters in Period":
     [Encounter] E
       where E.period during "Measurement Period"