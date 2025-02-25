library LibraryObservation version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

codesystem "SNOMED-CT": 'http://snomed.info/sct'

code "Height": '1153637007' from "SNOMED-CT" display 'Body height'

context Patient

define PatientHeight:
    [Observation: "Height"] O