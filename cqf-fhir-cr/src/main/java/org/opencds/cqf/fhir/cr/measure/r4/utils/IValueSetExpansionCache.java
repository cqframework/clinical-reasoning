package org.opencds.cqf.fhir.cr.measure.r4.utils;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;

public interface IValueSetExpansionCache {
    IBaseBundle getExpansionsForCanonical(String canonical);
    IBaseBundle getExpansionsForAllValueSetsInManifest(IKnowledgeArtifactAdapter manifestAdapter);
    String getExpansionParametersHash(IBaseParameters expansionParameters);
    IBaseBundle addExpansionToBundle(String expansionParametersHash, IValueSetAdapter expandedValueSet);
    void updateExpansionsForCanonical(IBaseBundle updatedExpansionsBundle);
  }
