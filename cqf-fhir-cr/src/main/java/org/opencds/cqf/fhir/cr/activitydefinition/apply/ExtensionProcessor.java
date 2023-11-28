package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import static org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyProcessor.EXCLUDED_EXTENSION_LIST;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.ExtensionResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;

public class ExtensionProcessor {
    protected final LibraryEngine libraryEngine;
    protected final ModelResolver modelResolver;

    public ExtensionProcessor(LibraryEngine libraryEngine, ModelResolver modelResolver) {
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
    }

    @SuppressWarnings({"rawtypes"})
    public void processExtensions(
            IIdType subjectId,
            IBaseBundle bundle,
            IBaseResource resource,
            IBaseResource definition,
            String defaultLibraryUrl,
            IBaseParameters parameters) {
        var extensionResolver = new ExtensionResolver(subjectId, parameters, bundle, libraryEngine);
        List<IBaseExtension> extensions = new ArrayList<>();
        var pathResult = modelResolver.resolvePath(definition, "extension");
        var list = (pathResult instanceof List ? (List<?>) pathResult : null);
        if (list != null && !list.isEmpty()) {
            extensions.addAll(list.stream()
                    .map(e -> (IBaseExtension) e)
                    .filter(e -> !EXCLUDED_EXTENSION_LIST.contains(e.getUrl()))
                    .collect(Collectors.toList()));
        }
        extensionResolver.resolveExtensions(resource, extensions, defaultLibraryUrl);
        modelResolver.setValue(resource, "extension", extensions);
    }
}
