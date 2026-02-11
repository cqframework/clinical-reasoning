package org.opencds.cqf.fhir.utility.dstu3;

import ca.uhn.fhir.repository.IRepository;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class consists exclusively of static methods that assist with packaging FHIR Resources.
 */
public class PackageHelper extends org.opencds.cqf.fhir.utility.PackageHelper {

    protected static final Logger logger = LoggerFactory.getLogger(PackageHelper.class);

    protected static final List<String> PACKABLE_RESOURCES = Arrays.asList(
            FHIRAllTypes.LIBRARY.toCode(),
            FHIRAllTypes.PLANDEFINITION.toCode(),
            FHIRAllTypes.ACTIVITYDEFINITION.toCode(),
            FHIRAllTypes.STRUCTUREDEFINITION.toCode(),
            FHIRAllTypes.CODESYSTEM.toCode(),
            FHIRAllTypes.VALUESET.toCode());

    public static void addRelatedArtifacts(
            IBaseBundle bundle, List<RelatedArtifact> artifacts, IRepository repository, boolean isPut) {
        for (var artifact : artifacts) {
            if (artifact.getType().equals(RelatedArtifactType.DEPENDSON) && artifact.hasResource()) {
                try {
                    var canonical = artifact.getResource().getReference();
                    if (PACKABLE_RESOURCES.contains(Canonicals.getResourceType(canonical))) {
                        var resource = SearchHelper.searchRepositoryByCanonical(repository, new StringType(canonical));
                        if (resource != null
                                && BundleHelper.getEntryResources(bundle).stream()
                                        .noneMatch(r -> r.getIdElement().equals(resource.getIdElement()))) {
                            BundleHelper.addEntry(bundle, createEntry(resource, isPut));
                            final var adapter = IAdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum())
                                    .createKnowledgeArtifactAdapter((IDomainResource) resource);
                            if (adapter.hasRelatedArtifact()) {
                                addRelatedArtifacts(bundle, adapter.getRelatedArtifact(), repository, isPut);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
