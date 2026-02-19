package org.opencds.cqf.fhir.cr.common;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReferenceExt;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.cqfMessagesExtension;
import static org.opencds.cqf.fhir.utility.OperationOutcomes.addExceptionToOperationOutcome;
import static org.opencds.cqf.fhir.utility.OperationOutcomes.newOperationOutcome;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

/**
 * This interface exposes common functionality across Operations
 */
@SuppressWarnings("UnstableApiUsage")
public interface IOperationRequest {
    String getOperationName();

    ModelResolver getModelResolver();

    FhirVersionEnum getFhirVersion();

    IRepository getRepository();

    default FhirContext getFhirContext() {
        return getRepository().fhirContext();
    }

    Map<String, String> getReferencedLibraries();

    IBaseOperationOutcome getOperationOutcome();

    void setOperationOutcome(IBaseOperationOutcome operationOutcome);

    default IAdapterFactory getAdapterFactory() {
        return IAdapterFactory.forFhirVersion(getFhirVersion());
    }

    default void logException(String exceptionMessage) {
        if (getOperationOutcome() == null) {
            setOperationOutcome(newOperationOutcome(getFhirVersion()));
        }
        addExceptionToOperationOutcome(getOperationOutcome(), exceptionMessage);
    }

    default void resolveOperationOutcome(IBaseResource resource) {
        var issues = resolvePathList(getOperationOutcome(), "issue");
        if (issues != null && !issues.isEmpty()) {
            getOperationOutcome()
                    .setId("%s-outcome-%s"
                            .formatted(
                                    getOperationName(), resource.getIdElement().getIdPart()));
            getModelResolver().setValue(resource, "contained", Collections.singletonList(getOperationOutcome()));
            getModelResolver()
                    .setValue(
                            resource,
                            "extension",
                            Collections.singletonList(buildReferenceExt(
                                    getFhirVersion(),
                                    cqfMessagesExtension(
                                            getOperationOutcome().getIdElement().getIdPart()),
                                    true)));
        }
    }

    @SuppressWarnings("unchecked")
    default <E extends IBaseExtension<?, ?>> List<E> getExtensions(IBase base) {
        return resolvePathList(base, "extension").stream().map(e -> (E) e).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    default <E extends IBaseExtension<?, ?>> List<E> getExtensionsByUrl(IBase base, String url) {
        return getExtensions(base).stream()
                .map(e -> (E) e)
                .filter(e -> e.getUrl().equals(url))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    default <E extends IBaseExtension<?, ?>> E getExtensionByUrl(IBase base, String url) {
        return getExtensionsByUrl(base, url).stream()
                .map(e -> (E) e)
                .findFirst()
                .orElse(null);
    }

    default boolean hasExtension(IBase base, String url) {
        return getExtensions(base).stream().anyMatch(e -> e.getUrl().equals(url));
    }

    default List<IBaseResource> getContained(IBaseResource base) {
        return resolvePathList(base, "contained", IBaseResource.class);
    }

    default boolean hasContained(IBaseResource base) {
        return !getContained(base).isEmpty();
    }

    @SuppressWarnings("unchecked")
    default List<IBase> resolvePathList(IBase base, String path) {
        var pathResult = this.getModelResolver().resolvePath(base, path);
        return pathResult instanceof List ? (List<IBase>) pathResult : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    default <T extends IBase> List<T> resolvePathList(IBase base, String path, Class<T> clazz) {
        return resolvePathList(base, path).stream().map(i -> (T) i).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    default String resolvePathString(IBase base, String path) {
        var pathResult = resolvePath(base, path);
        if (pathResult instanceof IPrimitiveType) {
            return ((IPrimitiveType<String>) pathResult).getValueAsString();
        }
        return null;
    }

    default Object resolveRawPath(Object base, String path) {
        return this.getModelResolver().resolvePath(base, path);
    }

    default IBase resolvePath(IBase base, String path) {
        return (IBase) resolveRawPath(base, path);
    }

    @SuppressWarnings("unchecked")
    default <T extends IBase> T resolvePath(IBase base, String path, Class<T> clazz) {
        return (T) resolvePath(base, path);
    }
}
