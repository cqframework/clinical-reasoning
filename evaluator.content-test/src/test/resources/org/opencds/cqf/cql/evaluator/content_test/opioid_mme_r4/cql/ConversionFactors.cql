library ConversionFactors version '3.0.0'

/*
This library provides logic to determine the appropriate conversion factor to be
used for calculating Morphine Milligram Equivalent (MME) for a given medication.

The conversion factors are configurable using a CodeSystem supplement. This
implementation guide contains two conversion factor tables:

CDCMMEClinicalConversionFactors
CDCMMEResearchConversionFactors

The Clinical Conversion Factors table uses values from CDC guidance here:
https://www.cdc.gov/drugoverdose/pdf/calculating_total_daily_dose-a.pdf

The Research Conversion Factors table uses values from CDC information here:
https://www.cdc.gov/drugoverdose/modules/data-files.html

The CodeSystem supplements are configured with 3 types of properties:

* conversion-factor: Defines the conversion factor for a specific ingredient,
when the conversion factor is the same for all dose forms and dose amounts for
the ingredient. The value of this property will be the decimal representation of
the conversion factor.
* dose-form-conversion-factor: Defines the conversion factor for an ingredient
when the conversion factor varies by the dose form of the medication. The value
of this property will be a string of the form <dose-form-code>:<conversion-factor>,
e.g. 970789:130, where '970789' is the RxNorm dose form code, and '130' is the
decimal representation of the conversion factor.
* dose-range-conversion-factor: Defines the conversion factor for an ingredient
when the conversion factor varies by the overall dose of the medication. The
value of this property will be a string of the form <low-value>-<high-value>:<conversion-factor>,
e.g. 1-20:4. Note that the low-value or high-value may be a wildcard '*' to
indicate the range continues (e.g. '61-*:12' indicates the range is 61 or greater).

For a given ingredient, only one of 'dose-form' or 'dose-range' will be present,
with or without an ingredient-specific conversion factor. The properties together
will enable a unique conversion factor to be determined if the input is within
the expected range. If the input is outside the expected range, and there is no
ingredient-specific conversion-factor specified, implementations should indicate
a conversion factor could not be determined from the supplied information.
All conversion factors supplied in these supplements are in 'mg/d'.
*/

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

codesystem "Usage Context Type": 'http://terminology.hl7.org/CodeSystem/usage-context-type'
codesystem "CDC MME Usage Context Codes": 'http://fhir.org/guides/cdc/opioid-mme-r4/CodeSystem/CDCMMEUsageContextCodes'

code "Task Usage Context": 'task' from "Usage Context Type"
code "MME Calculation": 'mme-calculation' from "CDC MME Usage Context Codes"

parameter ErrorLevel String default 'Warning'

/*
If this parameter is supplied, it specifies the name of the code system supplement
to be used to supply conversion factors for the calculator. If this parameter is not
provided, the _single_ CodeSystem with a task usage context code of "mme-calculation"
from the CDCMMEUsageContextCodes code system will be used to provide conversion factors.
If neither of these configurations is available, the hard-coded conversion factors
in GetConversionFactor will be used. The hard-coded conversion factors are a combination
of the research and clinical conversion factors. Specifically, they are the research
conversion factors, augmented with the clinical conversion factors for methadone
and transdermal fentanyl.
*/
parameter ConversionFactorSupplementName String

context Patient

