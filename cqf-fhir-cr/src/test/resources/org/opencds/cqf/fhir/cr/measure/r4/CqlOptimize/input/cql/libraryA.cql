library libraryA

using FHIR version '4.0.1'

include libraryCommon called lc

context Patient

define "Initial Population":
    exists (lc."All Encounters")

