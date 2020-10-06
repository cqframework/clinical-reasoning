library OMTKLogicTests version '3.0.0'
using FHIR version '4.0.1'
include OMTKLogic version '3.0.0'

// define function ToUCUM(unit String):
define TestToUCUM_MG: OMTKLogic.ToUCUM('MG') = 'mg'
define TestToUCUM_MGPerACTUAT: OMTKLogic.ToUCUM('MG/ACTUAT') = 'mg/{actuat}'
define TestToUCUM_MGPerML: OMTKLogic.ToUCUM('MG/ML') = 'mg/mL'
define TestToUCUM_MGPerHR: OMTKLogic.ToUCUM('MG/HR') = 'mg/h'
define TestToUCUM_Unknown: OMTKLogic.ToUCUM('Unknown') is null

// define function ToDaily(frequency Integer, period Quantity):
define TestToDaily_h: OMTKLogic.ToDaily(2, 4 'h') = 12
define TestToDaily_min: OMTKLogic.ToDaily(1, 30 'min') = 48
define TestToDaily_s: OMTKLogic.ToDaily(1, 30 's') = 2880
define TestToDaily_d: OMTKLogic.ToDaily(4, 1 'd') = 4
define TestToDaily_wk: OMTKLogic.ToDaily(2, 1 'wk') = 0.28571428
define TestToDaily_mo: OMTKLogic.ToDaily(10, 1 'mo') = 0.33333333
define TestToDaily_a: OMTKLogic.ToDaily(10, 1 'a') = 0.02739726
define TestToDaily_unknown: OMTKLogic.ToDaily(10, 1 'x') is null

//define function EnsureMicrogramQuantity(strength Quantity):
//define TestEnsureMicrogramQuantityNormal: OMTKLogic.EnsureMicrogramQuantity(10 'mg') = 10 'mg'
//define TestEnsureMicrogramQuantityMicrogram: OMTKLogic.EnsureMicrogramQuantity(0.001 'mg') = 1 'ug'
//define TestEnsureMicrogramQuantityMicrogramPerML: OMTKLogic.EnsureMicrogramQuantity(0.05 'mg/mL') = 50 'ug/mL'

//define function GetIngredients(rxNormCode Code):
//GetIngredients:
//  List<{
//    rxNormCode Code,
//    doseFormCode Code,
//    ingredientCode code,
//    strength Quantity
//  }>
define TestIngredients_Tapentadol: OMTKLogic.GetIngredients(Code '1148797' from OMTKLogic.RXNORM)
define TestIngredients_Phenylephrine: OMTKLogic.GetIngredients(Code '1357883' from OMTKLogic.RXNORM)
define TestIngredients_Fentanyl: OMTKLogic.GetIngredients(Code '1603495' from OMTKLogic.RXNORM)
define TestIngredients_Fentanyl_Spray: OMTKLogic.GetIngredients(Code '1115577' from OMTKLogic.RXNORM)
define TestIngredients_Fentanyl_Ropivicaine: OMTKLogic.GetIngredients(Code '1233687' from OMTKLogic.RXNORM)
define TestIngredients_Methadone: OMTKLogic.GetIngredients(Code '864706' from OMTKLogic.RXNORM)

// 100 mg X (60 tablets/30 days) X 0.4 = 80 MME/day
define TestDrug_Tapentadol: { rxNormCode: Code '1148797' from OMTKLogic.RXNORM display '12 HR tapentadol 100 MG Extended Release Oral Tablet', doseQuantity: 1 '{tab}', dosesPerDay: 2.0, mme: 80 '{MME}/d' }

