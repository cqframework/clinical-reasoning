package org.opencds.cqf.cql.evaluator.fhir.common;

import java.util.Objects;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.fhir.api.LibraryAdapter;
import org.opencds.cqf.cql.evaluator.fhir.api.ParametersAdapter;
import org.opencds.cqf.cql.evaluator.fhir.api.ParametersParameterComponentAdapter;
import org.opencds.cqf.cql.evaluator.fhir.api.ResourceAdapter;

public class AdapterFactory {
    /**
     * This function creates an adapter that exposes common resource operations
     * across multiple versions of FHIR
     * 
     * @param resource A FHIR Resource
     * @return an adapter exposing common api calls
     */
    public static ResourceAdapter resourceAdapterFor(IBaseResource resource) {
        Objects.requireNonNull(resource, "resource can not be null");

        switch (resource.getStructureFhirVersionEnum()) {
            case DSTU3:
                return new org.opencds.cqf.cql.evaluator.fhir.dstu3.ResourceAdapter();
            case R4:
                return new org.opencds.cqf.cql.evaluator.fhir.r4.ResourceAdapter();
            case R5:
                return new org.opencds.cqf.cql.evaluator.fhir.r5.ResourceAdapter();
            default:
                throw new IllegalArgumentException(String.format("unsupported FHIR version: %s",
                        resource.getStructureFhirVersionEnum().toString()));
        }
    }

    /**
     * This function creates an adapter that exposes common Library operations
     * across multiple versions of FHIR
     * 
     * @param library a FHIR Library resource
     * @return an adapter exposing common api calls
     */
    public static LibraryAdapter libraryAdapterFor(IBaseResource library) {
        Objects.requireNonNull(library, "library can not be null");
        if (!library.fhirType().equals("Library")) {
            throw new IllegalArgumentException("library is not a FHIR Library type");
        }

        switch (library.getStructureFhirVersionEnum()) {
            case DSTU3:
                return new org.opencds.cqf.cql.evaluator.fhir.dstu3.LibraryAdapter();
            case R4:
                return new org.opencds.cqf.cql.evaluator.fhir.r4.LibraryAdapter();
            case R5:
                return new org.opencds.cqf.cql.evaluator.fhir.r5.LibraryAdapter();
            default:
                throw new IllegalArgumentException(String.format("unsupported FHIR version: %s",
                        library.getStructureFhirVersionEnum().toString()));
        }
    }

    /**
     * This function creates an adapter that exposes common Parameters operations
     * across multiple versions of FHIR
     * 
     * @param parameters a FHIR Parameters resource
     * @return an adapter exposing common api calls
     */
    public static ParametersAdapter parametersAdapterFor(IBaseResource parameters) {
        Objects.requireNonNull(parameters, "parameters can not be null");
        if (!parameters.fhirType().equals("Parameters")) {
            throw new IllegalArgumentException("library is not a FHIR Library type");
        }

        switch (parameters.getStructureFhirVersionEnum()) {
            case DSTU3:
                return new org.opencds.cqf.cql.evaluator.fhir.dstu3.ParametersAdapter();
            case R4:
                return new org.opencds.cqf.cql.evaluator.fhir.r4.ParametersAdapter();
            case R5:
                return new org.opencds.cqf.cql.evaluator.fhir.r5.ParametersAdapter();
            default:
                throw new IllegalArgumentException(String.format("unsupported FHIR version: %s",
                        parameters.getStructureFhirVersionEnum().toString()));
        }
    }

    /**
     * This function creates an adapter that exposes common
     * ParametersParameterComponent operations across multiple versions of FHIR
     * 
     * @param ppc a FHIR ParametersParameterComponent
     * @return an adapter exposing common api calls
     */
    public static ParametersParameterComponentAdapter parametersParametersComponentAdapterFor(
            IBaseBackboneElement ppc) {
        Objects.requireNonNull(ppc, "ppc can not be null");
        if (!ppc.fhirType().equals("ParametersParameterComponent")) {
            throw new IllegalArgumentException("library is not a FHIR Library type");
        }

        switch (ppc.getClass().getPackage().getName()) {
            case "org.hl7.fhir.dstu3.model":
                return new org.opencds.cqf.cql.evaluator.fhir.dstu3.ParametersParameterComponentAdapter();
            case "org.hl7.fhir.r4.model":
                return new org.opencds.cqf.cql.evaluator.fhir.r4.ParametersParameterComponentAdapter();
            case "org.hl7.fhir.r5.model":
                return new org.opencds.cqf.cql.evaluator.fhir.r5.ParametersParameterComponentAdapter();
            default:
                throw new IllegalArgumentException(
                        String.format("unsupported FHIR package: %s", ppc.getClass().getPackage().getName()));
        }
    }
}