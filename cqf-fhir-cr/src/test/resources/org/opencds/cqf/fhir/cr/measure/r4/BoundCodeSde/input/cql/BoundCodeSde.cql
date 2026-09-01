library BoundCodeSde

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

define "Initial Population":
  true

/*
  Returns whole MedicationDispense resources as supplemental data. MedicationDispense.status is a
  bound code: HAPI declares it as Enumeration<MedicationDispenseStatus>, while the CQL value carries
  only the code text. A converter that picks the target HAPI type from the CQL type name rather than
  from the HAPI child definition builds a CodeType for it, which the field will not accept.
*/
define "SDE Medication Dispense":
  [MedicationDispense]
