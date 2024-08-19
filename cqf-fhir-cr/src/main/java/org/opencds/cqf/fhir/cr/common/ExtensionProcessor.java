package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.model.api.IElement;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.fhir.cql.ExtensionResolver;

public class ExtensionProcessor {
    public ExtensionProcessor() {}

    /**
     * This method gets extensions from the definition element, resolves any CQF Expression extensions found and copies the resolved extensions to the resource.
     * @param request The operation request containing data needed for evaluation
     * @param resource The resource to copy the resolved extensions to
     * @param definition The element containing the extensions to be resolved
     * @param excludedExtList A list of extension URL's to excluded from the definition
     */
    public void processExtensions(
            IOperationRequest request, IBase resource, IElement definition, List<String> excludedExtList) {
        var extensions = request.getExtensions(definition).stream()
                .filter(e -> !excludedExtList.contains(e.getUrl()))
                .collect(Collectors.toList());
        processExtensions(request, resource, extensions);
    }

    /**
     * This method gets extensions from the definition element, resolves any CQF Expression extensions found and copies the resolved extensions to the resource.
     * @param request The operation request containing data needed for evaluation
     * @param resource The resource to copy the resolved extensions to
     * @param definition The element containing the extensions to be resolved
     * @param extList A list of extension URL's to include from the definition
     */
    public void processExtensionsInList(
            IOperationRequest request, IBase resource, IElement definition, List<String> extList) {
        var extensions = request.getExtensions(definition).stream()
                .filter(e -> extList.contains(e.getUrl()))
                .collect(Collectors.toList());
        processExtensions(request, resource, extensions);
    }

    private void processExtensions(IOperationRequest request, IBase resource, List<IBaseExtension<?, ?>> extensions) {
        if (extensions.isEmpty()) {
            return;
        }
        var extensionResolver = new ExtensionResolver(
                request.getSubjectId(), request.getParameters(), request.getData(), request.getLibraryEngine());
        extensionResolver.resolveExtensions(resource, extensions, request.getDefaultLibraryUrl());
        request.getModelResolver().setValue(resource, "extension", extensions);
    }
}
