library HTN1 version '1.0.1'

/* Hypertension Management Measure 1 - Uncontrolled Hypertension */

using FHIR version '4.0.1'

include DQMHypertensionFHIRHelpers version '1.0.1' called FHIRHelpers
include DQMHypertensionFHIRCommon version '1.0.1' called FC
include DQMHypertensionCommonLogic version '1.0.1' called Common
include HTNSupplementalDataElements version '1.0.1' called SDE
include HTNAmbulatoryVisits version '1.0.1' called AV
include HTNConcepts version '1.0.1' called Concepts

codesystem "LOINC": 'http://loinc.org'

code "Systolic Blood Pressure code": '8480-6' from "LOINC" display 'Systolic blood pressure'
code "Diastolic Blood Pressure code": '8462-4' from "LOINC" display 'Diastolic blood pressure'

parameter "Measurement Period" Interval<DateTime>

context Patient

/* Supplemental Data Elements - Columns */
define "SDEPractitioner":
  SDE."Most Recent Practitioner"

define "SDEDaysSinceAppointment":
  SDE."Days Since Last Appointment"

define "SDELastAppointmentDate":
  SDE."Last Appointment"

define "SDEEssentialHypertension":
  "Essential Hypertension Constructed Calculation"

define "SDEAverageSystolicBP":
  "Average Systolic BP Constructed Calculation"

define "SDEAverageDiastolicBP":
  "Average Diastolic BP Constructed Calculation"


/* Supplemental Data Elements - Filters */
define "SDEFilterPractitionersList":
  SDE."Practitioners List"

// Filter - Last Appointment Date
define "SDEFilterLastAppointment":
  SDE."Last Appointment"

define "SDEFilterEssentialHypertension":
  "Essential Hypertension Constructed Calculation"

// Systolic Filter - Average of all outpatient systolic blood pressures over the last 2 years
// (For filter: <130, 130-139, 140-149, 150-159, >=160)
define "SDEFilterAverageSystolicBP":
  "Average Systolic BP Constructed Calculation"

// Diastolic Filter - Average of all outpatient diastolic blood pressures over the last 2 years 
// (For filter: <80, 80-89, 90-99, >=100)
define "SDEFilterAverageDiastolicBP":
  "Average Diastolic BP Constructed Calculation"


/* Population Criteria */
// Initial Population / Denominator: 
// Patients 18-85 years of age who had a visit during the measurement period.
define "Initial Population":
  AgeInYearsAt(date from start of "Measurement Period") between 18 and 85
    and exists (
      AV."Qualifying Encounters" QualifyingEncounter
        where FC.ToInterval(QualifyingEncounter.period) during "Measurement Period"
    )

define "Denominator":
  "Initial Population"

define "Denominator Exclusion":
  exists "End stage renal disease diagnosis"
  or exists "End stage renal disease procedures"
  or exists "Kidney transplant diagnosis"
  or exists "Kidney transplant procedures"
  or exists "Pregnancy diagnosis"

define "Numerator":
  "Most Recent Systolic Blood Pressure is Elevated" is true
  or "Most Recent Diastolic Blood Pressure is Elevated" is true


/* Definitions - Denominator Exclusions */
define "End stage renal disease diagnosis":
  (Common.QualifiedConditions([Condition: Concepts."End Stage Renal Disease"]))

define "End stage renal disease procedures":
  (Common.QualifiedProcedures([Procedure: Concepts."ESRD Monthly Outpatient Services"])) ESRDProcedure
    where FC.ToInterval(ESRDProcedure.performed) overlaps before "Measurement Period"

define "Kidney transplant diagnosis":
  (Common.QualifiedConditions([Condition: Concepts."Kidney Transplant Recipient"]))

define "Kidney transplant procedures":
  (Common.QualifiedProcedures([Procedure: Concepts."Kidney Transplant"])) KidneyProcedure
    where FC.ToInterval(KidneyProcedure.performed) overlaps before "Measurement Period"

