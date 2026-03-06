package org.opencds.cqf.fhir.utility.adapter.r4;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IActivityDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class ActivityDefinitionAdapter extends KnowledgeArtifactAdapter implements IActivityDefinitionAdapter {

    public ActivityDefinitionAdapter(IDomainResource activityDefinition) {
        super(activityDefinition);
        if (!(activityDefinition instanceof ActivityDefinition)) {
            throw new IllegalArgumentException(
                    "resource passed as activityDefinition argument is not an ActivityDefinition resource");
        }
    }

    public ActivityDefinitionAdapter(ActivityDefinition activityDefinition) {
        super(activityDefinition);
    }

    protected ActivityDefinition getActivityDefinition() {
        return (ActivityDefinition) resource;
    }

    @Override
    public ActivityDefinition get() {
        return getActivityDefinition();
    }

    @Override
    public ActivityDefinition copy() {
        return get().copy();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        /*
        relatedArtifact[].resource
        library[]

        */

        // relatedArtifact[].resource
        getRelatedArtifactsOfType(DEPENDSON).stream()
                .filter(RelatedArtifact::hasResource)
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .forEach(references::add);

        // library[]
        if (hasLibrary()) {
            for (var ct : getActivityDefinition().getLibrary()) {
                references.add(new DependencyInfo(referenceSource, ct.getValue(), ct.getExtension(), ct::setValue));
            }
        }

        return references;
    }

    @Override
    public Map<String, String> getReferencedLibraries() {
        var libraries = getActivityDefinition().getLibrary().stream()
                .collect(toMap(l -> requireNonNull(Canonicals.getIdPart(l)), CanonicalType::getValueAsString));
        libraries.putAll(resolveCqfLibraries());
        return libraries;
    }

    @Override
    public String getDescription() {
        return get().getDescription();
    }

    @Override
    public boolean hasLibrary() {
        return get().hasLibrary();
    }

    @Override
    public List<String> getLibrary() {
        return get().getLibrary().stream().map(PrimitiveType::asStringValue).toList();
    }
}
