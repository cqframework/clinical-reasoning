library ExtensionAndSlicingExample version '0.1.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

codesystem "LOINC": 'http://loinc.org'
codesystem "CDC Race and Ethnicity Codes": 'urn:oid:2.16.840.1.113883.6.238'

code "Blood pressure": '85354-9' from "LOINC" display 'blood pressure'
code "Diastolic blood pressure": '8462-4' from "LOINC" display 'Diastolic blood pressure'
code "Systolic blood pressure": '8480-6' from "LOINC" display 'Systolic blood pressure'
code "American Indian or Alaska Native": '1002-5' from "CDC Race and Ethnicity Codes"
code "Alaska Native": '1735-0' from "CDC Race and Ethnicity Codes"

context Patient

define TestSlices:
  [Observation: "Blood pressure"] BP
    let
      SystolicBP: singleton from (BP.component C where C.code ~ "Systolic blood pressure"),
      DiastolicBP: singleton from (BP.component C where C.code ~ "Diastolic blood pressure")
    where SystolicBP.value < 140 'mm[Hg]'
      and DiastolicBP.value < 90 'mm[Hg]'

define TestSimpleExtensions:
  Patient P
    let birthsex: singleton from (P.extension E
        where E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex'
      )
    where birthsex.value = 'M'

define TestComplexExtensions:
  Patient P
    let
      race: singleton from (P.extension E
        where E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-race'),
      ombCategory: race.extension E where E.url = 'ombCategory',
      detailed: race.extension E where E.url = 'detailed'
    where exists (ombCategory O where O.value ~ "American Indian or Alaska Native")
      and exists (detailed O where O.value ~ "Alaska Native")
