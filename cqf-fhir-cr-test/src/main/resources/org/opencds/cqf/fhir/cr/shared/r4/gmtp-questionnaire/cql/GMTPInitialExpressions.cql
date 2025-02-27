library GMTPInitialExpressions version '0.1.0'

using USCore version '3.1.1'
using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers
include USCoreCommon version '0.1.0' called UC
include USCoreElements version '0.1.0' called UCE

codesystem "LOINC": 'http://loinc.org'
codesystem "Identifier Type": 'http://terminology.hl7.org/CodeSystem/v2-0203'
code "Member Number": 'MB' from "Identifier Type"

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
  UCE."All Problem List Items" C
    let prevalence: C.prevalenceInterval()
    where C.isConfirmed()
    return {
      diagnosis: C.code.display,
      ageAtOnset: ToString(AgeInYearsAt(start of prevalence)),
      onset: ToString(start of prevalence),
      abatement: ToString(if prevalence.hasEnd() then end of prevalence else null)
    }

define "Requesting Provider Name":
  UCE."Requesting Provider".name.family 

define "Requesting Provider Phone":
  UC.Mobile(UCE."Requesting Provider".telecom).value
  //This definition looks for #mobile, but wouldn't a practitioner be #work?
/*
define "Requesting Provider Address":
  (singleton from "Requesting Provider".address).line
*/
define "Requesting Provider Address":
  { 'TODO: remove hard-coded address' }

define "Requesting Provider NPI":
  UCE."Requesting Provider".identifier I 
  where I.system = 'http://hl7.org/fhir/sid/us-npi'
  return I.value

define "Requesting Provider Fax":
  UCE."Requesting Provider".telecom T
  where T.system = 'fax'
  return T.value

define "Servicing Provider Name":
  UCE."Servicing Provider".name

define "Servicing Provider Phone":
  UC.Mobile(UCE."Servicing Provider".telecom).value

// TODO: Error: Could not resolve data provider for package 'java.util'.
/*
define "Servicing Provider Address":
  (singleton from "Servicing Provider".address).line
*/
define "Servicing Provider Address":
  { 'TODO: remove hard-coded address' }

define "Servicing Provider NPI":
  UCE."Servicing Provider".identifier I 
  where I.system = 'http://hl7.org/fhir/sid/us-npi'
  return I.value

define "Servicing Provider Fax":
  UCE."Servicing Provider".telecom T
  where T.system = 'fax'
  return T.value

define "Billing Provider Name":
  UCE."Billing Provider".name

define "Billing Provider Phone":
  UC.Mobile(UCE."Billing Provider".telecom).value

// TODO: Error: Could not resolve data provider for package 'java.util'.
/*
define "Billing Provider Address":
  (singleton from "Billing Provider".address).line
*/
define "Billing Provider Address":
  { 'TODO: remove hard-coded address' }

define "Billing Provider NPI":
  UCE."Billing Provider".identifier I 
  where I.system = 'http://hl7.org/fhir/sid/us-npi'
  return I.value

define "Billing Provider Fax":
  UCE."Billing Provider".telecom T
  where T.system = 'fax'
  return T.value

define "Date of Service":
  UCE."Most Recent ServiceRequest".occurrence.value

define "Requested Test":
  UCE."Most Recent ServiceRequest" SR
  return SR.code

define "Test ID":
  Combine("Requested Test".coding.code.value, ', ') 
  //"Requested Test".coding.code.value 
  /* feature req */

define "Test name":
   Combine("Requested Test".coding.display.value, ', ')
   //"Requested Test".coding.display.valu

define "ALL ICD and CPT Test Codes":
   "Test ICD Codes" union "Test CPT Codes" as List<FHIR.Coding>

define "Test ICD Codes":
 "Requested Test".coding C
  where C.system.value = 'http://hl7.org/fhir/sid/icd-10-cm'
  return C
  
define "Test CPT Codes":
  "Requested Test".coding C
  where C.system.value =  'http://www.ama-assn.org/go/cpt'
  return C

define "Test Related Condition":
  UCE."Test Related Condition"
  //.reasonReference changed to .reason in FHIR R5

define "Diagnosis Descriptions":
  "Test Related Condition".code.coding.display.value

define "Billing Provider different from Servicing Provider":
  if UCE."Billing Provider".id != UCE."Servicing Provider".id then
  'Yes'
  else 
  'No'

define "Clinical Trial Organization":
  First(UCE."Clinical Trial Organization" O 
    with "Clinical Trial"  T such that EndsWith(T.sponsor.reference, O.id))  
 //in FHIR R5 T.associatedParty instead of T.sponsor 

define "BillingProvider is Clinical Trial Organisation":
  UCE."Billing Provider" = "Clinical Trial Organization"

//TODO does not get populated properly
define "Part of clinical trial":
 if "Is Research Subject" and "BillingProvider is Clinical Trial Organisation" then
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
    UCE."Most Recent ServiceRequest" SR 
  where S.occurrence before SR.occurrence
    and S.id != SR.id
    and S.reasonReference = SR.reasonReference
  return S.id.value

//define "Family history of genetic testing"
//too general to prefill?



