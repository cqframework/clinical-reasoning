package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.LibraryEngine;

import ca.uhn.fhir.context.FhirVersionEnum;

public class DynamicValueProcessor {
    protected final LibraryEngine libraryEngine;
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;

    public DynamicValueProcessor(LibraryEngine libraryEngine, ModelResolver modelResolver, FhirVersionEnum fhirVersion) {
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        this.fhirVersion = fhirVersion;
    }

    public void processDynamicValues(String subjectId, IBaseBundle bundle, IBaseResource resource, IBaseResource activityDefinition, String defaultLibraryUrl, IBaseParameters inputParams) {
        var dynamicValues = getDynamicValues(activityDefinition);
        for (var dynamicValue : dynamicValues) {
            var path = getDynamicValuePath(dynamicValue);
            var cqfExpression = getDynamicValueExpression(dynamicValue, defaultLibraryUrl);
            if (path != null && cqfExpression != null) {
                var expressionResult = libraryEngine.resolveExpression(
                        subjectId,
                        cqfExpression,
                        inputParams,
                        bundle,
                        resource);
                resolveDynamicValue(expressionResult, cqfExpression.getExpression(), path, resource);
            }
        }
    }

    protected List<IBaseBackboneElement> getDynamicValues(IBaseResource activityDefinition) {
        List<IBaseBackboneElement> dynamicValues = new ArrayList<>();
        var pathResult = modelResolver.resolvePath(activityDefinition, "dynamicValue");
        var list = (pathResult instanceof List ? (List<?>) pathResult : null);
        if (list != null && !list.isEmpty()) {
            dynamicValues.addAll(list.stream().map(o -> (IBaseBackboneElement) o).collect(Collectors.toList()));
        }

        return dynamicValues;
    }

    protected String getDynamicValuePath(IBaseBackboneElement dynamicValue) {
        switch (fhirVersion) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.ActivityDefinition.ActivityDefinitionDynamicValueComponent) dynamicValue).getPath();
            case R4:
                return ((org.hl7.fhir.r4.model.ActivityDefinition.ActivityDefinitionDynamicValueComponent) dynamicValue).getPath();
            case R5:        
                return ((org.hl7.fhir.r5.model.ActivityDefinition.ActivityDefinitionDynamicValueComponent) dynamicValue).getPath();
            default:
                return null;
        }
    }

    protected CqfExpression getDynamicValueExpression(IBaseBackboneElement dynamicValue, String defaultLibraryUrl) {
        switch (fhirVersion) {
            case DSTU3:
                var dstu3Value = (org.hl7.fhir.dstu3.model.ActivityDefinition.ActivityDefinitionDynamicValueComponent) dynamicValue;
                return new CqfExpression(dstu3Value.getLanguage(), dstu3Value.getExpression(), defaultLibraryUrl);
            case R4:
                var r4Value = ((org.hl7.fhir.r4.model.ActivityDefinition.ActivityDefinitionDynamicValueComponent) dynamicValue).getExpression();
                return r4Value == null ? null : new CqfExpression(
                    r4Value.getLanguage(),
                    r4Value.getExpression(),
                    r4Value.hasReference() ? r4Value.getReference() : defaultLibraryUrl);
            case R5:        
                var r5Value = ((org.hl7.fhir.r5.model.ActivityDefinition.ActivityDefinitionDynamicValueComponent) dynamicValue).getExpression();
                return r5Value == null ? null : new CqfExpression(
                    r5Value.getLanguage(),
                    r5Value.getExpression(),
                    r5Value.hasReference() ? r5Value.getReference() : defaultLibraryUrl);
            default:
                return null;
        }
    }

    protected void resolveDynamicValue(List<IBase> result, String expression, String path, IBaseResource resource) {
        if (result == null || result.isEmpty()) {
            return;
        }
        if (result.size() > 1) {
            throw new IllegalArgumentException(
                    String.format("Dynamic value resolution received multiple values for expression: %s", expression));
        }
        modelResolver.setValue(resource, path, result.get(0));
    }
}
