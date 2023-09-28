library MATGlobalCommonFunctions_FHIR3 version '4.0.000'

/*
This example is a work in progress and should not be considered a final specification
or recommendation for guidance. This example will help guide and direct the process
of finding conventions and usage patterns that meet the needs of the various stakeholders
in the measure development community.
*/

using FHIR version '3.0.0'

include FHIRHelpers version '3.0.0' called FHIRHelpers

codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMEDCT": 'http://snomed.info/sct/731000124108'
codesystem "RoleCode": 'http://hl7.org/fhir/v3/RoleCode'
codesystem "Diagnosis Role": 'http://hl7.org/fhir/diagnosis-role'

valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'
valueset "Emergency Department Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292'
valueset "Observation Services": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1111.143'

code "Birthdate": '21112-8' from "LOINC" display 'Birth date'
code "Dead": '419099009' from "SNOMEDCT" display 'Dead'
code "ER": 'ER' from "RoleCode" display 'Emergency room'
code "ICU": 'ICU' from "RoleCode" display 'Intensive care unit'
code "Billing": 'billing' from "Diagnosis Role" display 'Billing'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Inpatient Encounter":
	[Encounter: "Encounter Inpatient"] EncounterInpatient
		where EncounterInpatient.status = 'finished'
		  and "LengthInDays"(EncounterInpatient.period) <= 120
			and EncounterInpatient.period ends during "Measurement Period"

define function "ToDate"(Value DateTime):
	DateTime(year from Value, month from Value, day from Value, 0, 0, 0, 0, 16)

define function "CalendarAgeInDaysAt"(BirthDateTime DateTime, AsOf DateTime):
	days between ToDate(BirthDateTime)and ToDate(AsOf)

define function "CalendarAgeInDays"(BirthDateTime DateTime):
	CalendarAgeInDaysAt(BirthDateTime, Today())

define function "CalendarAgeInMonthsAt"(BirthDateTime DateTime, AsOf DateTime):
	months between ToDate(BirthDateTime)and ToDate(AsOf)

define function "CalendarAgeInMonths"(BirthDateTime DateTime):
	CalendarAgeInMonthsAt(BirthDateTime, Today())

define function "CalendarAgeInYearsAt"(BirthDateTime DateTime, AsOf DateTime):
	years between ToDate(BirthDateTime)and ToDate(AsOf)

define function "CalendarAgeInYears"(BirthDateTime DateTime):
	CalendarAgeInYearsAt(BirthDateTime, Today())

define function "LengthInDays"(Value Interval<DateTime>):
	difference in days between start of Value and end of Value

define function "ED Visit"(TheEncounter FHIR.Encounter):
    singleton from (
        [Encounter: "Emergency Department Visit"] EDVisit
            where EDVisit.status = 'finished'
                and EDVisit.period ends 1 hour or less on or before start of FHIRHelpers.ToInterval(TheEncounter.period)
    )

define function "Hospitalization"(TheEncounter FHIR.Encounter):
	( "ED Visit"(TheEncounter) ) X
    return
        if X is null then TheEncounter.period
        else Interval[start of FHIRHelpers.ToInterval(X.period), end of FHIRHelpers.ToInterval(TheEncounter.period)]

define function "Hospitalization Locations"(TheEncounter FHIR.Encounter):
	( "ED Visit"(TheEncounter) ) EDEncounter
    return
        if EDEncounter is null then TheEncounter.location
        else flatten { EDEncounter.location, TheEncounter.location }

define function "Hospitalization Length of Stay"(TheEncounter FHIR.Encounter):
	LengthInDays("Hospitalization"(TheEncounter))

define function "Hospital Admission Time"(TheEncounter FHIR.Encounter):
	start of "Hospitalization"(TheEncounter)

define function "Hospital Discharge Time"(TheEncounter FHIR.Encounter):
	end of FHIRHelpers.ToInterval(TheEncounter.period)

define function "Hospital Arrival Time"(TheEncounter FHIR.Encounter):
	start of FHIRHelpers.ToInterval(First(
	    ( "Hospitalization Locations"(TheEncounter) ) HospitalLocation
			sort by start of FHIRHelpers.ToInterval(period)
	).period)

define function "HospitalizationWithObservation"(TheEncounter FHIR.Encounter):
	TheEncounter Visit
		let ObsVisit: Last([Encounter: "Observation Services"] LastObs
				where LastObs.period ends 1 hour or less on or before start of Visit.period
				sort by end of period
			),
			VisitStart: Coalesce(start of ObsVisit.period, start of Visit.period),
			EDVisit: Last([Encounter: "Emergency Department Visit"] LastED
				where LastED.period ends 1 hour or less on or before VisitStart
				sort by end of period
			)
		return Interval[Coalesce(start of EDVisit.period, VisitStart), end of Visit.period]

define function "HospitalizationWithObservationLengthofStay"(Encounter FHIR.Encounter):
	"LengthInDays"("HospitalizationWithObservation"(Encounter))

