package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactDiffProcessor implements IArtifactDiffProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactDiffProcessor.class);

    public ArtifactDiffProcessor() {
        /* Empty as we will not perform artifact diff outside HAPI context */
    }

    @Override
    public IBaseParameters getArtifactDiff(
            IBaseResource sourceResource,
            IBaseResource targetResource,
            Boolean compareComputable,
            Boolean compareExecutable,
            DiffCache cache,
            IBaseResource terminologyEndpoint) {
        logger.info("Unable to perform $artifact-diff outside of HAPI context");
        return new Parameters();
    }

    public static class DiffCache {
        private final Map<String, Parameters> diffs = new HashMap<>();
        // One map per side. This prevents the target resource from overwriting the source resource.
        private final Map<String, MetadataResource> sourceResources = new HashMap<>();
        private final Map<String, MetadataResource> targetResources = new HashMap<>();

        public DiffCache() {
            super();
        }

        public void addDiff(String sourceUrl, String targetUrl, Parameters diff) {
            this.diffs.put(sourceUrl + "-" + targetUrl, diff);
        }

        public Parameters getDiff(String sourceUrl, String targetUrl) {
            return this.diffs.get(sourceUrl + "-" + targetUrl);
        }

        public void addSource(String url, MetadataResource resource) {
            this.sourceResources.put(url, resource);
        }

        public void addTarget(String url, MetadataResource resource) {
            this.targetResources.put(url, resource);
        }

        // The resource the given side holds for a canonical
        public Optional<MetadataResource> getResource(String url, boolean isSource) {
            var side = isSource ? this.sourceResources : this.targetResources;
            var resource = Optional.ofNullable(side.get(url));
            if (resource.isEmpty()) {
                var possibleMatches = resourcesForUrl(url, side);
                if (!possibleMatches.isEmpty()) {
                    if (possibleMatches.size() > 1) {
                        throw new UnprocessableEntityException(
                                "Artifact contains multiple resources with the same URL:" + url);
                    }
                    resource = Optional.of(possibleMatches.get(0));
                }
            }
            return resource;
        }

        private static List<MetadataResource> resourcesForUrl(String url, Map<String, MetadataResource> side) {
            return side.keySet().stream()
                    .filter(k -> url.equals(Canonicals.getUrl(k)))
                    .map(side::get)
                    .toList();
        }

        public Optional<MetadataResource> getSourceResourceForUrl(String url) {
            return resourcesForUrl(url, this.sourceResources).stream().findFirst();
        }

        public Optional<MetadataResource> getTargetResourceForUrl(String url) {
            return resourcesForUrl(url, this.targetResources).stream().findFirst();
        }
    }
}
