library TestFHIRWithHelpers

/*
Every expression in this library should evaluate to non-null or non-empty,
with the exception of TestPatientDeceasedDateTime, which should evaluate to null
 */

using FHIR version '3.0.0'

include FHIRHelpers version '3.0.0'

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
    where P.gender = 'female'

define TestPatientActive:
  Patient P
    where P.active is true

define TestPatientBirthDate:
  Patient P
    where P.birthDate before Today()

define TestPatientMaritalStatusMembership:
  Patient P
    where FHIRHelpers.ToConcept(P.maritalStatus) in "Marital Status"

define TestPatientMartialStatusComparison:
  Patient P
    where FHIRHelpers.ToConcept(P.maritalStatus) ~ "Marital Status - Married"

define TestPatientDeceasedAsBoolean:
  Patient P
    where P.deceased is false

define TestPatientDeceasedAsDateTime:
  Patient P
    where P.deceased before Today()

define TestSlices:
  [Observation: "Blood pressure"] BP
    let
      SystolicBP: singleton from (BP.component C where FHIRHelpers.ToConcept(C.code) ~ "Systolic blood pressure"),
      DiastolicBP: singleton from (BP.component C where FHIRHelpers.ToConcept(C.code) ~ "Diastolic blood pressure")
    where SystolicBP.value < 140 'mm[Hg]'
      and DiastolicBP.value < 90 'mm[Hg]'

define TestSimpleExtensions:
  Patient P
    let birthsex: singleton from (P.extension E where E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex')
    where birthsex.value = 'F'

define TestComplexExtensions:
  Patient P
    let
      race: singleton from (P.extension E where E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-race'),
      ombCategory: race.extension E where E.url = 'ombCategory',
      detailed: race.extension E where E.url = 'detailed'
    where exists (ombCategory O where FHIRHelpers.ToCode(O.value) ~ "American Indian or Alaska Native")
      and exists (detailed O where FHIRHelpers.ToCode(O.value) ~ "Shoshone")

define TestCql:
  Message(TestSimpleExtensions, TestSimpleExtensions is not null, 'Error', 'TestSimpleExtensions is not null')