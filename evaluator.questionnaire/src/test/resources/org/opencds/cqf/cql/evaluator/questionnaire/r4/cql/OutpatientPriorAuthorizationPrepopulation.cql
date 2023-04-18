library OutpatientPriorAuthorizationPrepopulation version '1.0.0'

using FHIR version '4.0.1'

// include FHIRCommon version '4.0.1' called FHIRCommon


include FHIRHelpers version '4.0.1' called FHIRHelpers

/* parameter EncounterId String */


parameter ClaimId String default '14703'

context Patient

define ClaimResource:
  First([Claim] C
      where C.id = ClaimId
  )

define OrganizationFacility:
  First([Organization] O
      where EndsWith(ClaimResource.provider.reference, O.id)
  )

define "FacilityName":
  OrganizationFacility.name.value

define "FacilityNPI":
  ( OrganizationFacility.identifier I
      where I.system = 'http://hl7.org.fhir/sid/us-npi'
  ).value.value
    
//This value intentionally left blank
//TODO: Consider removing from the Questionnaire?  Where does this belong in a FHIR resource?



define "FacilityPTAN":
  null

//This value intentionally left blank (TODO)


define "FacilityContractRegion":
  null

define function FindPractitioner(myCode String, mySequence Integer):
  //If we can't find a Primary practicioner by code, use the sequence as a fallback.
  
  Coalesce(First([Practitioner] P //Practitioner who is on the careteam and contains a code that equals supplied code
        
        where EndsWith(First(ClaimResource.careTeam CT
              where exists(CT.role.coding CTCode
                  where CTCode.code.value = myCode
              )
          ).provider.reference, P.id
        )
    ), First([Practitioner] P //Practitioner who is on the careteam and is of the supplied sequence
        
        where EndsWith(First(ClaimResource.careTeam CT
              where CT.sequence = mySequence
          ).provider.reference, P.id
        )
    )
  )

define PractitionerOperatingPhysician:
  FindPractitioner('primary', 1)

define PractitionerAttendingPhysician:
  FindPractitioner('assist', 2)

// Should probably be seperate, Utiltiy files; but just for practice:

//PATIENT INFO



define OfficialName:
  First(Patient.name name
      where name.use.value = 'official'
  )

define FirstName:
  Patient.name[0]

define BeneficiaryName:
  Coalesce(OfficialName, FirstName)

define "BeneficiaryFirstName":
  BeneficiaryName.given[0].value

define "BeneficiaryLastName":
  BeneficiaryName.family.value

define "BeneficiaryDOB":
  Patient.birthDate.value

define "BeneficiaryGender":
  Patient.gender.value

define RequestCoverage:
  ClaimResource.insurance

define CoverageResource:
  First([Coverage] coverage
    // pull coverage resource id from the service request insurance extension
      
      where EndsWith(RequestCoverage[0].coverage.reference, coverage.id)
  )

//Pull Medicare Number from the Patient FHIR Resource using the coding system


define PatientMedicareNumber:
  First(Patient.identifier I
      where(I.type.coding[0].system = 'http://terminology.hl7.org/CodeSystem/v2-0203'
          and I.type.coding[0].code = 'MC'
      )
  ).value.value

// This first value was used in the first test case, but is probably incorrect in favor of PatientMedicareNumber
//  TODO: Remove



define "BeneficiaryMedicareID":
  Coalesce(CoverageResource.subscriberId.value, PatientMedicareNumber)

// OPERATING PHYSICIAN INFO


define "OperatingPhysicianFirstName":
  PractitionerOperatingPhysician.name.given[0].value

define "OperatingPhysicianLastName":
  PractitionerOperatingPhysician.name.family.value

define "OperatingPhysicianNPI":
  ( PractitionerOperatingPhysician.identifier I
      where I.system = 'http://hl7.org.fhir/sid/us-npi'
  ).value.value

//This value intentionally left blank (TODO)


define "OperatingPhysicianPTAN":
  null

