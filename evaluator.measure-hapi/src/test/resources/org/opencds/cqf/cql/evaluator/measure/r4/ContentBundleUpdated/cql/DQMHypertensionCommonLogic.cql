library DQMHypertensionCommonLogic version '1.0.1'

using FHIR version '4.0.1'

include DQMHypertensionFHIRHelpers version '1.0.1' called FHIRHelpers
include DQMHypertensionFHIRCommon version '1.0.1' called FC

codesystem "ConditionClinicalStatusCodes": 'http://terminology.hl7.org/CodeSystem/condition-clinical'
codesystem "ConditionVerificationStatusCodes": 'http://terminology.hl7.org/CodeSystem/condition-ver-status'
codesystem "ConditionCategoryCodes": 'http://terminology.hl7.org/CodeSystem/condition-category'
codesystem "ObservationStatusCodes": 'http://hl7.org/fhir/observation-status'
codesystem "ObservationCategoryCodes": 'http://terminology.hl7.org/CodeSystem/observation-category'
codesystem "ProcedureStatusCodes": 'http://hl7.org/fhir/event-status'
codesystem "MedicationStatusCodes": 'http://hl7.org/fhir/CodeSystem/medication-status'
codesystem "MedicationRequestStatusCodes": 'http://hl7.org/fhir/CodeSystem/medicationrequest-status'
codesystem "MedicationRequestStatusReasonCodes": 'http://terminology.hl7.org/CodeSystem/medicationrequest-status-reason'
codesystem "MedicationRequestCategoryCodes": 'http://terminology.hl7.org/CodeSystem/medicationrequest-category'
codesystem "AllergyTypeCodes": 'http://hl7.org/fhir/allergy-intolerance-type'
codesystem "AllergyCategoryCodes": 'http://hl7.org/fhir/allergy-intolerance-category'
codesystem "AllergyClinicalStatusCodes": 'http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical'
codesystem "AllergyVerificationStatusCodes": 'http://terminology.hl7.org/CodeSystem/allergyintolerance-verification'

// Condition Clinical Status Codes - Consider value sets for these
code "activeClinicalStatus": 'active' from "ConditionClinicalStatusCodes"
code "recurrence": 'recurrence' from "ConditionClinicalStatusCodes"
code "relapse": 'relapse' from "ConditionClinicalStatusCodes"
code "inactive": 'inactive' from "ConditionClinicalStatusCodes"
code "remission": 'remission' from "ConditionClinicalStatusCodes"
code "resolved": 'resolved' from "ConditionClinicalStatusCodes"

// Condition Verification Status Codes - Consider value sets for these
code "unconfirmed": 'unconfirmed' from ConditionVerificationStatusCodes
code "provisional": 'provisional' from ConditionVerificationStatusCodes
code "differential": 'differential' from ConditionVerificationStatusCodes
code "confirmed": 'confirmed' from ConditionVerificationStatusCodes
code "refuted": 'refuted' from ConditionVerificationStatusCodes
code "condition-entered-in-error": 'entered-in-error' from ConditionVerificationStatusCodes

// Condition Category Codes - Consider value sets for these
code "problem-list-item": 'problem-list-item' from ConditionCategoryCodes
code "encounter-diagnosis": 'encounter-diagnosis' from ConditionCategoryCodes

// Observation Status Codes - Consider value sets for these
code "registered": 'registered' from ObservationStatusCodes
code "preliminary": 'preliminary' from ObservationStatusCodes
code "final": 'final' from ObservationStatusCodes
code "amended": 'amended' from ObservationStatusCodes
code "corrected": 'corrected' from ObservationStatusCodes
code "cancelled": 'cancelled' from ObservationStatusCodes
code "observation-entered-in-error": 'entered-in-error' from ObservationStatusCodes
code "unknown": 'unknown' from ObservationStatusCodes

// Observation Category Codes - Consider value sets for these
code "social-history": 'social-history' from ObservationCategoryCodes
code "vital-signs": 'vital-signs' from ObservationCategoryCodes
code "imaging": 'imaging' from ObservationCategoryCodes
code "laboratory": 'laboratory' from ObservationCategoryCodes
code "procedure": 'procedure' from ObservationCategoryCodes
code "survey": 'survey' from ObservationCategoryCodes
code "exam": 'exam' from ObservationCategoryCodes
code "therapy": 'therapy' from ObservationCategoryCodes
code "activity": 'activity' from ObservationCategoryCodes

// Procedure Status Codes - Consider value sets for these
code "preparation": 'Preparation' from ProcedureStatusCodes
code "in-progress": 'In Progress' from ProcedureStatusCodes
code "completed": 'Completed' from ProcedureStatusCodes

// Medication Status Codes - Consider value sets for these
code "activeMedicationStatus": 'active' from "MedicationStatusCodes"

// MedicationRequest Status Codes - Consider value sets for these
code "activeMedicationRequestStatus": 'active' from "MedicationRequestStatusCodes"
code "completedMedicationRequestStatus": 'completed' from "MedicationRequestStatusCodes"
code "stoppedMedicationRequestStatus": 'stopped' from "MedicationRequestStatusCodes"

