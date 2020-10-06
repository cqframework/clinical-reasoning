library OMTKLogic version '3.0.0'

/*
This library provides functionality for calculating Morphine Milligram
Equivalents (MME) for opioid medications, as described in the CDC Opioid
Prescribing Guideline.

The functionality in this library was developed based on the Java-based
implementation described [here](http://build.fhir.org/ig/cqframework/opioid-cds-r4/service-documentation.html#solution-component-3-core-logic-processing-java-class),
as well as the MME conversion calculation published as part of the CDC Opioid
Prescribing Guideline.

Note that the logic in this library (and specifically the conversion factors captured here)
are based on the 2016 version of the publication available from the CDC here:
https://www.cdc.gov/drugoverdose/modules/data-files.html

National Center for Injury Prevention and Control. CDC compilation of benzodiazepines,
muscle relaxants, stimulants, zolpidem, and opioid analgesics with oral morphine milligram
equivalent conversion factors, 2018 version. Atlanta, GA: Centers for Disease Control
and Prevention; 2018. Available at https://www.cdc.gov/drugoverdose/resources/data.html

This version of the OMTKLogic library uses the OMTKData library as the
source for drug ingredient and strength information, rather than the
OMTK data source. This library has no external dependencies and so
can run in an environment that supports pure CQL.

NOTE: For performance, all terminology comparisons in this library use
direct integer comparison of the RxNorm codes.

This product uses publicly available data courtesy of the U.S. National Library of Medicine (NLM),
National Institutes of Health, Department of Health and Human Services; NLM is not responsible for
the product and does not endorse or recommend this or any other product.

Nelson SJ, Zeng K, Kilbourne J, Powell T, Moore R. Normalized names for clinical drugs: RxNorm at 6 years.
J Am Med Inform Assoc. 2011 Jul-Aug;18(4)441-8. doi: 10.1136/amiajnl-2011-000116.
Epub 2011 Apr 21. PubMed PMID: 21515544; PubMed Central PMCID: PMC3128404.
[Full text](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3128404/)
*/

include OMTKData version '3.0.0' called OMTKData
include ConversionFactors version '3.0.0' called ConversionFactors

codesystem "RXNORM": 'http://www.nlm.nih.gov/research/umls/rxnorm'

parameter ErrorLevel String default 'Warning'

/*  Normalizes the input units to UCUM units

  The values listed here are the only ones currently present in the OMTK data

  Based on the HL7 UCUM subset here:
  http://download.hl7.de/documents/ucum/ucumdata.html
*/
define function ToUCUM(unit String):
  case unit
    when 'MG' then 'mg'
    when 'MG/ACTUAT' then 'mg/{actuat}'
    when 'MG/HR' then 'mg/h'
    when 'MG/ML' then 'mg/mL'
    else Message(null, true, 'OMTKLogic.ToUCUM.UnknownUnit', ErrorLevel, 'Unknown unit ' & unit)
  end

/*
  Calculates daily frequency given frequency within a period
*/
define function ToDaily(frequency Integer, period Quantity):
  case period.unit
    when 'h' then frequency * (24.0 / period.value)
    when 'min' then frequency * (24.0 / period.value) * 60
    when 's' then frequency * (24.0 / period.value) * 60 * 60
    when 'd' then frequency * (24.0 / period.value) / 24
    when 'wk' then frequency * (24.0 / period.value) / (24 * 7)
    when 'mo' then frequency * (24.0 / period.value) / (24 * 30) /* assuming 30 days in month */
    when 'a' then frequency * (24.0 / period.value) / (24 * 365) /* assuming 365 days in year */
    when 'hour' then frequency * (24.0 / period.value)
    when 'minute' then frequency * (24.0 / period.value) * 60
    when 'second' then frequency * (24.0 / period.value) * 60 * 60
    when 'day' then frequency * (24.0 / period.value) / 24
    when 'week' then frequency * (24.0 / period.value) / (24 * 7)
    when 'month' then frequency * (24.0 / period.value) / (24 * 30) /* assuming 30 days in month */
    when 'year' then frequency * (24.0 / period.value) / (24 * 365) /* assuming 365 days in year */
    when 'hours' then frequency * (24.0 / period.value)
    when 'minutes' then frequency * (24.0 / period.value) * 60
    when 'seconds' then frequency * (24.0 / period.value) * 60 * 60
    when 'days' then frequency * (24.0 / period.value) / 24
    when 'weeks' then frequency * (24.0 / period.value) / (24 * 7)
    when 'months' then frequency * (24.0 / period.value) / (24 * 30) /* assuming 30 days in month */
    when 'years' then frequency * (24.0 / period.value) / (24 * 365) /* assuming 365 days in year */
    else Message(null, true, 'OMTKLogic.ToDaily.UnknownUnit', ErrorLevel, 'Unknown unit ' & period.unit)
  end

