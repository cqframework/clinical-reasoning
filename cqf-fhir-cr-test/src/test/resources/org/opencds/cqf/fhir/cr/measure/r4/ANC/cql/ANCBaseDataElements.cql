/*
Additional required data elements that do not have a data dictionary entry
*/
library ANCBaseDataElements

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'
include ANCCommon called AC
include FHIRCommon called FC
include ANCBaseConcepts called BCx

context Patient

/*
define "Pregnant":
  "Pregnancy" is not null
    or "Pregnancy status" in BCx."Pregnancy status - pregnant"

define "Pregnancy status":
  FHIRHelpers.ToConcept(
    Last(
      [Observation: BCx."Pregnancy status"] O
        where O.status = 'final'
        sort by FHIRHelpers.ToDateTime(effective as FHIR.dateTime)
    ).value
  )
*/

define "Antenatal care case":
  ["EpisodeOfCare": type in BCx."Antenatal care case"]

define "Antenatal care contact":
  ["Encounter": type in BCx."Antenatal care contact"]

/*
@description: Most recent antenatal care case
*/
define "Pregnancy":
  First(
    "Antenatal care case" E
		  sort by start of period descending
	)

define "First trimester":
  Interval[start of "Pregnancy".period, start of "Pregnancy".period + 12 weeks]

define "Second trimester":
  Interval[start of "Pregnancy".period + 13 weeks, start of "Pregnancy".period + 24 weeks]

/*
@description: Third trimester (assuming GA 25+ weeks)
*/
define "Third trimester":
  Interval[start of "Pregnancy".period + 25 weeks, end of "Pregnancy".period]

/*
@description: The diagnosis of pregnancy listed as the chief-complaint diagnosis on the most recent antenatal care case
*/
define "Pregnancy condition":
  ["Condition": code in BCx."Pregnancy condition"] C
    with "Pregnancy".diagnosis Dx
      such that Dx.role = FC."CC"
        and C.id = Last(Split(Dx.condition.reference, '/'))

/*
@description: The patient's occupation
*/
define "Patient Occupation":
  [Patient] P
    return (AC.Extension(P, 'occupation')).value as CodeableConcept

/*
@description: The expected next contact date based on current visits and the next expected visit date
Next contact date logic:
1. If GA < 20 weeks, contacts should be every 8 weeks
2. If GA >= 20 weeks, contacts should be every 6 weeks
3. If GA >= 26 weeks, contacts should be every 4 weeks
4. If GA >= 34 weeks, contacts should be every 2 weeks
5. If GA would be 40 weeks prior to next contact date, schedule next contact for 40 weeks
6. Women with a first contact between 22 and 23 weeks have visits every 2 weeks after 30 weeks
7. Women with a first contact between 28 and 29 weeks have visits every 2 weeks after 32 weeks
8. Women with a first contact after 30 weeks have all visits every 2 weeks
*/
define "Next contact date":
  Today() + 2 weeks // TODO: Calculate based on gestational age and next expected visit
