package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.path.EncodeContextPath;
import ca.uhn.fhir.parser.path.EncodeContextPathElement;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor.DiffCache;
import org.opencds.cqf.fhir.cr.common.CreateChangelogProcessor.ChangeLog;
import org.opencds.cqf.fhir.cr.common.ICreateChangelogProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.crmi.KnowledgeArtifactProcessor;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.springframework.beans.BeanWrapperImpl;

@SuppressWarnings("UnstableApiUsage")
public class HapiCreateChangelogProcessor implements ICreateChangelogProcessor {

    private final IRepository repository;
    private final FhirVersionEnum fhirVersion;
    private final PackageProcessor packageProcessor;

    private final HapiArtifactDiffProcessor hapiArtifactDiffProcessor;

    public HapiCreateChangelogProcessor(IRepository repository) {
        this.repository = repository;
        this.fhirVersion = repository.fhirContext().getVersion().getVersion();
        this.packageProcessor = new PackageProcessor(repository);
        this.hapiArtifactDiffProcessor = new HapiArtifactDiffProcessor(repository);
    }

    @Override
    public IBaseResource createChangelog(IBaseResource source, IBaseResource target, Endpoint terminologyEndpoint) {

        // 1) Use package to get a pair of bundles
        ExecutorService service = Executors.newCachedThreadPool();
        List<Future<IBaseBundle>> packages;
        Bundle sourceBundle;
        Bundle targetBundle;
        Parameters params = new Parameters();
        params.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);
        try {
            packages = service.invokeAll(Arrays.asList(
                    () -> packageProcessor.packageResource(source, params),
                    () -> packageProcessor.packageResource(target, params)));
            sourceBundle = (Bundle) packages.get(0).get();
            targetBundle = (Bundle) packages.get(1).get();
            service.shutdownNow();
        } catch (InterruptedException | ExecutionException e) {
            service.shutdownNow();
            throw new UnprocessableEntityException(e.getMessage());
        }

