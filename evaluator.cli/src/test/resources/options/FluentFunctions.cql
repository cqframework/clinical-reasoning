/*
@seealso: https://github.com/cqframework/clinical_quality_language/wiki/Well-Known-CQL-Documentation-Tags
*/
library FluentFunctions

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

context Patient

/*
Expression Definitions
@seealso: https://cql.hl7.org/02-authorsguide.html#statements
*/
define Four:
  4

/*
Function Definitions
@seealso: https://cql.hl7.org/03-developersguide.html#defining-functions
*/
define function Plus(x Integer, y Integer):
  x + y

/*
Expected result: true
*/
define TestPlus: Plus(2, 2) = 4

/*
Fluent Functions
@seealso: https://cql.hl7.org/03-developersguide.html#fluent-functions

Provide guidance about fluent function naming, point out that it's possible to define fluent functions with the same name in different libraries, so try to use unique names.
*/
define fluent function plus(x Integer, y Integer):
  Plus(x, y)

define fluent function minus(x Integer, y Integer):
  x - y

define Testplus:
  2.plus(2).minus(12)

