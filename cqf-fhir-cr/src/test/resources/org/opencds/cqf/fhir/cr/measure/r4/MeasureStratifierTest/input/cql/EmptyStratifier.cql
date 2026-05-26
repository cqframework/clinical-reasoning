library EmptyStratifier

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2025-01-01T00:00:00.000Z, @2025-12-31T23:59:59.999Z]

context Patient

// Populations referenced by the Measure resource (boolean basis).
// Definitions are intentionally trivial — the test exercises the stratifier
// validation path, which fails before any evaluation result is consulted
// because the Measure declares stratifiers with a null criteria.expression.

define "Initial Population":
  true

define "Denominator":
  "Initial Population"

define "Denominator Exclusions":
  false

define "Numerator":
  "Initial Population"

// Supplemental data elements referenced by Measure.supplementalData

define "SDE Sex":
  Patient.gender

define "SDE Race":
  null

define "SDE Ethnicity":
  null

define "SDE Payer":
  null
