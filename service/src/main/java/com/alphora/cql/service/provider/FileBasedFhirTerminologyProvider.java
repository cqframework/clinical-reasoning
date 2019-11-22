package com.alphora.cql.service.provider;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.alphora.cql.service.Helpers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import com.alphora.cql.service.util.ValueSetUtil;

public class FileBasedFhirTerminologyProvider implements TerminologyProvider {


    public FileBasedFhirTerminologyProvider(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    private String uri;
    private FhirContext fhirContext;

    private Map<ValueSetInfo, Iterable<Code>> valueSetIndex = new HashMap<>();

    public FileBasedFhirTerminologyProvider(String uri) {
        if (uri == null || uri.isEmpty() || !Helpers.isFileUri(uri)) {
            throw new IllegalArgumentException("File Terminology provider requires a valid path to Terminology resources");
        }

        this.uri = uri;
    }

    @Override
    public boolean in(Code code, ValueSetInfo valueSet) {
        if (code == null || valueSet == null) {
            throw new IllegalArgumentException("code and valueset must not be null when testing 'in'.");
        }

        Iterable<Code> codes = this.expand(valueSet);
        if (codes == null) {
            return false;
        }
        for (Code c : codes) {
            if (c.equivalent(code)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterable<Code> expand(ValueSetInfo valueSet) {
        if (valueSet == null) {
            throw new IllegalArgumentException("valueset must not be null when attempting to expand");
        }

        if (!this.valueSetIndex.containsKey(valueSet)) {
            this.loadValueSet(valueSet);
        }

        return this.valueSetIndex.get(valueSet);
    }

    @Override
	public Code lookup(Code code, CodeSystemInfo codeSystem) {
		return null;
    }

    private void loadValueSet(ValueSetInfo valueSet) {
        String id = valueSet.getId();
        Path vsPath = Path.of(this.uri, "valueset" + id + ".json");
        File file = vsPath.toFile();

        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("Unable to locate valueset %s", valueSet.getId()));
        }

        try {
            String content = new String (Files.readAllBytes(vsPath));
            IParser parser = this.fhirContext.newJsonParser();
            IBaseResource resource = parser.parseResource(new StringReader(content));

            Iterable<Code> codes = ValueSetUtil.getCodesInExpansion(this.fhirContext, resource);

            if (codes == null) {
                throw new IllegalArgumentException(String.format("No expansion found for ValueSet %s. The File-based ValueSet provider requires ValueSets to be expanded.", valueSet.getId()));
            }
            
            this.valueSetIndex.put(valueSet, codes);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to load valueset %s located at %s.", valueSet.getId(), vsPath.toString()));
        }
    }
}