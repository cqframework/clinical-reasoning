package org.opencds.cqf.fhir.utility;

import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

import ca.uhn.fhir.context.FhirVersionEnum;

/**
 * This class provides a generic way to handle adding exceptions encountered during an operation to an OperationOutcome.
 */
public class OperationOutcomes {

    private OperationOutcomes() {}

    public static IBaseOperationOutcome newOperationOutcome(FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.OperationOutcome();
            case R4:
                return new org.hl7.fhir.r4.model.OperationOutcome();
            case R5:
                return new org.hl7.fhir.r5.model.OperationOutcome();

            default:
                return null;
        }
    }

    public static void addExceptionToOperationOutcome(IBaseOperationOutcome operationOutcome, String exceptionMessage) {
        var fhirVersion = FhirVersions.forClass(operationOutcome.getClass());
        switch (fhirVersion) {
            case DSTU3:
                ((org.hl7.fhir.dstu3.model.OperationOutcome) operationOutcome)
                        .addIssue()
                        .setCode(org.hl7.fhir.dstu3.model.OperationOutcome.IssueType.EXCEPTION)
                        .setSeverity(org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity.ERROR)
                        .setDiagnostics(exceptionMessage);
                break;
            case R4:
                ((org.hl7.fhir.r4.model.OperationOutcome) operationOutcome)
                        .addIssue()
                        .setCode(org.hl7.fhir.r4.model.OperationOutcome.IssueType.EXCEPTION)
                        .setSeverity(org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.ERROR)
                        .setDiagnostics(exceptionMessage);
                break;
            case R5:
                ((org.hl7.fhir.r5.model.OperationOutcome) operationOutcome)
                        .addIssue()
                        .setCode(org.hl7.fhir.r5.model.OperationOutcome.IssueType.EXCEPTION)
                        .setSeverity(org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity.ERROR)
                        .setDiagnostics(exceptionMessage);

            default:
                break;
        }
    }
}