//Get work addres where available, then home, then just whatever you can.


define function FindAddress(P Practitioner):
  Coalesce(First(P.address address
        where address.use.value = 'work'
    ), First(P.address address
        where address.use.value = 'home'
    ), First(P.address address)
  )

define OperatingPhysicianAddress:
  FindAddress(PractitionerOperatingPhysician)

define "OperatingPhysicianAddress1":
  OperatingPhysicianAddress.line[0].value

define "OperatingPhysicianAddress2":
  OperatingPhysicianAddress.line[1].value

define "OperatingPhysicianAddressCity":
  OperatingPhysicianAddress.city.value

define "OperatingPhysicianAddressState":
  OperatingPhysicianAddress.state.value

define "OperatingPhysicianAddressZip":
  OperatingPhysicianAddress.postalCode.value

// Attending PHYSICIAN INFO


define "AttendingPhysicianSame":
  case
    when PractitionerAttendingPhysician is not null then false 
    else true end

define "AttendingPhysicianFirstName":
  PractitionerAttendingPhysician.name.given[0].value

define "AttendingPhysicianLastName":
  PractitionerAttendingPhysician.name.family.value

define "AttendingPhysicianNPI":
  ( PractitionerAttendingPhysician.identifier I
      where I.system = 'http://hl7.org.fhir/sid/us-npi'
  ).value.value

//This value intentionally left blank (TODO)


define "AttendingPhysicianPTAN":
  null

define AttendingPhysicianAddress:
  FindAddress(PractitionerAttendingPhysician)

define "AttendingPhysicianAddress1":
  AttendingPhysicianAddress.line[0].value

define "AttendingPhysicianAddress2":
  AttendingPhysicianAddress.line[1].value

define "AttendingPhysicianAddressCity":
  AttendingPhysicianAddress.city.value

define "AttendingPhysicianAddressState":
  AttendingPhysicianAddress.state.value

define "AttendingPhysicianAddressZip":
  AttendingPhysicianAddress.postalCode.value

//CLAIM INFORMATION


define ClaimDiagnosisReferenced:
  First([Condition] C
      where //First condition referenced by the Claim
      exists(ClaimResource.diagnosis.diagnosis Condition
          where EndsWith(Condition.reference, C.id)
      )
  ).code.coding[0].code.value

define ClaimDiagnosisCode:
  ClaimResource.diagnosis.diagnosis.coding[0].code.value //TODO: Check for primary vs. secondary?


define "RequestDetailsPrimaryDiagnosisCode":
  Coalesce(ClaimDiagnosisCode, ClaimDiagnosisReferenced)

//This value intentionally left blank (TODO)


define "RequestDetailsSecondaryDiagnosisCode":
  null

//This value intentionally left blank (TODO)


define "RequestDetailsAdditionalDiagnosisCode1":
  null

//This value intentionally left blank (TODO)


define "RequestDetailsAdditionalDiagnosisCode2":
  null

// SUBMISSION INFORMATION
//This whole area is intentionally left blank, as one large TODO, but it also begs the same question
//Does this information belong in FHIR?  Where?




define "RequestDetailsResubmition":
  null

define "RequestDetailsResubmissionUTN":
  null

define "RequestDetailsLifeThreatening":
  null

define "RequestDetailsLifeThreateningExplanation":
  null

//PROCEDURE INFORMATION


define RelevantReferencedProcedures:
  [Procedure] P
    where P.status.value != 'completed'
      and exists ( ClaimResource.procedure Procedure
          where EndsWith(Procedure.procedure.reference, P.id)
      )

define function FindProcedure(proc String):
  exists ( ClaimResource.procedure.procedure.coding P
      where P.code.value = proc
  )
    or exists ( RelevantReferencedProcedures.code.coding coding
        where coding.code.value = proc
    )

//There's gotta be a better way to do this.


define "RequestDetailsProcedureCode64612":
  FindProcedure('64612')

define "RequestDetailsProcedureCodeJ0585":
  FindProcedure('J0585')

