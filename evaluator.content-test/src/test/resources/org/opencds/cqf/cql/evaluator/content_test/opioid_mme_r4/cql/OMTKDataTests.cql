library OMTKDataTests version '3.0.0'

include OMTKData version '3.0.0'

define TestDoseForms:
  OMTKData.DrugIngredients DI
    where DI.ingredientCode = 4337
    return { doseFormCode: DI.doseFormCode, doseFormName: DI.doseFormName }

define TestUnits:
  OMTKData.DrugIngredients DI
    return DI.strengthUnit
