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
        private final Map<String, Parameters> diffs = new HashMap<String, Parameters>();
        private final Map<String, DiffCacheResource> resources = new HashMap<String, DiffCacheResource>();

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
            this.resources.put(url, new DiffCacheResource(resource, true));
        }

        public void addTarget(String url, MetadataResource resource) {
            this.resources.put(url, new DiffCacheResource(resource, false));
        }

        public Optional<MetadataResource> getResource(String url) {
            var resource = Optional.ofNullable(this.resources.get(url)).map(r -> r.resource);
            if (resource.isEmpty()) {
                var possibleMatches = getResourcesForUrl(url);
                if (!possibleMatches.isEmpty()) {
                    if (possibleMatches.size() > 1) {
                        throw new UnprocessableEntityException(
                                "Artifact contains multiple resources with the same URL:" + url);
                    }
                    resource = Optional.of(possibleMatches.get(0).resource);
                }
            }
            return resource;
        }

        public List<DiffCacheResource> getResourcesForUrl(String url) {
            return this.resources.keySet().stream()
                    .filter(k -> url.equals(Canonicals.getUrl(k)))
                    .map(this.resources::get)
                    .toList();
        }

        public static class DiffCacheResource {
            public final MetadataResource resource;
            public final boolean isSource;

            DiffCacheResource(MetadataResource resource, boolean isSource) {
                this.resource = resource;
                this.isSource = isSource;
            }
        }
    }
}