// 5 ug/hr buprenorphine patch X (4 patches/28 days)X 12.6 = 9 MME/day
// NOTE: Buprenorphine has variable equivalency based on route, dose, and length of use. Buprenorphine and methadone are excluded from calculation in some systems.
// They are still considered relevant opioids, but no MME calculation is given in those systems.
define TestDrug_Buprenorphine: { rxNormCode: Code '904880' from OMTKLogic.RXNORM display '168 HR buprenorphine 0.005 MG/HR Transdermal System', doseQuantity: 1 '{patch}', dosesPerDay: 0.1429, mme: 9 '{MME}/d' }

// 12 HR brompheniramine maleate 6 MG / phenylephrine hydrochloride 30 MG Extended Release Oral Tablet X (60 tables/30 days) X 0.0 = 0 MME/day
define TestDrug_Phenylephrine: { rxNormCode: Code '1357883' from OMTKLogic.RXNORM display '12 HR brompheniramine maleate 6 MG / phenylephrine hydrochloride 30 MG Extended Release Oral Tablet', doseQuantity: 1 '{tab}', dosesPerDay: 2.0, mme: 0 '{MME}/d' }

// 25 µg/hr fentanyl patch X (10 patches/ 30 days) X 7.2  = 60 MME/day
// See this issue for discussion: https://github.com/cqframework/opioid-cds/issues/60
define TestDrug_Fentanyl_Transdermal: { rxNormCode: Code '245134' from OMTKLogic.RXNORM display '72 HR fentanyl 0.025 MG/HR Transdermal System', doseQuantity: 1 '{patch}', dosesPerDay: 0.3333, mme: 60 '{MME}/d' }

// 37.5 µg/hr fentanyl patch X (10 patches/ 30 days) X 7.2  = 90 MME/day
define TestDrug_Fentanyl_0375_Transdermal: { rxNormCode: Code '1603495' from OMTKLogic.RXNORM display '72 HR fentanyl 0.0375 MG/HR Transdermal System', doseQuantity: 1 '{patch}', dosesPerDay: 0.3333, mme: 90 '{MME}/d' }

// Injection calculation not supported
define TestDrug_Fentanyl_Injection: { rxNormCode: Code '1244687' from OMTKLogic.RXNORM display 'fentanyl 0.004 MG/ML / ropivacaine hydrochloride 2 MG/ML Injectable Solution', doseQuantity: 1 '{injection}', dosesPerDay: 1.0, mme: null as Quantity }

// 10 MG methadone X (180 tablets/ 30 days) X 10 = 600 MME/day
// NOTE: Methodone has variable equivalency based on route, dose, and length of use. Buprenorphine and methadone are excluded from calculation in some systems.
// They are still considered relevant opioids, but no MME calculation is given in those systems.
define TestDrug_Methadone: { rxNormCode: Code '864706' from OMTKLogic.RXNORM display 'methadone hydrochloride 10 MG Oral Tablet', doseQuantity: 2 '{tab}', dosesPerDay: 3.0, mme: 600 '{MME}/d' }

// 0.4 MG fentanyl spray X (90 sprays/ 30 days) X 160 = 192 MME/day
// NOTE: Had to add Metered Dose Nasal Spray as a dose form code to the GetConversionFactor for Fentanyl, same category as other sprays
define TestDrug_Fentanyl_Spray: { rxNormCode: Code '1115577' from OMTKLogic.RXNORM display 'fentaNYL 0.4 MG/ACTUAT', doseQuantity: 1 '{actuat}', dosesPerDay: 3.0, mme: 192 '{MME}/d' }

// 1.2 mg buccal film X (60 films/ 30 days) X 180 = 432 MME/day
// NOTE: This drug is not in our OMTKData, not sure why 858087 is an SDC RXCUI and the code is active, and was introduced in 2009
define TestDrug_Fentanyl_Film: { rxNormCode: Code '858087' from OMTKLogic.RXNORM display 'fentanyl 1.2 MG Buccal Film', doseQuantity: 1 '{film}', dosesPerDay: 2.0, mme: 432 '{MME}/d' }

