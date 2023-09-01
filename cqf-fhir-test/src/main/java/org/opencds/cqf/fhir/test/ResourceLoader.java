package org.opencds.cqf.fhir.test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ResourceLoader {

    Class<?> getRelativeClass();

    default Object loadBundle(FhirContext context, String theLocation) {
        IBaseBundle resource = (IBaseBundle) readResource(context, theLocation);
        return resource;
    }

    default IBaseResource readResource(FhirContext context, String theLocation) {
        String resourceString = stringFromResource(theLocation);
        if (theLocation.endsWith("json")) {
            return parseResource(context, "json", resourceString);
        } else {
            return parseResource(context, "xml", resourceString);
        }
    }

    public default IBaseResource parseResource(FhirContext context, String encoding, String resourceString) {
        IParser parser;
        switch (encoding.toLowerCase()) {
            case "json":
                parser = context.newJsonParser();
                break;
            case "xml":
                parser = context.newXmlParser();
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Expected encoding xml, or json.  %s is not a valid encoding", encoding));
        }

        return parser.parseResource(resourceString);
    }

    default IBaseResource loadResource(FhirContext context, String theLocation) {
        String resourceString = stringFromResource(theLocation);
        if (theLocation.endsWith("json")) {
            return loadResource(context, "json", resourceString);
        } else {
            return loadResource(context, "xml", resourceString);
        }
    }

    default IBaseResource loadResource(FhirContext context, String encoding, String resourceString) {
        IBaseResource resource = parseResource(context, encoding, resourceString);
        return resource;
    }

    @SuppressWarnings("java:S112")
    default String stringFromResource(String theLocation) {
        InputStream is = null;
        try {
            File f = new File(theLocation);
            if (f.isFile()) {
                // if (theLocation.startsWith(File.separator)) {
                is = new FileInputStream(theLocation);
            } else {
                is = getRelativeClass().getResourceAsStream(theLocation);
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error loading resource from %s", theLocation), e);
        }
    }
}
