package org.opencds.cqf.fhir.cr.questionnaire.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;

public class PopulateRequest {
    private final IBaseResource questionnaire;
    private final IIdType subjectId;
    private final IBaseParameters parameters;
    private final IBaseBundle bundle;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final String defaultLibraryUrl;

    public PopulateRequest(
            IBaseResource questionnaire,
            IIdType subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        this.questionnaire = questionnaire;
        this.subjectId = subjectId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        this.fhirVersion = questionnaire.getStructureFhirVersionEnum();
        this.defaultLibraryUrl = resolveDefaultLibraryUrl();
    }

    public IBaseResource getQuestionnaire() {
        return questionnaire;
    }

    public IIdType getSubjectId() {
        return subjectId;
    }

    public IBaseBundle getBundle() {
        return bundle;
    }

    public IBaseParameters getParameters() {
        return parameters;
    }

    public LibraryEngine getLibraryEngine() {
        return libraryEngine;
    }

    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    public FhirVersionEnum getFhirVersion() {
        return fhirVersion;
    }

    public String getDefaultLibraryUrl() {
        return defaultLibraryUrl;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected final String resolveDefaultLibraryUrl() {
        var pathResult = modelResolver.resolvePath(questionnaire, "extension");
        var libraryExt = (pathResult instanceof List ? (List<?>) pathResult : new ArrayList<>())
                .stream()
                        .map(e -> (IBaseExtension) e)
                        .filter(e -> e.getUrl().equals(Constants.CQF_LIBRARY))
                        .findFirst()
                        .orElse(null);
        return libraryExt == null ? null : ((IPrimitiveType<String>) libraryExt.getValue()).getValue();
    }

    public List<IBaseBackboneElement> getItems(IBase base) {
        return resolvePathList(base, "item").stream()
                .map(i -> (IBaseBackboneElement) i)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<IBase> resolvePathList(IBase base, String path) {
        var pathResult = this.modelResolver.resolvePath(base, path);
        return pathResult instanceof List ? (List<IBase>) pathResult : new ArrayList<>();
    }

    public IBase resolvePath(IBase base, String path) {
        return (IBase) this.modelResolver.resolvePath(base, path);
    }
}
