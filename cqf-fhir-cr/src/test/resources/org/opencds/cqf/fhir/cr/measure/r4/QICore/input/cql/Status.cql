library Status version '1.15.000'

using QICore version '6.0.0'

include FHIRHelpers version '4.4.000' called FHIRHelpers

codesystem "ObservationCategoryCodes": 'http://terminology.hl7.org/CodeSystem/observation-category'
codesystem "ConditionVerificationStatusCodes": 'http://terminology.hl7.org/CodeSystem/condition-ver-status'

code "exam": 'exam' from "ObservationCategoryCodes" display 'exam'
code "imaging": 'imaging' from "ObservationCategoryCodes" display 'imaging'
code "confirmed": 'confirmed' from ConditionVerificationStatusCodes display 'confirmed'
code "unconfirmed": 'unconfirmed' from ConditionVerificationStatusCodes display 'unconfirmed'
code "provisional": 'provisional' from ConditionVerificationStatusCodes display 'provisional'
code "differential": 'differential' from ConditionVerificationStatusCodes display 'differential'

context Patient

//This library contains functions used to constrain FHIR resource elements for measures authored by NCQA, based on QICore 6.0.0 resources including IG and authoring patterns. The functions may appear similar to some QICoreCommon functions but differ in that they have constraints that are relevant for measures authored by NCQA.

//Condition
//Returns conditions in the given list that either have no verification status or have a verification status of confirmed, unconfirmed, provisional, or differential
define fluent function verified(conditions List<Choice<ConditionProblemsHealthConcerns, ConditionEncounterDiagnosis>>):
  conditions C
    where C.verificationStatus is not null implies
      (C.verificationStatus ~ "confirmed"
        or C.verificationStatus ~ "unconfirmed"
        or C.verificationStatus ~ "provisional"
        or C.verificationStatus ~ "differential"
      )

//Assessment, Performed
define fluent function isAssessmentPerformed(Obs List<ObservationScreeningAssessment>):
  Obs O
    where O.status in { 'final', 'amended', 'corrected' }

//Device, Order - Personal Use Devices
define fluent function isDeviceOrderPersonalUseDevices(DeviceRequest List<DeviceRequest>):
  DeviceRequest D
    where D.status in { 'active', 'completed' }
      and D.intent in {'order', 'original-order', 'reflex-order', 'filler-order', 'instance-order'}

//Diagnostic Study, Order
define fluent function isDiagnosticStudyOrder(ServiceRequest List<ServiceRequest>):
  ServiceRequest S
    where S.status in { 'active', 'completed' }
      and S.intent in {'order', 'original-order', 'reflex-order', 'filler-order', 'instance-order'}
      
//Intervention, Order
define fluent function isInterventionOrder(ServiceRequest List<ServiceRequest>):
  ServiceRequest S
    where S.status in { 'active', 'completed' }
      and S.intent in {'order', 'original-order', 'reflex-order', 'filler-order', 'instance-order'}    

//Laboratory Test, Order
define fluent function isLaboratoryTestOrder(ServiceRequest List<ServiceRequest>):
  ServiceRequest S
    where S.status in { 'active', 'completed' }
      and S.intent in {'order', 'original-order', 'reflex-order', 'filler-order', 'instance-order'}       

//Diagnostic Study, Performed
define fluent function isDiagnosticStudyPerformed(Obs List<ObservationClinicalResult>):
  Obs O
    where O.status in { 'final', 'amended', 'corrected' }
        and exists ( O.category ObservationCategory
        where ( ObservationCategory ) ~ "imaging"
    )
    
//Encounter, Performed
//General usage unless required otherwise by measure intent (e.g., follow-up encounters)
define fluent function isEncounterPerformed(Enc List<Encounter>):
  Enc E
    where E.status = 'finished'

//Immunization, Administered
define fluent function isImmunizationAdministered(Immunization List<Immunization>):
  Immunization I
    where I.status ~ 'completed'

//Intervention, Performed
define fluent function isInterventionPerformed(Proc List<Procedure>):
  Proc P
    where P.status ~ 'completed'

//Procedure, Performed
define fluent function isProcedurePerformed(Proc List<Procedure>):
  Proc P
    where P.status ~ 'completed'

//Laboratory Test, Performed
define fluent function isLaboratoryTestPerformed(Obs List<LaboratoryResultObservation>):
  Obs O
    where O.status in { 'final', 'amended', 'corrected' }

//Medication, Active
define fluent function isMedicationActive(MedicationRequest List<MedicationRequest>):
  MedicationRequest M
    where M.status = 'active'
      and M.intent in {'order', 'original-order', 'reflex-order', 'filler-order', 'instance-order'}

//Medication, Dispensed
define fluent function isMedicationDispensed(Med List<MedicationDispense>):
  Med M
    where M.status in { 'completed', 'in-progress', 'on-hold' }

//Medication, Order
define fluent function isMedicationOrder(MedicationRequest List<MedicationRequest>):
  MedicationRequest M
    where M.status in { 'active', 'completed' }
    and M.intent in {'order', 'original-order', 'reflex-order', 'filler-order', 'instance-order'}

//Physical Exam, Performed
define fluent function isPhysicalExamPerformed(Obs List<ObservationClinicalResult>):
  Obs O
    where O.status in { 'final', 'amended', 'corrected' }
      and exists ( O.category ObservationCategory
          where ( ObservationCategory ) ~ "exam"
      )

//Observation Vital Signs
define fluent function isObservationBP(Obs List<USCoreBloodPressureProfile>):
  Obs O
    where O.status in { 'final', 'amended', 'corrected' }
      
define fluent function isObservationBodyHeight(Obs List<USCoreBodyHeightProfile>):
  Obs O
    where O.status in { 'final', 'amended', 'corrected' }
      
define fluent function isObservationBodyWeight(Obs List<USCoreBodyWeightProfile>):
  Obs O
    where O.status in { 'final', 'amended', 'corrected' }
      
define fluent function isObservationBMI(Obs List<USCoreBMIProfile>):
  Obs O
    where O.status in { 'final', 'amended', 'corrected' }
 
//Observation Pediatric BMI
define fluent function isObservationPediatricBMI(Obs List<USCorePediatricBMIforAgeObservationProfile>):
  Obs O
    where O.status in { 'final', 'amended', 'corrected' } 
   
//Observation Smoking Status
define fluent function isObservationSmokingStatus(Obs List<USCoreSmokingStatusProfile>):
  Obs O
    where O.status = 'final' 

//Symptom
define fluent function isSymptom(Obs List<SimpleObservation>):
  Obs O
    where O.status in { 'preliminary', 'final', 'amended', 'corrected' }