define "Pregnancy diagnosis":
  (Common.QualifiedConditions([Condition: Concepts."Pregnancy"])) PregnancyCondition


/* Definitions - Numerator */
define "Panel Blood Pressure Readings During Measurement Period":
  (Common.QualifiedObservations(["Observation": Concepts."Blood Pressure Panel"])) BPR
    where FC.ToInterval(BPR.effective) during "Measurement Period"
    and (
        (BPR.component component 
          where (
            FHIRHelpers.ToQuantity(component.value) is not null
            and Common.WithQuantityUnit(component.value, 'mm[Hg]')
          )
        )
      ) is not null

define "Panel Systolic Blood Pressure Readings During Measurement Period":
  "Panel Blood Pressure Readings During Measurement Period" BPR
    where (
      singleton from (BPR.component component 
        where (
          // NOTE: Workaround for platform bug
          // FHIRHelpers.ToConcept(c.code) in Concepts."Systolic Blood Pressure"
          exists (component.code.coding componentCode
            where FHIRHelpers.ToCode(componentCode) ~ "Systolic Blood Pressure code"
          )
        )
      )
    ) is not null

define "Panel Diastolic Blood Pressure Readings During Measurement Period":
  "Panel Blood Pressure Readings During Measurement Period" BPR
    where (
      singleton from (BPR.component component 
        where (
          // NOTE: Workaround for platform bug
          // FHIRHelpers.ToConcept(c.code) in Concepts."Diastolic Blood Pressure"
          exists (component.code.coding componentCode
            where FHIRHelpers.ToCode(componentCode) ~ "Diastolic Blood Pressure code"
          )
        )
      )
    ) is not null

define "Singular Systolic Blood Pressure Readings During Measurement Period":
  (Common.QualifiedObservations([Observation: Concepts."Systolic Blood Pressure"])) SystolicBPR
    where FC.ToInterval(SystolicBPR.effective) during "Measurement Period"
      and FHIRHelpers.ToQuantity(SystolicBPR.value) is not null
      and Common.WithQuantityUnit(SystolicBPR.value, 'mm[Hg]')

define "Singular Diastolic Blood Pressure Readings During Measurement Period":
  (Common.QualifiedObservations([Observation: Concepts."Diastolic Blood Pressure"])) DiastolicBPR
    where FC.ToInterval(DiastolicBPR.effective) during "Measurement Period"
      and FHIRHelpers.ToQuantity(DiastolicBPR.value) is not null
      and Common.WithQuantityUnit(DiastolicBPR.value, 'mm[Hg]')

define "Systolic Blood Pressure Readings During Measurement Period":
  (
    "Panel Systolic Blood Pressure Readings During Measurement Period"
    union "Singular Systolic Blood Pressure Readings During Measurement Period"
  ) except { null }

define "Diastolic Blood Pressure Readings During Measurement Period":
  (
    "Panel Diastolic Blood Pressure Readings During Measurement Period"
    union "Singular Diastolic Blood Pressure Readings During Measurement Period"
  ) except { null }

define "Most Recent Systolic Blood Pressure Reading Date":
  (
    Last(
      "Systolic Blood Pressure Readings During Measurement Period" SystolicBPR
      sort by end of FC.ToInterval(effective) ascending
    )
  ) BPR
  return date from BPR.effective

define "Most Recent Diastolic Blood Pressure Reading Date":
  (
    Last(
      "Diastolic Blood Pressure Readings During Measurement Period" DiastolicBPR
      sort by end of FC.ToInterval(effective) ascending
    )
  ) BPR
  return date from BPR.effective

define "Systolic Blood Pressure Readings from Latest Blood Pressure Date":
  "Systolic Blood Pressure Readings During Measurement Period" BPR
    where end of FC.ToInterval(BPR.effective) same day as "Most Recent Systolic Blood Pressure Reading Date"

