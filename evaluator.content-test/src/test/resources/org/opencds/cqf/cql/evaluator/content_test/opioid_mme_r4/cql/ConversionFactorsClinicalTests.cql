library ConversionFactorsClinicalTests version '3.0.0'

using FHIR version '4.0.1'

include ConversionFactors version '3.0.0'
include OMTKLogic version '3.0.0'

// Ingredient Codes
code acetaminophen: '161' from OMTKLogic.RXNORM display 'acetaminophen'
code aspirin: '1191' from OMTKLogic.RXNORM display 'aspirin'
code atropine: '1223' from OMTKLogic.RXNORM display 'atropine'
code bromodiphenhydramine: '19759' from OMTKLogic.RXNORM display 'bromodiphenhydramine'
code brompheniramine: '1767' from OMTKLogic.RXNORM display 'brompheniramine'
code buprenorphine: '1819' from OMTKLogic.RXNORM display 'buprenorphine'
code butalbital: '19860' from OMTKLogic.RXNORM display 'butalbital'
code butorphanol: '1841' from OMTKLogic.RXNORM display 'butorphanol'
code caffeine: '1886' from OMTKLogic.RXNORM display 'caffeine'
code carisoprodol: '2101' from OMTKLogic.RXNORM display 'carisoprodol'
code chlorcyclizine: '2354' from OMTKLogic.RXNORM display 'chlorcyclizine'
code chlorpheniramine: '2400' from OMTKLogic.RXNORM display 'chlorpheniramine'
code codeine: '2670' from OMTKLogic.RXNORM display 'codeine'
code dexbrompheniramine: '22696' from OMTKLogic.RXNORM display 'dexbrompheniramine'
code dexchlorpheniramine: '22697' from OMTKLogic.RXNORM display 'dexchlorpheniramine'
code dihydrocodeine: '23088' from OMTKLogic.RXNORM display 'dihydrocodeine'
code diphenhydramine: '3498' from OMTKLogic.RXNORM display 'diphenhydramine'
code fentanyl: '4337' from OMTKLogic.RXNORM display 'fentanyl'
code guaiacolsulfonate: '636827' from OMTKLogic.RXNORM display 'guaiacolsulfonate'
code guaifenesin: '5032' from OMTKLogic.RXNORM display 'guaifenesin'
code homatropine: '27084' from OMTKLogic.RXNORM display 'homatropine'
code hydrocodone: '5489' from OMTKLogic.RXNORM display 'hydrocodone'
code hydromorphone: '3423' from OMTKLogic.RXNORM display 'hydromorphone'
code ibuprofen: '5640' from OMTKLogic.RXNORM display 'ibuprofen'
code kaolin: '6102' from OMTKLogic.RXNORM display 'kaolin'
code levomethadyl: '237005' from OMTKLogic.RXNORM display 'levomethadyl'
code levorphanol: '6378' from OMTKLogic.RXNORM display 'levorphanol'
code meperidine: '6754' from OMTKLogic.RXNORM display 'levorphanol'
code methadone: '6813' from OMTKLogic.RXNORM display 'methadone'
code morphine: '7052' from OMTKLogic.RXNORM display 'morphine'
code naloxone: '7242' from OMTKLogic.RXNORM display 'naloxone'
code naltrexone: '7243' from OMTKLogic.RXNORM display 'naltrexone'
code oxycodone: '7804' from OMTKLogic.RXNORM display 'oxycodone'
code oxymorphone: '7814' from OMTKLogic.RXNORM display 'oxymorphone'
code pentazocine: '8001' from OMTKLogic.RXNORM display 'pentazocine'
code phenylephrine: '8163' from OMTKLogic.RXNORM display 'phenylephrine'
code phenylpropanolamine: '8175' from OMTKLogic.RXNORM display 'phenylpropanolamine'
code promethazine: '8745' from OMTKLogic.RXNORM display 'promethazine'
code pseudoephedrine: '8896' from OMTKLogic.RXNORM display 'pseudoephedrine'
code pyrilamine: '9009' from OMTKLogic.RXNORM display 'pyrilamine'
code ropivacaine: '35780' from OMTKLogic.RXNORM display 'ropivacaine'
code tapentadol: '787390' from OMTKLogic.RXNORM display 'tapentadol'
code tramadol: '10689' from OMTKLogic.RXNORM display 'tramadol'
code triprolidine: '10849' from OMTKLogic.RXNORM display 'triprolidine'

