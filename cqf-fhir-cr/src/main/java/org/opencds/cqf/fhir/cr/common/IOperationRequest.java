package org.opencds.cqf.fhir.cr.common;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReferenceExt;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.cqfMessagesExtension;
import static org.opencds.cqf.fhir.utility.OperationOutcomes.addExceptionToOperationOutcome;
import static org.opencds.cqf.fhir.utility.OperationOutcomes.newOperationOutcome;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.Collections;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;

/**
 * This interface exposes common functionality across Operations
 */
@SuppressWarnings("UnstableApiUsage")
public interface IOperationRequest {
    String getOperationName();

    FhirVersionEnum getFhirVersion();

    IRepository getRepository();

    default FhirContext getFhirContext() {
        return getRepository().fhirContext();
    }

    Map<String, String> getReferencedLibraries();

    IBaseOperationOutcome getOperationOutcome();

    void setOperationOutcome(IBaseOperationOutcome operationOutcome);

    default IAdapterFactory getAdapterFactory() {
        return IAdapterFactory.forFhirVersion(getFhirVersion());
    }

    default void logException(String exceptionMessage) {
        if (getOperationOutcome() == null) {
            setOperationOutcome(newOperationOutcome(getFhirVersion()));
        }
        addExceptionToOperationOutcome(getOperationOutcome(), exceptionMessage);
    }

    default void resolveOperationOutcome(IResourceAdapter adapter) {
        var issues = adapter.resolvePathList(getOperationOutcome(), "issue");
        if (issues != null && !issues.isEmpty()) {
            getOperationOutcome()
                    .setId("%s-outcome-%s"
                            .formatted(
                                    getOperationName(), adapter.getId()));
            adapter.addContained(getOperationOutcome());
            adapter.addExtension(buildReferenceExt(getFhirVersion(), cqfMessagesExtension(getOperationOutcome().getIdElement().getIdPart()), true));
        }
    }
}
