library TestVSCastFunction

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

valueset "Narcolepsy": 'https://example.org/fhir/ValueSet/narcolepsy'

/*
Use this version of the function as a workaround for an issue in the JavaScript execution engine:
define function "VS Cast Function"(VSet List<System.Code>):
  ( ( cast { "VSet", 1 }[0]as Tuple {
      codes List<System.Code>,
      oid System.String,
      version System.String
    }
  ).codes ) VSetCodes
    return System.Code { code: VSetCodes.code, system: VSetCodes.system }
*/

/*
In the Java engine, this workaround is unnecessary

See this Gist for discussion:
https://gist.github.com/mattwiller/d008bc7a88d9a17675fddbc95b99403e
*/
define function "VS Cast Function"(VSet List<System.Code>):
  VSet

define function "Conditions in ValueSet"(conditions List<Condition>, codes List<System.Code>):
  conditions C
    where FHIRHelpers.ToConcept(C.code) in codes

define TestConditions:
  [Condition] C
    where C.code in "Narcolepsy"

define TestConditionsViaFunction:
  "Conditions in ValueSet"([Condition], "VS Cast Function"("Narcolepsy"))

define TestConditionsDirectly:
  "Conditions in ValueSet"([Condition], "Narcolepsy")
