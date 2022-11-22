library DependencyExample version '0.1.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

include fhir.cqf.common.FHIRCommon called FHIRCommon

context Patient

define Test: ["Observation"] O
  where FHIRCommon.ToInterval(O.effective) starts on or after day of @2013-01-01
