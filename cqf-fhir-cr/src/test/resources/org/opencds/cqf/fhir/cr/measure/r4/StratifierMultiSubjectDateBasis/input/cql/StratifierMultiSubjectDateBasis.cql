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

// --- HEDIS ENP-Reporting shape: single subject, 12 enrollment dates, Age + Product Line ---
// 12 enrollment "episode" dates for the member (2 in early Jan, 9 mid Jan, 1 late Jan).
define "Enrollment Initial Population":
    { @2026-01-01, @2026-01-02,
      @2026-01-03, @2026-01-04, @2026-01-05, @2026-01-06, @2026-01-07,
      @2026-01-08, @2026-01-09, @2026-01-10, @2026-01-11,
      @2026-01-12 }

// Age band is constant across the member's enrollment dates.
define function "Age Stratifier"(enrollmentDate Date):
    '18-19'

// Stubbed member-months input (one member-month per enrollment date), standing in for the
// [Coverage]-derived "All reported member months info" so we can exercise the REAL stratifier
// function without the full ENP eligibility chain. Payer is a System.Code, matching the real type.
define "All reported member months info":
    { Tuple { monthlyEnrollmentDate: @2026-01-01, payer: System.Code { code: 'MMO' } },
      Tuple { monthlyEnrollmentDate: @2026-01-02, payer: System.Code { code: 'MMO' } },
      Tuple { monthlyEnrollmentDate: @2026-01-03, payer: System.Code { code: 'MCD' } },
      Tuple { monthlyEnrollmentDate: @2026-01-04, payer: System.Code { code: 'MCD' } },
      Tuple { monthlyEnrollmentDate: @2026-01-05, payer: System.Code { code: 'MCD' } },
      Tuple { monthlyEnrollmentDate: @2026-01-06, payer: System.Code { code: 'MCD' } },
      Tuple { monthlyEnrollmentDate: @2026-01-07, payer: System.Code { code: 'MCD' } },
      Tuple { monthlyEnrollmentDate: @2026-01-08, payer: System.Code { code: 'MCD' } },
      Tuple { monthlyEnrollmentDate: @2026-01-09, payer: System.Code { code: 'MCD' } },
      Tuple { monthlyEnrollmentDate: @2026-01-10, payer: System.Code { code: 'MCD' } },
      Tuple { monthlyEnrollmentDate: @2026-01-11, payer: System.Code { code: 'MCD' } },
      Tuple { monthlyEnrollmentDate: @2026-01-12, payer: System.Code { code: 'MEP' } } }

// The REAL ENP "Product Line Stratifier" body, verbatim: it returns a LIST via `return all`.
define function "Product Line Stratifier"(enrollmentDate Date):
    ("All reported member months info") mmInfo
    where
        mmInfo.monthlyEnrollmentDate = enrollmentDate
    return all
        mmInfo.payer.code

// --- Null-valued stratifier components (reproduces the empty/collapsed stratifier) ---
// Two scalar stratifier expressions that evaluate to null for the member. With the defect,
// a null scalar component is silently dropped (never recorded), so when every component is
// null the whole stratifier collapses to zero strata: "stratifier": [ { "id": "..." } ].
// A null component must instead land the subject in a "null" stratum, matching how a
// null-returning *function* component already behaves.
define "Null Age Subject":
    null

define "Null Product Line Subject":
    null
