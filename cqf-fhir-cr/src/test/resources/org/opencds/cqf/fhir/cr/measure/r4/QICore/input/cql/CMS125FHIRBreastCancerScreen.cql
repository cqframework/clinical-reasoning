library CMS125FHIRBreastCancerScreen version '1.0.000'

using QICore version '6.0.0'

include FHIRHelpers version '4.4.000' called FHIRHelpers
include SupplementalDataElements version '5.1.000' called SDE
include QICoreCommon version '4.0.000' called QICoreCommon
include AdultOutpatientEncounters version '4.19.000' called AdultOutpatientEncounters
include Hospice version '6.18.000' called Hospice
include Status version '1.15.000' called Status
include PalliativeCare version '1.18.000' called PalliativeCare
include AdvancedIllnessandFrailty version '1.27.000' called AIFrailLTCF

codesystem "SNOMEDCT": 'http://snomed.info/sct'

valueset "Bilateral Mastectomy": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1005'
valueset "History of bilateral mastectomy": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1068'
valueset "Mammography": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.108.12.1018'
valueset "Status Post Left Mastectomy": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1069'
valueset "Status Post Right Mastectomy": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1070'
valueset "Unilateral Mastectomy Left": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1133'
valueset "Unilateral Mastectomy Right": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1134'
valueset "Unilateral Mastectomy, Unspecified Laterality": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1071'

code "Left (qualifier value)": '7771000' from "SNOMEDCT" display 'Left (qualifier value)'
code "Right (qualifier value)": '24028007' from "SNOMEDCT" display 'Right (qualifier value)'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2026-01-01T00:00:00.000Z, @2027-01-01T00:00:00.000Z)

context Patient

define "Initial Population":
  AgeInYearsAt(date from 
    end of "Measurement Period"
  ) in Interval[42, 74]
    and Patient.sex = '248152002'
    and exists AdultOutpatientEncounters."Qualifying Encounters"

define "Denominator":
  "Initial Population"

define "has frailty":
    AIFrailLTCF."Has Criteria Indicating Frailty"

define "has dementia meds":
    AIFrailLTCF."Has Dementia Medications in Year Before or During Measurement Period"

define "active meds":
    AIFrailLTCF."active dementia meds"

define "active meds period":
     AIFrailLTCF."med request period"

define "Denominator Exclusions":
  Hospice."Has Hospice Services"
    or ( ( exists ( "Right Mastectomy Diagnosis" )
          or exists ( "Right Mastectomy Procedure" )
      )
        and ( exists ( "Left Mastectomy Diagnosis" )
            or exists ( "Left Mastectomy Procedure" )
        )
    )
    or exists "Bilateral Mastectomy Diagnosis"
    or exists "Bilateral Mastectomy Procedure"
    or AIFrailLTCF."Is Age 66 or Older with Advanced Illness and Frailty"
    or AIFrailLTCF."Is Age 66 or Older Living Long Term in a Nursing Home"
    or PalliativeCare."Has Palliative Care in the Measurement Period"

define "Right Mastectomy Diagnosis":
  ( ( ( [ConditionEncounterDiagnosis: "Status Post Right Mastectomy"]
      union [ConditionProblemsHealthConcerns: "Status Post Right Mastectomy"] ) RightMastectomyProcedure
      union ( ( [ConditionEncounterDiagnosis: "Unilateral Mastectomy, Unspecified Laterality"]
          union [ConditionProblemsHealthConcerns: "Unilateral Mastectomy, Unspecified Laterality"] ) UnilateralMastectomyDiagnosis
          where exists UnilateralMastectomyDiagnosis.bodySite S
            where S ~ "Right (qualifier value)"
      )
  ).verified ( ) ) RightMastectomy
    where RightMastectomy.prevalenceInterval ( ) starts on or before end of "Measurement Period"

define "Right Mastectomy Procedure":
  ( ( [Procedure: "Unilateral Mastectomy Right"] ).isProcedurePerformed ( ) ) UnilateralMastectomyRightPerformed
    where UnilateralMastectomyRightPerformed.performed.toInterval ( ) ends on or before end of "Measurement Period"

define "Left Mastectomy Diagnosis":
  ( ( ( [ConditionEncounterDiagnosis: "Status Post Left Mastectomy"]
        union [ConditionProblemsHealthConcerns: "Status Post Left Mastectomy"]
    )
      union ( ( [ConditionEncounterDiagnosis: "Unilateral Mastectomy, Unspecified Laterality"]
          union [ConditionProblemsHealthConcerns: "Unilateral Mastectomy, Unspecified Laterality"] ) UnilateralMastectomyDiagnosis
          where exists UnilateralMastectomyDiagnosis.bodySite S
            where S ~ "Left (qualifier value)"
      )
  ).verified ( ) ) LeftMastectomy
    where LeftMastectomy.prevalenceInterval ( ) starts on or before end of "Measurement Period"

define "Left Mastectomy Procedure":
  ( ( [Procedure: "Unilateral Mastectomy Left"] ).isProcedurePerformed ( ) ) UnilateralMastectomyLeftPerformed
    where UnilateralMastectomyLeftPerformed.performed.toInterval ( ) ends on or before end of "Measurement Period"

define "Bilateral Mastectomy Diagnosis":
  ( ( [ConditionEncounterDiagnosis: "History of bilateral mastectomy"]
      union [ConditionProblemsHealthConcerns: "History of bilateral mastectomy"]
  ).verified ( ) ) BilateralMastectomyHistory
    where BilateralMastectomyHistory.prevalenceInterval ( ) starts on or before end of "Measurement Period"

define "Bilateral Mastectomy Procedure":
  ( ( [Procedure: "Bilateral Mastectomy"] ).isProcedurePerformed ( ) ) BilateralMastectomyPerformed
    where BilateralMastectomyPerformed.performed.toInterval ( ) ends on or before end of "Measurement Period"

define "Numerator":
  exists ( ( [ObservationClinicalResult: "Mammography"] ).isDiagnosticStudyPerformed ( ) ) Mammogram
    where Mammogram.effective.toInterval ( ) ends during day of Interval["October 1 Two Years Prior to the Measurement Period", end of "Measurement Period"]

define "October 1 Two Years Prior to the Measurement Period":
  DateTime((year from start of "Measurement Period" - 2), 10, 1, 0, 0, 0, 0, 0)

define "SDE Ethnicity":
  SDE."SDE Ethnicity"

define "SDE Payer":
  SDE."SDE Payer"

define "SDE Race":
  SDE."SDE Race"

define "SDE Sex":
  SDE."SDE Sex"

define "Stratification 1":
  AgeInYearsAt(date from 
    end of "Measurement Period"
  ) in Interval[42, 51]

define "Stratification 2":
  AgeInYearsAt(date from 
    end of "Measurement Period"
  ) in Interval[52, 74]