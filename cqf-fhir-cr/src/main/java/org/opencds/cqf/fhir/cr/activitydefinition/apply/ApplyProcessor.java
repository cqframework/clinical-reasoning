package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import ca.uhn.fhir.repository.Repository;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.DynamicValueProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplyProcessor implements IApplyProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ApplyProcessor.class);
    protected static final List<String> EXCLUDED_EXTENSION_LIST =
            Arrays.asList(Constants.CPG_KNOWLEDGE_CAPABILITY, Constants.CPG_KNOWLEDGE_REPRESENTATION_LEVEL);

    protected final Repository repository;
    protected final IRequestResolverFactory resolverFactory;
    protected final ExtensionProcessor extensionProcessor;
    protected final DynamicValueProcessor dynamicValueProcessor;

    public ApplyProcessor(Repository repository, IRequestResolverFactory resolverFactory) {
        this.repository = repository;
        this.resolverFactory = resolverFactory;
        this.extensionProcessor = new ExtensionProcessor();
        this.dynamicValueProcessor = new DynamicValueProcessor();
    }

    @Override
    public IBaseResource apply(ApplyRequest request) {
        logger.info(
                "Performing $apply operation on ActivityDefinition/{}",
                request.getActivityDefinition().getIdElement().getIdPart());

        var result = resolverFactory.create(request.getActivityDefinition()).resolve(request);
        var id = Ids.newId(
                request.getFhirVersion(),
                result.fhirType(),
                request.getActivityDefinition().getIdElement().getIdPart());
        result.setId(id);
        resolveMeta(request.getActivityDefinition(), result);
        extensionProcessor.processExtensions(request, result, request.getActivityDefinition(), EXCLUDED_EXTENSION_LIST);
        dynamicValueProcessor.processDynamicValues(request, result, request.getActivityDefinition());
        request.resolveOperationOutcome(result);

        return result;
    }

    protected void resolveMeta(IBaseResource activityDefinition, IBaseResource resource) {
        switch (repository.fhirContext().getVersion().getVersion()) {
            case DSTU3:
                // Dstu3 does not have a profile property on ActivityDefinition so we are not resolving meta
                break;
            case R4:
                if (((org.hl7.fhir.r4.model.ActivityDefinition) activityDefinition).hasProfile()) {
                    var meta = new org.hl7.fhir.r4.model.Meta();
                    meta.addProfile(((org.hl7.fhir.r4.model.ActivityDefinition) activityDefinition).getProfile());
                    ((org.hl7.fhir.r4.model.DomainResource) resource).setMeta(meta);
                }
                break;
            case R5:
                if (((org.hl7.fhir.r5.model.ActivityDefinition) activityDefinition).hasProfile()) {
                    var meta = new org.hl7.fhir.r5.model.Meta();
                    meta.addProfile(((org.hl7.fhir.r5.model.ActivityDefinition) activityDefinition).getProfile());
                    ((org.hl7.fhir.r5.model.DomainResource) resource).setMeta(meta);
                }
                break;
            default:
                break;
        }
    }
}
