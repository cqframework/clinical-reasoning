library HTNAmbulatoryVisits version '1.0.1'

using FHIR version '4.0.1'

include DQMHypertensionFHIRHelpers version '1.0.1' called FHIRHelpers
include DQMHypertensionFHIRCommon version '1.0.1' called FC
include HTNConcepts version '1.0.1' called Concepts

codesystem "EncounterStatusCodes": 'http://hl7.org/fhir/encounter-status'

code "EncounterStatusFinished": 'finished' from "EncounterStatusCodes"

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Qualifying Encounters":
  (
	[Encounter: Concepts."Office Visit"]
	  union [Encounter: Concepts."Annual Wellness Visit"]
	  union [Encounter: Concepts."Preventive Care Services-Initial Office Visit, 18 and Up"]
	  union [Encounter: Concepts."Preventive Care Services - Established Office Visit, 18 and Up"]
	  union [Encounter: Concepts."Home Healthcare Services"]
  ) ValidEncounter
	  where ValidEncounter.status ~ "EncounterStatusFinished".code
		and FC.ToInterval(ValidEncounter.period) during "Measurement Period"
