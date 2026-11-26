library FeasibilityMeasure version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

// Only female patients qualify, so 3 of the 5 patients land in the initial population.
define "InitialPopulation":
  Patient.gender = 'female'

define "PatientIdStratifier":
  Patient.id.value
