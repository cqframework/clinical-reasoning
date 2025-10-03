library "CriteriaBasedStratifiersComplex"

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

context Patient

define "Initial Population":
    "All Encounters"

define "Denominator":
    "Not Triaged Encounters"

define "Numerator":
    "Not Finished Encounters"

define "Not Triaged Encounters":
    "All Encounters" E
        where not(E.status.value in { 'triaged' })

define "Not Finished Encounters":
    "All Encounters" E
        where not(E.status.value in { 'finished' })

define "All Encounters":
    [Encounter] E
        where E.status.value in { 'planned', 'triaged', 'finished', 'in-progress' }

define "Encounters in Period":
     [Encounter] E
       where E.period during "Measurement Period"
