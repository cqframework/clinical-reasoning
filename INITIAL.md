# Acceptance Criteria

* Define a new QuantityDef class that takes value, unit, system, and code similar to an R4 FHIR quantity
* Ensure this takes the place of R4 or DSTU3 quantity being returned from
  ContinuousVariableObservationConverter implementations
* Make ContinuousVariableObservationConverter able to convert from a QuantityDef to an R4 or DSTU3 Quantity as well
* Ensure that the base interface ContinuousVariableObservationConverter does as much of the work as possible, delegating only to its implementors for R4/DSTU3 specific logic
* Ensure that R4MeasureScorer and Dstu3MeasureScorer adjust for the presence of QuantityDefs instead of the Quantities that were passed before
* Ensure any aggregation logic outputs QuantityDefs instead of Quantities
* Use the appropriate ContinuousVariableObservationConverter instance to convert to FHIR Quantities at the very last minute before population the DSTU3 or R4 MeasureReports
* This should require only changes to low-level measure scorer unit tests and nothing else