// TODO - fix these (must fetch Location resources and compare id to reference)
/*define function "Hospital Departure Time"(TheEncounter FHIR.Encounter):
	end of FHIRHelpers.ToInterval(Last(
	    ( "Hospitalization Locations"(TheEncounter) ) HospitalLocation
			sort by start of FHIRHelpers.ToInterval(period)
	).period)

define function "Emergency Department Arrival Time"(TheEncounter FHIR.Encounter):
	start of FHIRHelpers.ToInterval((
	    singleton from (
	        ( "Hospitalization Locations"(TheEncounter) ) HospitalLocation
				where HospitalLocation.type ~ "ER"
		)
	).period)

define function "First Inpatient Intensive Care Unit"(TheEncounter FHIR.Encounter):
	First(
	    ( TheEncounter.location ) HospitalLocation
			where HospitalLocation.type ~ "ICU"
				and HospitalLocation.period during TheEncounter.period
			sort by start of FHIRHelpers.ToInterval(period)
	)*/

/*
*
*    CQFMeasures Common Logic
*
*/

define function "Normalize Onset"(onset Choice<FHIR.dateTime, FHIR.Age, FHIR.Period, FHIR.Range, FHIR.string>):
  if onset is FHIR.dateTime then
	  Interval[FHIRHelpers.ToDateTime(onset as FHIR.dateTime), FHIRHelpers.ToDateTime(onset as FHIR.dateTime)]
	else if onset is FHIR.Period then
	  FHIRHelpers.ToInterval(onset as FHIR.Period)
	else if onset is FHIR.Age then
	  Interval[FHIRHelpers.ToDate(Patient.birthDate) + FHIRHelpers.ToQuantity(onset as FHIR.Age),
		  FHIRHelpers.ToDate(Patient.birthDate) + FHIRHelpers.ToQuantity(onset as FHIR.Age) + 1 year)
	else null

define function "Normalize Abatement"(condition Condition):
	if condition.abatement is FHIR.dateTime then
	  Interval[FHIRHelpers.ToDateTime(condition.abatement as FHIR.dateTime), FHIRHelpers.ToDateTime(condition.abatement as FHIR.dateTime)]
	else if condition.abatement is FHIR.Period then
	  FHIRHelpers.ToInterval(condition.abatement as FHIR.Period)
	else if condition.abatement is FHIR.Age then
		Interval[FHIRHelpers.ToDate(Patient.birthDate) + FHIRHelpers.ToQuantity(condition.abatement as FHIR.Age),
			FHIRHelpers.ToDate(Patient.birthDate) + FHIRHelpers.ToQuantity(condition.abatement as FHIR.Age) + 1 year)
	else if condition.abatement is FHIR.boolean then
	  Interval[end of "Normalize Onset"(condition.onset), condition.assertedDate)
	else null

define function "GetPrevalencePeriod"(condition Condition):
  Interval[start of "Normalize Onset"(condition.onset), end of "Normalize Abatement"(condition))

define function "Normalize Interval"(choice Choice<FHIR.dateTime, FHIR.Period>):
  if choice is FHIR.dateTime then
	  Interval[FHIRHelpers.ToDateTime(choice as FHIR.dateTime), FHIRHelpers.ToDateTime(choice as FHIR.dateTime)]
	else
	  FHIRHelpers.ToInterval(choice as FHIR.Period)

define function "GetId"(uri String):
	Last(Split(uri, '/'))

define function "EncounterDiagnosis"(Encounter Encounter):
  Encounter.diagnosis D
    return singleton from ([Condition: id in {"GetId"(D.condition.reference)}])

// Returns the condition that is specified as the principal diagnosis for the encounter
define function "PrincipalDiagnosis"(Encounter Encounter):
	(singleton from (Encounter.diagnosis D where D.role ~ ToConcept("Billing") and D.rank = 1)) PD
	//"GetId"(uri String): returns the last String of a Split by / List therefor cannot use in
		return singleton from ([Condition: id in {"GetId"(PD.condition.reference)}])

// Returns the location for the given location reference
define function GetLocation(reference Reference):
  singleton from (
    [Location: id in {GetId(reference.reference)}]
  )

/*
NOTE: Extensions are not the preferred approach, but are used as a way to access
content that is defined by extensions but not yet surfaced in the
CQL model info.
*/
define function "GetExtensions"(domainResource DomainResource, url String):
  domainResource.extension E
	  where E.url = ('http://hl7.org/fhir/us/qicore/StructureDefinition/' + url)
		return E

define function "GetExtension"(domainResource DomainResource, url String):
  singleton from "GetExtensions"(domainResource, url)

/*
NOTE: Extensions are not the preferred approach, but are used as a way to access
content that is defined by extensions but not yet surfaced in the
CQL model info.
*/
define function "GetBaseExtensions"(domainResource DomainResource, url String):
  domainResource.extension E
	  where E.url = ('http://hl7.org/fhir/StructureDefinition/' + url)
		return E

define function "GetBaseExtension"(domainResource DomainResource, url String):
  singleton from "GetBaseExtensions"(domainResource, url)

/*
NOTE: Provenance is not the preferred approach, this is provided only as an illustration
for what using Provenance could look like, and is not a tested pattern
*/
define function "GetProvenance"(resource Resource):
  singleton from ([Provenance: target in resource.id])

define function "GetMedicationCode"(request MedicationRequest):
  if request.medication is CodeableConcept then
	  request.medication as CodeableConcept
	else
	  (singleton from ([Medication: id in GetId((request.medication as Reference).reference)])).code