// MedicationRequest StatusReason Codes - Consider value sets for these
code "suspectedAllergy": 'salg' from "MedicationRequestStatusReasonCodes"
code "suspectedIntolerance": 'sintol' from "MedicationRequestStatusReasonCodes"

// Medication Category Codes - Consider value sets for these
code "inpatient": 'inpatient' from MedicationRequestCategoryCodes
code "outpatient": 'outpatient' from MedicationRequestCategoryCodes
code "community": 'community' from MedicationRequestCategoryCodes
code "discharge": 'discharge' from MedicationRequestCategoryCodes

//Allergy Intolerance Codes
code "allergyTypeAllergy": 'allergy' from "AllergyTypeCodes"
code "intoleranceTypeAllergy": 'intolerance' from "AllergyTypeCodes"
code "allergyCategoryMedication": 'medication' from "AllergyCategoryCodes"
code "activeAllergyClinicalStatus": 'active' from "AllergyClinicalStatusCodes"
code "confirmedAllergyStatus": 'confirmed' from "AllergyVerificationStatusCodes"

// NOTE: 2 Year Lookback Period from Today()
parameter "Measurement Period" Interval<DateTime>

context Patient

define function ToCode(coding FHIR.Coding):
  // Custom implementation that removes the version, due to bug in CPG engine implementation
  //FHIRHelpers.ToCode(coding)
  if coding is null then
    null
  else
    System.Code {
      code: coding.code.value,
      system: coding.system.value,
      //version: coding.version.value,
      display: coding.display.value
    }

define function QualifiedObservationStatus(value FHIR.Observation):
  value.status ~ "final".code
    or value.status ~ "amended".code
    or value.status ~ "corrected".code
    or value.status ~ "preliminary".code

define function QualifiedObservationCategory(value FHIR.Observation):
  value Observation
    return exists (
      Observation.category.coding C
        where FHIRHelpers.ToCode(C) ~ "vital-signs"
        or FHIRHelpers.ToCode(C) ~ "laboratory"
        or FHIRHelpers.ToCode(C) ~ "procedure"
        or FHIRHelpers.ToCode(C) ~ "social-history"
        or FHIRHelpers.ToCode(C) ~ "survey"
        or FHIRHelpers.ToCode(C) ~ "exam"
    )

define function QualifiedObservations(value List<FHIR.Observation>):
  value Observation
    where QualifiedObservationStatus(Observation)
      and QualifiedObservationCategory(Observation)

define function QualifiedConditionCategory(value FHIR.Condition):
  value Condition
    return exists (
      Condition.category.coding C
        where FHIRHelpers.ToCode(C) ~ "problem-list-item"
        or FHIRHelpers.ToCode(C) ~ "encounter-diagnosis"
    )
  
define function QualifiedConditions(value List<FHIR.Condition>):
  value Condition
    where Condition.clinicalStatus ~ "activeClinicalStatus"
      and Condition.verificationStatus ~ "confirmed"
      and QualifiedConditionCategory(Condition)

define function QualifiedProcedureStatus(value FHIR.Procedure):
  value.status ~ "preparation".code
    or value.status ~ "in-progress".code
    or value.status ~ "completed".code

define function QualifiedProcedures(value List<FHIR.Procedure>):
  value Procedure
    where QualifiedProcedureStatus(Procedure)

define function QualifiedMedications(value List<FHIR.Medication>):
  value Medication

define function QualifiedMedicationRequestCategory(value FHIR.MedicationRequest):
  value MedicationRequest
    return exists (
      MedicationRequest.category.coding C
        where FHIRHelpers.ToCode(C) ~ "outpatient"
        or FHIRHelpers.ToCode(C) ~ "community"
        or FHIRHelpers.ToCode(C) ~ "discharge"
    )

define function QualifiedMedicationRequests(value List<FHIR.MedicationRequest>):
  value MedicationRequest
    where QualifiedMedicationRequestCategory(MedicationRequest)

define function ActiveMedicationRequests(value List<FHIR.MedicationRequest>):
  (QualifiedMedicationRequests(value)) MedicationRequest
      where MedicationRequest.status ~ "activeMedicationRequestStatus".code

define function ActiveMedicationRequestWithMedications(value List<FHIR.Medication>):
  (ActiveMedicationRequests([MedicationRequest])) QualifiedMedicationRequest
    where exists (
      (QualifiedMedications(value)) QualifiedMedication
        where GetId(QualifiedMedicationRequest.medication.reference.value) ~ QualifiedMedication.id.value
    )

define function StoppedMedicationRequests(value List<FHIR.MedicationRequest>):
  (QualifiedMedicationRequests(value)) MedicationRequest
      where MedicationRequest.status ~ "stoppedMedicationRequestStatus".code

