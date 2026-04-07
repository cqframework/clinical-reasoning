library AdministerMedicationLibrary version '0.2.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

context Patient

define "Patient id":
  Patient.id

define "Medication Requests":
  [MedicationRequest]

define "Medication Proposal":
  First([MedicationRequest] M
      sort by authoredOn
  )

define "Medication Request Code":
  {system: 'http://hl7.org/fhir/uv/cpg/CodeSystem/cpg-activity-type-cs', code: 'order-medication', display: 'Order a medication'}

define "Medication Proposal Reference":
  if "Medication Proposal" is not null then
    { reference: 'MedicationRequest/' + "Medication Proposal".id }
  else
    null