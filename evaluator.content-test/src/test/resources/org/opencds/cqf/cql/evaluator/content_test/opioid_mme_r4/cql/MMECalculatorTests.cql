library MMECalculatorTests version '3.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'
include MMECalculator version '3.0.0'

context Patient

define TotalMME: MMECalculator.TotalMME([MedicationRequest])
