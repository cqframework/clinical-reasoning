library SharedResourceCreation version '0.0.001'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

define function "GetIdExtensions"(domainResource DomainResource):
  domainResource.extension E
  where E.url = 'https://www.cdc.gov/test-case/fhir/StructureDefinition/original-id'
  return E

define function "GetPatientExtensions"(domainResource DomainResource):
  domainResource.extension E
  where E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-race'
    or E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity'
    or E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex'
    or E.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-genderIdentity'
    or E.url = 'http://hl7.org/fhir/StructureDefinition/patient-genderIdentity'
  return E

define function "MedicationRequestResource"(medicationRequest MedicationRequest):
  medicationRequest m
  return MedicationRequest{
    id: FHIR.id {value: 'LCR-' + m.id},
    extension: m.extension,
    identifier: m.identifier,
    status: m.status,
    statusReason: m.statusReason,
    intent: m.intent,
    category: m.category,
    priority: m.priority,
    doNotPerform: m.doNotPerform,
    reported: m.reported,
    medication: m.medication,
    subject: m.subject,
    encounter: m.encounter,
    supportingInformation: m.supportingInformation,
    authoredOn: m.authoredOn,
    requester: m.requester,
    performer: m.performer,
    performerType: m.performerType,
    recorder: m.recorder,
    reasonCode: m.reasonCode,
    reasonReference: m.reasonReference,
    instantiatesCanonical: m.instantiatesCanonical,
    instantiatesUri: m.instantiatesUri,
    basedOn: m.basedOn,
    groupIdentifier: m.groupIdentifier,
    courseOfTherapyType: m.courseOfTherapyType,
    insurance: m.insurance,
    note: m.note,
    dosageInstruction: m.dosageInstruction,
    dispenseRequest: m.dispenseRequest,
    substitution: m.substitution,
    priorPrescription: m.priorPrescription,
    detectedIssue: m.detectedIssue,
    eventHistory: m.eventHistory
  }

define function "MedicationAdministrationResource"(medicationAdministration MedicationAdministration):
  medicationAdministration m
  return MedicationAdministration{
    id: FHIR.id {value: 'LCR-' + m.id},
    extension: m.extension,
    identifier: m.identifier,
    instantiates: m.instantiates,
    partOf: m.partOf,
    status: m.status,
    statusReason: m.statusReason,
    category: m.category,
    medication: m.medication,
    subject: m.subject,
    context: m.context,
    supportingInformation: m.supportingInformation,
    effective: m.effective,
    performer: m.performer,
    reasonCode: m.reasonCode,
    reasonReference: m.reasonReference,
    request: m.request,
    device: m.device,
    note: m.note,
    dosage: m.dosage,
    eventHistory: m.eventHistory
  }

define function "MedicationResource"(medication Medication):
  medication m
  return Medication{
    id: FHIR.id {value: 'LCR-' + m.id},
    extension: m.extension,
    identifier: m.identifier,
    code: m.code,
    status: m.status,
    manufacturer: m.manufacturer,
    form: m.form,
    amount: m.amount,
    ingredient: m.ingredient,
    batch: m.batch
  }

define function "ConditionResource"(condition Condition):
  condition c
  return Condition{
    id: FHIR.id {value: 'LCR-' + c.id},
    extension: c.extension,
    identifier: c.identifier,
    clinicalStatus: c.clinicalStatus,
    verificationStatus: c.verificationStatus,
    category: c.category,
    severity: c.severity,
    code: c.code,
    bodySite: c.bodySite,
    subject: c.subject,
    encounter: c.encounter,
    onset: c.onset,
    abatement: c.abatement,
    recordedDate: c.recordedDate,
    recorder: c.recorder,
    asserter: c.asserter,
    stage: c.stage,
    evidence: c.evidence,
    note: c.note
  }

define function "EncounterResource"(encounter Encounter):
  encounter e
  return Encounter{
    id: FHIR.id{value: 'LCR-' + e.id},
    extension: e.extension,
    identifier: e.identifier,
    status: e.status,
    statusHistory: e.statusHistory,
    class: e.class,
    classHistory: e.classHistory,
    type: e.type,
    serviceType: e.serviceType,
    priority: e.priority,
    subject: e.subject,
    episodeOfCare: e.episodeOfCare,
    basedOn: e.basedOn,
    participant: e.participant,
    appointment: e.appointment,
    period: e.period,
    length: e.length,
    reasonCode: e.reasonCode,
    reasonReference: e.reasonReference,
    diagnosis: e.diagnosis,
    account: e.account,
    hospitalization: e.hospitalization,
    location: e.location,
    serviceProvider: e.serviceProvider,
    partOf: e.partOf
  }

