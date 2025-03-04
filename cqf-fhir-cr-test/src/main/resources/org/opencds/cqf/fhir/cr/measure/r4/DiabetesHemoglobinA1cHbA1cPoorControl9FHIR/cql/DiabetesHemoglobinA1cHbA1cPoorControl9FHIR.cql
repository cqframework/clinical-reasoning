/*
FHIR Translation of CMS122: Diabetes: HemoglobinA1c Poor Control
*/
library DiabetesHemoglobinA1cHbA1cPoorControl9FHIR version '0.0.001'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.001' called FHIRHelpers
include SupplementalDataElementsFHIR4 version '2.0.000' called SDE
include MATGlobalCommonFunctionsFHIR4 version '6.0.000' called Global
include AdultOutpatientEncountersFHIR4 version '2.0.000' called AdultOutpatientEncounters
include HospiceFHIR4 version '2.0.000' called Hospice
include AdvancedIllnessandFrailtyExclusionECQMFHIR4 version '5.12.000' called Frailty
include SurveillanceDataElementsFHIR4 version '1.0.000' called SurveillanceDataElements

codesystem "LOINC": 'http://loinc.org'

valueset "Annual Wellness Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.526.3.1240'
valueset "Diabetes": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.103.12.1001'
valueset "Discharged to Health Care Facility for Hospice Care": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.207'
valueset "Discharged to Home for Hospice Care": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.209'
valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'
valueset "HbA1c Laboratory Test": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1013'
valueset "Home Healthcare Services": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1016'
valueset "Hospice care ambulatory": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1108.15'
valueset "Office Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001'
valueset "Preventive Care Services - Established Office Visit, 18 and Up": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1025'
valueset "Preventive Care Services-Initial Office Visit, 18 and Up": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1023'

code "Birth date": '21112-8' from "LOINC" display 'Birth date'

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

define "SDE Age":
  SurveillanceDataElements."Age"

define "SDE Age Group":
  SurveillanceDataElements."Age Group"

define "SDE State of Residence":
  SurveillanceDataElements."State of Residence"

define "SDE Postal Code of Residence":
  SurveillanceDataElements."Postal Code of Residence"

define "SDE Food Insecurity Risk Status":
  SurveillanceDataElements."Food Insecurity Risk Status"

define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") in Interval[18, 75)
    and exists AdultOutpatientEncounters."Qualifying Encounters"
    and exists (["Condition": "Diabetes"] Diabetes
      where Diabetes.clinicalStatus ~ Global."active"
        and Global."Prevalence Period"(Diabetes) overlaps "Measurement Period"
    )

define "Denominator":
  "Initial Population"

define "Numerator":
  "Has Most Recent HbA1c Without Result"
    or "Has Most Recent Elevated HbA1c"
    or "Has No Record Of HbA1c"

define "Has Most Recent Elevated HbA1c":
  "Most Recent HbA1c".value > 9 '%'

define "Has Most Recent HbA1c Without Result":
  "Most Recent HbA1c" is not null
    and "Most Recent HbA1c".value is null

define "Most Recent HbA1c":
  Last(["Observation": "HbA1c Laboratory Test"] RecentHbA1c
    where RecentHbA1c.status in {'final', 'amended', 'preliminary', 'corrected'}
      and end of Global."Normalize Interval"(RecentHbA1c.effective) during "Measurement Period"
    sort by end of Global."Normalize Interval"(effective)
  )

define "Has No Record Of HbA1c":
  not exists ( ["Observation": "HbA1c Laboratory Test"] NoHbA1c
    where ( NoHbA1c.status in {'final', 'amended', 'preliminary', 'corrected'}
      and end of Global."Normalize Interval"(NoHbA1c.effective) during "Measurement Period"
  ))

define "Denominator Exclusions":
  Hospice."Has Hospice"
    or Frailty."Advanced Illness and Frailty Exclusion Not Including Over Age 80"
    or (AgeInYearsAt(start of "Measurement Period") >= 65
      and Frailty."Long Term Care Periods Longer Than 90 Consecutive Days"
    )