// Dose Form Codes
code "Transdermal System": '316987' from OMTKLogic.RXNORM display 'Transdermal System'
code "Oral Tablet": '317541' from OMTKLogic.RXNORM display 'Oral Tablet'
code "Buccal Tablet": '970789' from OMTKLogic.RXNORM display 'Buccal Tablet'
code "Chewable Tablet": '91058' from OMTKLogic.RXNORM display 'Chewable Tablet'
code "Sublingual Tablet": '317007' from OMTKLogic.RXNORM display 'Sublingual Tablet'
code "Oral Lozenge": '316992' from OMTKLogic.RXNORM display 'Oral Lozenge'
code "Oral Capsule": '316965' from OMTKLogic.RXNORM display 'Oral Capsule'
code "Buccal Film": '858080' from OMTKLogic.RXNORM display 'Buccal Film'
code "Nasal Spray": '126542' from OMTKLogic.RXNORM display 'Nasal Spray'
code "Mucosal Spray": '346163' from OMTKLogic.RXNORM display 'Mucosal Spray'
code "Metered Dose Nasal Spray": '1797831' from OMTKLogic.RXNORM display 'Metered Dose Nasal Spray'
code "Topical Ointment": '316985' from OMTKLogic.RXNORM display 'Topical Ointment'
code "Extended Release Oral Capsule": '316943' from OMTKLogic.RXNORM display 'Extended Release Oral Capsule'
code "Extended Release Oral Tablet": '316946' from OMTKLogic.RXNORM display 'Extended Release Oral Tablet'
code "Oral Solution": '316968' from OMTKLogic.RXNORM display 'Oral Solution'
code "Injection": '1649574' from OMTKLogic.RXNORM display 'Injection'
code "Prefilled Syringe": '721656' from OMTKLogic.RXNORM display 'Prefilled Syringe'

context Patient

//define ConversionFactorSupplement:
define TestConversionFactorSupplement: ConversionFactors.ConversionFactorSupplement.name.value = 'CDCMMEClinicalConversionFactors'

/*
Looks up the conversion factor for the given ingredient, daily dose, and dose form from the
configured conversion factor supplement, if available.

If no conversion factor supplement is configured, returns null
*/
//define function LookupConversionFactor(ingredientCode System.Code, dailyDose System.Quantity, doseFormCode System.Code):

// With CDCMMEClinicalConversionFactors:
define TestLookupCDCClinicalConversionFactor_2670: ConversionFactors.LookupConversionFactor(codeine, 12.5 'mg', "Oral Tablet") = 0.15
define TestLookupCDCClinicalConversionFactor_4337_TransdermalSystem: ConversionFactors.LookupConversionFactor(fentanyl, 0.012 'mg/h', "Transdermal System") = 7200
define TestLookupCDCClinicalConversionFactor_4337_NasalSpray: ConversionFactors.LookupConversionFactor(fentanyl, 0.1 'mg/{actuat}', "Mucosal Spray") is null
define TestLookupCDCClinicalConversionFactor_4337_BuccalFilm: ConversionFactors.LookupConversionFactor(fentanyl, 1.2 'mg', "Buccal Film") is null
define TestLookupCDCClinicalConversionFactor_6813: ConversionFactors.LookupConversionFactor(methadone, 10 'mg', "Oral Tablet") = 4
define TestLookupCDCClinicalConversionFactor_6813_30: ConversionFactors.LookupConversionFactor(methadone, 30 'mg', "Oral Tablet") = 8
define TestLookupCDCClinicalConversionFactor_6813_50: ConversionFactors.LookupConversionFactor(methadone, 50 'mg', "Oral Tablet") = 10
define TestLookupCDCClinicalConversionFactor_6813_70: ConversionFactors.LookupConversionFactor(methadone, 70 'mg', "Oral Tablet") = 12
define TestLookupCDCClinicalConversionFactor_Invalid: ConversionFactors.LookupConversionFactor(aspirin, 200 'mg', "Oral Tablet") is null
