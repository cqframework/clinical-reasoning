library StratifierMultiSubjectDateBasis

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

context Patient

// Synthetic date-basis initial population: a fixed list of 12 dates. Values are CQL literals so the
// scenario is fully self-contained and patient-independent.
define "Enrollment Initial Population":
    { @2026-01-01, @2026-01-02,
      @2026-01-03, @2026-01-04, @2026-01-05, @2026-01-06, @2026-01-07,
      @2026-01-08, @2026-01-09, @2026-01-10, @2026-01-11,
      @2026-01-12 }

// Scalar-valued component-stratifier function: returns a constant category for every date.
define function "Age Stratifier"(stratificationDate Date):
    '18-19'

// Stubbed per-date input list (one row per date), each tagged with a category code. Stands in for
// data-derived input so the list-returning stratifier function below can be exercised.
//
// @2026-01-12 is intentionally tagged with TWO categories (MEP and MMO) so the stratifier function
// returns a multi-element list for a single input. This is the shape that must fan out into one
// stratum per value (that input counts toward BOTH strata), rather than collapsing into a single
// stratum with a comma-joined "MEP,MMO" value.
define "Category Rows":
    { Tuple { rowDate: @2026-01-01, category: System.Code { code: 'MMO' } },
      Tuple { rowDate: @2026-01-02, category: System.Code { code: 'MMO' } },
      Tuple { rowDate: @2026-01-03, category: System.Code { code: 'MCD' } },
      Tuple { rowDate: @2026-01-04, category: System.Code { code: 'MCD' } },
      Tuple { rowDate: @2026-01-05, category: System.Code { code: 'MCD' } },
      Tuple { rowDate: @2026-01-06, category: System.Code { code: 'MCD' } },
      Tuple { rowDate: @2026-01-07, category: System.Code { code: 'MCD' } },
      Tuple { rowDate: @2026-01-08, category: System.Code { code: 'MCD' } },
      Tuple { rowDate: @2026-01-09, category: System.Code { code: 'MCD' } },
      Tuple { rowDate: @2026-01-10, category: System.Code { code: 'MCD' } },
      Tuple { rowDate: @2026-01-11, category: System.Code { code: 'MCD' } },
      Tuple { rowDate: @2026-01-12, category: System.Code { code: 'MEP' } },
      Tuple { rowDate: @2026-01-12, category: System.Code { code: 'MMO' } } }

// List-valued component-stratifier function: returns the category code(s) for the given date via
// `return all`. A list-returning component function is the shape that exposed the CLI bug where the
// stratifier functions were never evaluated (yielding null component values).
define function "Product Line Stratifier"(stratificationDate Date):
    ("Category Rows") row
    where
        row.rowDate = stratificationDate
    return all
        row.category.code
