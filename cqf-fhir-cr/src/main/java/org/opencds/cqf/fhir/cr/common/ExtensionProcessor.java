package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.model.api.IElement;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.fhir.cql.ExtensionResolver;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;

public class ExtensionProcessor {
    /**
     * This method gets extensions from the definition element, resolves any CQF Expression extensions found and copies the resolved extensions to the resource.
     * @param request The operation request containing data needed for evaluation
     * @param adapter The fhir adapter to copy the resolved extensions to
     * @param definition The element containing the extensions to be resolved
     * @param excludedExtList A list of extension URL's to excluded from the definition
     */
    public void processExtensions(
            ICqlOperationRequest request, IAdapter<?> adapter, IElement definition, List<String> excludedExtList) {
        var extensions = adapter.getExtension(definition).stream()
                .filter(e -> !excludedExtList.contains(e.getUrl()))
                .collect(Collectors.toList());
        processExtensions(request, adapter, extensions);
    }

    private void processExtensions(
            ICqlOperationRequest request, IAdapter<?> adapter, List<? extends IBaseExtension<?, ?>> extensions) {
        if (extensions.isEmpty()) {
            return;
        }
        var extensionResolver = new ExtensionResolver(
                request.getSubjectId(), request.getParameters(), request.getData(), request.getLibraryEngine());
        extensionResolver.resolveExtensions(adapter.get(), extensions, request.getReferencedLibraries());
        adapter.setExtension(extensions);
    }
}
