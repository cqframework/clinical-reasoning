package org.opencds.cqf.fhir.utility.r5;

import ca.uhn.fhir.repository.IRepository;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class consists exclusively of static methods that assist with packaging FHIR Resources.
 */
public class PackageHelper extends org.opencds.cqf.fhir.utility.PackageHelper {

    private PackageHelper() {}

    protected static final Logger logger = LoggerFactory.getLogger(PackageHelper.class);

    protected static final List<String> PACKABLE_RESOURCES = Arrays.asList(
            FHIRTypes.LIBRARY.toCode(),
            FHIRTypes.PLANDEFINITION.toCode(),
            FHIRTypes.ACTIVITYDEFINITION.toCode(),
            FHIRTypes.STRUCTUREDEFINITION.toCode(),
            FHIRTypes.CODESYSTEM.toCode(),
            FHIRTypes.VALUESET.toCode());

    public static void addRelatedArtifacts(
            IBaseBundle bundle, List<RelatedArtifact> artifacts, IRepository repository, boolean isPut) {
        for (var artifact : artifacts) {
            if (artifact.getType().equals(RelatedArtifactType.DEPENDSON) && artifact.hasResourceElement()) {
                try {
                    var canonical = artifact.getResourceElement();
                    if (PACKABLE_RESOURCES.contains(Canonicals.getResourceType(canonical))) {
                        var resource = SearchHelper.searchRepositoryByCanonical(repository, canonical);
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
