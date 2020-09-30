library TestFHIR

/*
Every expression in this library should evaluate to non-null or non-empty,
with the exception of TestPatientDeceasedDateTime, which should evaluate to null
 */

using FHIR version '3.0.0'

codesystem "LOINC": 'http://loinc.org'
codesystem "Marital Status Codes": 'http://hl7.org/fhir/v3/MaritalStatus'
codesystem "CDC Race and Ethnicity Codes": 'urn:oid:2.16.840.1.113883.6.238'

valueset "Marital Status": 'http://hl7.org/fhir/ValueSet/marital-status'

code "Marital Status - Married": 'M' from "Marital Status Codes"
code "American Indian or Alaska Native": '1002-5' from "CDC Race and Ethnicity Codes"
code "Shoshone": '1586-7' from "CDC Race and Ethnicity Codes"
code "Blood pressure": '85354-9' from "LOINC" display 'blood pressure'
code "Diastolic blood pressure": '8462-4' from "LOINC" display 'Diastolic blood pressure'
code "Systolic blood pressure": '8480-6' from "LOINC" display 'Systolic blood pressure'

context Patient

define TestAllergyIntolerance: ["AllergyIntolerance"]
define TestCarePlan: ["CarePlan"]
define TestCareTeam: ["CareTeam"]
define TestCondition: ["Condition"]
define TestDevice: ["Device"]
define TestDiagnosticReport: ["DiagnosticReport"]
define TestDocumentReference: ["DocumentReference"]
define TestEncounter: ["Encounter"]
define TestGoal: ["Goal"]
define TestImmunization: ["Immunization"]
define TestLocation: ["Location"]
define TestMedication: ["Medication"]
define TestMedicationRequest: ["MedicationRequest"]
define TestMedicationStatement: ["MedicationStatement"]
define TestObservation: ["Observation"]
define TestOrganization: ["Organization"]
define TestPatient: ["Patient"]
define TestPractitioner: ["Practitioner"]
define TestProcedure: ["Procedure"]

define TestPatientGender:
  Patient P
    where P.gender.value = 'female'

define TestPatientActive:
  Patient P
    where P.active.value is true

define TestPatientBirthDate:
  Patient P
    where P.birthDate.value before Today()

define TestPatientMaritalStatusMembership:
  Patient P
    where ToConcept(P.maritalStatus) in "Marital Status"

define TestPatientMartialStatusComparison:
  Patient P
    where ToConcept(P.maritalStatus) ~ "Marital Status - Married"

define TestPatientDeceasedAsBoolean:
  Patient P
    where (P.deceased as FHIR.boolean).value is false

define TestPatientDeceasedAsDateTime:
  Patient P
    where (P.deceased as FHIR.dateTime).value before Today()

define TestSlices:
  [Observation: "Blood pressure"] BP
    let
      SystolicBP: singleton from (BP.component C where ToConcept(C.code) ~ "Systolic blood pressure"),
      DiastolicBP: singleton from (BP.component C where ToConcept(C.code) ~ "Diastolic blood pressure")
    where ToQuantity(SystolicBP.value as FHIR.Quantity) < 140 'mm[Hg]'
      and ToQuantity(DiastolicBP.value as FHIR.Quantity) < 90 'mm[Hg]'

define TestSimpleExtensions:
  Patient P
    let birthsex: singleton from (P.extension E where E.url.value = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex')
    where (birthsex.value as FHIR.code).value = 'F'

define TestComplexExtensions:
  Patient P
    let
      race: singleton from (P.extension E where E.url.value = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-race'),
      ombCategory: race.extension E where E.url.value = 'ombCategory',
      detailed: race.extension E where E.url.value = 'detailed'
    where exists (ombCategory O where ToCode(O.value as FHIR.Coding) ~ "American Indian or Alaska Native")
      and exists (detailed O where ToCode(O.value as FHIR.Coding) ~ "Shoshone")

define function ToInterval(period FHIR.Period):
    if period is null then
        null
    else
        Interval[period."start".value, period."end".value]

define function ToQuantity(quantity FHIR.Quantity):
    if quantity is null then
        null
    else
        System.Quantity { value: quantity.value.value, unit: quantity.unit.value }

define function ToInterval(range FHIR.Range):
    if range is null then
        null
    else
        Interval[ToQuantity(range.low), ToQuantity(range.high)]

define function ToCode(coding FHIR.Coding):
    if coding is null then
        null
    else
        System.Code {
          code: coding.code.value,
          system: coding.system.value,
          version: coding.version.value,
          display: coding.display.value
        }

define function ToConcept(concept FHIR.CodeableConcept):
    if concept is null then
        null
    else
        System.Concept {
            codes: concept.coding C return ToCode(C),
            display: concept.text.value
        }



