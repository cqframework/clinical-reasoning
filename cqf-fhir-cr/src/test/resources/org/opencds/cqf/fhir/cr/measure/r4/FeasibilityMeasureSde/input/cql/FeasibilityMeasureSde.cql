library FeasibilityMeasureSde version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

// Only female patients qualify, so 3 of the 5 patients land in the initial population.
define "InitialPopulation":
  Patient.gender = 'female'

// Supplemental data element that emits the Patient id, but only for the members of the
// initial population. It re-uses the InitialPopulation define so the list stays consistent
// with the cohort. Patients not in the population evaluate to null and are dropped during
// SDE accumulation (CriteriaResult.nonNullValues), so they never appear in the report.
define "InitialPopulationIds":
  if "InitialPopulation" then Patient.id.value else null
