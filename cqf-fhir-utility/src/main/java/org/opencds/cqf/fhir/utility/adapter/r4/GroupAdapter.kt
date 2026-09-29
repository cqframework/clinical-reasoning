package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.UriType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IGroupAdapter;

public class GroupAdapter extends KnowledgeArtifactAdapter implements IGroupAdapter {

    public GroupAdapter(IDomainResource group) {
        super(group);
        if (!(group instanceof Group)) {
            // This is NOT due to a bad request/user error.  It's a system error.
            throw new IllegalArgumentException("resource passed as group argument is not a Group resource");
        }
    }

    public GroupAdapter(Group group) {
        super(group);
    }

    protected Group getGroup() {
        return (Group) resource;
    }

    @Override
    public Group get() {
        return getGroup();
    }

    @Override
    public Group copy() {
        return get().copy();
    }

    private boolean checkedEffectiveDataRequirements;
    private Library effectiveDataRequirements;
    private LibraryAdapter effectiveDataRequirementsAdapter;

    private String getEdrReferenceString(Extension edrExtension) {
        return edrExtension.getUrl().contains("cqfm")
                ? ((Reference) edrExtension.getValue()).getReference()
                : ((UriType) edrExtension.getValue()).getValue();
    }

    private Consumer<String> getEdrReferenceConsumer(Extension edrExtension) {
        return edrExtension.getUrl().contains("cqfm")
                ? reference -> edrExtension.setValue(new Reference(reference))
                : reference -> edrExtension.setValue(new CanonicalType(reference));
    }

    private void findEffectiveDataRequirements() {
        if (!checkedEffectiveDataRequirements) {
            var edrExtensions = this.getGroup().getExtension().stream()
                    .filter(ext -> ext.getUrl().endsWith("-effectiveDataRequirements"))
                    .filter(Extension::hasValue)
                    .collect(Collectors.toList());

            var edrExtension = edrExtensions.size() == 1 ? edrExtensions.get(0) : null;
            // cqfm-effectiveDataRequirements is a Reference, crmi-effectiveDataRequirements is a canonical
            var maybeEdrReference = Optional.ofNullable(edrExtension).map(this::getEdrReferenceString);
            if (edrExtension != null) {
                var edrReference = maybeEdrReference.get();
                for (var c : getGroup().getContained()) {
                    if (c.hasId()
                            && (edrReference.equals(c.getId()) || edrReference.equals("#" + c.getId()))
                            && c instanceof Library library) {
                        effectiveDataRequirements = library;
                        effectiveDataRequirementsAdapter = new LibraryAdapter(effectiveDataRequirements);
                    }
                }
            }
            checkedEffectiveDataRequirements = true;
        }
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        // If an effectiveDataRequirements library is present, use it exclusively
        findEffectiveDataRequirements();
        if (effectiveDataRequirements != null) {
            references.addAll(effectiveDataRequirementsAdapter.getDependencies());
            return references;
        }

        // Otherwise, fall back to the relatedArtifact and library

        /*
         relatedArtifact[].resource
         extension[cqf-library]
         extension[characteristicExpression].reference
         extension[cqfm-inputParameters][]
         extension[cqfm-expansionParameters][]
         extension[cqfm-effectiveDataRequirements]
         extension[cqfm-cqlOptions]
         extension[crmi-effectiveDataRequirements]
        */

        // relatedArtifact[].resource
        getRelatedArtifactsOfType(DEPENDSON).stream()
                .filter(RelatedArtifact::hasResource)
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .forEach(references::add);

        for (var expressionExtension :
                getGroup().getExtensionsByUrl("http://hl7.org/fhir/StructureDefinition/characteristicExpression")) {
            if (expressionExtension.getValue() instanceof Expression expression) {
                if (expression.hasReference()) {
                    references.add(new DependencyInfo(
                            referenceSource,
                            expression.getReference(),
                            expression.getExtension(),
                            reference -> expression.setReference(reference)));
                }
            }
        }

        // extension[cqfm-effectiveDataRequirements]
        // extension[crmi-effectiveDataRequirements]
        get().getExtension().stream()
                .filter(e -> CANONICAL_EXTENSIONS.contains(e.getUrl()))
                .forEach(referenceExt -> references.add(new DependencyInfo(
                        referenceSource,
                        getEdrReferenceString(referenceExt),
                        referenceExt.getExtension(),
                        getEdrReferenceConsumer(referenceExt))));

        // extension[cqfm-inputParameters][]
        // extension[cqfm-expansionParameters][]
        // extension[cqfm-cqlOptions]
        get().getExtension().stream()
                .filter(e -> REFERENCE_EXTENSIONS.contains(e.getUrl()))
                .forEach(referenceExt -> references.add(new DependencyInfo(
                        referenceSource,
                        ((Reference) referenceExt.getValue()).getReference(),
                        referenceExt.getExtension(),
                        reference -> referenceExt.setValue(new Reference(reference)))));

        // extension[cqfm-component][].resource
        get().getExtensionsByUrl(Constants.CQFM_COMPONENT).forEach(ext -> {
            final var ref = (RelatedArtifact) ext.getValue();
            if (ref.hasResource()) {
                final var dep =
                        new DependencyInfo(referenceSource, ref.getResource(), ref.getExtension(), ref::setResource);
                references.add(dep);
            }
        });

        return references;
    }
}
