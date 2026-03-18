library PalliativeCare version '1.18.000'

using QICore version '6.0.0'

include FHIRHelpers version '4.4.000' called FHIRHelpers
include QICoreCommon version '4.0.000' called QICoreCommon
include Status version '1.15.000' called Status

codesystem "LOINC": 'http://loinc.org'

valueset "Palliative Care Encounter": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1090'
valueset "Palliative Care Intervention": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1135'
valueset "Palliative Care Diagnosis": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.1167'

code "Functional Assessment of Chronic Illness Therapy - Palliative Care Questionnaire (FACIT-Pal)": '71007-9' from "LOINC" display 'Functional Assessment of Chronic Illness Therapy - Palliative Care Questionnaire (FACIT-Pal)'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2026-01-01T00:00:00.000Z, @2027-01-01T00:00:00.000Z)

context Patient

define "Has Palliative Care in the Measurement Period":
  exists ((([ObservationScreeningAssessment: "Functional Assessment of Chronic Illness Therapy - Palliative Care Questionnaire (FACIT-Pal)"]).isAssessmentPerformed()) PalliativeAssessment
      where PalliativeAssessment.effective.toInterval() overlaps day of "Measurement Period"
  )
    or exists ((([ConditionProblemsHealthConcerns: "Palliative Care Diagnosis"]
    union [ConditionEncounterDiagnosis: "Palliative Care Diagnosis"]).verified()) PalliativeDiagnosis
        where PalliativeDiagnosis.prevalenceInterval() overlaps day of "Measurement Period"
    )
    or exists ((([Encounter: "Palliative Care Encounter"]).isEncounterPerformed()) PalliativeEncounter
        where PalliativeEncounter.period overlaps day of "Measurement Period"
    )
    or exists ((([Procedure: "Palliative Care Intervention"]).isInterventionPerformed()) PalliativeIntervention
        where PalliativeIntervention.performed.toInterval() overlaps day of "Measurement Period"
    )