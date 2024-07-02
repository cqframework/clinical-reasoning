package org.opencds.cqf.fhir.utility.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;

public interface ResourceAdapter extends Adapter<IBaseResource> {

    public IBaseResource get();

    public IBase setProperty(String name, IBase value) throws FHIRException;

    public IBase addChild(String name) throws FHIRException;

    public IBase getSingleProperty(String name) throws FHIRException;

    public IBase[] getProperty(String name) throws FHIRException;

    public IBase[] getProperty(String name, boolean checkValid) throws FHIRException;

    public IBase makeProperty(String name) throws FHIRException;

    public String[] getTypesForProperty(String name) throws FHIRException;

    public IBaseResource copy();

    public void copyValues(IBaseResource destination);

    public boolean equalsDeep(IBase other);

    public boolean equalsShallow(IBase other);

    public void setExtension(List<IBaseExtension<?, ?>> extensions);

    public <T extends IBaseExtension<?, ?>> void addExtension(T extension);

    public default List<? extends IBaseExtension<?, ?>> getExtension() {
        return getExtension(get());
    }

    public default IBaseExtension<?, ?> getExtensionByUrl(String url) {
        return getExtensionByUrl(get(), url);
    }

    public default List<? extends IBaseExtension<?, ?>> getExtensionsByUrl(String url) {
        return getExtensionsByUrl(get(), url);
    }

    public default List<? extends IBaseResource> getContained() {
        return getContained(get());
    }

    public default boolean hasContained() {
        return hasContained(get());
    }

    public ModelResolver getModelResolver();

    @SuppressWarnings("unchecked")
    public default <E extends IBaseExtension<?, ?>> List<E> getExtension(IBase base) {
        return resolvePathList(base, "extension").stream().map(e -> (E) e).collect(Collectors.toList());
    }

    public default List<IBaseExtension<?, ?>> getExtensionsByUrl(IBase base, String url) {
        return getExtension(base).stream().filter(e -> e.getUrl().equals(url)).collect(Collectors.toList());
    }

    public default IBaseExtension<?, ?> getExtensionByUrl(IBase base, String url) {
        return getExtensionsByUrl(base, url).stream().findFirst().orElse(null);
    }

    public default Boolean hasExtension(IBase base, String url) {
        return getExtension(base).stream().anyMatch(e -> e.getUrl().equals(url));
    }

    public default List<? extends IBaseResource> getContained(IBaseResource base) {
        return resolvePathList(base, "contained", IBaseResource.class);
    }

    public default Boolean hasContained(IBaseResource base) {
        return !getContained(base).isEmpty();
    }

    @SuppressWarnings("unchecked")
    public default List<IBase> resolvePathList(IBase base, String path) {
        var pathResult = getModelResolver().resolvePath(base, path);
        return pathResult instanceof List ? (List<IBase>) pathResult : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public default <T extends IBase> List<T> resolvePathList(IBase base, String path, Class<T> clazz) {
        return resolvePathList(base, path).stream().map(i -> (T) i).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public default String resolvePathString(IBase base, String path) {
        var result = (IPrimitiveType<String>) resolvePath(base, path);
        return result == null ? null : result.getValue();
    }

    public default IBase resolvePath(IBase base, String path) {
        return (IBase) getModelResolver().resolvePath(base, path);
    }

    @SuppressWarnings("unchecked")
    public default <T extends IBase> T resolvePath(IBase base, String path, Class<T> clazz) {
        return (T) resolvePath(base, path);
    }
}