/*
  Returns the conversion factor for the given ingredient

CDC Guidance:
https://www.cdc.gov/drugoverdose/pdf/calculating_total_daily_dose-a.pdf
https://www.cdc.gov/drugoverdose/modules/data-files.html

CMS Guidance:
https://www.cms.gov/Medicare/Prescription-Drug-Coverage/PrescriptionDrugCovContra/Downloads/Oral-MME-CFs-vFeb-2018.pdf

Opioid (strength in mg except where noted)	MME Conversion Factor*
Buprenorphine, transdermal patch (MCG/HR)	12.6
Buprenorphine, tablet or film	30
Buprenorphine, film (MCG)	0.03
Butorphanol	7
Codeine	0.15
Dihydrocodeine	0.25
Fentanyl, buccal/SL tablet or lozenge/troche (MCG)	0.13
Fentanyl, film or oral spray (MCG)	0.18
Fentanyl, nasal spray (MCG)	0.16
Fentanyl, transdermal patch (MCG/HR)	2.4
Hydrocodone	1
Hydromorphone	4
Levomethadyl acetate	8
Levorphanol tartrate	11
Meperidine 	0.1
Methadone	3
  1-20 mg/d 4
  21-40 mg/d 8
  41-60 mg/d 10
  61-80 mg/d 12
Morphine	1
Opium	1
Oxycodone	1.5
Oxymorphone	3
Pentazocine	0.37
Tapentadol	0.4
Tramadol	0.1

*/
define function GetConversionFactor(ingredientCode System.Code, dailyDose System.Quantity, doseFormCode System.Code):
  Coalesce(
    LookupConversionFactor(ingredientCode, dailyDose, doseFormCode),
    case ToInteger(ingredientCode.code)
      when 161 then 0 /* Acetaminophen */
      when 1191 then 0 /*	Aspirin */
      when 1223 then 0 /*	Atropine */
      when 1767 then 0 /*	Brompheniramine */
      when 1819 then ( /*	Buprenorphine */
        case
          when ToInteger(doseFormCode.code) = 316987 then 12600 /* Transdermal system */
          else 30 /* Tablet or Film (or Film in MCG) */
        end
      )
      when 1841 then 7 /*	Butorphanol */
      when 1886 then 0 /*	Caffeine */
      when 2101 then 0 /*	Carisoprodol */
      when 2354 then 0 /*	chlorcyclizine */
      when 2400 then 0 /*	Chlorpheniramine */
      when 2670 then 0.15 /*	Codeine */
      when 3423 then 4 /*	Hydromorphone */
      when 3498 then 0 /*	Diphenhydramine */
      when 4337 then ( /*	Fentanyl */
        case
          when ToInteger(doseFormCode.code) in { 970789, 317007, 316992 } then 130 /* Buccal Tablet, Sublingual Tablet, Oral Lozenge */
          when ToInteger(doseFormCode.code) = 858080 then 180 /* Buccal Film */
          when ToInteger(doseFormCode.code) in { 126542, 346163, 1797831 } then 160 /* Nasal Spray, Mucosal Spray, Metered Dose Nasal Spray */
          when ToInteger(doseFormCode.code) = 316987 then 7200 /* Transdermal system */
          else Message(null, true, 'OMTKLogic.GetConversionFactor.UnknownDoseForm', ErrorLevel, 'Unknown dose form code ' & doseFormCode.code)
        end
      )
      when 5032 then 0 /*	Guaifenesin */
      when 5489 then 1 /*	Hydrocodone */
      when 5640 then 0 /*	Ibuprofen */
      when 6102 then 0 /*	Kaolin */
      when 6378 then 11 /*	Levorphanol (NOTE: Given as Levorphanol tartrate in the CDC conversion table) */
      when 6754 then 0.1 /*	Meperidine */
      when 6813 then ( /*	Methadone */
        case
          when dailyDose.value between 1 and 20 then 4
          when dailyDose.value between 21 and 40 then 8
          when dailyDose.value between 41 and 60 then 10
          when dailyDose.value >= 61 then 12
          else Message(null, dailyDose.value < 1, 'OMTKLogic.GetConversionFactor.DailyDoseLessThanOne', ErrorLevel, 'Daily dose less than 1')
        end
      )
      when 7052 then 1 /*	Morphine */
      when 7242 then 0 /*	Naloxone */
      when 7243 then 0 /*	Naltrexone */
      when 7676 then 1 /* Opium */
      when 7804 then 1.5 /*	Oxycodone */
      when 7814 then 3 /*	Oxymorphone */
      when 8001 then 0.37 /*	Pentazocine */
      when 8163 then 0 /*	Phenylephrine */
      when 8175 then 0 /*	Phenylpropanolamine */
      when 8745 then 0 /*	Promethazine */
      when 8896 then 0 /*	Pseudoephedrine */
      when 9009 then 0 /*	Pyrilamine */
      when 10689 then 0.1 /*	Tramadol */
      when 10849 then 0 /*	Triprolidine */
      when 19759 then 0 /*	bromodiphenhydramine */
      when 19860 then 0 /*	butalbital */
      when 22696 then 0 /*	dexbrompheniramine */
      when 22697 then 0 /*	dexchlorpheniramine */
      when 23088 then 0.25 /*	dihydrocodeine */
      when 27084 then 0 /*	homatropine */
      when 35780 then 0 /*	ropivacaine */
      when 237005 then 8 /*	Levomethadyl (NOTE: given as Levomethadyl acetate in the CDC conversion table) */
      when 636827 then 0 /*	guaiacolsulfonate */
      when 787390 then 0.4 /*	tapentadol */
      else Message(null, true, 'OMTKLogic.GetConversionFactor.UnknownIngredientCode', ErrorLevel, 'Unknown ingredient code: ' & ingredientCode.code)
    end
  )

