package org.opencds.cqf.cql.evaluator.fhir.common;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.api.BundleInclusionRule;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.util.BundleUtil;

class DirectoryBundler implements org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler {


    private static final Logger logger = LoggerFactory.getLogger(DirectoryBundler.class);

    private FhirContext fhirContext;
    private IParser xml = null;
    private IParser json = null;

    @Inject
    DirectoryBundler(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }
    /**
     * Recursively searches all files and sub-directory and parses all xml and json
     * FHIR resources. Adds all resources to a collection-type Bundle (recursively
     * flattening Bundle resources).
     * @param path The root directory to bundle.
     * @return A Bundle of all the resources in the root directory and
     *         subdirectories
     */
    @Override
    public IBaseBundle bundle(String path) {
        Objects.requireNonNull(path, "path must not be null.");

        File resourceDirectory = new File(path);
        if (!resourceDirectory.exists()) {
            throw new IllegalArgumentException("The specified path to resource files does not exist.");
        }

        if (!resourceDirectory.isDirectory()) {
            throw new IllegalArgumentException("The specified path to resource files is not a directory.");
        }

        Collection<File> files = FileUtils.listFiles(resourceDirectory, new String[] { "xml", "json" }, true);

        return new DirectoryBundler(fhirContext).bundleFiles(files);
    }

    private IBaseBundle bundleFiles(Collection<File> files) {
        List<IBaseResource> resources = new ArrayList<>();

        for (File f : files) {
            IBaseResource resource = parseFile(f);

            if (resource == null) {
                continue;
            }

            if (resource instanceof IBaseBundle) {
                List<IBaseResource> innerResources = flatten(this.fhirContext, (IBaseBundle) resource);
                resources.addAll(innerResources);
            } else {
                resources.add(resource);
            }
        }

        IVersionSpecificBundleFactory bundleFactory = this.fhirContext.newBundleFactory();

        bundleFactory.addRootPropertiesToBundle("bundled-directory", null, null, null, null, resources.size(),
                BundleTypeEnum.COLLECTION, null);

        bundleFactory.addResourcesToBundle(resources, BundleTypeEnum.COLLECTION, "",
                BundleInclusionRule.BASED_ON_INCLUDES, null);

        return (IBaseBundle) bundleFactory.getResourceBundle();
    }

    private IBaseResource parseFile(File f) {
        try {
            String resource = FileUtils.readFileToString(f, Charset.forName("UTF-8"));
            if (f.getName().endsWith("json")) {
                if (this.json == null) {
                    this.json = this.fhirContext.newJsonParser();
                }

                return this.json.parseResource(resource);
            } else {
                if (this.xml == null) {
                    this.xml = this.fhirContext.newXmlParser();
                }

                return this.xml.parseResource(resource);
            }
        } catch (Exception e) {
            logger.warn("Error parsing resource {}: {}", f.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    private List<IBaseResource> flatten(FhirContext fhirContext, IBaseBundle bundle) {
        List<IBaseResource> resources = new ArrayList<>();

        List<IBaseResource> bundleResources = BundleUtil.toListOfResources(fhirContext, bundle);
        for (IBaseResource r : bundleResources) {
            if (r instanceof IBaseBundle) {
                List<IBaseResource> innerResources = flatten(fhirContext, (IBaseBundle) r);
                resources.addAll(innerResources);
            } else {
                resources.add(r);
            }
        }

        return resources;
    }
}