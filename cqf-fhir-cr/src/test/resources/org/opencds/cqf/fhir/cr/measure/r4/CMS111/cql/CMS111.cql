library CMS111 version '0.0.013'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.001' called FHIRHelpers
include SupplementalDataElementsFHIR4 version '2.0.000' called SDE
include MATGlobalCommonFunctionsFHIR4 version '6.0.000' called Global

valueset "Admit Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1111.164'
valueset "Decision to Admit to Hospital Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.295'
valueset "Emergency Department Evaluation": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1111.163'
valueset "Emergency Department Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292'
valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'
valueset "Ethnicity": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.114222.4.11.837'
valueset "Hospital Settings": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1111.126'
valueset "ONC Administrative Sex": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1'
valueset "Payer": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.114222.4.11.3591'
valueset "Psychiatric/Mental Health Diagnosis": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.299'
valueset "Race": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.114222.4.11.836'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2019-01-01T00:00:00.0, @2020-01-01T00:00:00.0)

context Patient

define "SDE Ethnicity":
  SDE."SDE Ethnicity"

define "SDE Payer":
  SDE."SDE Payer"

define "SDE Race":
  SDE."SDE Race"

define "SDE Sex":
  SDE."SDE Sex"

define "Initial Population":
  "ED Encounter with Decision to Admit"

define "Measure Population":
  "Initial Population"

define "Measure Population Exclusions":
  /* Exclude the most recent ED encounter (LastEDVisit) that occurred within an hour of the inpatient admission with ED admission source in "Hospital Setting" (any different facility- by location or CCN ) */
                Global."Inpatient Encounter" EncounterInpatient
                    where "LastEDEncounter"(EncounterInpatient).hospitalization.admitSource in "Hospital Settings"

define "Stratification 2":
  /* Patient encounters with a principal diagnosis of "Psychiatric/Mental Health Diagnosis */
                Global."Inpatient Encounter" EncounterInpatient
                    where   Global."PrincipalDiagnosis"(EncounterInpatient).code in "Psychiatric/Mental Health Diagnosis"

define "ED Encounter with Decision to Admit":
  /* Constrains the inpatient encounter to having an ED visit with a decision to admit (assessment or order) to inpatient and ED facility location period is not null */
                from
                  Global."Inpatient Encounter" EncounterInpatient
                  let LastEDVisit: LastEDEncounter(EncounterInpatient)
                  where (Global."Normalize Interval"("AdmitDecisionUsingAssessmentDuringLastEDBeforeDeparture"(EncounterInpatient).effective) starts during LastEDVisit.period
                        or "AdmitDecisionUsingEncounterOrderDuringLastEDandBeforeDeparture"(EncounterInpatient).authoredOn during LastEDVisit.period)
                        and exists ( LastEDVisit.location Location
                            where Global.GetLocation(Location.location).type in "Emergency Department Visit"
                                  and end of Location.period != maximum DateTime)

define "Stratification 1":
  /* Patient encounters without a principal diagnosis of "Psychiatric/Mental Health Diagnosis */
                 Global."Inpatient Encounter" EncounterInpatient
                 let Diag: Global."PrincipalDiagnosis"(EncounterInpatient)
                        where  IsNull(Diag) or not (Diag.code in "Psychiatric/Mental Health Diagnosis")

define function "LastEDEncounter"(EncounterInpatient Encounter ):
  /* The most recent (last) ED encounter that is within an hour of an inpatient encounter */
  Last(["Encounter":"Emergency Department Visit"] EDVisit
      where EDVisit.period ends 1 hour or less before or on start of EncounterInpatient.period
            and EDVisit.status='finished'
      sort by
      end of period ascending)

define function "MeasureObservation"(EncounterInpatient Encounter ):
  /* The duration from the Decision to Admit (order or assessment) to the departure from the Emergency Department */
  duration in minutes of Interval[ Coalesce( start of Global."Normalize Interval" ("AdmitDecisionUsingAssessmentDuringLastEDBeforeDeparture"(EncounterInpatient).effective),
                                            "AdmitDecisionUsingEncounterOrderDuringLastEDandBeforeDeparture"(EncounterInpatient).authoredOn),
                                   "EDDepartureTime"("LastEDEncounter"(EncounterInpatient))]

define function "AdmitDecisionUsingEncounterOrderDuringLastEDandBeforeDeparture"(EncounterInpatient Encounter ):
  /* Captures the decision to admit order and time that occured during the last ED visit */
  Last(["ServiceRequest": "Decision to Admit to Hospital Inpatient"] AdmitOrder
      let LastEDVisit: "LastEDEncounter"(EncounterInpatient)
      where AdmitOrder.authoredOn during LastEDVisit.period
        and AdmitOrder.authoredOn before or on "EDDepartureTime"(LastEDVisit)
        and AdmitOrder.intent in {'plan', 'order'}
      sort by FHIRHelpers.ToDateTime(authoredOn))

define function "EDDepartureTime"(Encounter Encounter ):
  /* The time the patient physically departed the Emergency Department */
  Last(Encounter.location Location
      where ((Global.GetLocation(Location.location))Place
      where exists(Place.type PlaceType
  where FHIRHelpers.ToConcept(PlaceType ) in "Emergency Department Visit")) is not null
        /* and (end of Location.period) != maximum DateTime */
      return
      end of Location.period
      sort ascending)

define function "AdmitDecisionUsingAssessmentDuringLastEDBeforeDeparture"(EncounterInpatient Encounter ):
  /* Captures the decision to admit assessment, time, and result that was performed during the last ED visit */
  Last(
    ["Observation": "Emergency Department Evaluation"] EDEvaluation
      let LastEDVisit: "LastEDEncounter"(EncounterInpatient)
      where Global."Normalize Interval"(EDEvaluation.effective) starts during LastEDVisit.period
     and FHIRHelpers.ToConcept(EDEvaluation.value as FHIR.CodeableConcept) in "Admit Inpatient"

            and EDEvaluation.status in {'final', 'amended', 'corrected'}
            and Global."Normalize Interval"(EDEvaluation.effective) starts before or on "EDDepartureTime"(LastEDVisit)
   sort by start of Global."Normalize Interval" (effective))
