library TestUSCore

using USCore version '3.1.0'

include FHIRHelpers version '4.0.1'

codesystem "Marital Status Codes": 'http://terminology.hl7.org/CodeSystem/v3-MaritalStatus'
codesystem "CDC Race and Ethnicity Codes": 'urn:oid:2.16.840.1.113883.6.238'

valueset "Marital Status": 'http://hl7.org/fhir/ValueSet/marital-status'

code "Marital Status - Married": 'M' from "Marital Status Codes"
code "American Indian or Alaska Native": '1002-5' from "CDC Race and Ethnicity Codes"
code "Shoshone": '1586-7' from "CDC Race and Ethnicity Codes"

context Patient

define TestPatient: ["PatientProfile"]
define TestAllergyIntolerance: ["AllergyIntolerance"]
define TestCarePlan: ["CarePlanProfile"]
define TestCareTeam: ["CareTeam"]
define TestCondition: ["Condition"]
define TestDiagnosticReport: ["DiagnosticReportProfileLaboratoryReporting"]
define TestDiagnosticReportNote: ["DiagnosticReportProfileNoteExchange"]
define TestDocumentReference: ["DocumentReferenceProfile"]
define TestEncounter: ["EncounterProfile"]
define TestGoal: ["GoalProfile"]
define TestImmunization: ["ImmunizationProfile"]
define TestImplantableDevice: ["ImplantableDeviceProfile"]
define TestObservation: ["LaboratoryResultObservationProfile"]
define TestLocation: ["Location"]
define TestMedication: ["MedicationProfile"]
define TestMedicationRequest: ["MedicationRequestProfile"]
define TestOrganization: ["OrganizationProfile"]
define TestVitalsPanel: ["observation-vitalspanel"]
define TestRespRate: ["observation-resprate"]
define TestHeartRate: ["observation-heartrate"]
define TestOxygenSat: ["observation-oxygensat"]
define TestBodyTemp: ["observation-bodytemp"]
define TestBodyHeight: ["observation-bodyheight"]
define TestHeadCircum: ["observation-headcircum"]
define TestBodyWeight: ["observation-bodyweight"]
define TestBMI: ["observation-bmi"]
define TestBP: ["observation-bp"]
define TestSmokingStatus: ["SmokingStatusProfile"]
define TestPulseOximetry: ["PulseOximetryProfile"]
define TestPediatricBMIForAge: ["PediatricBMIforAgeObservationProfile"]
define TestPediatricWeightForHeight: ["PediatricWeightForHeightObservationProfile"]
define TestPractitioner: ["PractitionerProfile"]
define TestPractitionerRole: ["PractitionerRoleProfile"]
define TestProcedure: ["ProcedureProfile"]
define TestProvenance: ["Provenance"]

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
    where P.maritalStatus in "Marital Status"

define TestPatientMartialStatusComparison:
  Patient P
    where P.maritalStatus ~ "Marital Status - Married"

define TestPatientDeceasedAsBoolean:
  Patient P
    where P.deceased is false

define TestPatientDeceasedAsDateTime:
  Patient P
    where P.deceased before Today()

define TestSlices:
  ["observation-bp"] BP
    where BP.SystolicBP.value < 140 'mm[Hg]'
      and BP.DiastolicBP.value < 90 'mm[Hg]'

define TestSimpleExtensions:
  Patient P
    where P.birthsex = 'F'

define TestComplexExtensions:
  Patient P
    where exists (P.race.ombCategory C where C ~ "American Indian or Alaska Native")
      and exists (P.race.detailed C where C ~ "Shoshone")
