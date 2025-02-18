/*-
 * #%L
 * HAPI FHIR - Clinical Reasoning
 * %%
 * Copyright (C) 2014 - 2024 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.cr;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.util.ClasspathUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.opencds.cqf.fhir.utility.Ids;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * This is a utility interface that allows a class that has a DaoRegistry to load Bundles and read Resources.
 * This is used primarily to set up integration tests for clinical reasoning operations since they often
 * require big bundles of content, such as Libraries, ValueSets, Measures, and so on.
 */
public interface IResourceLoader extends IDaoRegistryUser {
    /**
     * Method to load bundles
     * @param type, resource type
     * @param theLocation, location of the resource
     * @return of type bundle
     * @param <T>
     */
    default <T extends IBaseBundle> T loadBundle(Class<T> type, String theLocation) {
        var bundle = readResource(type, theLocation);
        getDaoRegistry().getSystemDao().transaction(new SystemRequestDetails(), bundle);

        return bundle;
    }

    /**
     * Method to read resource
     * @param type, resource type
     * @param theLocation, location of the resource
     * @return of type resource
     * @param <T>
     */
    default <T extends IBaseResource> T readResource(Class<T> type, String theLocation) {
        return ClasspathUtil.loadResource(getFhirContext(), type, theLocation);
    }

    /**
     * Method to load resource
     * @param type, resource type
     * @param theLocation, location of the resource
     * @return of type resource
     * @param <T>
     */
    default <T extends IBaseResource> T loadResource(Class<T> type, String theLocation, RequestDetails requestDetails) {
        var resource = readResource(type, theLocation);
        getDaoRegistry().getResourceDao(type).update(resource, requestDetails);

        return resource;
    }

    public default IBaseResource readResource(String theLocation) {
        String resourceString = stringFromResource(theLocation);
        return EncodingEnum.detectEncoding(resourceString)
                .newParser(getFhirContext())
                .parseResource(resourceString);
    }

    public default IBaseResource readAndLoadResource(String theLocation) {
        String resourceString = stringFromResource(theLocation);
        if (theLocation.endsWith("json")) {
            return loadResource(parseResource("json", resourceString));
        } else {
            return loadResource(parseResource("xml", resourceString));
        }
    }

    public default IBaseResource loadResource(IBaseResource resource) {
        if (getDaoRegistry() == null) {
            return resource;
        }

        update(resource);
        return resource;
    }

    public default IBaseResource parseResource(String encoding, String resourceString) {
        IParser parser;
        switch (encoding.toLowerCase()) {
            case "json":
                parser = getFhirContext().newJsonParser();
                break;
            case "xml":
                parser = getFhirContext().newXmlParser();
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Expected encoding xml, or json.  %s is not a valid encoding", encoding));
        }

        return parser.parseResource(resourceString);
    }

    default String stringFromResource(String theLocation) {
        InputStream is = null;
        try {
            if (theLocation.startsWith(File.separator)) {
                is = new FileInputStream(theLocation);
            } else {
                DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
                org.springframework.core.io.Resource resource = resourceLoader.getResource(theLocation);
                is = resource.getInputStream();
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error loading resource from %s", theLocation), e);
        }
    }

    private Bundle.BundleEntryRequestComponent createRequest(IBaseResource resource) {
        Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
        if (resource.getIdElement().hasValue()) {
            request.setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl(resource.getIdElement().getValue());
        } else {
            request.setMethod(Bundle.HTTPVerb.POST).setUrl(resource.fhirType());
        }

        return request;
    }

    default <T extends IBaseResource> T newResource(Class<T> resourceClass, String idPart) {
        checkNotNull(resourceClass);
        checkNotNull(idPart);

        T newResource = newResource(resourceClass);
        newResource.setId((IIdType) Ids.newId(getFhirContext(), newResource.fhirType(), idPart));

        return newResource;
    }

    @SuppressWarnings("unchecked")
    default <T extends IBaseResource> T newResource(Class<T> resourceClass) {
        checkNotNull(resourceClass);

        return (T) this.getFhirContext().getResourceDefinition(resourceClass).newInstance();
    }
}
