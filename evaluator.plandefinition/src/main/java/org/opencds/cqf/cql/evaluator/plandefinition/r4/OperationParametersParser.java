package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersParameterComponentAdapter;

/**
 * This class maps the standard input parameters of an Operation to key, value pairs.
 */
@Named
public class OperationParametersParser {

    protected AdapterFactory adapterFactory;
    protected FhirTypeConverter fhirTypeConverter;

    @Inject
    public OperationParametersParser(AdapterFactory adapterFactory, FhirTypeConverter fhirTypeConverter) {
        requireNonNull(adapterFactory, "adapterFactory must not be null");
        requireNonNull(fhirTypeConverter, "fhirTypeConverter must not be null");
        this.adapterFactory = adapterFactory;
        this.fhirTypeConverter = fhirTypeConverter;
    }

    protected void addResourceChild(IBaseParameters parameters, String name, IBaseResource resource) {
        requireNonNull(parameters, "parameters must not be null");
        requireNonNull(name, "name must not be null");
        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        List<ParametersParameterComponentAdapter> parts = parametersAdapter.getParameter().stream()
            .map(x -> this.adapterFactory.createParametersParameters(x)).collect(Collectors.toList());

        ParametersParameterComponentAdapter part = parts.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
        if (part == null) {
            part = this.adapterFactory.createParametersParameters(parametersAdapter.addParameter());
         
        }

        part.setName(name);
        part.setResource(resource);
        part.setValue(null);
    }

    protected void addValueChild(IBaseParameters parameters, String name, IBaseDatatype value) {
        requireNonNull(parameters, "parameters must not be null");
        requireNonNull(name, "name must not be null");
        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        List<ParametersParameterComponentAdapter> parts = parametersAdapter.getParameter().stream()
            .map(x -> this.adapterFactory.createParametersParameters(x)).collect(Collectors.toList());

        ParametersParameterComponentAdapter part = parts.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
        if (part == null) {
            part = this.adapterFactory.createParametersParameters(parametersAdapter.addParameter());
         
        }

        part.setName(name);
        part.setResource(null);
        part.setValue(value);
    }

    protected IBaseResource getResourceChild(IBaseParameters parameters, String name) {
        if (parameters == null) {
            return null;
        }
        requireNonNull(name, "name must not be null");
        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        List<ParametersParameterComponentAdapter> parts = parametersAdapter.getParameter().stream()
            .map(x -> this.adapterFactory.createParametersParameters(x)).collect(Collectors.toList());

            IBaseResource value = parts.stream()
            .filter(x -> x.getName().equals(name))
            .map(x -> x.getResource())
            .findFirst()
            .orElse(null);
            
        return value;
    }

    protected Map<String, IBaseResource> getResourceChildren(IBaseParameters parameters) {
        if (parameters == null) {
            return null;
        }
        requireNonNull(parameters, "parameters must not be null");
        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        List<ParametersParameterComponentAdapter> parts = parametersAdapter.getParameter().stream()
            .map(x -> this.adapterFactory.createParametersParameters(x)).collect(Collectors.toList());

        Map<String, IBaseResource> resources = parts.stream()
            .collect(Collectors.toMap(x -> x.getName(), x -> x.getResource()));
            
        return resources;
    }

    protected IBaseDatatype getValueChild(IBaseParameters parameters, String name) {
        if (parameters == null) {
            return null;
        }
        requireNonNull(parameters, "parameters must not be null");
        requireNonNull(name, "name must not be null");
        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        List<ParametersParameterComponentAdapter> parts = parametersAdapter.getParameter().stream()
            .map(x -> this.adapterFactory.createParametersParameters(x)).collect(Collectors.toList());

        IBaseDatatype value = parts.stream()
            .filter(x -> x.getName().equals(name))
            .map(x -> x.getValue())
            .findFirst()
            .orElse(null);
            
        return value;
    }

    protected Map<String, IBaseDatatype> getValueChildren(IBaseParameters parameters) {
        if (parameters == null) {
            return null;
        }
        requireNonNull(parameters, "parameters must not be null");
        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        List<ParametersParameterComponentAdapter> parts = parametersAdapter.getParameter().stream()
            .map(x -> this.adapterFactory.createParametersParameters(x)).collect(Collectors.toList());

        Map<String, IBaseDatatype> values = parts.stream()
            .collect(Collectors.toMap(x -> x.getName(), x -> x.getValue()));
            
        return values;
    }

    // @SuppressWarnings("unused")
    protected Map<String, Object> getParameterParts(IBaseParameters parameters) {
        throw new NotImplementedException("OperationParametersParser.getParameterParts is not implemented yet");
        // requireNonNull(parameters, "parameters must not be null");
        // ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        // List<ParametersParameterComponentAdapter> parts = parametersAdapter.getParameter().stream()
        //     .map(x -> this.adapterFactory.createParametersParameters(x)).collect(Collectors.toList());

        // Map<String, Object> parameterParts = parts.stream()
        //     .collect(Collectors.toMap(x -> x.getName(), x -> {
        //         // This needs to work in order to support this
        //         List<ParametersParameterComponentAdapter> part = x.getPart();

        //         // Recursive parts
        //         // Get all Resources mapped to Name
        //         // Get all Datatypes mapped to Name

        //         return part;
        //     }));
            
        // return parameterParts;
        // return Collections.emptyMap();
    }
}

