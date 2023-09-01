library TJCOverall_FHIR3 version '3.6.000'

using FHIR version '3.0.0'
include FHIRHelpers version '3.0.0' called FHIRHelpers
//TODO: MATGlobalCommonFunctions_FHIR version '2.0.000' need to be updated to 4.0
include MATGlobalCommonFunctions_FHIR3 version '4.0.000' called Global
include SupplementalDataElements_FHIR3 version '1.0.0' called SDE

codesystem "SNOMEDCT": 'http://snomed.info/sct/731000124108'
codesystem "LOINC": 'http://loinc.org'

valueset "Comfort Measures": 'http://cts.nlm.nih.gov/fhir/ValueSet/1.3.6.1.4.1.33895.1.3.0.45'
valueset "Discharge To Acute Care Facility": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.87'
valueset "Discharged to Health Care Facility for Hospice Care": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.207'
valueset "Discharged to Home for Hospice Care": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.209'
valueset "Emergency Department Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292'
valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'
valueset "Hemorrhagic Stroke": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.212'
valueset "Ischemic Stroke": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.247'
valueset "Left Against Medical Advice": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.308'
valueset "Non-Elective Inpatient Encounter": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.424'
valueset "Patient Expired": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.309'
valueset "Ticagrelor Therapy": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1110.39'
valueset "Observation Services": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1111.143'

code "Birth date": '21112-8' from "LOINC" display 'Birth date'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Intervention Comfort Measures":
	(["ProcedureRequest": "Comfort Measures"] P
		where P.intent = 'order')
		union
		(["Procedure": "Comfort Measures"] InterventionPerformed
		where InterventionPerformed.status = 'completed')

define "All Stroke Encounter":
	"Non Elective Inpatient Encounter" NonElectiveEncounter
    where Global.PrincipalDiagnosis(NonElectiveEncounter).code in "Hemorrhagic Stroke"
      or Global.PrincipalDiagnosis(NonElectiveEncounter).code in "Ischemic Stroke"

define "Ischemic Stroke Encounter":
	"Encounter with Principal Diagnosis and Age" EncounterWithAge
		where Global.PrincipalDiagnosis(EncounterWithAge).code in "Ischemic Stroke"

define "Non Elective Inpatient Encounter":
	["Encounter": "Non-Elective Inpatient Encounter"] NonElectiveEncounter
		where Global."LengthInDays"(NonElectiveEncounter.period)<= 120
			and NonElectiveEncounter.period ends during "Measurement Period"

define "Ischemic Stroke Encounters with Discharge Disposition":
	( ( "Ischemic Stroke Encounter" IschemicStrokeEncounter
				where IschemicStrokeEncounter.hospitalization.dischargeDisposition in "Discharge To Acute Care Facility"
					or IschemicStrokeEncounter.hospitalization.dischargeDisposition in "Left Against Medical Advice"
					or IschemicStrokeEncounter.hospitalization.dischargeDisposition in "Patient Expired"
					or IschemicStrokeEncounter.hospitalization.dischargeDisposition in "Discharged to Home for Hospice Care"
					or IschemicStrokeEncounter.hospitalization.dischargeDisposition in "Discharged to Health Care Facility for Hospice Care"
		)
	)

define "Comfort Measures during Hospitalization":
	"Ischemic Stroke Encounter" IschemicStrokeEncounter
		with "Intervention Comfort Measures" ComfortMeasure
			such that Coalesce(start of FHIRHelpers.ToInterval(ComfortMeasure.performed), ComfortMeasure.authoredOn)during Global."HospitalizationWithObservation"(IschemicStrokeEncounter)

define "Encounter with Principal Diagnosis and Age":
	"All Stroke Encounter" AllStrokeEncounter
		with ["Patient"] BirthDate
			such that Global."CalendarAgeInYearsAt"(FHIRHelpers.ToDate(BirthDate.birthDate), start of FHIRHelpers.ToInterval(AllStrokeEncounter.period)) >= 18

define function "CalendarDayOfOrDayAfter"(StartValue DateTime ):
	Interval(Global."ToDate"(StartValue), Global."ToDate"(StartValue + 2 days))