        // 2) Fill the cache with the bundle contents
        var cache = new DiffCache();
        Optional<Library> sourceResource = Optional.empty();
        Optional<Library> targetResource = Optional.empty();
        for (final var entry : sourceBundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof MetadataResource metadataResource) {
                cache.addSource(metadataResource.getUrl() + "|" + metadataResource.getVersion(), metadataResource);
                if (metadataResource.getIdPart().equals(source.getIdElement().getIdPart())) {
                    sourceResource = Optional.of((Library) metadataResource);
                }
            }
        }
        for (final var entry : targetBundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof MetadataResource metadataResource) {
                cache.addTarget(metadataResource.getUrl() + "|" + metadataResource.getVersion(), metadataResource);
                if (metadataResource.getIdPart().equals(target.getIdElement().getIdPart())) {
                    targetResource = Optional.of((Library) metadataResource);
                }
            }
        }

        // 3) Use cached resources to create diff and changelog
        var targetAdapter = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4)
                .createKnowledgeArtifactAdapter(targetResource.orElse(null));
        var diffParameters = hapiArtifactDiffProcessor.getArtifactDiff(
                sourceResource.orElse(null), targetResource.orElse(null), true, true, cache, terminologyEndpoint);
        var manifestUrl = targetAdapter.getUrl();
        var changelog = new ChangeLog(manifestUrl);
        processChanges(((Parameters) diffParameters).getParameter(), changelog, cache, manifestUrl);

        // 4) Handle the Conditions and Priorities which are in RelatedArtifact changes
        changelog.handleRelatedArtifacts();

        // 5) Generate the output JSON
        var bin = new Binary();
        var mapper = createSerializer();
        try {
            bin.setContent(mapper.writeValueAsString(changelog).getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new UnprocessableEntityException(e.getMessage());
        }

        return bin;
    }

    private ObjectMapper createSerializer() {
        var mapper = new ObjectMapper()
                .setDefaultPropertyInclusion(Include.NON_NULL)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        SimpleModule module = new SimpleModule("IBaseSerializer", new Version(1, 0, 0, null, null, null));
        module.addSerializer(IBase.class, new IBaseSerializer(FhirContext.forVersion(this.fhirVersion)));
        mapper.registerModule(module);
        return mapper;
    }

    private void processChanges(
            List<Parameters.ParametersParameterComponent> changes, ChangeLog changelog, DiffCache cache, String url) {
        // 1) Get the source and target resources so we can pull additional info as necessary
        var resources = cache.getResourcesForUrl(url);
        var resourceType = Canonicals.getResourceType(url);
        // Check if the resource pair was already processed
        var wasPageAlreadyProcessed = changelog.getPage(url).isPresent();
        if (!resources.isEmpty() && !wasPageAlreadyProcessed) {
            final MetadataResource sourceResource = resources.get(0).isSource
                    ? resources.get(0).resource
                    : (resources.size() > 1 ? resources.get(1).resource : null);
            final MetadataResource targetResource = resources.get(0).isSource
                    ? (resources.size() > 1 ? resources.get(1).resource : null)
                    : resources.get(0).resource;
            // don't generate changeLog pages for non-grouper ValueSets
            if (resourceType.equals("ValueSet")
                    && ((sourceResource != null && !KnowledgeArtifactProcessor.isGrouper(sourceResource))
                            || (targetResource != null && !KnowledgeArtifactProcessor.isGrouper(targetResource)))) {
                return;
            }
            // 2) Generate a page for each resource pair based on ResourceType
            var page = changelog.getPage(url).orElseGet(() -> switch (resourceType) {
                case "ValueSet" -> changelog.addPage((ValueSet) sourceResource, (ValueSet) targetResource, cache);
                case "Library" -> changelog.addPage((Library) sourceResource, (Library) targetResource);
                case "PlanDefinition" -> changelog.addPage(
                        (PlanDefinition) sourceResource, (PlanDefinition) targetResource);
                default -> changelog.addPage(sourceResource, targetResource, url);
            });
            for (var change : changes) {
                if (change.hasName()
                        && !change.getName().equals("operation")
                        && change.hasResource()
                        && change.getResource() instanceof Parameters parameters) {
                    // Nested Parameters objects get recursively processed
                    processChanges(parameters.getParameter(), changelog, cache, change.getName());
                } else if (change.getName().equals("operation")) {
                    // 3) For each operation get the relevant parameters
                    var type = getStringParameter(change, "type")
                            .orElseThrow(() -> new UnprocessableEntityException(
                                    "Type must be provided when adding an operation to the ChangeLog"));
                    var newValue = getParameter(change, "value");
                    var path = getPathParameterNoBase(change);
                    var originalValue = getParameter(change, "previousValue").map(o -> (Object) o);
                    // try to extract the original value from the
                    // source object if not present in the Diff
                    // Parameters object
                    try {
                        if (originalValue.isEmpty() && !type.equals("insert")) {
                            originalValue =
                                    Optional.of((new BeanWrapperImpl(sourceResource).getPropertyValue(path.get())));
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                        // var message = e.getMessage();
                        throw new InternalErrorException("Could not process path: " + path + ": " + e.getMessage());
                    }

                    // 4) Add a new operation to the ChangeLog
                    page.addOperation(
                            type, path.orElse(null), newValue.orElse(null), originalValue.orElse(null));
                }
            }
        }
    }

    private Optional<String> getPathParameterNoBase(Parameters.ParametersParameterComponent change) {
        return getStringParameter(change, "path").map(p -> {
            var e = new EncodeContextPath(p);
            return removeBase(e);
        });
    }

    private String removeBase(EncodeContextPath path) {
        return path.getPath().subList(1, path.getPath().size()).stream()
                .map(EncodeContextPathElement::toString)
                .collect(Collectors.joining("."));
    }

    private Optional<String> getStringParameter(Parameters.ParametersParameterComponent part, String name) {
        return part.getPart().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .filter(p -> p.getValue() instanceof IPrimitiveType)
                .map(p -> (IPrimitiveType<?>) p.getValue())
                .map(s -> (String) s.getValue())
                .findAny();
    }

    private Optional<IBase> getParameter(Parameters.ParametersParameterComponent part, String name) {
        return part.getPart().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .filter(ParametersParameterComponent::hasValue)
                .map(p -> (IBase) p.getValue())
                .findAny();
    }

    public static class IBaseSerializer extends StdSerializer<IBase> {
        private final transient IParser parser;

        public IBaseSerializer(FhirContext fhirCtx) {
            super(IBase.class);
            parser = fhirCtx.newJsonParser().setPrettyPrint(true);
        }

        @Override
        public void serialize(IBase resource, JsonGenerator jsonGenerator, SerializerProvider provider)
                throws IOException {
            String resourceJson = parser.encodeToString(resource);
            jsonGenerator.writeRawValue(resourceJson);
        }
    }
}
