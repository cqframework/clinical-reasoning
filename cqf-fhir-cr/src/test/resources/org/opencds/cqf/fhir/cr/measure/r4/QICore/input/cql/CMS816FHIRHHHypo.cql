library CMS816FHIRHHHypo version '1.0.000'

using QICore version '6.0.0'

include SupplementalDataElements version '5.1.000' called SDE
include CQMCommon version '4.1.000' called CQMCommon
include QICoreCommon version '4.0.000' called QICoreCommon
include FHIRHelpers version '4.4.000' called FHIRHelpers

valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'
valueset "Glucose Lab Test Mass Per Volume": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1248.34'
valueset "Hypoglycemics Severe Hypoglycemia": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1196.393'
valueset "Positive Pregnancy Status": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1099.24'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2026-01-01T00:00:00.000Z, @2027-01-01T00:00:00.000Z)

context Patient

define "Denominator":
  "Initial Population"

define "Encounter With Severe Hypoglycemic Harm Event":
  from
    "Denominator" InpatientHospitalization,
    "Severe Hypoglycemic Harm Event" HypoglycemicEvent
    let GlucoseTestTime: HypoglycemicEvent.effective.earliest ( ),
    HospitalizationInterval: InpatientHospitalization.hospitalizationWithObservation ( )
    where GlucoseTestTime during HospitalizationInterval
    return InpatientHospitalization

define "Glucose Test With Result Less Than 40":
  from
    "Denominator" InpatientHospitalization,
    "Hypoglycemic Medication Administration" HypoglycemicMedication,
    ["LaboratoryResultObservation": "Glucose Lab Test Mass Per Volume"] GlucoseTest
    let HospitalizationInterval: InpatientHospitalization.hospitalizationWithObservation ( ),
    HypoglycemicMedicationStart: HypoglycemicMedication.effective.toInterval ( ),
    GlucoseTestTime: GlucoseTest.effective.earliest ( )
    where GlucoseTestTime during HospitalizationInterval
      and GlucoseTest.status in { 'final', 'amended', 'corrected' }
      and GlucoseTest.value < 40 'mg/dL'
      and HypoglycemicMedicationStart starts 24 hours or less before or on GlucoseTestTime
    return GlucoseTest

define "Hypoglycemic Medication Administration":
  ["MedicationAdministration": "Hypoglycemics Severe Hypoglycemia"] HypoMedication
    where HypoMedication.status = 'completed'

define "Initial Population":
  "Encounter With Hypoglycemic Medication Administration"

define "Numerator":
  "Encounter With Severe Hypoglycemic Harm Event"

define "Qualifying Encounter":
  ["Encounter": "Encounter Inpatient"] InpatientEncounter
    where AgeInYearsAt(date from start of InpatientEncounter.period) >= 18
      and InpatientEncounter.period ends during day of "Measurement Period"
      and InpatientEncounter.status = 'finished'

define "SDE Ethnicity":
  SDE."SDE Ethnicity"

define "SDE Payer":
  SDE."SDE Payer"

define "SDE Race":
  SDE."SDE Race"

define "SDE Sex":
  SDE."SDE Sex"

define "Encounter With Hypoglycemic Medication Administration":
  "Qualifying Encounter" InpatientHospitalization
    with "Hypoglycemic Medication Administration" HypoglycemicMedication
      such that HypoglycemicMedication.effective.toInterval ( ) starts during InpatientHospitalization.hospitalizationWithObservation ( )

define "Low Glucose Test Followed By Glucose Test Result Greater Than 80":
  from
    "Denominator" InpatientHospitalization,
    "Glucose Test With Result Less Than 40" LowGlucoseTest,
    ["LaboratoryResultObservation": "Glucose Lab Test Mass Per Volume"] FollowupGlucoseTest
    let GlucoseTestTime: LowGlucoseTest.effective.earliest ( ),
    FollowupGlucoseTestTime: FollowupGlucoseTest.effective.earliest ( )
    where FollowupGlucoseTestTime 5 minutes or less after GlucoseTestTime
      and GlucoseTestTime during InpatientHospitalization.hospitalizationWithObservation ( )
      and FollowupGlucoseTestTime during InpatientHospitalization.hospitalizationWithObservation ( )
      and FollowupGlucoseTest.id !~ LowGlucoseTest.id
      and FollowupGlucoseTest.status in { 'final', 'amended', 'corrected' }
      and FollowupGlucoseTest.value > 80 'mg/dL'
    return LowGlucoseTest

define "Severe Hypoglycemic Harm Event":
  "Glucose Test With Result Less Than 40" LowGlucoseTest
    where not ( LowGlucoseTest.id in "Low Glucose Test Followed By Glucose Test Result Greater Than 80".id )