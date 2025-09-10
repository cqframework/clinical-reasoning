package org.opencds.cqf.fhir.cr.graphdefinition.apply;

import static ca.uhn.fhir.context.FhirVersionEnum.R4;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.GraphDefinition.GraphDefinitionLinkComponent;
import org.hl7.fhir.r4.model.GraphDefinition.GraphDefinitionLinkTargetComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ApplyProcessor implements IApplyProcessor {
    public static final String DEFAULT_IDENTIFIER_VALUE_PREFIX = "urn:uuid:";
    private static final String BUNDLE_TYPE = "document";
    public static final Coding DEFAULT_CODING = new Coding().setSystem("http://loinc.org").setCode("18776-5").setDisplay("Plan of care note");

    private static final Logger logger = LoggerFactory.getLogger(ApplyProcessor.class);
    protected final IRepository repository;
    protected final ModelResolver modelResolver;
    protected final ExtensionProcessor extensionProcessor;
    protected final FhirVersionEnum fhirVersionEnum;

    public ApplyProcessor(IRepository repository, ModelResolver modelResolver,FhirVersionEnum fhirVersionEnum) {
        this.repository = repository;
        this.modelResolver = modelResolver;
        this.fhirVersionEnum = fhirVersionEnum;
        extensionProcessor = new ExtensionProcessor();
    }

    @Override
    public IBaseResource apply(ApplyRequest request) {
        validateVersion(request);

        GraphDefinition graphDefinition = (GraphDefinition) request.getGraphDefinition();
        Bundle bundle = createResponseBundle();
        Composition responseComposite = createAndInitializeCompositeEntry(request);

        List<SectionComponent> sections = transformBackBoneElementsToSections(graphDefinition.getLink());
        sections.forEach(responseComposite::addSection);

        addResourceToResponseBundleEntries(bundle, responseComposite, createEntryUrl(graphDefinition.getIdElement()));
        addRelatedResourcesToResponseBundle(bundle, request);

        return bundle;
    }

    protected List<SectionComponent> transformBackBoneElementsToSections(List<GraphDefinitionLinkComponent> linkComponents) {
        return linkComponents.stream()
            .map(this::transformLinkToSection)
            .filter(Objects::nonNull)
            .toList();
    }

    protected SectionComponent transformLinkToSection(GraphDefinitionLinkComponent linkComponent){
        if(!linkComponent.hasTarget()){
            return null;
        }

        String description = linkComponent.getDescription();
        SectionComponent sectionComponent = new SectionComponent().setTitle(description);

        linkComponent.getTarget()
            .stream()
            .filter(this::isGraphDefinitionType)
            .filter(GraphDefinitionLinkTargetComponent::hasExtension)
            .map(this::transformTargetToSection)
            .forEach(sectionComponent::addSection);

        return sectionComponent;
    }

    protected SectionComponent transformTargetToSection(GraphDefinitionLinkTargetComponent target){
        SectionComponent retVal = new SectionComponent();

        target.getExtension().stream()
            .map(this::transformExtensionToReference)
            .forEach(retVal::addEntry);

        return retVal;
    }

    protected Reference transformExtensionToReference(Extension extension){
        Reference retVal = new Reference();
        retVal.addExtension(extension.copy());
        return retVal;
    }

    protected void addRelatedResourcesToResponseBundle(Bundle responseBundle, ApplyRequest request) {
        Patient patient = this.repository.read(Patient.class, request.getSubjectId());
        addResourceToResponseBundleEntries(responseBundle, patient, createEntryUrl(patient.getIdElement()));

        Practitioner practitioner = this.repository.read(Practitioner.class, request.getPractitionerId());
        addResourceToResponseBundleEntries(responseBundle, practitioner, createEntryUrl(practitioner.getIdElement()));

        List<IBaseResource> practitionerRoles = findPractitionerRoles(practitioner.getIdElement());

        practitionerRoles.forEach(theIBaseResource ->
            addResourceToResponseBundleEntries(responseBundle, theIBaseResource, createEntryUrl(theIBaseResource.getIdElement())));

    }

    protected List<IBaseResource> findPractitionerRoles(IdType practitionerId) {
        Map<String, List<IQueryParameterType>> searchParameters = new HashMap<>();
        searchParameters.put("practitioner", Collections.singletonList(new ReferenceParam(practitionerId)));

        Bundle bundle = repository.search(Bundle.class, PractitionerRole.class, searchParameters);

        return BundleHelper.getEntryResources(bundle);
    }

    protected Bundle createResponseBundle() {
        Bundle retVal = (Bundle) BundleHelper.newBundle(fhirVersionEnum, BUNDLE_TYPE);
        retVal.setTimestamp(new Date());
        return retVal;
    }

    protected void addResourceToResponseBundleEntries(Bundle bundle, IBaseResource resource, String entryUrl) {
        IBaseBackboneElement iBaseBackboneElement = BundleHelper.newEntryWithResource(resource);
        BundleHelper.setEntryFullUrl(fhirVersionEnum, iBaseBackboneElement, entryUrl);
        BundleHelper.addEntry(bundle, iBaseBackboneElement);
    }

    private void validateVersion(ApplyRequest request) {
        if(!R4.isEquivalentTo(request.getFhirVersion())){
            throw new InvalidRequestException("Apply is not supported for FHIR version " + request.getFhirVersion());
        }
    }

    private String createEntryUrl(IIdType idElement) {
        return DEFAULT_IDENTIFIER_VALUE_PREFIX + idElement.getIdPart();
    }

    private boolean isGraphDefinitionType(GraphDefinitionLinkTargetComponent target) {
        return target.hasType() && target.getType().equals("GraphDefinition");
    }

    private Composition createAndInitializeCompositeEntry(ApplyRequest request) {
        Composition retVal = new Composition();

        retVal.getMeta().addProfile(Constants.CPG_CASE_PLAN_SUMMARY);

        retVal.addExtension(Constants.CPG_SUMMARY_FOR, new StringType(
            request.getSubjectId().toUnqualifiedVersionless().getValue()));

        GraphDefinition graphDefinition = (GraphDefinition) request.getGraphDefinition();

        if(graphDefinition.hasUrl()) {
            retVal.addExtension(Constants.CPG_GENERATED_FOR, new StringType(graphDefinition.getUrl()));
        }

        retVal.setStatus(Composition.CompositionStatus.FINAL);
        retVal.setType(new CodeableConcept(DEFAULT_CODING));
        retVal.setTitle(graphDefinition.getDescription());
        retVal.setDate(new Date());
        retVal.setSubject(new Reference(request.getSubjectId()));
        retVal.setAuthor(List.of(new Reference(request.getPractitionerId())));

        return retVal;
    }

}