define "Diastolic Blood Pressure Readings from Latest Blood Pressure Date":
  "Diastolic Blood Pressure Readings During Measurement Period" BPR
    where end of FC.ToInterval(BPR.effective) same day as "Most Recent Diastolic Blood Pressure Reading Date"

define function "Systolic Blood Pressure Values"(value List<FHIR.Observation>):
  value Observation
  let BPValue: Coalesce(
          FHIRHelpers.ToQuantity(Observation.value),
          FHIRHelpers.ToQuantity(
            singleton from (Observation.component component
              // NOTE: Workaround for platform bug
              // where FHIRHelpers.ToConcept(c.code) in Concepts."Systolic Blood Pressure"
              where FHIRHelpers.ToConcept(component.code) ~ "Systolic Blood Pressure code"
            ).value
          )
      )
  return Tuple { value: BPValue } 

define function "Diastolic Blood Pressure Values"(value List<FHIR.Observation>):
  value Observation
  let BPValue: Coalesce(
          FHIRHelpers.ToQuantity(Observation.value),
          FHIRHelpers.ToQuantity(
            singleton from (Observation.component component
              // NOTE: Workaround for platform bug
              // where FHIRHelpers.ToConcept(c.code) in Concepts."Diastolic Blood Pressure"
              where FHIRHelpers.ToConcept(component.code) ~ "Diastolic Blood Pressure code"
            ).value
          )
      )
  return Tuple { value: BPValue } 

define "Lowest Systolic Reading on Latest Blood Pressure Date":
  First(
    (
      (
        "Systolic Blood Pressure Values"("Systolic Blood Pressure Readings from Latest Blood Pressure Date")
      ) BPR sort by value ascending)
  )

define "Lowest Diastolic Reading on Latest Blood Pressure Date":
  First(
    (
      (
        "Diastolic Blood Pressure Values"("Diastolic Blood Pressure Readings from Latest Blood Pressure Date")
      ) BPR sort by value ascending)
  )

define "Most Recent Systolic Blood Pressure is Elevated":
  "Lowest Systolic Reading on Latest Blood Pressure Date" BPR
    return BPR.value >= 140 'mm[Hg]'

define "Most Recent Diastolic Blood Pressure is Elevated":
  "Lowest Diastolic Reading on Latest Blood Pressure Date" BPR
    return BPR.value >= 90 'mm[Hg]'


/* Definitions - SDEs */
define "First 6 Month Period":
  (Interval[start of "Measurement Period", start of "Measurement Period" + 6 months])

define "Essential Hypertension Diagnosis":
  (Common.QualifiedConditions([Condition: Concepts."Essential Hypertension"])) HypertensionDiagnosis
    where FC.ToPrevalenceInterval(HypertensionDiagnosis) overlaps "First 6 Month Period"

define "Essential Hypertension Constructed Calculation":
  FHIR.Observation {
    id: FHIR.id { id: 'SDEEssentialHypertension-' + ReplaceMatches(ToString(Now()), ':', '') },
    status: FHIR.ObservationStatus { value: 'final' },
    code: FHIR.CodeableConcept { 
      coding: { FHIR.Coding { 
        system: FHIR.uri { value: 'http://cds.optum.com/dqm-hypertension/fhir/CodeSystem/UIFilters' }, 
        code: FHIR.code { value: 'essential-hypertension' },
        display: FHIR.string { value: 'Has Essential Hypertension' }
        } 
      } 
    },
    subject: FHIR.Reference { reference: FHIR.string { value: 'Patient/' + Patient.id.value }},
    value: FHIR.string { value: ToString(exists "Essential Hypertension Diagnosis") }
  }

define "Average Systolic Blood Pressure over Measurement Period":
  Avg("Systolic Blood Pressure Values"("Systolic Blood Pressure Readings During Measurement Period").value)

define "Average Diastolic Blood Pressure over Measurement Period":
  Avg("Diastolic Blood Pressure Values"("Diastolic Blood Pressure Readings During Measurement Period").value)

