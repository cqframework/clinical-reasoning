library simpleCqlParameters

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]
// cql parameter that can pass in fhir id of an encounter
parameter practitionerParam String

parameter encounterParam String

context Patient

// boolean population results
// has matching encounter

define "Initial Population Boolean":
  exists "All Encounters"

define "Denominator Boolean":
    "Initial Population Boolean"

define "All Encounters":
     [Encounter] E

define "Matching Either Practitioner Or Encounter":
    exists ("Matching General Practitioner")
        or exists ("Matching Patient By Encounter")

define "Matching General Practitioner":
    [Patient] p
       where Last(Split(First(p.generalPractitioner.reference),'/')) = Last(Split(practitionerParam,'/'))

define "Matching Patient By Encounter":
    [Patient] p
        with [Encounter] e
            such that Last(Split(e.id,'')) = encounterParam
                and Last(Split(e.subject.reference, '/')) = Last(Split(p.id,'/'))
