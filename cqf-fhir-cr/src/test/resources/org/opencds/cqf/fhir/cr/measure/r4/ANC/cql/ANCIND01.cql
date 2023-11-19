/*
Library ANC.IND.1
WHO Antenatal Care (ANC) Indicator 1
Percentage of pregnant women with first ANC contact in the first trimester (before 12 weeks of gestation)

Numerator: Number of pregnant women who had their first ANC contact before 12 weeks (facility level)
Numerator Computation: Count of women whose gestational age <= 12 weeks at the time of first contact
Denominator: Total number of antenatal clients with a first contact
Denominator Computation: Count of all women who had first contact within the last reporting period
Disaggregation:
Age (10-14, 15-19, 20+)
Education Level (none, don't know, primary, secondary, higher)
Reference:
WHO ANC monitoring framework (1) 

WHO–UNICEF guidance for RMNCAH programme managers on the analysis and use of health-care facility data (2) 

Attribution:
Typically facility and organization are 1..1, but there are cases where there are
multiple organizations reporting from the same facility. In these cases, reporter -> location is insufficient
*/
library ANCIND01

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

include ANCBaseConcepts called BCx
include ANCConcepts called Cx
include ANCBaseDataElements called BaseData
include ANCDataElements called PatientData
include ANCContactDataElements called ContactData
include ANCStratifiers called Stratifiers

parameter "Measurement Period" Interval<Date> default Interval[@2020-01-01, @2020-01-31]

context Patient

/*
Initial population: Antenatal clients
*/
define "Initial Population":
  exists (BaseData."Antenatal care case")

define "First antenatal care contact":
  BaseData."Antenatal care contact" C
    where C.reasonCode in Cx."First Antenatal Care Contact Choices"

define "Gestational age":
  ContactData."Gestational age"

/*
Numerator: Number of pregnant women who had their first ANC contact before 12 weeks (facility level)
Numerator Computation: Count of women whose gestational age <= 12 weeks at the time of first contact
*/
define Numerator:
  exists (
    "First antenatal care contact" C
      where C.period starts during "Measurement Period"
        and ContactData."Gestational age" <= 12 weeks
  )

/*
Denominator: Total number of antenatal clients with a first contact
Denominator Computation: Count of all women who had first contact within the last reporting period
*/
define Denominator:
  exists (
    "First antenatal care contact" C
      where C.period starts during "Measurement Period"
  )

/*
Disaggregation: Age (10-14, 15-19, 20+)
*/
define "Age Stratifier":
  Stratifiers."By Age"

/*
Disaggregation: Education Level (none, don't know, primary, secondary, higher)
*/
define "Education Level Stratifier":
  Stratifiers."By Education Level"

/*
Most recent encounter location
*/
define "Most Recent Encounter Location Reference":
  Last(
    [Encounter] E
      return First(E.location L return L.location)
      //sort by start of period
  )

/*
Location Code
*/
define "Location Code":
  singleton from (
    [Location] L
      let identifier: singleton from (L.identifier I where I.system = 'http://example.org/opencr-identifiers')
      where L.id = Last(Split("Most Recent Encounter Location Reference".reference, '/'))
      return CodeableConcept {
        coding: {
          Coding {
            code: code { value: identifier.value },
            system: identifier.system
          }
        }
      }
  )
