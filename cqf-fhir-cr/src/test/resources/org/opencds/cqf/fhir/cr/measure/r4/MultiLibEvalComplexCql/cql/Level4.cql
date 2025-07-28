library Level4

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

include Level5 called Level5

define "Encounter Profiles":
    Level5."Encounters" E
        where not (E.meta.profile[0] in { 'http://example.com/category3' })
    return E.meta.profile[0]
