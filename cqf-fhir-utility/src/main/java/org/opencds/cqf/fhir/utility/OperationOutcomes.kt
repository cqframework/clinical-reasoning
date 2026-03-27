package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome

/**
 * Provides a generic way to handle adding exceptions encountered during an operation to an
 * OperationOutcome.
 */
object OperationOutcomes {

    /** Returns a new OperationOutcome resource for the given FHIR version, or null if unsupported. */
    @JvmStatic
    fun newOperationOutcome(fhirVersion: FhirVersionEnum): IBaseOperationOutcome? =
        when (fhirVersion) {
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.OperationOutcome()
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.OperationOutcome()
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.OperationOutcome()
            else -> null
        }

    /**
     * Adds an issue to the OperationOutcome with a code of EXCEPTION, severity of ERROR.
     *
     * @param operationOutcome the OperationOutcome to add an issue to
     * @param exceptionMessage the exception message for the issue
     */
    @JvmStatic
    fun addExceptionToOperationOutcome(
        operationOutcome: IBaseOperationOutcome,
        exceptionMessage: String,
    ) {
        when (operationOutcome) {
            is org.hl7.fhir.dstu3.model.OperationOutcome ->
                operationOutcome.addIssue().apply {
                    code = org.hl7.fhir.dstu3.model.OperationOutcome.IssueType.EXCEPTION
                    severity = org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity.ERROR
                    diagnostics = exceptionMessage
                }
            is org.hl7.fhir.r4.model.OperationOutcome ->
                operationOutcome.addIssue().apply {
                    code = org.hl7.fhir.r4.model.OperationOutcome.IssueType.EXCEPTION
                    severity = org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.ERROR
                    diagnostics = exceptionMessage
                }
            is org.hl7.fhir.r5.model.OperationOutcome ->
                operationOutcome.addIssue().apply {
                    code = org.hl7.fhir.r5.model.OperationOutcome.IssueType.EXCEPTION
                    severity = org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity.ERROR
                    diagnostics = exceptionMessage
                }
            else -> Unit
        }
    }
}