define ConversionFactorSupplement:
  singleton from (
    [CodeSystem] C
      where C.supplements.value = 'http://www.nlm.nih.gov/research/umls/rxnorm'
      and (
        C.name.value = ConversionFactorSupplementName
          or exists (
            C.useContext UC
              where UC.code ~ "Task Usage Context"
                and UC.value ~ "MME Calculation"
          )
      )
  )

/*
Converts a range value boundary to a Decimal (or null for a wildcard)
*/
define function ToRangeValue(value System.String):
  if value = '*' then null else ToDecimal(value)

/*
Converts the property value of a dose-range-conversion-factor property to the dose range
<low-value>-<high-value>:<conversion-factor>
Note that low-value and/or high-value may be wildcards '*'
*/
define function ToDoseRange(propertyValue FHIR.string):
  ({ : }) X
    let rangeValues: Split(Split(propertyValue.value, ':')[0], '-')
    return Interval[ToRangeValue(rangeValues[0]), ToRangeValue(rangeValues[1])]

/*
Converts the property value of a dose-range-conversion-factor property to the conversion factor
<low-value>-<high-value>:<conversion-factor>
*/
define function ToDoseRangeConversionFactor(propertyValue FHIR.string):
  ToDecimal(Split(propertyValue.value, ':')[1])

/*
Converts the property value of a dose-form-conversion-factor property to the dose form
<dose-form-code>:<conversion-factor>
*/
define function ToDoseForm(propertyValue FHIR.string):
  Split(propertyValue.value, ':')[0]

/*
Converts the property value of a dose-form-conversion-factor property to the conversion factor
<dose-form-code>:<conversion-factor>
*/
define function ToDoseFormConversionFactor(propertyValue FHIR.string):
  ToDecimal(Split(propertyValue.value, ':')[1])

/*
Returns the conversion factor for a conversion-factor property
NOTE: These functions are used to avoid use of _is_ and _as_
*/
define function ToConversionFactor(propertyValue FHIR.decimal):
  propertyValue.value

/*
Looks up the conversion factor for the given ingredient, daily dose, and dose form from the
configured conversion factor supplement, if available.

If no conversion factor supplement is configured, returns null
*/
define function LookupConversionFactor(ingredientCode System.Code, dailyDose System.Quantity, doseFormCode System.Code):
  ConversionFactorSupplement CFS
    let
      ingredientConcept: singleton from (CFS."concept" C where C.code.value = ingredientCode.code),
      conversionFactor: singleton from (ingredientConcept.property P where P.code.value = 'conversion-factor'),
      doseFormConversionFactors: (ingredientConcept.property P where P.code.value = 'dose-form-conversion-factor'),
      doseRangeConversionFactors: (ingredientConcept.property P where P.code.value = 'dose-range-conversion-factor')
    return
      Coalesce
      (
        case
          when exists (doseRangeConversionFactors) then
            singleton from (
              doseRangeConversionFactors DRCF
                where dailyDose.value in ToDoseRange(DRCF.value)
                return ToDoseRangeConversionFactor(DRCF.value)
            )
          when exists (doseFormConversionFactors) then
            singleton from (
              doseFormConversionFactors DFCF
                where doseFormCode.code = ToDoseForm(DFCF.value)
                return ToDoseFormConversionFactor(DFCF.value)
            )
          else null
        end,
        ToConversionFactor(conversionFactor.value)
      )