/*
  Returns the opioid ingredients and their strengths that
  make up the drug identified by the given rxNormCode as a list of tuples:

  List<Tuple {
    rxNormCode Code,
    doseFormCode Code,
    doseFormName String,
    ingredientCode Code,
    ingredientName String,
    strength Quantity
  }>
*/

/*
GetIngredients:
  List<{
    rxNormCode Code,
    doseFormCode Code,
    ingredientCode code,
    strength Quantity
  }>
*/
define function GetIngredients(rxNormCode Code):
  OMTKData.DrugIngredients DI
    where DI.drugCode = ToInteger(rxNormCode.code)
    return {
      rxNormCode: Code { code: ToString(DI.drugCode), system: 'http://www.nlm.nih.gov/research/umls/rxnorm', display: DI.drugName },
      doseFormCode: Code { code: ToString(DI.doseFormCode), system: 'http://www.nlm.nih.gov/research/umls/rxnorm', display: DI.doseFormName },
      ingredientCode: Code { code: ToString(DI.ingredientCode), system: 'http://www.nlm.nih.gov/research/umls/rxnorm', display: DI.ingredientName },
      strength: Quantity {
        value: DI.strengthValue,
        unit: ToUCUM(DI.strengthUnit)
      }
    }

/*
Returns the first RxNorm code in the given concept that matches a drug code
specified in OMTKData.
*/
define function GetMedicationCode(concept Concept):
  First(
    (concept.codes) C
      where C.system = 'http://www.nlm.nih.gov/research/umls/rxnorm'
        return singleton from (
          OMTKData.DrugIngredients DI
            where DI.drugCode = ToInteger(C.code)
            return Code {
              code: ToString(DI.drugCode),
              system: 'http://www.nlm.nih.gov/research/umls/rxnorm',
              display: DI.drugName
            }
        )
  )

/*
Returns the display of the given concept, if present, otherwise, looks up medication
names for any RxNormCodes within the concept using the GetMedicationName function
*/
define function GetMedicationConceptName(concept Concept):
  if concept.display is null then
    First(
      (concept.codes) C
        where C.system = 'http://www.nlm.nih.gov/research/umls/rxnorm'
        return GetMedicationName(C)
    )
  else
    concept.display

/*
Returns the display of the given code, if present, otherwise looks it up from the OMTK data
*/
define function GetMedicationName(rxNormCode Code):
  if rxNormCode.display is null then
    singleton from (
      OMTKData.DrugIngredients DI
        where DI.drugCode = ToInteger(rxNormCode.code)
        return DI.drugName
    )
    else rxNormCode.display

/*
Returns the display of the given ingredient, if present, otherwise looks it up from the OMTK data
*/
define function GetIngredientName(ingredientCode Code):
  if ingredientCode.display is null then
    singleton from (
      OMTKData.DrugIngredients DI
        where DI.ingredientCode = ToInteger(ingredientCode.code)
        return DI.ingredientName
    )
    else ingredientCode.display

/*
Returns the display of the given dose form, if present, otherwise looks it up from the OMTK data
*/
define function GetDoseFormName(doseFormCode Code):
  if doseFormCode.display is null then
    singleton from (
      OMTKData.DrugIngredients DI
        where DI.doseFormCode = ToInteger(doseFormCode.code)
        return DI.doseFormName
    )
    else doseFormCode.display

/*
  Removes the last per argument from a unit
*/
define function StripPer(unit String):
  if LastPositionOf('/', unit) >= 0
    then Substring(unit, 0, LastPositionOf('/', unit))
    else unit

