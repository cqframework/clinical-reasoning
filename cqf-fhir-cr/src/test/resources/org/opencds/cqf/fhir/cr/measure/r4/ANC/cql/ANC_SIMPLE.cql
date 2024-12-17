library ANC_SIMPLE

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

include ANCStratifiers called Stratifiers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

// cql parameter that can pass in fhir id of an encounter
parameter practitionerParam String

context Patient

define "Matching General Practitioner":
    [Patient] p
       where Last(Split(First(p.generalPractitioner.reference),'/')) = Last(Split(practitionerParam,'/'))

/*
Disaggregation: Age (10-14, 15-19, 20+)
*/
define "Age Stratifier":
  Stratifiers."By Age"

