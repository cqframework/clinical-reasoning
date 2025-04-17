package org.opencds.cqf.fhir.cql;

import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;

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
            IBase resource, List<E> extensions, Map<String, String> referencedLibraries) {
        for (var extension : extensions) {
            var nestedExtensions = extension.getExtension();
            if (nestedExtensions != null && !nestedExtensions.isEmpty()) {
                resolveExtensions(resource, nestedExtensions, referencedLibraries);
            }
            var value = extension.getValue();
            if (value instanceof IBaseHasExtensions) {
                var valueExtensions = ((IBaseHasExtensions) value).getExtension();
                if (valueExtensions != null) {
                    var expressionExtensions = valueExtensions.stream()
                            .filter(e -> e.getUrl() != null && e.getUrl().equals(Constants.CQF_EXPRESSION))
                            .findFirst();
                    if (expressionExtensions.isPresent()) {
                        var result = getExpressionResult(expressionExtensions.get(), referencedLibraries, resource);
                        if (result != null) {
                            extension.setValue(result);
                        }
                    }
                }
            }
        }
    }

    protected <E extends IBaseExtension<?, ?>> IBaseDatatype getExpressionResult(
            E expressionExtension, Map<String, String> referencedLibraries, IBase resource) {
        var result = libraryEngine.resolveExpression(
                subjectId.getIdPart(),
                CqfExpression.of(expressionExtension, referencedLibraries),
                parameters,
                null,
                bundle,
                resource,
                null);

        return result != null && !result.isEmpty() ? (IBaseDatatype) result.get(0) : null;
    }
}
