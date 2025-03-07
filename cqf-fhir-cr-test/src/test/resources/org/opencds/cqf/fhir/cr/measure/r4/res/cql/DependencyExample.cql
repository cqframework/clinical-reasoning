library DependencyExample version '0.1.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

include example.fhir.uv.myig.example called test

context Patient

define Test: ["Observation"] O
  where exists (O.category C
    where C ~ test."Laboratory Observation Category"
  )