/*
  Calculates daily dose for a specific ingredient based on the ingredient strength, dose form, dose quantity, and daily frequency
*/
define function GetDailyDose(ingredientCode Code, strength Quantity, doseFormCode Code, doseQuantity Quantity, dosesPerDay Decimal):
  case
    when dosesPerDay is null or doseQuantity is null or strength is null then
      null as Quantity
	  /* if patch --> daily dose = dose value (e.g, number patches with doseQuantity unit = "patch") * per-hour strength */
    when ToInteger(doseFormCode.code) = 316987 then
      /* buprenorphine or fentanyl patch */
      if ToInteger(ingredientCode.code) in { 1819, 4337 } then
        Quantity { value: dosesPerDay * doseQuantity.value * strength.value, unit: strength.unit }
      else
        Message(null, true, 'OMTKLogic.GetDailyDoseDescription.UnknownPatchIngredient', ErrorLevel, 'Unknown patch ingredient: ' & ingredientCode.code & ':' & ingredientCode.display)

    /* if dose unit in actual mass units (mg or ug -- when it's a single med) --> daily dose = numTimesPerDay * dose */
    when doseQuantity.unit in { 'mg', 'ug' } then
      Quantity { value: dosesPerDay * doseQuantity.value, unit: doseQuantity.unit }

    /* if doseQuantity is in actual volume units (mL) --> daily dose = numTimesPerDay * dose * strength */
    when doseQuantity.unit = 'mL' and (PositionOf('/mL', strength.unit) = Length(strength.unit) - 3) then
      Quantity { value: dosesPerDay * doseQuantity.value * strength.value, unit: StripPer(strength.unit) }

		/* if doseQuantity is not in actual units (e.g., 1 tab, 1 spray -- when it's a combo med with a unit of tablet, or it's mg/actuat) -->  daily dose = numTimesPerDay * dose value * strength value */
    else
      Quantity { value: dosesPerDay * doseQuantity.value * strength.value, unit: StripPer(strength.unit) }
  end

/*
  Builds a description for the daily dose for an ingredient
*/
define function GetDailyDoseDescription(ingredientCode Code, strength Quantity, doseFormCode Code, doseQuantity Quantity, dosesPerDay Decimal, dailyDose Quantity):
  case
    /* if patch */
    when ToInteger(doseFormCode.code) = 316987 then
      /* buprenorphine or fentanyl patch */
      if ToInteger(ingredientCode.code) in { 1819, 4337 } then
        GetIngredientName(ingredientCode) & ' patch: ' & ToString(doseQuantity.value) & ' * ' & ToString(dosesPerDay) & '/d * '& ToString(strength) & ' = ' + ToString(dailyDose)
      else
        Message(null, true, 'OMTKLogic.GetDailyDoseDescription.UnknownPatchIngredient', ErrorLevel, 'Unknown patch ingredient: ' & ingredientCode.code & ':' & ingredientCode.display)

    /* if dose unit in actual mass units (mg or ug -- when it's a single med) */
    when doseQuantity.unit in { 'mg', 'ug' } then
      GetIngredientName(ingredientCode) + ' ' + GetDoseFormName(doseFormCode) + ': ' + ToString(dosesPerDay) + '/d * ' + ToString(doseQuantity) + ' = ' + ToString(dailyDose)

    /* if doseQuantity in actual volume units (mL) or not in actual units (e.g. 1 tab, 1 spray) */
    else
      GetIngredientName(ingredientCode) + ' ' + GetDoseFormName(doseFormCode) + ': ' + ToString(dosesPerDay) + '/d * ' + ToString(doseQuantity) + ' * ' + ToString(strength) + ' = ' + ToString(dailyDose)
  end

/*
  Calculates MMEs for the given input prescription information and returns it
  as a list of tuples:

  List<Tuple {
    rxNormCode Code,
    doseFormCode Code,
    doseQuantity Quantity,
    dosesPerDay Decimal,
    ingredientCode Code,
    strength Quantity,
    dailyDose Quantity,
    dailyDoseDescription String,
    conversionFactor Decimal,
    mme Quantity
  }>
*/
define function CalculateMMEs(medications List<Tuple { rxNormCode Code, doseQuantity Quantity, dosesPerDay Decimal }>):
  Flatten(
    medications M
      let Ingredients: GetIngredients(M.rxNormCode)
      return
        Ingredients I
          let
            adjustedDoseQuantity: M.doseQuantity,
            dailyDose: GetDailyDose(I.ingredientCode, I.strength, I.doseFormCode, adjustedDoseQuantity, M.dosesPerDay),
            dailyDoseDescription: GetDailyDoseDescription(I.ingredientCode, I.strength, I.doseFormCode, adjustedDoseQuantity, M.dosesPerDay, dailyDose),
            factor: ConversionFactors.GetConversionFactor(I.ingredientCode, dailyDose, I.doseFormCode)
          return {
            rxNormCode: M.rxNormCode,
            doseFormCode: I.doseFormCode,
            doseQuantity: adjustedDoseQuantity,
            dosesPerDay: M.dosesPerDay,
            ingredientCode: I.ingredientCode,
            strength: I.strength,
            dailyDose: dailyDose,
            dailyDoseDescription: dailyDoseDescription & (' * factor: ' + Coalesce(ToString(factor), 'No conversion factor available')),
            conversionFactor: factor,
            mme: Quantity(
              Round(dailyDose.value * factor, 1),
              '{MME}/d'
            )
          }
  )

define function Quantity(value Decimal, unit String):
  if value is not null then
    Quantity { value: value, unit: unit }
  else
    null
