package org.opencds.cqf.fhir.cr.visitor;

import java.util.Optional;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;

public interface IValueSetExpansionCache {
    IValueSetAdapter getExpansionForCanonical(String canonical, String expansionParametersHash);

    // make it private/protected
    Optional<String> getExpansionParametersHash(IKnowledgeArtifactAdapter artifactContainingExpansionParameters);

    boolean addToCache(IValueSetAdapter expandedValueSet, String expansionParametersHash);
}