define "Average Systolic BP Constructed Calculation":
  FHIR.Observation {
    id: FHIR.id { id: 'SDEDAverageSystolicBP-' + ReplaceMatches(ToString(Now()), ':', '') },
    status: FHIR.ObservationStatus { value: 'final' },
    code: FHIR.CodeableConcept { 
      coding: { FHIR.Coding { 
        system: FHIR.uri { value: 'http://cds.optum.com/dqm-hypertension/fhir/CodeSystem/UIFilters' }, 
        code: FHIR.code { value: 'bp-systolic' },
        display: FHIR.string { value: 'Systolic blood pressure' }
        } 
      } 
    },
    subject: FHIR.Reference { reference: FHIR.string { value: 'Patient/' + Patient.id.value }},
    value: FHIR.Quantity {
      value: FHIR.decimal { value: "Average Systolic Blood Pressure over Measurement Period".value }, 
      unit: FHIR.string { value: "Average Systolic Blood Pressure over Measurement Period".unit },
      system: FHIR.uri { value: 'http://unitsofmeasure.org' },
      code: FHIR.code { value: "Average Systolic Blood Pressure over Measurement Period".unit }
    }
  }

define "Average Diastolic BP Constructed Calculation":
  FHIR.Observation {
    id: FHIR.id { id: 'SDEDAverageDiastolicBP-' + ReplaceMatches(ToString(Now()), ':', '') },
    status: FHIR.ObservationStatus { value: 'final' },
    code: FHIR.CodeableConcept { 
      coding: { FHIR.Coding { 
        system: FHIR.uri { value: 'http://cds.optum.com/dqm-hypertension/fhir/CodeSystem/UIFilters' }, 
        code: FHIR.code { value: 'bp-diastolic' },
        display: FHIR.string { value: 'Diastolic blood pressure' }
        } 
      } 
    },
    subject: FHIR.Reference { reference: FHIR.string { value: 'Patient/' + Patient.id.value }},
    value: FHIR.Quantity {
      value: FHIR.decimal { value: "Average Diastolic Blood Pressure over Measurement Period".value }, 
      unit: FHIR.string { value: "Average Diastolic Blood Pressure over Measurement Period".unit },
      system: FHIR.uri { value: 'http://unitsofmeasure.org' },
      code: FHIR.code { value: "Average Diastolic Blood Pressure over Measurement Period".unit }
    }
  }


// Tests
define "SDEResourceTest":
  SDE."Resource Test"

define "SDEProviderTest":
  SDE."Provider Test"

define "SDETupleTest":
  SDE."Tuple Test"

define "SDEListTest":
  SDE."List Test"

define "SDEResourceConstructionTest":
  SDE."Resource Construction Test"

define "SDEComponentTest1.1":
  "Panel Systolic Blood Pressure Readings During Measurement Period"

define "SDEComponentTest1.2": 
  "Panel Diastolic Blood Pressure Readings During Measurement Period"

define "SDEComponentTest1.3": 
  "Singular Systolic Blood Pressure Readings During Measurement Period"

define "SDEComponentTest1.4": 
  "Singular Diastolic Blood Pressure Readings During Measurement Period"

define "SDEComponentTest2.1":
  "Most Recent Systolic Blood Pressure Reading Date"

define "SDEComponentTest2.2":
  "Most Recent Diastolic Blood Pressure Reading Date"

define "SDEComponentTest3.1":
  "Lowest Systolic Reading on Latest Blood Pressure Date"

define "SDEComponentTest3.2":
  "Lowest Diastolic Reading on Latest Blood Pressure Date"

define "SDEComponentTest3.3":
  "Lowest Systolic Reading on Latest Blood Pressure Date".value

define "SDEComponentTest3.4":
  "Lowest Diastolic Reading on Latest Blood Pressure Date".value

define "SDEComponentTest4.1":
  "Most Recent Systolic Blood Pressure is Elevated" is true

define "SDEComponentTest4.2":
  "Most Recent Diastolic Blood Pressure is Elevated" is true
