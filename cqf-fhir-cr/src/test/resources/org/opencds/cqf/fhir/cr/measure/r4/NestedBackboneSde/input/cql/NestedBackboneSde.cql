library NestedBackboneSde

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

define "Initial Population":
  true

/*
  Returns whole ExplanationOfBenefit resources as supplemental data. The SDE pipeline converts each
  engine-native ClassInstance back to HAPI FHIR, which walks every populated element of the resource,
  including ExplanationOfBenefit.item.adjudication - a backbone element nested inside another
  backbone element.
*/
define "SDE Explanation Of Benefit":
  [ExplanationOfBenefit]