// 0.1 mg fentanyl spray X (90 sprays/ 30 days) X 160 = 48 MME/day
define TestDrug_Fentanyl_Mucosal_Spray: { rxNormCode: Code '1237050' from OMTKLogic.RXNORM display 'fentanyl 0.1 MG/ACTUAT Mucosal Spray', doseQuantity: 1 '{actuat}', dosesPerDay: 3.0, mme: 48 '{MME}/d' }

// 10 mg oxycodone tablets X (120 tablets/ 30 days) X 1.5 = 60 MME/day
define TestDrug_Oxycodone: { rxNormCode: Code '1049683' from OMTKLogic.RXNORM display 'oxycodone hydrochloride 10 MG Oral Tablet', doseQuantity: 1 'tab', dosesPerDay: 4.0, mme: 60 '{MME}/d' }

// 30 mg oxycodone tables X (60 tablets/ 30 days) X 1.5 = 90 MME/day
define TestDrug_Oxycodone_ER: { rxNormCode: Code '1049574' from OMTKLogic.RXNORM display '12 HR oxycodone hydrochloride 30 MG Extended Release Oral Tablet', doseQuantity: 1 '{tab}', dosesPerDay: 2.0, mme: 90 '{MME}/d' }

// TODO: Determine how this will be reflected, I don't see any RxNav data that uses bottles?
// For example, a butorphanol spray is packaged as a 2.5ml bottle.
// This MME file indicates the strength is 10mg/ml and the conversion factor is 7.
// The prescription data lists a quantity of 1 and a days supply of 30.
// If it is determined that the quantity likely reflects the number of bottles,
// the quantity should be changed to 2.5 since each bottle of that butorphanol
// product contains 2.5 ml. Using the formula above, this yields 10mg/ml  x 2.5ml/30 days x 7= 5.8 MME/day.

define TestDrugs: {
  TestDrug_Tapentadol,
  TestDrug_Buprenorphine,
  TestDrug_Phenylephrine,
  TestDrug_Fentanyl_Transdermal,
  TestDrug_Fentanyl_0375_Transdermal,
  TestDrug_Fentanyl_Injection,
  TestDrug_Methadone,
  TestDrug_Fentanyl_Spray,
  TestDrug_Fentanyl_Film,
  TestDrug_Fentanyl_Mucosal_Spray,
  TestDrug_Oxycodone,
  TestDrug_Oxycodone_ER
}

//define function StripPer(unit String):
define TestStripPer_UGPerH: OMTKLogic.StripPer('ug/h') = 'ug'
define TestStripPer_MGPerTab: OMTKLogic.StripPer('mg/{tab}') = 'mg'

//define function GetDailyDose(ingredientCode Code, strength Quantity, doseFormCode Code, doseQuantity Quantity, dosesPerDay Decimal):
define TestGetDailyDose:
  flatten (
    TestDrugs D
      let i: OMTKLogic.GetIngredients(D.rxNormCode)
      return i I
        return OMTKLogic.GetDailyDose(I.ingredientCode, I.strength, I.doseFormCode, D.doseQuantity, D.dosesPerDay)
  )

//define function GetMedicationName(rxNormCode Code):
//define function GetDailyDoseDescription(ingredientCode Code, ingredientName String, strength Quantity, doseFormCode Code, doseFormName String, doseQuantity Quantity, dosesPerDay Decimal, dailyDose Quantity):

//define function CalculateMMEs(medications List<Tuple { rxNormCode Code, doseQuantity Quantity, dosesPerDay Decimal }>):
define TestCalculateMMEs:
  TestDrugs D
    let mmes: OMTKLogic.CalculateMMEs({ D d return { rxNormCode: d.rxNormCode, doseQuantity: d.doseQuantity, dosesPerDay: d.dosesPerDay }})
    return {
      rxNormCode: D.rxNormCode,
      mme: Sum(mmes.mme),
      expectedMme: D.mme,
      description: Combine(mmes.dailyDoseDescription, ', ')
    }
