package org.opencds.cqf.fhir.cql;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Constants;

/**
 * This class is used to resolve any CQFExpression extensions that exist on an extension.
 */
public class ExtensionResolver {
    private final IIdType subjectId;
    private final IBaseParameters parameters;
    private final IBaseBundle bundle;
    private final LibraryEngine libraryEngine;

    public ExtensionResolver(
            IIdType subjectId, IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine) {
        this.subjectId = subjectId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.libraryEngine = libraryEngine;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends IBaseExtension> void resolveExtensions(
            IBase resource, List<E> extensions, String defaultLibraryUrl) {
        for (var extension : extensions) {
            var nestedExtensions = extension.getExtension();
            if (nestedExtensions != null && !nestedExtensions.isEmpty()) {
                resolveExtensions(resource, nestedExtensions, defaultLibraryUrl);
            }
            var value = extension.getValue();
            if (value instanceof IBaseHasExtensions) {
                var valueExtensions = ((IBaseHasExtensions) value).getExtension();
                if (valueExtensions != null) {
                    var expressionExtensions = valueExtensions.stream()
                            .filter(e -> e.getUrl() != null && e.getUrl().equals(Constants.CQF_EXPRESSION))
                            .collect(Collectors.toList());
                    if (expressionExtensions != null && !expressionExtensions.isEmpty()) {
                        var expression = expressionExtensions.get(0).getValue();
                        if (expression != null) {
                            var result = getExpressionResult(expression, defaultLibraryUrl, resource);
                            if (result != null) {
                                extension.setValue(result);
                            }
                        }
                    }
                }
            }
        }
    }

    protected IBaseDatatype getExpressionResult(IBaseDatatype expression, String defaultLibraryUrl, IBase resource) {
        List<IBase> result = null;
        if (expression instanceof org.hl7.fhir.r4.model.Expression) {
            result = libraryEngine.resolveExpression(
                    subjectId.getIdPart(),
                    CqfExpression.of((org.hl7.fhir.r4.model.Expression) expression, defaultLibraryUrl),
                    parameters,
                    bundle,
                    resource,
                    null);
        }

        if (expression instanceof org.hl7.fhir.r5.model.Expression) {
            result = libraryEngine.resolveExpression(
                    subjectId.getIdPart(),
                    CqfExpression.of((org.hl7.fhir.r5.model.Expression) expression, defaultLibraryUrl),
                    parameters,
                    bundle,
                    resource,
                    null);
        }

        return result != null && !result.isEmpty() ? (IBaseDatatype) result.get(0) : null;
    }
}
