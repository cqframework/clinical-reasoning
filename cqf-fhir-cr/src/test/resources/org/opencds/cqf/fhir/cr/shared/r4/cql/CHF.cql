library CHF version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

codesystem "SNOMED-CT": 'http://snomed.info/sct'
codesystem "LOINC": 'http://loinc.org'
codesystem "CHFCodes": 'http://hl7.org/fhir/uv/cpg/CodeSystem/chf-codes'
codesystem "Condition Clinical Status Code": 'http://terminology.hl7.org/CodeSystem/condition-clinical'
codesystem "Condition Verification Status Code": 'http://terminology.hl7.org/CodeSystem/condition-ver-status'

code "Congestive heart failure": '42343007' from "SNOMED-CT"

code "Body weight": '29463-7' from "LOINC"
code "Body weight change": 'body-weight-change' from "CHFCodes"
code "Urine output": '9192-6' from "LOINC"
code "Net intake/output": 'net-intake-output' from "CHFCodes"
code "Jugular venous pressure": 'jvp' from "CHFCodes"
code "Oxygen saturation": '2708-6' from "LOINC"
code "Potassium goal": '86919-8' from "LOINC" display 'Potassium goal [Moles/volume] Serum or Plasma' // meq/L
code "Creatinine in serum": '39802-4' from "LOINC" display 'Creatinine in dialysis fluid/Creatinine in serum or plasma' // NOTE: example shows mg/dL, but no LOINC code has those units?
code "eGFR result": 'egfr' from "CHFCodes" // NOTE: Too many to pick from, needs clinical/terminological SME input, in mol/mm/m2

code "Active condition": 'active' from "Condition Clinical Status Code"
code "Provisional condition": 'provisional' from "Condition Verification Status Code"
code "Confirmed condition": 'confirmed' from "Condition Verification Status Code"

parameter Encounter Encounter

context Patient

// Case Features
define "Body Weight":
  [Observation: "Body weight"] O
    where O.status = 'final'
      and references(O.encounter, Encounter)

define "Body Weight Change Assertion":
  [Observation: "Body weight change"] O
    where O.status = 'final'
      and references(O.encounter, Encounter)

define "Body Weight Change":
  "Daily Body Weight Change" WC
    return Observation {
      id: id { value: Encounter.id + '-bodyweight-change' + ToString(WC.date) },
      extension: {
        Extension {
          url: url { value: 'http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-instantiatesCaseFeature' },
          value: canonical { value: 'http://hl7.org/fhir/uv/cpg/StructureDefinition/chf-bodyweight-change' }
        },
        Extension {
          url: url { value: 'http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-caseFeatureType' },
          value: code { value: 'asserted' }
        }
      },
      status: ObservationStatus { value: 'final' },
      code: CodeableConcept {
        coding: {
          Coding {
            system: uri { value: 'http://hl7.org/fhir/uv/cpg/CodeSystem/chf-codes' },
            code: code { value: 'body-weight-change' }
          }
        }
      },
      effective: dateTime { value: WC.date },
      issued: instant { value: Now() },
      subject: referenceTo(Patient),
      encounter: referenceTo(Encounter),
      value: Quantity {
        value: decimal { value: WC.change.value },
        unit: string { value: WC.change.unit },
        system: uri { value: 'http://unitsofmeasure.org' },
        code: code { value: WC.change.unit }
      }
    }

define "Daily Body Weight":
  (expand Encounter.period per day) Date
    let maxWeight: Max("Body Weight" WT where WT.issued same day as Date return WT.value as FHIR.Quantity)
    return { date: Date, weight: maxWeight }

define "Daily Body Weight Change":
  "Daily Body Weight" WT
    let priorWeight: First("Daily Body Weight" PWT where PWT.date < WT.date sort by date descending).weight
    return { date: WT.date, weight: WT.weight, priorWeight: priorWeight, change: WT.weight - priorWeight}

define "Current Body Weight":
  First("Body Weight" WT where WT.issued same day as Today() sort by issued descending)

define "Previous Body Weight":
  First("Body Weight" WT where WT.issued 1 day before day of Today() sort by issued descending)

define "Current Body Weight Change":
  "Current Body Weight".value - "Previous Body Weight".value

define "Urine Output":
  [Observation: "Urine output"] O
    where O.status = 'final'
    and references(O.encounter, Encounter)

define "Current Urine Output":
  First("Urine Output" UO where UO.issued same day as Today() sort by issued descending)

define "Net Intake/Output":
  [Observation: "Net intake/output"] O
    where O.status = 'final'
      and references(O.encounter, Encounter)

define "Current Net Intake/Output":
  First("Net Intake/Output" IO where IO.issued same day as Today() sort by issued descending)

define "Jugular Venous Pressure":
  [Observation: "Jugular venous pressure"] O
    where O.status = 'final'
      and references(O.encounter, Encounter)

define "Oxygen Saturation":
  [Observation: "Oxygen saturation"] O
    where O.status = 'final'
      and references(O.encounter, Encounter)

define "Potassium":
  [Observation: "Potassium goal"] O
    where O.status = 'final'
      and references(O.encounter, Encounter)

define "Creatinine":
  [Observation: "Creatinine in serum"] O
    where O.status = 'final'
      and references(O.encounter, Encounter)

define "eGFR":
  [Observation: "eGFR result"] O
    where O.status = 'final'
      and references(O.encounter, Encounter)


// Eligibility Criteria
define "Eligibility Criteria":
  [Condition] C
    where C.code ~ "Congestive heart failure"
      and C.clinicalStatus ~ "Active condition"
      and C.verificationStatus ~ "Confirmed condition"

// TODO: Handle contained references
// TODO: Handle bundle references
// TODO: Handle remote references
define function references(reference Reference, encounter Encounter):
  EndsWith(reference.reference, '/' + encounter.id)

define function referenceTo(encounter Encounter):
  Reference { reference: string { value: 'Encounter/' + encounter.id } }

define function referenceTo(patient Patient):
  Reference { reference: string { value: 'Patient/' + patient.id } }


// Initial Expressions
define "Body Weight Change.status":
    "Body Weight".status

define "Body Weight Change.valueQuantity.value":
    "Body Weight".value.value

define "Body Weight Change.valueQuantity.unit":
    "Body Weight".value.unit