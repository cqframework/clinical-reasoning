
package org.opencds.cqf.cql.evaluator.engine.terminology;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.cql.evaluator.engine.util.ValueSetUtil;
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
        requireNonNull(fhirContext, "fhirContext can not be null.");
        requireNonNull(bundle, "bundle can not be null.");

        this.fhirContext = fhirContext;
        this.valueSets = BundleUtil.toListOfResourcesOfType(this.fhirContext, bundle, this.fhirContext.getResourceDefinition("ValueSet").getImplementingClass());
    }

    
    /** 
     * This method checks for membership of a Code in a ValueSet
     * @param code The Code to check.
     * @param valueSet The ValueSetInfo for the ValueSet to check membership of. Can not be null.
     * @return True if code is in the ValueSet.
     */
    @Override
    public boolean in(Code code, ValueSetInfo valueSet) {
        requireNonNull(code, "code can not be null when using 'expand'");
        requireNonNull(valueSet, "valueSet can not be null when using 'expand'");

        Iterable<Code> codes = this.expand(valueSet);
        for (Code c : codes) {
            if (c.getCode().equals(code.getCode()) && c.getSystem().equals(code.getSystem())) {
                return true;
            }
        }

        return false;
    }

    
    /** 
     * This method expands a ValueSet into a list of Codes. It will use the "expansion" element of the ValueSet if present.
     * It will fall back the to "compose" element if not present. <b>NOTE:</b> This provider does not provide a full expansion
     * of the "compose" element. If only lists the codes present in the "compose". 
     * @param valueSet The ValueSetInfo of the ValueSet to expand
     * @return The Codes in the ValueSet. <b>NOTE:</b> This method never returns null.
     */
    @Override
    
    public Iterable<Code> expand(ValueSetInfo valueSet) {
        requireNonNull(valueSet, "valueSet can not be null when using 'expand'");

        this.initialize();

        if (!this.valueSetIndex.containsKey(valueSet.getId())) {
            throw new IllegalArgumentException(String.format("Unable to locate ValueSet %s", valueSet.getId()));
        }

        return this.valueSetIndex.get(valueSet.getId());
    }

    
    /** 
     * Lookup is only partially implemented for this TerminologyProvider. Full implementation requires the ability to
     * access the full CodeSystem. This implementation only checks the code system of the code matches the CodeSystemInfo
     * url, and verifies the version if present.
     * @param code The Code to lookup
     * @param codeSystem The CodeSystemInfo of the CodeSystem to check.
     * @return The Code if the system of the Code (and version if specified) matches the CodeSystemInfo url (and version)
     */
    @Override
	public Code lookup(Code code, CodeSystemInfo codeSystem) {
        if (code.getSystem() == null) {
            return null;
        }

        if (code.getSystem().equals(codeSystem.getId()) && (code.getVersion() == null || code.getVersion().equals(codeSystem.getVersion()))) {
            logger.warn("Unvalidated CodeSystem lookup: {} in {}", code.toString(), codeSystem.getId());
            return code;
        }

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
                logger.warn("ValueSet {} is not expanded. Falling back to compose definition. This will potentially produce incorrect results. ", url);
                codes = ValueSetUtil.getCodesInCompose(this.fhirContext, resource);
            }

            if (codes == null) {
                codes = Collections.emptySet();
            }

            this.valueSetIndex.put(url, codes);
        }


        this.initialized = true;
    }

}