library "CriteriaBasedStratifiersSimple"

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

define "initial population boolean":
    exists "initial population resource"

define "initial population resource":
    [Encounter]

define "bad criteria stratifier":
  exists "in-progress encounters resource"

define "in-progress encounters resource":
    [Encounter] E
        where E.status='in-progress'