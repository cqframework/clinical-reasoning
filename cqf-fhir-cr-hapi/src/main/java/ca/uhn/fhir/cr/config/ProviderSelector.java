package ca.uhn.fhir.cr.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.Map;

public class ProviderSelector {
    private final FhirContext fhirContext;
    private final Map<FhirVersionEnum, List<Class<?>>> providerMap;

    public ProviderSelector(FhirContext fhirContext, Map<FhirVersionEnum, List<Class<?>>> providerMap) {
        this.fhirContext = fhirContext;
        this.providerMap = providerMap;
    }

    public List<Class<?>> getProviderType() {
        return providerMap.get(fhirContext.getVersion().getVersion());
    }
}
