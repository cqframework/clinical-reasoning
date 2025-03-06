library RetrieveExample version '0.1.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

codesystem "LOINC": 'http://loinc.org'

valueset "Lung Cancer":  'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1116.89'
valueset "Condition Clinical Status": 'http://utah.edu/fhir/lcs-cds/ValueSet/conditionclinical'

code "Tobacco Smoking Status": '72166-2' from "LOINC"

context Patient

define "Smoking status observation":
  [Observation: "Tobacco Smoking Status"] O
    where O.status in { 'final', 'amended' }

define "Lung cancer diagnosis":
  [Condition: "Lung Cancer"] C
    where C.clinicalStatus in "Condition Clinical Status"
