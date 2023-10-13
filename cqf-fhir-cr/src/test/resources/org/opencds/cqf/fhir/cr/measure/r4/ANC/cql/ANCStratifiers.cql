library ANCStratifiers

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'
include ANCDataElements called PatientData
// TODO: Report based on latest contact that occurred in the reporting period
include ANCContactDataElements called ContactData

codesystem "ISO-8601-Derived Periods": 'http://ohie.org/CodeSystem/iso-8601-derived-periods' // { 'P0Y--P1Y', 'P1Y--P5Y', ... }

valueset "Qualifying Reason for closing ANC record Choices": 'http://fhir.org/guides/who/anc-cds/ValueSet/qualifying-reason-for-closing-anc-record-choices'

// Age Groups
code "P10Y--P14Y": 'P10Y--P14Y' from "ISO-8601-Derived Periods" display '10-14 years'
code "P15Y--P19Y": 'P15Y--P19Y' from "ISO-8601-Derived Periods" display '15-19 years'
code "P20Y--P9999Y": 'P20Y--P9999Y' from "ISO-8601-Derived Periods" display '20+ years'

parameter "Measurement Period" Interval<Date> default Interval[@2020-01-01, @2020-12-31]

context Patient

/*
By Patient Age as of the start of the Measurement Period
Age (10-14, 15-19, 20+)
*/
define "By Age":
  case
    when AgeInYearsAt(start of "Measurement Period") in Interval[10, 14] then "P10Y--P14Y"
    when AgeInYearsAt(start of "Measurement Period") in Interval[15, 19] then "P15Y--P19Y"
    when AgeInYearsAt(start of "Measurement Period") >= 20 then "P20Y--P9999Y"
    else null
  end

/*
By Patient Education Level (none, don't know, primary, secondary, higher)
*/
define "By Education Level":
  Patient.extension E
    where E.url = 'http://fhir.org/guides/who/anc-cds/StructureDefinition/educationlevel'
    return E.value as CodeableConcept

/*
By Number of Contacts (1, 2-3, 4-8, 9+)
*/
define "By Number of Contacts":
  Count(PatientData."ANC contact number")

/*
By Trimester Category: 1st (GA ≤ 12 weeks); 2nd (GA > 12 weeks to ≤ 28 weeks); 3rd (GA > 28 weeks)
*/
define "By Trimester Category":
  case
    when ContactData."Gestational age" <= 12 weeks then 'GA ≤ 12 weeks'
    when ContactData."Gestational age" <= 28 weeks then 'GA > 12 weeks to ≤ 28 weeks'
    else 'GA > 28 weeks'
  end
