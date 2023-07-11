library AmbulatoryVisits version '1.0.1'

using FHIR version '4.0.1'

include DQMDiabetesFHIRHelpers version '1.0.1' called FHIRHelpers
include FHIRCommon version '1.0.1' called FC
include DMConcepts version '1.0.1' called DC

codesystem "EncounterStatusCodes": 'http://hl7.org/fhir/encounter-status'

code "EncounterStatusFinished": 'finished' from "EncounterStatusCodes"

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Qualifying Encounters":
  (
	[Encounter: DC."Office Visit"]
	  union [Encounter: DC."Annual Wellness Visit"]
	  union [Encounter: DC."Preventive Care Services-Initial Office Visit, 18 and Up"]
	  union [Encounter: DC."Preventive Care Services - Established Office Visit, 18 and Up"]
	  union [Encounter: DC."Home Healthcare Services"]
  ) ValidEncounter
	  where ValidEncounter.status ~ "EncounterStatusFinished".code
		and FC.ToInterval(ValidEncounter.period) during "Measurement Period"
