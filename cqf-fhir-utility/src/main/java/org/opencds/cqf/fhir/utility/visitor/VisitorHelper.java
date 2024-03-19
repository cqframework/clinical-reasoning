package org.opencds.cqf.fhir.utility.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;

public class VisitorHelper {

    @SuppressWarnings("unchecked")
    public static <T extends IBaseDatatype> Optional<T> getParameter(
            String name, IBaseParameters operationParameters, Class<T> type) {
        var factory = AdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(p -> factory.createParameters(p))
                .map(p -> p.getParameter(name))
                .map(p -> factory.createParametersParameters(p))
                .map(parametersParameters -> (T) parametersParameters.getValue());
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseResource> Optional<T> getResourceParameter(
            String name, IBaseParameters operationParameters, Class<T> type) {
        var factory = AdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(p -> factory.createParameters(p))
                .map(p -> p.getParameter(name))
                .map(p -> factory.createParametersParameters(p))
                .map(parametersParameters -> (T) parametersParameters.getResource());
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseDatatype> Optional<List<T>> getListParameter(
            String name, IBaseParameters operationParameters, Class<T> type) {
        var factory = AdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(p -> factory.createParameters(p))
                .map(p -> p.getParameterValues(name))
                .map(vals -> vals.stream().map(val -> (T) val).collect(Collectors.toList()));
    }

    public static List<IBaseResource> getMetadataResourcesFromBundle(IBaseBundle bundle) {
        List<IBaseResource> resourceList = new ArrayList<>();
        var version = bundle.getStructureFhirVersionEnum();
        if (!BundleHelper.getEntryFirstRep(bundle).isEmpty()) {
            BundleHelper.getEntry(bundle).stream()
                    .map(e -> BundleHelper.getEntryResource(version, e))
                    .filter(r -> r != null)
                    .forEach(r -> {
                        switch (version) {
                            case DSTU3:
                                if (r instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
                                    resourceList.add(r);
                                }
                                break;
                            case R4:
                                if (r instanceof org.hl7.fhir.r4.model.MetadataResource) {
                                    resourceList.add(r);
                                }
                                break;
                            case R5:
                                if (r instanceof org.hl7.fhir.r5.model.MetadataResource) {
                                    resourceList.add(r);
                                }
                                break;
                            default:
                                break;
                        }
                    });
        }

        return resourceList;
    }
}
