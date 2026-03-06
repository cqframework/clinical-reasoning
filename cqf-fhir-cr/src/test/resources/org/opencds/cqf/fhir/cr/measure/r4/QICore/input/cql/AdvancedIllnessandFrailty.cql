library AdvancedIllnessandFrailty version '1.27.000'

using QICore version '6.0.0'

include FHIRHelpers version '4.4.000' called FHIRHelpers
include QICoreCommon version '4.0.000' called QICoreCommon
include CumulativeMedicationDuration version '6.0.000' called CMD
include Status version '1.15.000' called Status 

codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMEDCT": 'http://snomed.info/sct'

valueset "Advanced Illness": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.110.12.1082'
valueset "Dementia Medications": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.196.12.1510'
valueset "Frailty Device": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.118.12.1300'
valueset "Frailty Diagnosis": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.113.12.1074'
valueset "Frailty Encounter": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1088'
valueset "Frailty Symptom": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.113.12.1075'
 
code "Housing status": '71802-3' from "LOINC" display 'Housing status'
code "Lives in nursing home (finding)": '160734000' from "SNOMEDCT" display 'Lives in nursing home (finding)'
code "Medical equipment used": '98181-1' from "LOINC" display 'Medical equipment used'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2026-01-01T00:00:00.000Z, @2027-01-01T00:00:00.000Z)

context Patient

define "Is Age 66 or Older with Advanced Illness and Frailty":
   AgeInYearsAt(date from end of "Measurement Period")>= 66
    and "Has Criteria Indicating Frailty"
    and ( "Has Advanced Illness in Year Before or During Measurement Period"
        or "Has Dementia Medications in Year Before or During Measurement Period"
    )

define "Is Age 66 to 80 with Advanced Illness and Frailty or Is Age 81 or Older with Frailty":
  ( AgeInYearsAt(date from end of "Measurement Period")in Interval[66, 80]
      and "Has Criteria Indicating Frailty"
      and ( "Has Advanced Illness in Year Before or During Measurement Period"
          or "Has Dementia Medications in Year Before or During Measurement Period"
      )
  )
    or ( AgeInYearsAt(date from end of "Measurement Period")>= 81
        and "Has Criteria Indicating Frailty"
    )

define "Has Criteria Indicating Frailty":
  exists ( (([DeviceRequest: "Frailty Device"]).isDeviceOrderPersonalUseDevices()) FrailtyDeviceOrder
      where FrailtyDeviceOrder.authoredOn.toInterval() during day of "Measurement Period"
  )
    or exists ( (([ObservationScreeningAssessment: "Medical equipment used"]).isAssessmentPerformed()) EquipmentUsed
        where EquipmentUsed.value as Concept in "Frailty Device" 
        and EquipmentUsed.effective.toInterval() ends during day of "Measurement Period"
    )
    or exists ( (([ConditionProblemsHealthConcerns: "Frailty Diagnosis"]
    union [ConditionEncounterDiagnosis: "Frailty Diagnosis"]).verified()) FrailtyDiagnosis
        where FrailtyDiagnosis.prevalenceInterval() overlaps day of "Measurement Period"
    )
    or exists ( (([Encounter: "Frailty Encounter"]).isEncounterPerformed()) FrailtyEncounter
        where FrailtyEncounter.period overlaps day of "Measurement Period"
    )
    or exists ( (([SimpleObservation: "Frailty Symptom"]).isSymptom()) FrailtySymptom
        where FrailtySymptom.effective.toInterval() overlaps day of "Measurement Period"
    )

define "Has Advanced Illness in Year Before or During Measurement Period":
exists ((([ConditionProblemsHealthConcerns: "Advanced Illness"]
union [ConditionEncounterDiagnosis: "Advanced Illness"]).verified()) AdvancedIllnessDiagnosis
where AdvancedIllnessDiagnosis.prevalenceInterval() starts during day of Interval[start of "Measurement Period" - 1 year, end of "Measurement Period"])

define "Has Dementia Medications in Year Before or During Measurement Period":
  exists (( ([MedicationRequest: "Dementia Medications"]).isMedicationActive()) DementiaMedication
      where DementiaMedication.medicationRequestPeriod() overlaps day of Interval[start of "Measurement Period" - 1 year, 
        end of "Measurement Period"]
  )

 define "active dementia meds":
         ([MedicationRequest: "Dementia Medications"]).isMedicationActive()

 define "med request period":
          First([MedicationRequest: "Dementia Medications"]).medicationRequestPeriod()

define "Is Age 66 or Older Living Long Term in a Nursing Home":
  AgeInYearsAt(date from 
     end of "Measurement Period"
  )>= 66
    and ( ( Last( (([ObservationScreeningAssessment: "Housing status"]).isAssessmentPerformed()) HousingStatus    
          where HousingStatus.effective.toInterval() ends on or before 
          day of end of "Measurement Period"
          sort by 
          end of effective.toInterval() asc
      )) LastHousingStatus
        where LastHousingStatus.value ~ "Lives in nursing home (finding)"
    ) is not null