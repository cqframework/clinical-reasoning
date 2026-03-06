library CMSFHIR529HybridHospitalWideReadmission version '0.5.001'

using QICore version '6.0.0'

include CQMCommon version '4.1.000' called CQMCommon
include FHIRHelpers version '4.4.000' called FHIRHelpers
include QICoreCommon version '4.0.000' called QICoreCommon
include SupplementalDataElements version '5.1.000' called SDE

codesystem "LOINC": 'http://loinc.org'

valueset "Bicarbonate lab test": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1045.139'
valueset "Creatinine lab test": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.2363'
valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'
valueset "Glucose lab test": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1045.134'
valueset "Hematocrit lab test": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1045.114'
valueset "Non Invasive Oxygen Therapy by Nasal Cannula or Mask": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1248.209'
valueset "Non Invasive Oxygen Therapy Device Codes": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1170.57'
valueset "Medicare Advantage payer": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1104.12'
valueset "Medicare FFS payer": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1104.10'
valueset "Oxygen Saturation by Pulse Oximetry": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1045.151'
valueset "Potassium lab test": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1045.117'
valueset "Sodium lab test": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1045.119'
valueset "White blood cells count lab test": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1045.129'

code "Systolic blood pressure": '8480-6' from "LOINC" display 'Systolic BP'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2026-01-01T00:00:00.000Z, @2027-01-01T00:00:00.000Z)

context Patient

define "Initial Population":
  "Inpatient Encounters"

define "Inpatient Encounters":
  [Encounter: "Encounter Inpatient"] InpatientEncounter
    with ( [Coverage: "Medicare FFS payer"]
      union [Coverage: "Medicare Advantage payer"] ) MedicarePayer
      such that ( InpatientEncounter.hospitalizationWithObservationAndOutpatientSurgeryService ( ).lengthInDays ( ) ) < 365
        and InpatientEncounter.status = 'finished'
        and AgeInYearsAt(date from start of InpatientEncounter.period) >= 65
        and InpatientEncounter.period ends during day of "Measurement Period"

define "SDE Encounter With First Body Temperature":
  "Inpatient Encounters" EncounterInpatient
    let FirstTemperature: First([USCoreBodyTemperatureProfile] Temperature
        where Temperature.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and Temperature.status in { 'final', 'amended', 'corrected' }
          and Temperature.value is not null
        sort by effective.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstTemperatureResult: FirstTemperature.value as Quantity,
      Timing: FirstTemperature.effective.earliest ( )
    }

define "SDE Encounter With First Heart Rate":
  "Inpatient Encounters" EncounterInpatient
    let FirstHeartRate: First([USCoreHeartRateProfile] HeartRate
        where HeartRate.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and HeartRate.status in { 'final', 'amended', 'corrected' }
          and HeartRate.value is not null
        sort by effective.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstHeartRateResult: FirstHeartRate.value as Quantity,
      Timing: FirstHeartRate.effective.earliest ( )
    }

define "SDE Encounter With First Oxygen Saturation":
  "Inpatient Encounters" EncounterInpatient
    let FirstOxygenSat: First(["USCorePulseOximetryProfile": "Oxygen Saturation by Pulse Oximetry"] O2Saturation
        where O2Saturation.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and O2Saturation.status in { 'final', 'amended', 'corrected' }
          and O2Saturation.value is not null
        sort by effective.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstOxygenSatResult: FirstOxygenSat.value as Quantity,
      Timing: FirstOxygenSat.effective.earliest ( )
    }

define "SDE Encounter With First Respiratory Rate":
  "Inpatient Encounters" EncounterInpatient
    let FirstRespRate: First([USCoreRespiratoryRateProfile] Respirations
        where Respirations.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and Respirations.status in { 'final', 'amended', 'corrected' }
          and Respirations.value is not null
        sort by effective.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstRespRateResult: FirstRespRate.value as Quantity,
      Timing: FirstRespRate.effective.earliest ( )
    }

define "SDE Encounter With First Systolic Blood Pressure":
  "Inpatient Encounters" EncounterInpatient
    let FirstSystolicBP: First(["USCoreBloodPressureProfile"] BP
        where BP.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and BP.status in { 'final', 'amended', 'corrected' }
          and BP.component.value is not null
        sort by effective.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstSBPResult: FirstSystolicBP.component C
        where C.code ~ "Systolic blood pressure"
        return C.value as Quantity,
      Timing: FirstSystolicBP.effective.earliest ( )
    }

define "SDE Encounter With First Bicarbonate Lab Test":
  "Inpatient Encounters" EncounterInpatient
    let FirstBicarbonateLab: First(["LaboratoryResultObservation": "Bicarbonate lab test"] bicarbonatelab
        where bicarbonatelab.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and bicarbonatelab.status in { 'final', 'amended', 'corrected' }
          and bicarbonatelab.value is not null
        sort by issued.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstResult: FirstBicarbonateLab.value as Quantity,
      Timing: FirstBicarbonateLab.issued
    }

define "SDE Encounter With First Creatinine Lab Test":
  "Inpatient Encounters" EncounterInpatient
    let FirstCreatinineLab: First(["LaboratoryResultObservation": "Creatinine lab test"] CreatinineLab
        where CreatinineLab.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and CreatinineLab.status in { 'final', 'amended', 'corrected' }
          and CreatinineLab.value is not null
        sort by issued.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstResult: FirstCreatinineLab.value as Quantity,
      Timing: FirstCreatinineLab.issued
    }

