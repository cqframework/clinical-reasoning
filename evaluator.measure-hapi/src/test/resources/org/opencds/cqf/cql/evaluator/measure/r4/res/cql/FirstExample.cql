library FirstExample version '0.1.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

valueset "Marital Status": 'http://hl7.org/fhir/ValueSet/marital-status'

context Patient

define PatientExample:
  Patient P
    where P.gender = 'male'
      and P.active is true
      and P.birthDate 30 years or more before Today()
      and not(P.deceased)
      and P.maritalStatus in "Marital Status"
