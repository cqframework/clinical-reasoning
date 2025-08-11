library libraryCommon

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

define "All Encounters":
    ["Encounter"]

define "finished encounters":
    ("All Encounters") e 
        where e.status = 'finished'