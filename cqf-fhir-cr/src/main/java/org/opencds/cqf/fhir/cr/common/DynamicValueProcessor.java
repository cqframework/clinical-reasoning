package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IElement;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides processing for dynamicValues in PlanDefinition.action elements and ActivityDefinition resources.
 */
public class DynamicValueProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DynamicValueProcessor.class);

    public DynamicValueProcessor() {}

    /**
     * Processes all dynamicValues on a definition element and sets the resulting values to the corresponding path on the resource passed in
     *
     * @param request the $apply request parameters
     * @param resource the resource to apply the resolved value to
     * @param definitionElement the definition of the dynamicValue containing the expression and path
     */
    public void processDynamicValues(ICpgRequest request, IBaseResource resource, IElement definitionElement) {
        var context = definitionElement instanceof IBaseResource ? (IBaseResource) definitionElement : resource;
        processDynamicValues(request, context, resource, definitionElement, null);
    }

    /**
     * Processes all dynamicValues on a definition element and sets the resulting values to the corresponding path on the resource or requestAction passed in
     *
     * @param request the $apply request parameters
     * @param context the original resource the dynamicValue is from
     * @param resource the resource to apply the resolved value to
     * @param definitionElement the definition of the dynamicValue containing the expression and path
     * @param requestAction the action of the RequestOrchestration created from the definition action
     */
    public void processDynamicValues(
            ICpgRequest request,
            IBaseResource context,
            IBaseResource resource,
            IElement definitionElement,
            IElement requestAction) {
        var dynamicValues = request.getDynamicValues(definitionElement);
        for (var dynamicValue : dynamicValues) {
            try {
                resolveDynamicValue(request, dynamicValue, context, resource, requestAction);
            } catch (Exception e) {
                var message = String.format(
                        "DynamicValue resolution for path %s encountered exception: %s",
                        request.resolvePathString(dynamicValue, "path"), e.getMessage());
                logger.error(message);
                request.logException(message);
            }
        }
    }

    protected CqfExpression getDynamicValueExpression(ICpgRequest request, IBaseBackboneElement dynamicValue) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return new CqfExpression(
                        request.resolvePathString(dynamicValue, "language"),
                        request.resolvePathString(dynamicValue, "expression"),
                        request.getDefaultLibraryUrl());
            case R4:
                return CqfExpression.of(
                        request.resolvePath(dynamicValue, "expression", org.hl7.fhir.r4.model.Expression.class),
                        request.getDefaultLibraryUrl());
            case R5:
                return CqfExpression.of(
                        request.resolvePath(dynamicValue, "expression", org.hl7.fhir.r5.model.Expression.class),
                        request.getDefaultLibraryUrl());
            default:
                return null;
        }
    }

    protected void resolveDynamicValue(
            ICpgRequest request,
            IBaseBackboneElement dynamicValue,
            IBaseResource context,
            IBaseResource resource,
            IElement requestAction) {
        var path = request.resolvePathString(dynamicValue, "path");
        // Strip % so it is supported as defined in the spec
        path = path.replace("%", "");
        var cqfExpression = getDynamicValueExpression(request, dynamicValue);
        if (path != null && cqfExpression != null) {
            var result = getDynamicValueExpressionResult(request, cqfExpression, context, resource);
            if (result == null || result.isEmpty()) {
                return;
            }
            var value = result.size() == 1 ? result.get(0) : result;
            if (requiresRequestAction(path, resource)) {
                if (requestAction == null) {
                    throw new IllegalArgumentException(String.format(
                            "Error resolving dynamicValue with path %s: expected requestAction not found", path));
                }
                if (isPriorityExtension(path)) {
                    // Custom logic to handle setting the indicator of a CDS Card because RequestGroup.action does not
                    // have a priority property in DSTU3
                    if (request.getFhirVersion() != FhirVersionEnum.DSTU3) {
                        throw new IllegalArgumentException(
                                "Please use the priority path when setting indicator values when using FHIR R4 or higher for CDS Hooks evaluation");
                    }
                    // default to adding extension to last action
                    ((org.hl7.fhir.dstu3.model.Element) requestAction)
                            .addExtension()
                            .setValue((org.hl7.fhir.dstu3.model.Type) value);
                } else {
                    request.getModelResolver().setValue(requestAction, path.replace("action.", ""), value);
                }
            } else {
                request.getModelResolver().setValue(resource, path, value);
            }
        }
    }

    protected List<IBase> getDynamicValueExpressionResult(
            ICpgRequest request, CqfExpression cqfExpression, IBaseResource context, IBaseResource resource) {
        return request.getLibraryEngine()
                .resolveExpression(
                        request.getSubjectId().getIdPart(),
                        cqfExpression,
                        request.getParameters(),
                        request.getBundle(),
                        context,
                        resource);
    }

    private Boolean requiresRequestAction(String path, IBaseResource resource) {
        return isPriorityExtension(path) || isAction(path, resource);
    }

    private Boolean isPriorityExtension(String path) {
        return path.startsWith("activity.extension") || path.startsWith("action.extension");
    }

    private Boolean isAction(String path, IBaseResource resource) {
        return path.startsWith("action.") || resource == null;
    }
}