define function StoppedMedicationRequestWithMedications(value List<FHIR.Medication>):
  (StoppedMedicationRequests([MedicationRequest])) QualifiedMedicationRequest
    where exists (
      (QualifiedMedications(value)) QualifiedMedication
        where GetId(QualifiedMedicationRequest.medication.reference.value) ~ QualifiedMedication.id.value
    )

define function StoppedDueToIntoleranceMedicationRequests(value List<FHIR.MedicationRequest>):
  (StoppedMedicationRequests(value)) StoppedMedicationRequest
    where (StoppedMedicationRequest.statusReason ~ "suspectedAllergy"
      or StoppedMedicationRequest.statusReason ~ "suspectedIntolerance"
      )

define function StoppedDueToIntoleranceMedicationRequestWithMedications(value List<FHIR.Medication>):
  (StoppedDueToIntoleranceMedicationRequests([MedicationRequest])) QualifiedMedicationRequest
    where exists (
      (QualifiedMedications(value)) QualifiedMedication
        where GetId(QualifiedMedicationRequest.medication.reference.value) ~ QualifiedMedication.id.value
    )

define function MedicationRequestWithMedications(value List<FHIR.Medication>):
  (QualifiedMedicationRequests([MedicationRequest])) QualifiedMedicationRequest
    where exists (
      (value) QualifiedMedication
        where GetId(QualifiedMedicationRequest.medication.reference.value) ~ QualifiedMedication.id.value
    )

define function QualifiedAllergyIntoleranceCategory(value FHIR.AllergyIntolerance):
  value AllergyIntolerance
    return exists (
      AllergyIntolerance.category C
        where C ~ "allergyCategoryMedication".code
    )

define function QualifiedAllergies(value List<FHIR.AllergyIntolerance>):
  value Allergy
    where Allergy.type ~ "allergyTypeAllergy".code
      and Allergy.clinicalStatus ~ "activeAllergyClinicalStatus"
      and Allergy.verificationStatus ~ "confirmedAllergyStatus"
      and QualifiedAllergyIntoleranceCategory(Allergy)

define function QualifiedIntolerances(value List<FHIR.AllergyIntolerance>):
  value Allergy
    where Allergy.type ~ "intoleranceTypeAllergy".code
      and Allergy.clinicalStatus ~ "activeAllergyClinicalStatus"
      and Allergy.verificationStatus ~ "confirmedAllergyStatus"
      and QualifiedAllergyIntoleranceCategory(Allergy)

define function MostRecentObservation(value List<FHIR.Observation>):
  First(
    value Observation
    sort by end of effective descending
  )

define function MostRecentEncounter(value List<FHIR.Encounter>):
  First(
    value Encounter
    sort by end of period descending
  )

// TODO: revert this to an overload once bug is fixed in CQL evaluation
define function MostRecentGoal(value List<FHIR.Goal>):
  First(
    value Goal
    sort by start.value descending
  )

define function ValueAsBoolean(value FHIR.Observation):
  value.value as FHIR.boolean

define function Has(value List<System.Any>):
  if exists value then
    true
  else
    null

define function HasSingleton(value System.Any):
  if not IsNull(value) then
    true
  else
    null

define function GetId(uri String):
  Last(Split(uri, '/'))

define function GetRelativeType(uri String):
  First(Split(uri, '/'))

define function ToDate(Value DateTime):
  DateTime(year from Value, month from Value, day from Value, 0, 0, 0, 0, timezoneoffset from Value)

define function WithUnit(ObsList List<Observation>, Unit String):
  ObsList O where (O.value as Quantity).unit.value = Unit
    or (O.value as Quantity).code.value = Unit

define function WithQuantityUnit(value FHIR.Quantity, unit String):
  value.unit.value = unit
  or value.code.value = unit

/*Given an interval, return true if the interval has a starting boundary specified (i.e. the start of the interval is not null and not the minimum DateTime value)*/
define function HasStart(period Interval<DateTime> ):
  not ( start of period is null
      or start of period = minimum DateTime
  )

/*Given an interval, return true if the interval has an ending boundary specified (i.e. the end of the interval is not null and not the maximum DateTime value)*/
define function HasEnd(period Interval<DateTime>):
  not(
    end of period is null
    or end of period = maximum DateTime
  )

/*Given an interval, return the ending point if the interval has an ending boundary specified, otherwise, return the starting point*/
define function Latest(choice Choice<FHIR.dateTime, FHIR.Period, FHIR.Timing, FHIR.instant, FHIR.string, FHIR.Age, FHIR.Range>):
  (FC.ToInterval(choice)) period
    return
      if (HasEnd(period)) then end of period
      else start of period

/*Given an interval, return the starting point if the interval has a starting boundary specified, otherwise, return the ending point*/
define function Earliest(choice Choice<FHIR.dateTime, FHIR.Period, FHIR.Timing, FHIR.instant, FHIR.string, FHIR.Age, FHIR.Range> ):
  (FC.ToInterval(choice)) period
    return
      if (HasStart(period)) then start of period
      else end of period
