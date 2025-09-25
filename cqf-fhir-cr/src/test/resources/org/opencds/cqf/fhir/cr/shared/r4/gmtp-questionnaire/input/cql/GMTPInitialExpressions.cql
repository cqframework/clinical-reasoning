library GMTPInitialExpressions

using USCore version '7.0.0'
using FHIR version '4.0.1'

include hl7.fhir.uv.cql.FHIRHelpers version '4.0.1' called FHIRHelpers
include hl7.fhir.uv.cql.FHIRCommon version '2.0.0'
include USCoreCommon called UC
include USCoreElements called UCE

codesystem "LOINC": 'http://loinc.org'
codesystem "Identifier Type": 'http://terminology.hl7.org/CodeSystem/v2-0203'
code "Member Number": 'MB' from "Identifier Type"

parameter "Coverage" FHIR.Coverage
parameter "ServiceRequest" FHIR.ServiceRequest

context Patient

define "All Problem List Items":
  UCE."All Problem List Items"

/*
@question: Patient history (including age at diagnosis):
TODO: Determine whether we should be considering encounter diagnoses and/or health concerns
TODO: Determine whether age at onset is actually relevant, or if we would, in an automated context, just return the Condition resource
TODO: Does "age at diagnosis" mean "age at onset" or is it really asking for when the diagnosis was recorded?
TODO: Is there a version of patient history that is reusable here?
*/
define "Patient history":
  "All Problem List Items" C
    let prevalence: C.prevalenceInterval()
    where C.isConfirmed()
    return {
      diagnosis: C.code.coding.first().code,
      ageAtOnset: ToString(Patient.ageInYearsAt(ToDate(start of prevalence))),
      onset: ToString(start of prevalence),
      abatement: ToString(if prevalence.hasEnd() then end of prevalence else null)
    }

/*
@linkId: history|patient-history
*/
define "Patient history including age at diagnosis":
  "Patient history" H
    return H.diagnosis & (', ' + H.ageAtOnset + ' yrs') & (', abated ' + H.abatement)

define "Billing Provider Name":
  "Billing Provider".name

define "Billing Provider Phone":
  "Billing Provider".telecom.value

define "Billing Provider Address":
  { 'TODO: remove hard-coded address' }

define "Retrieve Member Coverage test parameter":
  // When executing CQL with the VSCode extension retrieve the resource specific for the test case
  //   the list of resource ids match the resources from the test case folders
  singleton from ([FHIR.Coverage] CV where CV.id in {
    'Coverage-example-GMTP', // testcase: GMTP-example-patient-2
    'coverage-GMTP-1'        // testcase: USCorePatient-GMTP-1
  })

define "Member Coverage":
  Coalesce(
    Coverage,
    "Retrieve Member Coverage test parameter"
  )

define "Retrieve Service Request test parameter":
  // When executing CQL with the VSCode extension retrieve the resource specific for the test case
  //   the list of resource ids match the resources from the test case folders
  singleton from ([FHIR.ServiceRequest] SR where SR.id in {
    'ServiceRequest-example-1-GMTP',       // testcase: GMTP-example-patient-2
    'service-request-USCorePatient-GMTP-1' // testcase: USCorePatient-GMTP-1
  })

define "Most Recent ServiceRequest":
  Coalesce(
    ServiceRequest,
    "Retrieve Service Request test parameter"
  )

define "Billing Provider":
  UCE.BillingProvider("Member Coverage")

define "Billing Provider NPI":
  "Billing Provider".identifier I
  where I.system = 'http://hl7.org/fhir/sid/us-npi'
  return I.value

define "Billing Provider Fax":
  "Billing Provider".telecom T
  where T.system = 'fax'
  return T.value

define "Date of Service":
  "Most Recent ServiceRequest".occurrence.value

define "Requested Test":
  "Most Recent ServiceRequest".code

define "Test ID":
  Combine("Requested Test".coding.code.value, ', ')

define "Test name":
   Combine("Requested Test".coding.display.value, ', ')

define "ALL ICD and CPT Test Codes":
   "Test ICD Codes" union "Test CPT Codes"

define "Test ICD Codes":
 "Requested Test".coding C
  where C.system.value = 'http://hl7.org/fhir/sid/icd-10-cm'
  return C.code.value

define "Test CPT Codes":
  "Requested Test".coding C
  where C.system.value =  'http://www.ama-assn.org/go/cpt'
  return C.code.value

define "Test Related Condition":
  UCE.RelatedCondition("Most Recent ServiceRequest")
  //.reasonReference changed to .reason in FHIR R5

define "Diagnosis Descriptions":
  "Test Related Condition".code.coding.display.value

define "Billing Provider different from Servicing Provider":
  if "Billing Provider".id != First(UCE.ServicingProvider("Most Recent ServiceRequest").id) then
  'Yes'
  else
  'No'

define "Clinical Trial Organization":
  First(UCE."Clinical Trial Organization" O
    with "Clinical Trial"  T such that EndsWith(T.sponsor.reference, O.id))
 //in FHIR R5 T.associatedParty instead of T.sponsor

define "BillingProvider is Clinical Trial Organization":
  "Billing Provider" = "Clinical Trial Organization"

//TODO does not get populated properly
define "Part of clinical trial":
 if "Is Research Subject" and "BillingProvider is Clinical Trial Organization" then
 'Yes'
 else
 'No'

define "Is Research Subject":
  exists("Research Subject")

define "Research Subject":
  UCE."Research Subject"
     //ResearchSubject.individual is from FHIR version v4.0.1 and has been replaced by R.subject in FHIR R5

define "Clinical Trial":
  UCE."All Clinical Trials" R
    with "Research Subject" S such that EndsWith(S.study.reference, R.id)
    with  "Test Related Condition" C such that C.code in R.condition

define "Clinical Trial ID":
  "Clinical Trial".identifier C
  where C.system = 'https://clinicaltrials.gov'
  return C.value.value

define "Previous genetic testing for condition":
  from
    UCE."All ServiceRequests" S,
    ServiceRequest SR
  where S.occurrence before SR.occurrence
    and S.id != SR.id
    and S.reasonReference = SR.reasonReference
  return S.id.value

//define "Family history of genetic testing"
//too general to prefill?