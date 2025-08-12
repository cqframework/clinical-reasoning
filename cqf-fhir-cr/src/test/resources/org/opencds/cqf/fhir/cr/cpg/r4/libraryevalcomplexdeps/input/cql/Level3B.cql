library Level3B

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

include Level4 called Level4

define "Encounter Profiles":
    Level4."Encounter Profiles" P
        where not (P in { 'http://example.com/category2' })
