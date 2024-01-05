package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IElement;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.CqfExpression;

public class DynamicValueProcessor {
    public DynamicValueProcessor() {}

    public void processDynamicValues(
            IApplyOperationRequest request, IBaseResource resource, IElement definitionElement) {
        processDynamicValues(request, resource, definitionElement, null);
    }

    public void processDynamicValues(
            IApplyOperationRequest request,
            IBaseResource resource,
            IElement definitionElement,
            IElement requestAction) {
        var dynamicValues = request.getDynamicValues(definitionElement);
        for (var dynamicValue : dynamicValues) {
            resolveDynamicValue(request, dynamicValue, resource, requestAction);
        }
    }

    protected CqfExpression getDynamicValueExpression(
            IApplyOperationRequest request, IBaseBackboneElement dynamicValue) {
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
            IApplyOperationRequest request,
            IBaseBackboneElement dynamicValue,
            IBaseResource resource,
            IElement requestAction) {
        var path = request.resolvePathString(dynamicValue, "path");
        // Strip % so it is supported as defined in the spec
        path = path.replace("%", "");
        var cqfExpression = getDynamicValueExpression(request, dynamicValue);
        if (path != null && cqfExpression != null) {
            var result = request.getLibraryEngine()
                    .resolveExpression(
                            request.getSubjectId().getIdPart(),
                            cqfExpression,
                            request.getParameters(),
                            request.getBundle(),
                            resource);
            if (result == null || result.isEmpty()) {
                return;
            }
            var value = result.size() == 1 ? result.get(0) : result;
            // if (result.size() > 1) {
            //     throw new IllegalArgumentException(
            //             String.format("Dynamic value resolution received multiple values for expression: %s",
            // cqfExpression.getExpression()));
            // }
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
