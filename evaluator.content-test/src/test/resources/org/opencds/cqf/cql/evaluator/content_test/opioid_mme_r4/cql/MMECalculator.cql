library MMECalculator version '3.0.0'

/*
This library contains logic to surface the MME calculation functionality provided
by the OMTKLogic library by extracting appropriate information from FHIR R4
MedicationRequest resource.
*/

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'
include OMTKLogic version '3.0.0'

parameter ErrorLevel String default 'Warning'

context Patient

/*
Helper function to force the choice of the FHIR.Range value.
This avoids the need for the _is_ and _as_ operators on choices,
which is not implemented in the JavaScript CQL engine.
*/
define function ToFHIRRange(range FHIR.Range):
  range

/*
Helper function to force the choice of the FHIR.SimpleQuantity value.
This avoids the need for the _is_ and _as_ operators on choices,
which is not implemented in the JavaScript CQL engine.
*/
define function ToFHIRQuantity(quantity FHIR.SimpleQuantity):
  quantity

/*
Helper function to force the choice of the FHIR.CodeableConcept value.
This avoids the need for the _is_ and _as_ operators on choices,
which is not implemented in the JavaScript CQL engine.
*/
define function ToFHIRCodeableConcept(codeableConcept FHIR.CodeableConcept):
  codeableConcept

/*
FHIRHelpers ToQuantity logic incorrectly uses the unit element of the FHIR Quantity,
when it should be using the code element as the actual coded unit.
https://github.com/cqframework/clinical_quality_language/issues/557

Until the above issue is addressed, this function provides ToQuantity functionality for this library
*/
define function ToQuantity(quantity FHIR.SimpleQuantity):
  case
    when quantity is null then null
    when quantity.value is null then null
    when quantity.system is null or quantity.system = 'http://unitsofmeasure.org' then
      System.Quantity {
        value: quantity.value.value,
        unit: quantity.code.value
      }
    else
      Message(null, true, 'MMECalculator.ToQuantity.InvalidFHIRQuantity', ErrorLevel, 'Invalid FHIR Quantity code: ' & quantity.code.value)
  end

/*
Extracts the relevant information for prescription calculation from a list of
FHIR MedicationRequest resources.
*/
define function Prescriptions(Orders List<MedicationRequest>):
  Orders O
    let
      rxNormCode: OMTKLogic.GetMedicationCode(O.medication),
      medicationName: OMTKLogic.GetMedicationName(rxNormCode),
      // NOTE: Assuming a single dosage instruction element
      dosageInstruction: O.dosageInstruction[0],
      // NOTE: Assuming a single dose and rate element
      doseAndRate: dosageInstruction.doseAndRate[0],
      repeat: dosageInstruction.timing.repeat,
      frequency: Coalesce(repeat.frequencyMax.value, repeat.frequency.value),
      period: OMTKLogic.Quantity(repeat.period.value, repeat.periodUnit.value),
      doseRange: ToFHIRRange(doseAndRate.dose),
      doseQuantity: ToFHIRQuantity(doseAndRate.dose),
      doseDescription:
        Coalesce(
          // There should be a conversion from FHIR.SimpleQuantity to System.Quantity
          if doseRange is not null
            then ToString(doseRange.low)
                + '-' + ToString(doseRange.high)
                + doseRange.high.unit.value
            else ToString(ToQuantity(doseQuantity)),
            ''
        ),
      frequencyDescription:
        ToString(dosageInstruction.timing.repeat.frequency.value) +
          Coalesce(
            '-' + ToString(dosageInstruction.timing.repeat.frequencyMax.value),
            ''
          )
    return {
      rxNormCode: rxNormCode,
      isDraft: O.status.value = 'draft',
      // NOTE: Assuming asNeeded is expressed as a boolean
      isPRN: dosageInstruction.asNeeded,
      prescription:
        if dosageInstruction.text is not null then
          medicationName + ' ' + dosageInstruction.text.value
        else
          // TODO: Shouldn't need the .value here on asNeededBoolean
          medicationName + ' ' + doseDescription + ' q' + frequencyDescription + (if dosageInstruction.asNeeded then ' PRN' else ''),
      dose: if doseRange is not null
            then ToQuantity(doseRange.high)
            else ToQuantity(doseQuantity),
      dosesPerDay: Coalesce(OMTKLogic.ToDaily(frequency, period), 1.0)
    }

/*
Calculates Milligram Morphine Equivalent (MME) for each medication in the given
list. The calculation assumes the most aggresive dosing, and is performed for all
medications in the given list (i.e. no additional filtering for status is performed).
*/
define function MME(prescriptions List<MedicationRequest>):
  (Prescriptions(prescriptions)) P
    let mmePerIngredient: OMTKLogic.CalculateMMEs({ { rxNormCode: P.rxNormCode, doseQuantity: P.dose, dosesPerDay: P.dosesPerDay } })
    return {
      rxNormCode: P.rxNormCode,
      isDraft: P.isDraft,
      isPRN: P.isPRN,
      prescription: P.prescription,
      dailyDose: Combine(mmePerIngredient X return X.dailyDoseDescription, '\r\n'),
      mme: Sum(mmePerIngredient X return X.mme)
    }

/*
Calculates total Milligram Morphine Equivalent (MME) for the given list of medications.
The calculation assumes the most aggressive dosing, and is performed for all
medications in the given list (i.e. no additional filtering for status is performed).
*/
define function TotalMME(prescriptions List<MedicationRequest>):
  OMTKLogic.Quantity(
    Sum((MME(prescriptions)) M return M.mme.value),
    '{MME}/d'
  )
