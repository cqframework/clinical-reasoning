package org.opencds.cqf.fhir.cql.npm;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.jetbrains.annotations.NotNull;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NpmBackedRepository implements INpmRepository {

    /**
     * Internal wrapper class to hold resource and provide access to
     * canonical url field (if available).
     */
    private class WrappedResource {

        /**
         * The resource being wrapped
         */
        private final IBaseResource resource;

        /**
         * The canonical url with version (if available);
         * eg: http://example.com/resource|1.2.3
         */
        private String canonicalWithVersion;

        public WrappedResource(IBaseResource resource) {
            this.resource = resource;

            RuntimeResourceDefinition def = fhirContext.getResourceDefinition(resource);

            Optional<BaseRuntimeChildDefinition> urlFieldOp = def.getChildren().stream()
                    .filter(f -> f.getElementName().equals("url"))
                    .findFirst();

            if (urlFieldOp.isPresent()) {
                BaseRuntimeChildDefinition urlField = urlFieldOp.get();
                Optional<IBase> valueOp = urlField.getAccessor().getFirstValueOrNull(resource);
                valueOp.ifPresent(v -> {
                    if (v instanceof IPrimitiveType<?> pt) {
                        canonicalWithVersion = pt.getValueAsString();
                    }
                });
            }
        }

        public IBaseResource getResource() {
            return resource;
        }

        public boolean hasCanonicalUrl() {
            return !isEmpty(canonicalWithVersion);
        }

        public String getCanonicalUrl(boolean withVersion) {
            String url = canonicalWithVersion;
            if (!withVersion && hasVersion(url)) {
                return getUrlWithoutVersion(url);
            }
            return url;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(NpmBackedRepository.class);

    private final FhirContext fhirContext;
    private final EvaluationSettings settings;
    private NpmProcessor npmProcessor;

    // cache these because the NpmPackage holds the unparsed files
    // and parsing each time is work
    private final Multimap<String, WrappedResource> resourceType2Resource = HashMultimap.create();
    private final Multimap<String, WrappedResource> canonicalUrl2Resource = HashMultimap.create();

    public NpmBackedRepository(FhirContext context, EvaluationSettings settings) {
        this.fhirContext = context;
        this.settings = settings;
    }

    public void loadIg(String folder, String pkgName) {
        ensureInitialized();

        try {
            NpmPackage pkg = NpmPackage.fromFolder(Paths.get(folder, pkgName).toString(), false);
            pkg.loadAllFiles();
            npmProcessor.getPackageManager().getNpmList().add(pkg);
        } catch (Exception ex) {
            String msg = String.format("Could not load package %s in folder %s.", pkgName, folder);
            log.error(msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    private void ensureInitialized() {
        if (npmProcessor != null) {
            return;
        }

        npmProcessor = settings.getNpmProcessor();
        if (npmProcessor == null) {
            // for some reason we require a 'base' sourceig...
            // and this base *must be* R5
            org.hl7.fhir.r5.model.ImplementationGuide guide = new org.hl7.fhir.r5.model.ImplementationGuide();
            guide.addFhirVersion(getFhirVersionFromFhirVersion());
            guide.setName("default"); // does this matter?
            IGContext igContext = new IGContext();
            igContext.setSourceIg(guide);
            npmProcessor = new NpmProcessor(igContext);
            settings.setNpmProcessor(npmProcessor);
        }
    }

    private org.hl7.fhir.r5.model.Enumerations.FHIRVersion getFhirVersionFromFhirVersion() {
        FhirVersionEnum fv = fhirContext.getVersion().getVersion();
        switch (fv) {
            case DSTU3 -> {
                return Enumerations.FHIRVersion._3_0_0;
            }
            case R4 -> {
                return org.hl7.fhir.r5.model.Enumerations.FHIRVersion._4_0_0;
            }
            case R5 -> {
                return Enumerations.FHIRVersion._5_0_0;
            }
            default -> {
                String msg = String.format("Unsupported FHIR version %s", fv.getFhirVersionString());
                log.error(msg);
                throw new InvalidParameterException(msg);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> List<T> resolveByUrl(@Nonnull Class<T> clazz, String url) {
        requireNonNull(clazz, "clazz cannot be null");
        String type = clazz.getSimpleName();
        boolean hasUrl = !isEmpty(url);

        if (!resourceType2Resource.containsKey(type)) {
            populateCaches(clazz, type);
        }

        Collection<WrappedResource> resources = null;
        if (hasUrl) {
            String searchUrl = url;
            boolean hasVersion = hasVersion(searchUrl);
            if (hasVersion) {
                searchUrl = getUrlWithoutVersion(url);
            }
            resources = canonicalUrl2Resource.get(searchUrl);
            if (hasVersion) {
                resources = resources.stream()
                        .filter(wr -> wr.getCanonicalUrl(true).equals(url))
                        .collect(Collectors.toList());
            }
        } else {
            resources = resourceType2Resource.get(type);
        }

        if (isListEmpty(resources)) {
            return List.of();
        }

        return resources.stream().map(wr -> (T) wr.getResource()).toList();
    }

    private <T extends IBaseResource> void populateCaches(@NotNull Class<T> clazz, String type) {
        var list = npmProcessor.getPackageManager().getNpmList();

        for (var pkg : list) {
            if (!pkg.getTypes().containsKey(type)) {
                // if it doesn't have the provided resource type we can skip it
                continue;
            }

            List<T> pkgResources = getResourcesFromPkg(pkg, clazz);
            if (!isListEmpty(pkgResources)) {
                for (T resource : pkgResources) {
                    WrappedResource wrappedResource = new WrappedResource(resource);
                    resourceType2Resource.put(type, wrappedResource);
                    if (wrappedResource.hasCanonicalUrl()) {
                        canonicalUrl2Resource.put(wrappedResource.getCanonicalUrl(false), wrappedResource);
                    }
                }
            }
        }
    }

    private <T extends IBaseResource> List<T> getResourcesFromPkg(NpmPackage pkg, Class<T> clazz) {
        IParser parser = fhirContext.newJsonParser();

        List<T> resources = new ArrayList<>();
        String type = clazz.getSimpleName();
        List<String> files = pkg.getTypes().get(type);
        for (String file : files) {
            try (InputStream is = pkg.loadResource(file)) {
                String resourceStr = new String(is.readAllBytes());

                T resource = parser.parseResource(clazz, resourceStr);
                resources.add(resource);
            } catch (IOException | DataFormatException ex) {
                String msg = String.format("Could not parse resource from package %s", pkg.url());
                log.error(msg, ex);
                throw new RuntimeException(msg, ex);
            }
        }

        return resources;
    }

    private boolean isListEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    private boolean hasVersion(String url) {
        return url.contains("|");
    }

    private String getUrlWithoutVersion(String url) {
        int barIndex = url.indexOf("|");
        if (barIndex != -1) {
            return url.substring(0, barIndex);
        }
        return url;
    }
}
