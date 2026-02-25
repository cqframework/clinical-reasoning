package org.opencds.cqf.fhir.cr.ecr.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.fhir.cr.crmi.TransformProperties;
import org.opencds.cqf.fhir.cr.ecr.FhirResourceExistsException;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4ImportBundleProducer {

    private static final Logger myLogger = LoggerFactory.getLogger(R4ImportBundleProducer.class);
    private static final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);

    private R4ImportBundleProducer() {}

    /**
     * Determines whether a given ValueSet is a grouper
     * @param resource
     * @return
     */
    public static boolean isGrouper(MetadataResource resource) {
        return resource.getResourceType() == ResourceType.ValueSet
                && resource.getUseContext().stream()
                        .anyMatch(uc -> uc.hasCode() && uc.getCode().getCode().equals(TransformProperties.grouperType));
    }

    /**
     * Old logic to determine whether a given ValueSet is a grouper.
     * We need to use this version for $ersd-v2-import as we need to ensure that the appropriate use context is added,
     * if we check for its presence, and it's missing it will not be considered a grouper and therefore is not added.
     * @param valueSet
     * @return
     */
    public static boolean hasGrouperCompose(ValueSet valueSet) {
        return valueSet.hasCompose()
                && !valueSet.getCompose().getIncludeFirstRep().getValueSet().isEmpty();
    }

    public static boolean isRootSpecificationLibrary(Resource resource) {
        return resource.hasMeta() && resource.getMeta().hasProfile(TransformProperties.usPHSpecLibProfile);
    }

    public static boolean isModelGrouperUseContextMissing(ValueSet vs) {
        return vs.getUseContext().stream()
                .noneMatch(uc -> uc.getValue() instanceof CodeableConcept
                        && uc.getValueCodeableConcept()
                                .getCodingFirstRep()
                                .getCode()
                                .equals(TransformProperties.modelGrouper)
                        && uc.getCode().getCode().equals(TransformProperties.grouperType));
    }

    private static void addModelGrouperUseContextIfMissing(ValueSet vs) {
        if (isModelGrouperUseContextMissing(vs)) {
            var usageContext = new UsageContext();

            var code = new Coding();
            code.setSystem(TransformProperties.grouperUsageContextCodeURL);
            code.setCode(TransformProperties.grouperType);

            var valueCodeableConceptCoding = new Coding();
            valueCodeableConceptCoding.setCode(TransformProperties.modelGrouper);
            valueCodeableConceptCoding.setSystem(TransformProperties.grouperUsageContextCodableConceptSystemURL);

            usageContext.setCode(code);
            usageContext.getValueCodeableConcept().setText("Model grouper");
            usageContext.getValueCodeableConcept().getCoding().add(valueCodeableConceptCoding);

            vs.addUseContext(usageContext);
        }
    }

    static void addAuthoritativeSource(ValueSet vs, String url) {
        if (vs.getExtensionByUrl(TransformProperties.authoritativeSourceExtUrl) == null) {
            var ext = new Extension();
            ext.setUrl(TransformProperties.authoritativeSourceExtUrl);
            ext.setValue(new UriType(url));
            vs.getExtension().add(ext);
        }
    }

    public static String ensureHttps(String urlString) throws MalformedURLException, URISyntaxException {
        URL url = URI.create(urlString).toURL();

        // Check if the protocol is already HTTPS
        if ("https".equalsIgnoreCase(url.getProtocol())) {
            return urlString;
        }

        // Construct a new URL with the HTTPS protocol
        URI httpsUrl = new URI(
                "https", // scheme
                null, // userinfo
                url.getHost(), // host
                url.getPort(), // port
                url.getFile(), // path
                null, // query
                null // fragment
                );
        return httpsUrl.toString();
    }

    public static List<Bundle.BundleEntryComponent> transformImportBundle(
            Bundle parameterBundle, IRepository repository, String appAuthoritativeUrl)
            throws FhirResourceExistsException {
        // store for processing root library
        Set<RelatedArtifact> groupers = new HashSet<>();
        Set<RelatedArtifact> leafs = new HashSet<>();

        PlanDefinition planDefinition = null;
        Library rootLibrary = null;
        Library rctcLibrary = null;

        List<Bundle.BundleEntryComponent> bundleEntries = new ArrayList<>();
        var entries = parameterBundle.getEntry();
        for (final var entry : entries) {
            if (entry.getResource() instanceof MetadataResource) {
                var resource = (MetadataResource) entry.getResource();
                switch (resource.getResourceType()) {
                    case ValueSet:
                        var valueSet = (ValueSet) resource;
                        List<UsageContext> conditionsList = new ArrayList<>();
                        List<UsageContext> priorityList = new ArrayList<>();
                        valueSet.setIdentifier(fixIdentifiers(valueSet.getIdentifier()));
                        var valueSetCanonicalUrl = adapterFactory
                                .createKnowledgeArtifactAdapter(valueSet)
                                .getCanonical();
                        extractPrioritiesAndConditions(
                                valueSet.getUseContext(), priorityList, conditionsList, valueSetCanonicalUrl);
                        if (hasGrouperCompose(valueSet)) {
                            prepareGrouperValueSet(valueSet, appAuthoritativeUrl);
                            groupers.add(
                                    relatedArtifactFromGrouperUrl(valueSetCanonicalUrl, conditionsList, priorityList));
                        } else {
                            prepareLeafValueSet(valueSet);
                            leafs.add(relatedArtifactFromLeafUrl(valueSetCanonicalUrl, conditionsList, priorityList));
                        }
                        // Remove conditions and priority from useContext of leaf valuesets and groupers
                        var cleanedContext = valueSet.getUseContext().stream()
                                .filter(ctx -> ctx.hasCode()
                                        && !(ctx.getCode().getCode().equals("focus")
                                                || ctx.getCode().getCode().equals("priority")))
                                .collect(Collectors.toList());
                        valueSet.setUseContext(cleanedContext);
                        // Check if ValueSet already exists
                        if (!doesResourceExist(valueSet.getUrl(), valueSet.getVersion(), ValueSet.class, repository)) {
                            // Save the resource into entry bundle
                            bundleEntries.add(getPutResourceRequest(valueSet, "/ValueSet", valueSet.getIdPart()));
                        }
                        break;
                    case Library:
                        var library = (Library) resource;
                        library.setIdentifier(fixIdentifiers(library.getIdentifier()));
                        if (doesResourceExist(library.getUrl(), library.getVersion(), Library.class, repository)) {
                            throw new FhirResourceExistsException("Library", library.getUrl(), library.getVersion());
                        } else {
                            if (isRootSpecificationLibrary(resource)) {
                                rootLibrary = library;
                            } else {
                                library.getMeta()
                                        .setProfile(removeProfileFromList(
                                                library.getMeta().getProfile(), TransformProperties.ersdVSLibProfile));
                                rctcLibrary = library;
                            }
                        }
                        break;
                    case PlanDefinition:
                        planDefinition = (PlanDefinition) resource;
                        planDefinition.setIdentifier(fixIdentifiers(planDefinition.getIdentifier()));
                        break;
                    default:
                        myLogger.info("resourceType:  " + resource.getResourceType()
                                + " is not supported by $import operation");
                        break;
                }
            }
        }

        assert rctcLibrary != null;
        assert planDefinition != null;
        assert rootLibrary != null;

        prepareRCTCLibrary(rctcLibrary, groupers);
        prepareRootLibrary(planDefinition, rctcLibrary, groupers, leafs, rootLibrary);
        preparePlanDef(planDefinition, adapterFactory.createKnowledgeArtifactAdapter(rctcLibrary));

        bundleEntries.add(getPutResourceRequest(rootLibrary, "/Library", rootLibrary.getIdPart()));
        bundleEntries.add(getPutResourceRequest(rctcLibrary, "/Library", rctcLibrary.getIdPart()));
        bundleEntries.add(getPutResourceRequest(planDefinition, "/PlanDefinition", planDefinition.getIdPart()));
        return bundleEntries;
    }

    private static void prepareGrouperValueSet(ValueSet valueSet, String appAuthoritativeUrl) {
        addModelGrouperUseContextIfMissing(valueSet);
        valueSet.setExpansion(null);
        var grouperProfiles = addMetaProfileUrl(
                valueSet.getMeta(), Collections.singletonList(TransformProperties.valueSetGrouperProfile));
        var filteredGrouperProfiles = removeProfileFromList(grouperProfiles, TransformProperties.ersdVSProfile);
        valueSet.getMeta().setProfile(filteredGrouperProfiles);
        addAuthoritativeSource(valueSet, appAuthoritativeUrl + "/ValueSet/" + valueSet.getIdPart());
    }

    private static void prepareLeafValueSet(ValueSet valueSet) {
        var leafVsProfiles = addMetaProfileUrl(
                valueSet.getMeta(),
                Arrays.asList(
                        TransformProperties.leafValueSetVsmHostedProfile,
                        TransformProperties.leafValueSetConditionProfile));
        var filtered = removeProfileFromList(leafVsProfiles, TransformProperties.ersdVSProfile);
        valueSet.getMeta().setProfile(filtered);

        String valueSetAuthoritativeSourceUrl = valueSet.getUrl();

        try {
            valueSetAuthoritativeSourceUrl = ensureHttps(valueSetAuthoritativeSourceUrl);
        } catch (URISyntaxException | MalformedURLException e) {
            // Do nothing here and let the malformed URL flow through.
        }

        // Add authoritative source extension
        addAuthoritativeSource(valueSet, valueSetAuthoritativeSourceUrl);
    }

    private static void preparePlanDef(PlanDefinition planDefinition, IKnowledgeArtifactAdapter rctcAdapter) {
        if (rctcAdapter.hasUrl() && rctcAdapter.hasVersion()) {
            planDefinition.getRelatedArtifact().forEach(ra -> {
                if (ra.getResource().contains(rctcAdapter.getUrl())) {
                    ra.setResource(rctcAdapter.getCanonical());
                }
            });
        }
    }

    static List<Identifier> fixIdentifiers(List<Identifier> identifiers) {
        return identifiers.stream()
                .map(i -> {
                    if (i.getSystem().equals("urn:ietf:rfc:3986")
                            && i.hasValue()
                            && !i.getValue().startsWith("http")
                            && !i.getValue().startsWith("urn:oid")
                            && !i.getValue().startsWith("urn:uuid")
                            && Character.isDigit(i.getValue().charAt(0))) {
                        i.setValue("urn:oid:" + i.getValue());
                    }
                    return i;
                })
                .collect(Collectors.toList());
    }

    static List<CanonicalType> removeProfileFromList(List<CanonicalType> profiles, String profileToRemove) {
        if (profiles == null) {
            return new ArrayList<CanonicalType>();
        }
        return profiles.stream()
                .filter(profile -> profile.hasValue() && !profile.getValue().equals(profileToRemove))
                .collect(Collectors.toList());
    }

    static void extractPrioritiesAndConditions(
            List<UsageContext> contexts,
            List<UsageContext> priorityList,
            List<UsageContext> conditionsList,
            String valueSetCanonicalUrl) {

        if (contexts == null || contexts.isEmpty()) return;

        for (UsageContext context : contexts) {
            var isValidUsageContext = isValidUsageContext(context);
            if (!isValidUsageContext) continue;

            switch (context.getCode().getCode()) {
                case "focus" -> conditionsList.add(context);
                case "priority" -> handlePriorityContext(context, priorityList, valueSetCanonicalUrl);
                default -> {
                    /* ignore other usage contexts */
                }
            }
        }
    }

    private static boolean isValidUsageContext(UsageContext context) {
        return context != null
                && context.hasCode()
                && context.hasValueCodeableConcept()
                && context.getValueCodeableConcept().hasCoding();
    }

    private static void handlePriorityContext(
            UsageContext newContext, List<UsageContext> existingPriorities, String valueSetCanonicalUrl) {

        String newCode = getFirstCodingCode(newContext.getValueCodeableConcept());
        if (newCode == null) return;

        // Check for conflicting codes
        for (UsageContext existing : existingPriorities) {
            String existingCode = getFirstCodingCode(existing.getValueCodeableConcept());
            if (existingCode != null && !existingCode.equals(newCode)) {
                throw new UnprocessableEntityException(
                        "ValueSet with URL " + valueSetCanonicalUrl + " has conflicting priority codes");
            }
        }

        // Only add if not already present
        boolean alreadyExists = existingPriorities.stream()
                .anyMatch(existing -> newCode.equals(getFirstCodingCode(existing.getValueCodeableConcept())));
        if (!alreadyExists) {
            existingPriorities.add(newContext);
        }
    }

    private static String getFirstCodingCode(CodeableConcept concept) {
        if (concept == null
                || !concept.hasCoding()
                || !concept.getCodingFirstRep().hasCode()) {
            return null;
        }
        return concept.getCodingFirstRep().getCode();
    }

    private static boolean doesResourceExist(
            String url, String version, Class<? extends IBaseResource> resourceType, IRepository repository) {
        try {
            var searchParams = version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
            var searchResult = repository.search(Bundle.class, resourceType, searchParams);
            return searchResult.hasEntry();
        } catch (Exception e) {
            return false;
        }
    }

    private static Bundle.BundleEntryComponent getPutResourceRequest(
            MetadataResource value, String resourceType, String id) {
        var bundleEntry = new Bundle.BundleEntryComponent();

        var bundleRequest = new Bundle.BundleEntryRequestComponent();
        bundleRequest.setMethod(Bundle.HTTPVerb.PUT);
        bundleRequest.setUrl(resourceType + "?_id=" + id);
        bundleEntry.setRequest(bundleRequest);
        bundleEntry.setResource(value);
        bundleEntry.setFullUrl(value.getUrl());
        return bundleEntry;
    }

    static List<CanonicalType> addMetaProfileUrl(Meta meta, List<String> urls) {
        List<CanonicalType> profiles = meta.getProfile();

        // Add to profile and ensure not duplicated
        List<CanonicalType> finalProfiles = profiles;
        urls.forEach(url -> finalProfiles.add(new CanonicalType(url)));
        profiles = profiles.stream()
                .filter(distinctByKey(CanonicalType::getValueAsString))
                .collect(Collectors.toList());
        return profiles;
    }

    private static void prepareRCTCLibrary(Library rctcLibrary, Set<RelatedArtifact> groupers) {
        groupers.forEach(grouper -> {
            rctcLibrary
                    .getRelatedArtifact()
                    .removeIf(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF
                            && ra.hasResource()
                            && grouper.getResource()
                                    .split("\\|")[0]
                                    .equals(ra.getResource().split("\\|")[0]));
            rctcLibrary.getRelatedArtifact().add(grouper);
        });
    }

    private static RelatedArtifact relatedArtifactFromGrouperUrl(
            String grouperUrl, List<UsageContext> conditions, List<UsageContext> priorities) {
        var relatedArtifact = new RelatedArtifact();
        relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);
        relatedArtifact.setResource(grouperUrl);
        var isOwnedExtension = new Extension();
        isOwnedExtension.setUrl(TransformProperties.crmiIsOwned);
        isOwnedExtension.setValue(new BooleanType(true));
        var extensions = new ArrayList<Extension>();
        extensions.addAll(
                processUsageContextMapForLibrary(conditions, TransformProperties.CRMI_INTENDED_USAGE_CONTEXT_EXT_URL));
        extensions.addAll(
                processUsageContextMapForLibrary(priorities, TransformProperties.CRMI_INTENDED_USAGE_CONTEXT_EXT_URL));
        extensions.add(isOwnedExtension);
        relatedArtifact.setExtension(extensions);
        return relatedArtifact;
    }

    private static RelatedArtifact relatedArtifactFromLeafUrl(
            String leafUrl, List<UsageContext> conditions, List<UsageContext> priorities) {
        var relatedArtifact = new RelatedArtifact();
        relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
        relatedArtifact.setResource(leafUrl);
        var extensions = new ArrayList<Extension>();
        extensions.addAll(
                processUsageContextMapForLibrary(conditions, TransformProperties.CRMI_INTENDED_USAGE_CONTEXT_EXT_URL));
        extensions.addAll(
                processUsageContextMapForLibrary(priorities, TransformProperties.CRMI_INTENDED_USAGE_CONTEXT_EXT_URL));
        relatedArtifact.setExtension(extensions);
        return relatedArtifact;
    }

    private static void prepareRootLibrary(
            PlanDefinition planDefinition,
            Library rctcLibrary,
            Set<RelatedArtifact> groupers,
            Set<RelatedArtifact> leafs,
            Library rootLibrary) {
        // Add to profile and ensure not duplicated
        var rootLibraryProfiles = addMetaProfileUrl(
                rootLibrary.getMeta(), Collections.singletonList(TransformProperties.crmiManifestLibrary));
        rootLibrary.getMeta().setProfile(rootLibraryProfiles);

        List<RelatedArtifact> relatedArtifacts = new ArrayList<>();

        // Set PlanDefinition
        var planDefResourceUrl =
                adapterFactory.createKnowledgeArtifactAdapter(planDefinition).getCanonical();
        var relatedArtifactPlanDefComposedOf = new RelatedArtifact();
        relatedArtifactPlanDefComposedOf.setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);
        relatedArtifactPlanDefComposedOf.setResource(planDefResourceUrl);
        var extension = new Extension();
        extension.setUrl(TransformProperties.crmiIsOwned);

        extension.setValue(new BooleanType(true));
        relatedArtifactPlanDefComposedOf.setExtension(new ArrayList<>(Collections.singletonList(extension)));
        relatedArtifacts.add(relatedArtifactPlanDefComposedOf);

        var relatedArtifactPlanDefDependsOn = new RelatedArtifact();
        relatedArtifactPlanDefDependsOn.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
        relatedArtifactPlanDefDependsOn.setResource(planDefResourceUrl);
        relatedArtifacts.add(relatedArtifactPlanDefDependsOn);

        // Set rctc Library
        var rctcUrl = adapterFactory.createKnowledgeArtifactAdapter(rctcLibrary).getCanonical();
        var relatedArtifactRCTCComposedOf = new RelatedArtifact();
        relatedArtifactRCTCComposedOf.setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);
        relatedArtifactRCTCComposedOf.setResource(rctcUrl);
        extension = new Extension();
        extension.setUrl(TransformProperties.crmiIsOwned);

        extension.setValue(new BooleanType(true));
        relatedArtifactRCTCComposedOf.setExtension(new ArrayList<>(Collections.singletonList(extension)));
        relatedArtifacts.add(relatedArtifactRCTCComposedOf);

        var relatedArtifactRCTCDependsOn = new RelatedArtifact();
        relatedArtifactRCTCDependsOn.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
        relatedArtifactRCTCDependsOn.setResource(rctcUrl);
        relatedArtifacts.add(relatedArtifactRCTCDependsOn);

        relatedArtifacts.addAll(groupers);
        relatedArtifacts.addAll(leafs);

        rootLibrary.setRelatedArtifact(relatedArtifacts);
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        var seen = new HashSet<>();
        return t -> seen.add(keyExtractor.apply(t));
    }

    static List<Extension> processUsageContextMapForLibrary(List<UsageContext> v, String extensionUrl) {
        var extensions = new ArrayList<Extension>();
        v.forEach(usageContext -> {
            var extension = new Extension();
            extension.setUrl(extensionUrl);
            extension.setValue(usageContext);
            extensions.add(extension);
        });
        return extensions;
    }
}
