library SeenPatients version '0.1.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.001'
include SupplementalDataElementsFHIR4 version '2.0.000' called SDE
include SurveillanceDataElementsFHIR4 version '1.0.000' called SurveillanceDataElements

parameter "Measurement Period" Interval<DateTime> default Interval[@2019-01-01T00:00:00.0-06:00, @2019-12-31T23:59:59.999-06:00]

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

define "SDE State of Residence":
  SurveillanceDataElements."State of Residence"

define "SDE Postal Code of Residence":
  SurveillanceDataElements."Postal Code of Residence"

define "SDE Food Insecurity Risk Status":
  SurveillanceDataElements."Food Insecurity Risk Status"

define "Initial Population":
  exists (
    [Encounter] E
      where E.period ends during "Measurement Period"
  )
