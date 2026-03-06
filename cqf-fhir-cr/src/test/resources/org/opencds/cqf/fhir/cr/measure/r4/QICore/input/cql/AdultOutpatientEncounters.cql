library AdultOutpatientEncounters version '4.19.000'

using QICore version '6.0.0'

include FHIRHelpers version '4.4.000' called FHIRHelpers
include QICoreCommon version '4.0.000' called QICoreCommon
include Status version '1.15.000' called Status

valueset "Annual Wellness Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.526.3.1240' 
valueset "Home Healthcare Services": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1016' 
valueset "Office Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001' 
valueset "Virtual Encounter": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1089' 
valueset "Preventive Care Services Established Office Visit, 18 and Up": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1025' 
valueset "Preventive Care Services Initial Office Visit, 18 and Up": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1023' 
valueset "Telephone Visits": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1080' 

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2026-01-01T00:00:00.000Z, @2027-01-01T00:00:00.000Z)

context Patient

define "Qualifying Encounters":
  ( ( [Encounter: "Office Visit"]
    union [Encounter: "Annual Wellness Visit"]
    union [Encounter: "Preventive Care Services Established Office Visit, 18 and Up"]
    union [Encounter: "Preventive Care Services Initial Office Visit, 18 and Up"]
    union [Encounter: "Home Healthcare Services"]
    union [Encounter: "Virtual Encounter"]
    union [Encounter: "Telephone Visits"] ).isEncounterPerformed() ) ValidEncounter
    where ValidEncounter.period during day of "Measurement Period"