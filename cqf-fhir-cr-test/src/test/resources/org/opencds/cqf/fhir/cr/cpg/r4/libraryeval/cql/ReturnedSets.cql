library ReturnedSets

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime>

context Patient

 define "Related Encounters":
   from
    "Encounters" Encounter,
     "Conditions" Condition
     return { condition: Condition, encounter: Encounter }

define "Encounters" :
    [Encounter] Encounter

define "Conditions" :
    [Condition] Condition
