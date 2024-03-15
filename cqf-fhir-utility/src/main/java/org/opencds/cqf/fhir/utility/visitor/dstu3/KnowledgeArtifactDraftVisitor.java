package org.opencds.cqf.fhir.utility.visitor.dstu3;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.UsageContext;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory;

public class KnowledgeArtifactDraftVisitor {

    public static Optional<MetadataResource> processReferencedResourceForDraft(
            Repository repository, RelatedArtifact ra, String version) {
        Optional<MetadataResource> referencedResource = Optional.empty();
        if (ra.hasUrl() || ra.hasResource()) {
            Bundle referencedResourceBundle;
            if (ra.hasUrl()) {
                referencedResourceBundle = (Bundle)
                        SearchHelper.searchRepositoryByCanonicalWithPaging(repository, ra.getUrl());
            } else {
                referencedResourceBundle = (Bundle)
                        SearchHelper.searchRepositoryByCanonicalWithPaging(repository, ra.getResource().getReference());
            }
            if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
                var referencedResourceEntry = referencedResourceBundle.getEntryFirstRep();
                if (referencedResourceEntry.hasResource()
                        && referencedResourceEntry.getResource() instanceof MetadataResource) {
                    referencedResource = Optional.of((MetadataResource) referencedResourceEntry.getResource());
                }
            }
        }
        return referencedResource;
    }

    public static void updateUsageContextReferencesWithUrns(
            MetadataResource newResource,
            List<MetadataResource> resourceListWithOriginalIds,
            List<IdType> idListForTransactionBundle) {
        List<UsageContext> useContexts = newResource.getUseContext();
        for (UsageContext useContext : useContexts) {
            // TODO: will we ever need to resolve these references?
            if (useContext.hasValue() && useContext.getValue() instanceof Reference) {
                Reference useContextRef = (Reference) useContext.getValue();
                if (useContextRef != null) {
                    resourceListWithOriginalIds.stream()
                            .filter(resource -> (resource.getClass().getSimpleName() + "/"
                                            + resource.getIdElement().getIdPart())
                                    .equals(useContextRef.getReference()))
                            .findAny()
                            .ifPresent(resource -> {
                                int indexOfDraftInIdList = resourceListWithOriginalIds.indexOf(resource);
                                useContext.setValue(
                                        new Reference(idListForTransactionBundle.get(indexOfDraftInIdList)));
                            });
                }
            }
        }
    }
}