define function "PatientResource"(patient Patient):
  patient p
  return Patient{
    id: FHIR.id{value: 'LCR-' + p.id},
    extension: GetPatientExtensions(p) union GetIdExtensions(p),
    identifier: p.identifier,
    active: p.active,
    name: p.name,
    telecom: p.telecom,
    gender: p.gender,
    birthDate: p.birthDate,
    deceased: p.deceased,
    address: p.address,
    contact: p.contact,
    communication: p.communication,
    link: p.link
  }

define function "ObservationResource"(observation Observation):
  observation o
  return Observation{
    id: FHIR.id {value: 'LCR-' + o.id},
    extension: o.extension,
    identifier: o.identifier,
    basedOn: o.basedOn,
    partOf: o.partOf,
    status: o.status,
    category: o.category,
    code: o.code,
    subject: o.subject,
    focus: o.focus,
    encounter: o.encounter,
    effective: o.effective,
    issued: o.issued,
    performer: o.performer,
    value: o.value,
    dataAbsentReason: o.dataAbsentReason,
    interpretation: o.interpretation,
    note: o.note,
    bodySite: o.bodySite,
    method: o.method,
    specimen: o.specimen,
    device: o.device,
    referenceRange: o.referenceRange,
    hasMember: o.hasMember,
    derivedFrom: o.derivedFrom,
    component: o.component
  }

define function "ServiceRequestResource"(serviceRequest ServiceRequest):
  serviceRequest sR
  return ServiceRequest{
    id: FHIR.id {value: 'LCR-' + sR.id},
    extension: sR.extension,
    identifier: sR.identifier,
    instantiatesCanonical: sR.instantiatesCanonical,
    instantiatesUri: sR.instantiatesUri,
    basedOn: sR.basedOn,
    replaces: sR.replaces,
    requisition: sR.requisition,
    status: sR.status,
    intent: sR.intent,
    category: sR.category,
    priority: sR.priority,
    doNotPerform: sR.doNotPerform,
    code: sR.code,
    orderDetail: sR.orderDetail,
    quantity: sR.quantity,
    subject: sR.subject,
    encounter: sR.encounter,
    occurrence: sR.occurrence,
    asNeeded: sR.asNeeded,
    authoredOn: sR.authoredOn,
    requester: sR.requester,
    performerType: sR.performerType,
    performer: sR.performer,
    locationCode: sR.locationCode,
    locationReference: sR.locationReference,
    reasonCode: sR.reasonCode,
    reasonReference: sR.reasonReference,
    insurance: sR.insurance,
    supportingInfo: sR.supportingInfo,
    specimen: sR.specimen,
    bodySite: sR.bodySite,
    note: sR.note,
    patientInstruction: sR.patientInstruction,
    relevantHistory: sR.relevantHistory
  }

define function "SpecimenResource"(specimen Specimen):
  specimen s
  return Specimen{
    id: FHIR.id {value: 'LCR-' + s.id},
    extension: s.extension,
    identifier: s.identifier,
    accessionIdentifier: s.accessionIdentifier,
    status: s.status,
    type: s.type,
    subject: s.subject,
    receivedTime: s.receivedTime,
    parent: s.parent,
    request: s.request,
    collection: s.collection,
    processing: s.processing,
    container: s.container,
    condition: s.condition,
    note: s.note
  }

define function "CoverageResource"(coverage Coverage):
  coverage c
  return Coverage{
    id: FHIR.id{value: 'LCR-' + c.id},
    extension: c.extension,
    identifier: c.identifier,
    status: c.status,
    type: c.type,
    policyHolder: c.policyHolder,
    subscriber: c.subscriber,
    subscriberId: c.subscriberId,
    beneficiary: c.beneficiary,
    dependent: c.dependent,
    relationship: c.relationship,
    period: c.period,
    payor: c.payor,
    class: c.class,
    order: c.order,
    network: c.network,
    costToBeneficiary: c.costToBeneficiary,
    subrogation: c.subrogation,
    contract: c.contract
  }

define function "OperationOutcomeResource"(errorId String, resourceId FHIR.id, message String):
  OperationOutcome{
      id: FHIR.id{value: errorId},
      issue: {
          FHIR.OperationOutcome.Issue{
          severity: FHIR.IssueSeverity{value: 'error'},
          code: FHIR.IssueType{value: 'exception'},
          details:
              FHIR.CodeableConcept{
                  coding: {
                      Coding{
                      system: uri{value: 'https://test-case.com/validation-error'},
                      code: code{value: 'Error'},
                      display: string{value: 'Resource ' + resourceId.value + ' failed validation: ' + message}
                      }
                  }
              }
          }
      }
  }