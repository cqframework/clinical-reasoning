library "CriteriaBasedStratifiersSimple"

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

define "initial population":
    [Encounter]

define "bad criteria stratifier":
  exists "in-progress encounters"

define "in-progress encounters":
    [Encounter] E
        where E.status='in-progress'