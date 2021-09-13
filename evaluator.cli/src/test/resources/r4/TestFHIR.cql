library TestFHIR

/*
Every expression in this library should return non-null or a list with at least one element,
with the exception of TestPatientDeceasedDateTime, which should return null
 */

using FHIR version '4.0.1'

codesystem "LOINC": 'http://loinc.org'
codesystem "Marital Status Codes": 'http://terminology.hl7.org/CodeSystem/v3-MaritalStatus'
codesystem "CDC Race and Ethnicity Codes": 'urn:oid:2.16.840.1.113883.6.238'

valueset "Marital Status": 'http://hl7.org/fhir/ValueSet/marital-status'

code "Marital Status - Married": 'M' from "Marital Status Codes"
code "American Indian or Alaska Native": '1002-5' from "CDC Race and Ethnicity Codes"
code "Alaska Native": '1735-0' from "CDC Race and Ethnicity Codes"
code "Blood pressure": '85354-9' from "LOINC" display 'blood pressure'
code "Diastolic blood pressure": '8462-4' from "LOINC" display 'Diastolic blood pressure'
code "Systolic blood pressure": '8480-6' from "LOINC" display 'Systolic blood pressure'

context Patient

define TestAdverseEvent: ["AdverseEvent"]
define TestAllergyIntolerance: ["AllergyIntolerance"]
define TestBodyStructure: ["BodyStructure"]
define TestCarePlan: ["CarePlan"]
define TestCareTeam: ["CareTeam"]
define TestClaim: ["Claim"]
define TestCommunication: ["Communication"]
define TestCommunicationRequest: ["CommunicationRequest"]
define TestCondition: ["Condition"]
define TestCoverage: ["Coverage"]
define TestDevice: ["Device"]
define TestDeviceRequest: ["DeviceRequest"]
define TestDeviceUseStatement: ["DeviceUseStatement"]
define TestDiagnosticReport: ["DiagnosticReport"]
define TestDocumentReference: ["DocumentReference"]
define TestEncounter: ["Encounter"]
define TestFamilyMemberHistory: ["FamilyMemberHistory"]
define TestFlag: ["Flag"]
define TestGoal: ["Goal"]
define TestImagingStudy: ["ImagingStudy"]
define TestImmunization: ["Immunization"]
define TestImmunizationEvaluation: ["ImmunizationEvaluation"]
define TestImmunizationRecommendation: ["ImmunizationRecommendation"]
define TestLocation: ["Location"]
define TestMedication: ["Medication"]
define TestMedicationAdministration: ["MedicationAdministration"]
define TestMedicationDispense: ["MedicationDispense"]
define TestMedicationRequest: ["MedicationRequest"]
define TestMedicationStatement: ["MedicationStatement"]
define TestObservation: ["Observation"]
define TestOrganization: ["Organization"]
define TestPatient: ["Patient"]
define TestPractitioner: ["Practitioner"]
define TestPractitionerRole: ["PractitionerRole"]
define TestProcedure: ["Procedure"]
define TestRelatedPerson: ["RelatedPerson"]
define TestServiceRequest: ["ServiceRequest"]
define TestSpecimen: ["Specimen"]
define TestSubstsance: ["Substance"]
define TestTask: ["Task"]

define TestPatientGender:
  Patient P
    where P.gender.value = 'male'

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
    where (birthsex.value as FHIR.code).value = 'M'

define TestComplexExtensions:
  Patient P
    let
      race: singleton from (P.extension E where E.url.value = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-race'),
      ombCategory: race.extension E where E.url.value = 'ombCategory',
      detailed: race.extension E where E.url.value = 'detailed'
    where exists (ombCategory O where ToCode(O.value as FHIR.Coding) ~ "American Indian or Alaska Native")
      and exists (detailed O where ToCode(O.value as FHIR.Coding) ~ "Alaska Native")

define function ToInterval(period FHIR.Period):
    if period is null then
        null
    else
        if period."start" is null then
            Interval(period."start".value, period."end".value]
        else
            Interval[period."start".value, period."end".value]

define function ToCalendarUnit(unit System.String):
    case unit
        when 'ms' then 'millisecond'
        when 's' then 'second'
        when 'min' then 'minute'
        when 'h' then 'hour'
        when 'd' then 'day'
        when 'wk' then 'week'
        when 'mo' then 'month'
        when 'a' then 'year'
        else unit
    end

define function ToQuantity(quantity FHIR.Quantity):
    case
        when quantity is null then null
        when quantity.value is null then null
        when quantity.comparator is not null then
            Message(null, true, 'FHIRHelpers.ToQuantity.ComparatorQuantityNotSupported', 'Error', 'FHIR Quantity value has a comparator and cannot be converted to a System.Quantity value.')
        when quantity.system is null or quantity.system.value = 'http://unitsofmeasure.org'
              or quantity.system.value = 'http://hl7.org/fhirpath/CodeSystem/calendar-units' then
            System.Quantity { value: quantity.value.value, unit: ToCalendarUnit(Coalesce(quantity.code.value, quantity.unit.value, '1')) }
        else
            Message(null, true, 'FHIRHelpers.ToQuantity.InvalidFHIRQuantity', 'Error', 'Invalid FHIR Quantity code: ' & quantity.unit.value & ' (' & quantity.system.value & '|' & quantity.code.value & ')')
    end

define function ToQuantityIgnoringComparator(quantity FHIR.Quantity):
    case
        when quantity is null then null
        when quantity.value is null then null
        when quantity.system is null or quantity.system.value = 'http://unitsofmeasure.org'
              or quantity.system.value = 'http://hl7.org/fhirpath/CodeSystem/calendar-units' then
            System.Quantity { value: quantity.value.value, unit: ToCalendarUnit(Coalesce(quantity.code.value, quantity.unit.value, '1')) }
        else
            Message(null, true, 'FHIRHelpers.ToQuantity.InvalidFHIRQuantity', 'Error', 'Invalid FHIR Quantity code: ' & quantity.unit.value & ' (' & quantity.system.value & '|' & quantity.code.value & ')')
    end

define function ToInterval(quantity FHIR.Quantity):
    if quantity is null then null else
        case quantity.comparator.value
            when '<' then
                Interval[
                    null,
                    ToQuantityIgnoringComparator(quantity)
                )
            when '<=' then
                Interval[
                    null,
                    ToQuantityIgnoringComparator(quantity)
                ]
            when '>=' then
                Interval[
                    ToQuantityIgnoringComparator(quantity),
                    null
                ]
            when '>' then
                Interval(
                    ToQuantityIgnoringComparator(quantity),
                    null
                ]
            else
                Interval[ToQuantity(quantity), ToQuantity(quantity)]
        end

define function ToRatio(ratio FHIR.Ratio):
    if ratio is null then
        null
    else
        System.Ratio { numerator: ToQuantity(ratio.numerator), denominator: ToQuantity(ratio.denominator) }

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



