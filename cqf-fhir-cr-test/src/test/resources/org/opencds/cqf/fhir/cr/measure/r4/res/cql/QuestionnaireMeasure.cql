library QuestionnaireMeasure

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

include PHQ9Logic

parameter "Measurement Period" Interval<Date> default Interval[@2021-01-01, @2021-12-31]

context Patient

define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18

// Likely some condition or other relevant characteristic such as a depression diagnosis
define "Measure Population":
  true

/*
Measure observation is a function defined in terms of the types of members of the measure
population, Patient in this case. In a population evaluation scenario, the measure
observation is evaluated for every member of the population.

In an individual report scenario, however, the measure observation is evaluated for
the current patient. Note that the patient context is flowing through the call here
*/
define function "MeasureObservation"(patient Patient):
  PHQ9Logic.CalculateTotalScore
