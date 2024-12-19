library GroupLevelMeasureScorerNoMeasureLevel version '1.0.001'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

define "Initial Population":
  "All Encounters"

define "All Encounters":
     [Encounter] E

define "Denominator":
    "Initial Population"

define "Numerator":
  "Encounters in Period"

define "Encounters in Period":
     [Encounter] E
       where E.period during "Measurement Period" and E.status='finished'
