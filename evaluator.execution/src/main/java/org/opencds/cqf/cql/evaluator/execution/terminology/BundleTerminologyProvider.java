
package org.opencds.cqf.cql.evaluator.execution.terminology;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.cql.evaluator.execution.util.ValueSetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;

public class BundleTerminologyProvider implements TerminologyProvider {

    private static final Logger logger = LoggerFactory.getLogger(BundleTerminologyProvider.class);

    private FhirContext fhirContext;
    private List<? extends IBaseResource> valueSets;
    private Map<String, Iterable<Code>> valueSetIndex = new HashMap<>();

    private boolean initialized = false;

    public BundleTerminologyProvider(FhirContext fhirContext, IBaseBundle bundle) {
        Objects.requireNonNull(fhirContext, "fhirContext can not be null.");
        Objects.requireNonNull(bundle, "bundle can not be null.");

        this.fhirContext = fhirContext;
        this.valueSets = BundleUtil.toListOfResourcesOfType(this.fhirContext, bundle, this.fhirContext.getResourceDefinition("ValueSet").getImplementingClass());
    }

    @Override
    public boolean in(Code code, ValueSetInfo valueSet) {
        if (code == null || valueSet == null) {
            throw new IllegalArgumentException("code and valueSet must not be null when testing 'in'.");
        }

        Iterable<Code> codes = this.expand(valueSet);
        if (codes == null) {
            return false;
        }

        for (Code c : codes) {
            if (c.getCode().equals(code.getCode()) && c.getSystem().equals(code.getSystem())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterable<Code> expand(ValueSetInfo valueSet) {
        this.initialize();

        if (valueSet == null) {
            throw new IllegalArgumentException("valueSet must not be null when attempting to expand");
        }

        if (!this.valueSetIndex.containsKey(valueSet.getId())) {
            throw new IllegalArgumentException(String.format("Unable to locate valueset %s", valueSet.getId()));
        }

        return this.valueSetIndex.get(valueSet.getId());
    }

    @Override
    // TODO: One possible option here is to generate codes systems
    // based on the valuesets we have loaded.
	public Code lookup(Code code, CodeSystemInfo codeSystem) {
        logger.warn("Unsupported CodeSystem lookup: %s in %s", code.toString(), codeSystem.getId());
        return null;
    }

    private void initialize() {
        if (this.initialized) {
            return;
        }

        for (IBaseResource resource : this.valueSets) {
            String url = ValueSetUtil.getUrl(fhirContext, resource);
            Iterable<Code> codes = ValueSetUtil.getCodesInExpansion(this.fhirContext, resource);

            if (codes == null) {
                logger.warn("ValueSet %s is not expanded. Falling back to compose definition. This will potentially produce incorrect results. ", url);
                codes = ValueSetUtil.getCodesInCompose(this.fhirContext, resource);
            }

            this.valueSetIndex.put(url, codes);
        }


        this.initialized = true;
    }

}