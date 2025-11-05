library "CriteriaBasedStratifiersComplex"

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

context Patient

define "Initial Population Boolean":
    exists "Initial Population Resource"

define "Initial Population Resource":
    "All Encounters"

define "Denominator Boolean":
    exists "Denominator Resource"

define "Denominator Resource":
    "Not Triaged Encounters"

define "Numerator Boolean":
    exists "Numerator Resource"

define "Numerator Resource":
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

define "Encounters in Period Boolean":
    exists "Encounters in Period Resource"

define "Encounters in Period Resource":
     [Encounter] E
       where E.period during "Measurement Period"
