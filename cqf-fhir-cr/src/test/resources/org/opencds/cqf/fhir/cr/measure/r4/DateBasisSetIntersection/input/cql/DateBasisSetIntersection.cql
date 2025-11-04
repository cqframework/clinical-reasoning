library DateBasisSetIntersection

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

define "Initial Population":
  { @2024-02-01, @2024-01-02 }

define "Denominator Scenario 1":
  { @2024-02-01, @2024-01-02 }

define "Numerator Scenario 1":
  { @2024-02-03, @2024-01-02 }
