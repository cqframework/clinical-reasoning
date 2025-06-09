package org.opencds.cqf.fhir.utility.repository;

import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ACTIVITY_DEFINITION;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_CANONICAL;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_CONTENT_ENDPOINT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_DATA;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_DATA_ENDPOINT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ENCOUNTER;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ORGANIZATION;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PARAMETERS;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PRACTITIONER;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SETTING;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SETTING_CONTEXT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SUBJECT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_TERMINOLOGY_ENDPOINT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USER_LANGUAGE;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USER_TASK_CONTEXT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USER_TYPE;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USE_SERVER_DATA;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessorFactory;
import org.opencds.cqf.fhir.utility.repository.operations.OperationParametersParser;

public class RepositoryOperationProvider implements IRepositoryOperationProvider {
    private final FhirContext fhirContext;
    private final OperationParametersParser operationParametersParser;
    private IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory;

    public RepositoryOperationProvider(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
        this.operationParametersParser = new OperationParametersParser(
                IAdapterFactory.forFhirVersion(this.fhirContext.getVersion().getVersion()));
    }

    public void setActivityDefinitionProcessorFactory(
            IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory) {
        this.activityDefinitionProcessorFactory = activityDefinitionProcessorFactory;
    }

    @SuppressWarnings("unchecked")
    public <C extends IPrimitiveType<String>, R extends IBaseResource> R invokeOperation(
            IRepository repository, IIdType id, String resourceType, String operationName, IBaseParameters parameters) {
        if (resourceType.equals("ActivityDefinition") && activityDefinitionProcessorFactory != null) {
            var processor = activityDefinitionProcessorFactory.create(repository);
            var paramMap = operationParametersParser.getParameterParts(parameters);
            switch (operationName) {
                case "$apply":
                    var activityDefinition = Eithers.for3((C) paramMap.get(APPLY_PARAMETER_CANONICAL), id, (R)
                            paramMap.get(APPLY_PARAMETER_ACTIVITY_DEFINITION));
                    var subject = ((IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_SUBJECT)).getValue();
                    var encounter = (IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_ENCOUNTER);
                    var practitioner = (IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_PRACTITIONER);
                    var organization = (IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_ORGANIZATION);
                    return (R) processor.apply(
                            activityDefinition,
                            subject,
                            encounter == null ? null : encounter.getValue(),
                            practitioner == null ? null : practitioner.getValue(),
                            organization == null ? null : organization.getValue(),
                            (IBaseDatatype) paramMap.get(APPLY_PARAMETER_USER_TYPE),
                            (IBaseDatatype) paramMap.get(APPLY_PARAMETER_USER_LANGUAGE),
                            (IBaseDatatype) paramMap.get(APPLY_PARAMETER_USER_TASK_CONTEXT),
                            (IBaseDatatype) paramMap.get(APPLY_PARAMETER_SETTING),
                            (IBaseDatatype) paramMap.get(APPLY_PARAMETER_SETTING_CONTEXT),
                            (IBaseParameters) paramMap.get(APPLY_PARAMETER_PARAMETERS),
                            (Boolean) paramMap.get(APPLY_PARAMETER_USE_SERVER_DATA),
                            (IBaseBundle) paramMap.get(APPLY_PARAMETER_DATA),
                            (IBaseResource) paramMap.get(APPLY_PARAMETER_DATA_ENDPOINT),
                            (IBaseResource) paramMap.get(APPLY_PARAMETER_CONTENT_ENDPOINT),
                            (IBaseResource) paramMap.get(APPLY_PARAMETER_TERMINOLOGY_ENDPOINT));

                default:
                    throw new IllegalArgumentException(
                            "(%s) operation not supported for type (%s)".formatted(operationName, resourceType));
            }
        }
        return null;
    }
}
