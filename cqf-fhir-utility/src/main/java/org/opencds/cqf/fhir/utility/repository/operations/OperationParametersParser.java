package org.opencds.cqf.fhir.utility.repository.operations;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;

/**
 * This class maps the standard input parameters of an Operation to key, value pairs.
 */
public class OperationParametersParser {

    protected IAdapterFactory adapterFactory;
    // protected FhirTypeConverter fhirTypeConverter;

    public OperationParametersParser(IAdapterFactory adapterFactory) {
        requireNonNull(adapterFactory, "adapterFactory must not be null");
        // requireNonNull(fhirTypeConverter, "fhirTypeConverter must not be null");
        this.adapterFactory = adapterFactory;
        // this.fhirTypeConverter = fhirTypeConverter;
    }

    public void addResourceChild(IBaseParameters parameters, String name, IBaseResource resource) {
        requireNonNull(parameters, "parameters must not be null");
        requireNonNull(name, "name must not be null");
        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        var parts = parametersAdapter.getParameter();

        IParametersParameterComponentAdapter part =
                parts.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
        if (part == null) {
            part = this.adapterFactory.createParametersParameter(parametersAdapter.addParameter());
        }

        part.setName(name);
        part.setResource(resource);
        part.setValue(null);
    }

    public void addValueChild(IBaseParameters parameters, String name, IBaseDatatype value) {
        requireNonNull(parameters, "parameters must not be null");
        requireNonNull(name, "name must not be null");
        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        var parts = parametersAdapter.getParameter();

        IParametersParameterComponentAdapter part =
                parts.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
        if (part == null) {
            part = this.adapterFactory.createParametersParameter(parametersAdapter.addParameter());
        }

        part.setName(name);
        part.setResource(null);
        part.setValue(value);
    }

    public IBaseResource getResourceChild(IBaseParameters parameters, String name) {
        if (parameters == null) {
            return null;
        }
        requireNonNull(name, "name must not be null");
        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        return parametersAdapter.getParameter().stream()
                .filter(x -> x.getName().equals(name))
                .map(IParametersParameterComponentAdapter::getResource)
                .findFirst()
                .orElse(null);
    }

    public Map<String, IBaseResource> getResourceChildren(IBaseParameters parameters) {
        if (parameters == null) {
            return null;
        }
        requireNonNull(parameters, "parameters must not be null");
        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        return parametersAdapter.getParameter().stream()
                .collect(Collectors.toMap(
                        IParametersParameterComponentAdapter::getName,
                        IParametersParameterComponentAdapter::getResource));
    }

    public IBaseDatatype getValueChild(IBaseParameters parameters, String name) {
        if (parameters == null) {
            return null;
        }
        requireNonNull(parameters, "parameters must not be null");
        requireNonNull(name, "name must not be null");
        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        return parametersAdapter.getParameter().stream()
                .filter(x -> x.getName().equals(name))
                .map(IParametersParameterComponentAdapter::getValue)
                .findFirst()
                .orElse(null);
    }

    public Map<String, IBaseDatatype> getValueChildren(IBaseParameters parameters) {
        if (parameters == null) {
            return null;
        }
        requireNonNull(parameters, "parameters must not be null");
        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        return parametersAdapter.getParameter().stream()
                .collect(Collectors.toMap(
                        IParametersParameterComponentAdapter::getName, IParametersParameterComponentAdapter::getValue));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getParameterParts(IBaseParameters parameters) {
        requireNonNull(parameters, "parameters must not be null");
        var parametersAdapter = adapterFactory.createParameters(parameters);
        var parts = parametersAdapter.getParameter();

        Map<String, Object> parameterParts = new HashMap<>();
        for (var part : parts) {
            IBase value = part.hasValue() ? part.getValue() : part.hasResource() ? part.getResource() : null;
            if (value != null) {
                if (!parameterParts.containsKey(part.getName())) {
                    parameterParts.put(part.getName(), value);
                } else {
                    var existingValue = parameterParts.get(part.getName());
                    if (existingValue instanceof List) {
                        ((List<IBase>) existingValue).add(value);
                        parameterParts.put(part.getName(), existingValue);
                    } else {
                        var newListValue = Arrays.asList(existingValue, value);
                        parameterParts.put(part.getName(), newListValue);
                    }
                }
            }
        }
        return parameterParts;
    }
}