define "SDE Encounter With First Glucose Lab Test":
  "Inpatient Encounters" EncounterInpatient
    let FirstGlucoseLab: First(["LaboratoryResultObservation": "Glucose lab test"] GlucoseLab
        where GlucoseLab.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and GlucoseLab.status in { 'final', 'amended', 'corrected' }
          and GlucoseLab.value is not null
        sort by issued.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstResult: FirstGlucoseLab.value as Quantity,
      Timing: FirstGlucoseLab.issued
    }

define "SDE Encounter With First Hematocrit Lab Test":
  "Inpatient Encounters" EncounterInpatient
    let FirstHematocritLab: First(["LaboratoryResultObservation": "Hematocrit lab test"] HematocritLab
        where HematocritLab.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and HematocritLab.status in { 'final', 'amended', 'corrected' }
          and HematocritLab.value is not null
        sort by issued.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstResult: FirstHematocritLab.value as Quantity,
      Timing: FirstHematocritLab.issued
    }

define "SDE Encounter With First Potassium Lab Test":
  "Inpatient Encounters" EncounterInpatient
    let FirstPotassiumLab: First(["LaboratoryResultObservation": "Potassium lab test"] PotassiumLab
        where PotassiumLab.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and PotassiumLab.status in { 'final', 'amended', 'corrected' }
          and PotassiumLab.value is not null
        sort by issued.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstResult: FirstPotassiumLab.value as Quantity,
      Timing: FirstPotassiumLab.issued
    }

define "SDE Encounter With First Sodium Lab Test":
  "Inpatient Encounters" EncounterInpatient
    let FirstSodiumLab: First(["LaboratoryResultObservation": "Sodium lab test"] SodiumLab
        where SodiumLab.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and SodiumLab.status in { 'final', 'amended', 'corrected' }
          and SodiumLab.value is not null
        sort by issued.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstResult: FirstSodiumLab.value as Quantity,
      Timing: FirstSodiumLab.issued
    }

define "SDE Encounter With First White Blood Cells Lab Test":
  "Inpatient Encounters" EncounterInpatient
    let FirstWhiteBloodCellLab: First(["LaboratoryResultObservation": "White blood cells count lab test"] WhiteBloodCellLab
        where WhiteBloodCellLab.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and WhiteBloodCellLab.status in { 'final', 'amended', 'corrected' }
          and WhiteBloodCellLab.value is not null
        sort by issued.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstResult: FirstWhiteBloodCellLab.value as Quantity,
      Timing: FirstWhiteBloodCellLab.issued
    }

define "SDE Encounter With First Weight Recorded":
  "Inpatient Encounters" EncounterInpatient
    let FirstWeight: First(["USCoreBodyWeightProfile"] WeightExam
        where WeightExam.effective.earliest() during EncounterInpatient.hospitalizationWithObservationAndOutpatientSurgeryService()
          and WeightExam.status in { 'final', 'amended', 'corrected' }
          and WeightExam.value is not null
        sort by effective.earliest()
    )
    return {
      EncounterId: EncounterInpatient.id,
      FirstResult: FirstWeight.value as Quantity,
      Timing: FirstWeight.effective.earliest ( )
    }

define "SDE Encounter With Oxygen 60 Minutes Or Less Prior To ED Admission Or During ED":
  "Initial Population" EncounterInpatient
    where exists ( ( [ServiceRequest: "Non Invasive Oxygen Therapy by Nasal Cannula or Mask"]
        union [ServiceRequest: "Non Invasive Oxygen Therapy Device Codes"] ) OxygenTherapyOrder
        where ( OxygenTherapyOrder.authoredOn during EncounterInpatient.edVisit ( ).period
            or OxygenTherapyOrder.authoredOn 60 minutes or less before or on start of EncounterInpatient.edVisit ( ).period
        )
          and OxygenTherapyOrder.status in { 'active', 'completed' }
          and OxygenTherapyOrder.intent = 'order'
        return {
          EncounterId: EncounterInpatient.id,
          OrderStatus: OxygenTherapyOrder.status,
          OrderTiming: OxygenTherapyOrder.authoredOn
        }
    )
      or exists ( [Procedure: "Non Invasive Oxygen Therapy by Nasal Cannula or Mask"] OxygenAdminInterv
          where ( OxygenAdminInterv.performed.toInterval ( ) starts during EncounterInpatient.edVisit ( ).period
              or OxygenAdminInterv.performed.toInterval ( ) 60 minutes or less before or on start of EncounterInpatient.edVisit ( ).period
          )
            and OxygenAdminInterv.status = 'completed'
          return {
            EncounterId: EncounterInpatient.id,
            EDEncounterTiming: EncounterInpatient.edVisit ( ).period,
            PerformedStatus: OxygenAdminInterv.status,
            PerformedTiming: OxygenAdminInterv.performed.toInterval ( )
          }
      )

define "SDE Ethnicity":
  SDE."SDE Ethnicity"

define "SDE Payer":
  SDE."SDE Payer"

define "SDE Race":
  SDE."SDE Race"

define "SDE Sex":
  SDE."SDE Sex"