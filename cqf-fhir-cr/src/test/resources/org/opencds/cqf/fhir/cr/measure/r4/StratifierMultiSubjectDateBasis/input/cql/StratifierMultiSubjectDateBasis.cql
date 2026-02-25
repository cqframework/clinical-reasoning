library StratifierMultiSubjectDateBasis

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

// Date-basis initial population: returns 5 dates per patient
// All patients return the same 5 dates, which is the key to testing the deduplication bug
define "Initial Population":
  { @2024-01-01, @2024-01-02, @2024-01-03, @2024-01-04, @2024-01-05 }

// Function stratifier that takes a date as input and returns a constant
// This creates a NON_SUBJECT_VALUE stratifier where all dates map to the same stratum
define function "Constant Stratifier"(stratificationDate Date):
    'all-dates'

// Function stratifier that returns the month of the date
// This creates multiple strata based on the month (all January in our test data)
define function "Month Stratifier"(stratificationDate Date):
    month from stratificationDate