define "RequestDetailsProcedureCodeJ0587":
  FindProcedure('J0587')

define "RequestDetailsProcedureCode64615":
  FindProcedure('64615')

define "RequestDetailsProcedureCodeJ0586":
  FindProcedure('J0586')

define "RequestDetailsProcedureCodeJ0588":
  FindProcedure('J0588')

define "RequestDetailsProcedureCode15820":
  FindProcedure('15820')

define "RequestDetailsProcedureCode15821":
  FindProcedure('15821')

define "RequestDetailsProcedureCode15822":
  FindProcedure('15822')

define "RequestDetailsProcedureCode15823":
  FindProcedure('15823')

define "RequestDetailsProcedureCode67900":
  FindProcedure('67900')

define "RequestDetailsProcedureCode67901":
  FindProcedure('67901')

define "RequestDetailsProcedureCode67902":
  FindProcedure('67902')

define "RequestDetailsProcedureCode67903":
  FindProcedure('67903')

define "RequestDetailsProcedureCode67904":
  FindProcedure('67904')

define "RequestDetailsProcedureCode67906":
  FindProcedure('67906')

define "RequestDetailsProcedureCode67908":
  FindProcedure('67908')

define "RequestDetailsProcedureCode22551":
  FindProcedure('22551')

define "RequestDetailsProcedureCode22552":
  FindProcedure('22552')

define "RequestDetailsProcedureCode63650":
  FindProcedure('63650')

define "RequestDetailsProcedureCode15830":
  FindProcedure('15830')

define "RequestDetailsProcedureCode15847":
  FindProcedure('15847')

define "RequestDetailsProcedureCode15877":
  FindProcedure('15877')

define "RequestDetailsProcedureCode20912":
  FindProcedure('20912')

define "RequestDetailsProcedureCode21210":
  FindProcedure('21210')

define "RequestDetailsProcedureCode30400":
  FindProcedure('30400')

define "RequestDetailsProcedureCode30410":
  FindProcedure('30410')

define "RequestDetailsProcedureCode30420":
  FindProcedure('30420')

define "RequestDetailsProcedureCode30430":
  FindProcedure('30430')

define "RequestDetailsProcedureCode30435":
  FindProcedure('30435')

define "RequestDetailsProcedureCode30450":
  FindProcedure('30450')

define "RequestDetailsProcedureCode30460":
  FindProcedure('30460')

define "RequestDetailsProcedureCode30462":
  FindProcedure('30462')

define "RequestDetailsProcedureCode30465":
  FindProcedure('30465')

define "RequestDetailsProcedureCode30520":
  FindProcedure('30520')

define "RequestDetailsProcedureCode36473":
  FindProcedure('36473')

define "RequestDetailsProcedureCode36474":
  FindProcedure('36474')

define "RequestDetailsProcedureCode36475":
  FindProcedure('36475')

define "RequestDetailsProcedureCode36476":
  FindProcedure('36476')

define "RequestDetailsProcedureCode36478":
  FindProcedure('36478')

define "RequestDetailsProcedureCode36479":
  FindProcedure('36479')

define "RequestDetailsProcedureCode36482":
  FindProcedure('36482')

define "RequestDetailsProcedureCode36483":
  FindProcedure('36483')

//This value intentionally left blank (TODO)


define "RequestDetailsProcedureCodeStaged":
  null

//This value intentionally left blank (TODO)


define "RequestDetailsProcedureCodeNumOfUnits":
  null

//REQUESTOR INFORMATION
//Again, this whole area left blank.
//I think we should probably remove these expressions from the Questionnaire altogether, but then,
//In a smart context, anyway, the 'current practitioner' is recorded, so... maybe it could be prefilled?
//Not sure. (TODO)






define "RequestorInformationName":
  null

define "RequestorInformationRepresentative":
  null

define "RequestorInformationPhone":
  null

define "RequestorInformationFax":
  null

define "RequestorInformationFacilityFax":
  null