library DQMDiabetesCommonLogic version '1.0.1'

using FHIR version '4.0.1'

include DQMDiabetesFHIRHelpers version '1.0.1' called FHIRHelpers
include FHIRCommon version '1.0.1' called FC

codesystem "ConditionClinicalStatusCodes": 'http://terminology.hl7.org/CodeSystem/condition-clinical'
codesystem "ConditionVerificationStatusCodes": 'http://terminology.hl7.org/CodeSystem/condition-ver-status'
codesystem "ObservationStatusCodes": 'http://hl7.org/fhir/observation-status'
codesystem "MedicationStatusCodes": 'http://hl7.org/fhir/CodeSystem/medication-status'
codesystem "MedicationRequestStatusCodes": 'http://hl7.org/fhir/CodeSystem/medicationrequest-status'
codesystem "MedicationRequestStatusReasonCodes": 'http://terminology.hl7.org/CodeSystem/medicationrequest-status-reason'
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

// Observation Status Codes - Consider value sets for these
code "registered": 'registered' from ObservationStatusCodes
code "preliminary": 'preliminary' from ObservationStatusCodes
code "final": 'final' from ObservationStatusCodes
code "amended": 'amended' from ObservationStatusCodes
code "corrected": 'corrected' from ObservationStatusCodes
code "observation-entered-in-error": 'entered-in-error' from ObservationStatusCodes
code "unknown": 'unknown' from ObservationStatusCodes

// Medication Status Codes - Consider value sets for these
code "activeMedicationStatus": 'active' from "MedicationStatusCodes"

// MedicationRequest Status Codes - Consider value sets for these
code "activeMedicationRequestStatus": 'active' from "MedicationRequestStatusCodes"
code "completedMedicationRequestStatus": 'completed' from "MedicationRequestStatusCodes"
code "stoppedMedicationRequestStatus": 'stopped' from "MedicationRequestStatusCodes"

// MedicationRequest StatusReason Codes - Consider value sets for these
code "suspectedAllergy": 'salg' from "MedicationRequestStatusReasonCodes"
code "suspectedIntolerance": 'sintol' from "MedicationRequestStatusReasonCodes"

// Allergy Intolerance Codes
code "allergyTypeAllergy": 'allergy' from "AllergyTypeCodes"
code "intoleranceTypeAllergy": 'intolerance' from "AllergyTypeCodes"
code "allergyCategoryMedication": 'medication' from "AllergyCategoryCodes"
code "activeAllergyClinicalStatus": 'active' from "AllergyClinicalStatusCodes"
code "confirmedAllergyStatus": 'confirmed' from "AllergyVerificationStatusCodes"

// NOTE: 2 Year Lookback Period from Today()
parameter "Measurement Period" Interval<DateTime>

context Patient

define function QualifiedObservations(value List<FHIR.Observation>):
  value Observation
    where (Observation.status ~ 'final'
        or Observation.status ~ 'amended'
        or Observation.status ~ 'corrected'
        or Observation.status ~ 'preliminary'
      )

define function QualifiedConditions(value List<FHIR.Condition>):
  value Condition
      where Condition.clinicalStatus ~ "activeClinicalStatus"
          and Condition.verificationStatus ~ "confirmed"

define function QualifiedMedications(value List<FHIR.Medication>):
  value Medication

define function ActiveMedicationRequests(value List<FHIR.MedicationRequest>):
  value MedicationRequest
      where MedicationRequest.status ~ "activeMedicationRequestStatus".code

define function ActiveMedicationRequestWithMedications(value List<FHIR.Medication>):
  (ActiveMedicationRequests([MedicationRequest])) QualifiedMedicationRequest
    where exists (
      (QualifiedMedications(value)) QualifiedMedication
        where GetId(QualifiedMedicationRequest.medication.reference.value) ~ QualifiedMedication.id.value
    )

define function StoppedMedicationRequests(value List<FHIR.MedicationRequest>):
  value MedicationRequest
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
([MedicationRequest]) QualifiedMedicationRequest
    where exists (
      (value) QualifiedMedication
        where GetId(QualifiedMedicationRequest.medication.reference.value) ~ QualifiedMedication.id.value
    )

define function QualifiedAllergies(value List<FHIR.AllergyIntolerance>):
  value Allergy
    where Allergy.type ~ "allergyTypeAllergy".code
      and Allergy.clinicalStatus ~ "activeAllergyClinicalStatus"
      and Allergy.verificationStatus ~ "confirmedAllergyStatus"
      and exists (
        Allergy.category Category
          where Category ~ "allergyCategoryMedication".code
      )

define function QualifiedIntolerances(value List<FHIR.AllergyIntolerance>):
  value Allergy
    where Allergy.type ~ "intoleranceTypeAllergy".code
      and Allergy.clinicalStatus ~ "activeAllergyClinicalStatus"
      and Allergy.verificationStatus ~ "confirmedAllergyStatus"
      and exists (
        Allergy.category Category
          where Category ~ "allergyCategoryMedication".code
      )

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
