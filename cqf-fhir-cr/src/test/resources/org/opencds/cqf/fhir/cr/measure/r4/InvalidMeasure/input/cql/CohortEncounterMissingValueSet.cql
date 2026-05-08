library CohortEncounterMissingValueSet

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

valueset "Encounter Status Missing": 'http://example.com/ValueSet/encounter-status-missing'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

context Patient

define "Initial Population":
  [Encounter] E
    where E.status in "Encounter Status Missing"

define "Patient Gender String":
  Patient.gender.value

define function "Encounter Status Function"(encounter Encounter):
  encounter.status.value
