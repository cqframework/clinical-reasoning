package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IDomainResource;
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
        references.addAll(getRelatedArtifact().stream()
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .toList());

        // library[]
        if (hasLibrary()) {
            for (var reference : getActivityDefinition().getLibrary()) {
                references.add(new DependencyInfo(
                        referenceSource, reference.getReference(), reference.getExtension(), reference::setReference));
            }
        }

        return references;
    }

    @Override
    public Map<String, String> getReferencedLibraries() {
        var libraries = getActivityDefinition().getLibrary().stream()
                .collect(toMap(l -> requireNonNull(Canonicals.getIdPart(l.getReference())), Reference::getReference));
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
        return get().getLibrary().stream().map(Reference::getReference).collect(Collectors.toList());
    }
}
