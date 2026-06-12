library DuplicateTypeIntraSubject

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

// CDO-714: intra-subject duplicate values for a non-boolean population basis.
//
// Case 1: CQL primitive type (System.Date).
// Expression yields three CQL Date values with @2025-07-03 appearing twice.
// Expected count = 3, actual count today = 2 (CqlType dedup path collapses them).
define "Date Initial Population":
  { @2025-07-03, @2025-07-03, @2025-07-04 }

// Case 2: FHIR primitive type (IPrimitiveType<String> via Patient.address.line).
// The patient fixture has address[0].line = ["dup-line", "dup-line", "other-line"]
// so the expression yields three FHIR string primitives, with "dup-line" twice.
// Expected count = 3.
define "Fhir String Initial Population":
  Patient.address[0].line

// Case 3: CQL primitive type (System.Integer, represented as java.lang.Integer).
// Expression yields three integers with 42 appearing twice.
// java.lang.Integer is neither IBaseResource nor CqlType, so it falls through to default
// HashSet semantics, and Integer's value-based equals/hashCode dedups the two 42s.
// Expected count = 3, actual count today = 2.
define "Integer Initial Population":
  { 42, 42, 43 }

// Boolean basis comparison: one patient => count = 1, unchanged by the fix.
define "Boolean Initial Population":
  true
