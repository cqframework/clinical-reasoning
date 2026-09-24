package org.opencds.cqf.fhir.cr.common;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IElement;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.fhir.cql.ExtensionResolver;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;

public class ExtensionProcessor {
    private final ExtensionPropagationPolicy propagationPolicy;

    public ExtensionProcessor() {
        this(ExtensionPropagationPolicy.legacy());
    }

    public ExtensionProcessor(ExtensionPropagationPolicy propagationPolicy) {
        this.propagationPolicy = requireNonNull(propagationPolicy, "propagationPolicy can not be null");
    }

    /**
     * This method gets extensions from the definition element, resolves any CQF Expression extensions found and copies the resolved extensions to the resource.
     * @param request The operation request containing data needed for evaluation
     * @param adapter The fhir adapter to copy the resolved extensions to
     * @param definition The element containing the extensions to be resolved
     * @param excludedExtList A list of extension URL's to excluded from the definition
     */
    public void processExtensions(
            ICqlOperationRequest request, IAdapter<?> adapter, IElement definition, List<String> excludedExtList) {
        processExtensions(
                request,
                adapter,
                definition,
                excludedExtList,
                definition.fhirType(),
                adapter.get().fhirType());
    }

    /** Resolves extensions selected for the specified source and destination element paths. */
    public void processExtensions(
            ICqlOperationRequest request,
            IAdapter<?> adapter,
            IElement definition,
            List<String> excludedExtList,
            String sourcePath,
            String targetPath) {
        var extensions = copyExtensions(
                adapter.fhirContext(), adapter.getExtension(definition), excludedExtList, sourcePath, targetPath);
        processExtensions(request, adapter, extensions);
    }

    /**
     * Selects and deep-copies extensions without resolving expressions. This also supports sites
     * such as goal targets, where application historically copies literal extension values.
     */
    public <E extends IBaseExtension<?, ?>> List<E> copyExtensions(
            FhirContext fhirContext,
            List<E> extensions,
            List<String> excludedExtList,
            String sourcePath,
            String targetPath) {
        return extensions.stream()
                .filter(e -> !excludedExtList.contains(e.getUrl()))
                .filter(e -> propagationPolicy.shouldPropagate(new ExtensionPropagationContext(
                        e.getUrl(), fhirContext.getVersion().getVersion(), sourcePath, targetPath)))
                .map(e -> copyExtension(fhirContext, e))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <E extends IBaseExtension<?, ?>> E copyExtension(FhirContext fhirContext, E extension) {
        var copy = fhirContext.getElementDefinition(extension.getClass()).newInstance();
        return (E) fhirContext.newTerser().cloneInto(extension, copy, false);
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
