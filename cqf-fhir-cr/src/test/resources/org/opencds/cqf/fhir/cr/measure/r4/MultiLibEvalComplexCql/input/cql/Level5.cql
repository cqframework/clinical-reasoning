library Level5

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

context Patient

//// Level 5 > { 'cat1', cat2', 'cat3' }
//define "Encounters":
//    [Encounter] E
//        where E.meta.profile[0] in { 'http://example.com/category1', 'http://example.com/category2', 'http://example.com/category3' }
// Level 5 > { 'arrived', 'finished', 'in-progress', 'planned', 'triaged' }
define "Encounters":
    [Encounter] E
        where E.status.value in { 'arrived', 'planned', 'triaged', 'finished', 'in-progress' }
