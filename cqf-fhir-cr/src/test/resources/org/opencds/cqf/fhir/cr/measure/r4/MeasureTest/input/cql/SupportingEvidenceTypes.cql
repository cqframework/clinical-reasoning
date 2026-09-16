library SupportingEvidenceTypes

using FHIR version '4.0.1'

codesystem "Example CodeSystem": 'http://example.org/CodeSystem/example' version '2.1'
valueset "Example ValueSet": 'http://example.org/ValueSet/example-conditions'
valueset "Versioned ValueSet": 'http://example.org/ValueSet/example-conditions-versioned' version '1.0'

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

context Patient

define "Initial Population":
  true

// One define per CQL System type, values chosen to make the serialized form unambiguous.

define "Boolean Value":
  true

define "Integer Value":
  31

define "Long Value":
  31L

define "Decimal Value":
  31.31

define "String Value":
  'string test'

define "Date Value":
  @2026-01-01

define "DateTime Value":
  @2026-12-30T23:59:59

define "Time Value":
  @T14:30:00

define "Quantity Value":
  31.5 'mg'

define "Ratio Value":
  1 'mg' : 2 'mg'

define "Code Value":
  Code { code: 'M', system: 'http://hl7.org/fhir/v3/AdministrativeGender', display: 'Male' }

define "Concept Value":
  Concept {
    codes: { Code { code: 'M', system: 'http://hl7.org/fhir/v3/AdministrativeGender', display: 'Male' } },
    display: 'Male concept'
  }

define "Interval Date Value":
  Interval[@2024-01-01, @2024-12-31]

define "Interval DateTime Value":
  Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

define "Interval Integer Value":
  Interval[1, 10]

define "Interval Quantity Value":
  Interval[1 'mg', 10 'mg']

define "Tuple Value":
  Tuple { number: 31, text: 'string test', nestedDates: { @2024-01-01, @2024-01-02 } }

define "List Integer Value":
  { 31, 88, 11 }

define "List Time Value":
  { @T08:00:00, @T14:30:00 }

define "Empty List Value":
  {}

define "Null Value":
  null

define "ValueSet Value":
  "Example ValueSet"

define "Versioned ValueSet Value":
  "Versioned ValueSet"

// The engine's EvaluationVisitor has no visitCodeSystemRef, so a CodeSystem-typed define
// evaluates to null and surfaces as a data-absent marker rather than a canonical reference.
define "CodeSystem Value":
  "Example CodeSystem"

// Resource-valued expressions, surfaced as reference strings.

define "Resource List Value":
  [Patient]

define "Single Resource Value":
  First([Patient])
