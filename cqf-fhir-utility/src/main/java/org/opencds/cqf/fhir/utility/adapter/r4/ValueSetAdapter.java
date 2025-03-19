package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.opencds.cqf.fhir.utility.ValueSets.getCodesInCompose;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetExpansionContainsAdapter;

public class ValueSetAdapter extends KnowledgeArtifactAdapter implements IValueSetAdapter {

    public ValueSetAdapter(IDomainResource valueSet) {
        super(valueSet);
        if (!(valueSet instanceof ValueSet)) {
            throw new IllegalArgumentException("resource passed as valueSet argument is not a ValueSet resource");
        }
    }

    public ValueSetAdapter(ValueSet valueSet) {
        super(valueSet);
    }

    protected ValueSet getValueSet() {
        return (ValueSet) resource;
    }

    @Override
    public ValueSet get() {
        return (ValueSet) resource;
    }

    @Override
    public ValueSet copy() {
        return get().copy();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        /*
          compose.include[].valueSet
          compose.include[].system
          compose.exclude[].valueSet
          compose.exclude[].system
        */
        Stream.concat(
                        getValueSet().getCompose().getInclude().stream(),
                        getValueSet().getCompose().getExclude().stream())
                .forEach(component -> {
                    if (component.hasValueSet()) {
                        component
                                .getValueSet()
                                .forEach(ct -> references.add(new DependencyInfo(
                                        referenceSource, ct.getValue(), ct.getExtension(), ct::setValue)));
                    }
                    if (component.hasSystem()) {
                        references.add(new DependencyInfo(
                                referenceSource,
                                component.getSystem(),
                                component.getSystemElement().getExtension(),
                                component::setSystem));
                    }
                });
        return references;
    }

    @Override
    public <T extends IBaseBackboneElement> void setExpansion(T expansion) {
        getValueSet().setExpansion((ValueSetExpansionComponent) expansion);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValueSetExpansionComponent getExpansion() {
        return getValueSet().getExpansion();
    }

    @Override
    public boolean hasExpansion() {
        return getValueSet().hasExpansion();
    }

    @Override
    public boolean hasExpansionContains() {
        return getExpansion().hasContains();
    }

    @Override
    public List<IValueSetExpansionContainsAdapter> getExpansionContains() {
        return getExpansion().getContains().stream()
                .map(ValueSetExpansionContainsAdapter::new)
                .collect(Collectors.toUnmodifiableList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValueSetExpansionComponent newExpansion() {
        var expansion = new ValueSet.ValueSetExpansionComponent(new DateTimeType(Date.from(Instant.now())));
        expansion.getContains();
        return expansion;
    }

    @Override
    public boolean hasCompose() {
        return this.get().hasCompose();
    }

    @Override
    public boolean hasComposeInclude() {
        return this.get().getCompose().hasInclude();
    }

    @Override
    public List<IValueSetConceptSetAdapter> getComposeInclude() {
        return getValueSet().getCompose().getInclude().stream()
                .map(ValueSetConceptSetAdapter::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getValueSetIncludes() {
        return getValueSet().getCompose().getInclude().stream()
                .map(ConceptSetComponent::getValueSet)
                .flatMap(Collection::stream)
                .map(PrimitiveType::asStringValue)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasSimpleCompose() {
        return getValueSet().hasCompose()
                && !getValueSet().getCompose().hasExclude()
                && getValueSet().getCompose().getInclude().stream()
                        .noneMatch(
                                csc -> csc.hasFilter() || csc.hasValueSet() || !csc.hasSystem() || !csc.hasConcept());
    }

    @Override
    public boolean hasGroupingCompose() {
        return getValueSet().hasCompose()
                && !getValueSet().getCompose().hasExclude()
                && getValueSet().getCompose().getInclude().stream()
                        .noneMatch(csc -> !csc.hasValueSet() || csc.hasFilter());
    }

    @Override
    public boolean hasNaiveParameter() {
        return getValueSet().getExpansion().getParameter().stream()
                .anyMatch(p -> p.getName().equals("naive"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValueSet.ValueSetExpansionParameterComponent createNaiveParameter() {
        return new ValueSet.ValueSetExpansionParameterComponent()
                .setName("naive")
                .setValue(new BooleanType(true));
    }

    @Override
    public void naiveExpand() {
        var expansion = newExpansion().addParameter(createNaiveParameter());

        final List<Code> codesInCompose = getCodesInCompose(fhirContext, getValueSet());
        if (codesInCompose == null) {
            return;
        }
        for (var code : codesInCompose) {
            expansion
                    .addContains()
                    .setCode(code.getCode())
                    .setSystem(code.getSystem())
                    .setVersion(code.getVersion())
                    .setDisplay(code.getDisplay());
        }
        getValueSet().setExpansion(expansion);
    }
}
