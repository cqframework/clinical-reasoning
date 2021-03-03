package org.opencds.cqf.cql.evaluator.fhir;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.api.BundleInclusionRule;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.BundleLinks;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.util.BundleUtil;

/**
 * This class takes a directory and bundles all FHIR resources found in it
 * recursively.
 */
@Named
public class DirectoryBundler {


    private static final Logger logger = LoggerFactory.getLogger(DirectoryBundler.class);

    private FhirContext fhirContext;
    private IParser xml = null;
    private IParser json = null;

    @Inject
    public DirectoryBundler(FhirContext fhirContext) {
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
    public IBaseBundle bundle(String path) {
        requireNonNull(path, "path must not be null.");

        URI uri;
        try{
            // TODO: Should use builder.UriUtil.isUri
            if (!path.startsWith("file:/") && !path.matches("\\w+?://.*")) {
                File file = new File(path);
                uri = file.toURI();
            }
            else {
                uri = new URI(path);
            }
        }
        catch(Exception e) {
            logger.error(String.format("error parsing uri from path: %s", path), e);
            throw new RuntimeException(e);
        }

        Collection<File> files;
        if (uri.getScheme() != null && uri.getScheme().startsWith("jar")) {
            files = this.listJar(uri, path);
        }
        else {
            files = this.listDirectory(uri.getPath());
        }

        return this.bundleFiles(path, files);
    }

    private Collection<File> listJar(URI uri, String path) {
        try {
            FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
            Path jarPath = fileSystem.getPath(path);
            try(Stream<Path> walk = Files.walk(jarPath, FileVisitOption.FOLLOW_LINKS)) {
                return walk.map(x -> x.toFile()).filter(x -> x.isFile()).filter(
                    x -> x.getName().endsWith("json") || x.getName().endsWith("xml")).collect(Collectors.toList());
            }
        }
        catch (Exception e) {
            logger.error(String.format("error attempting to list jar: %s", uri.toString()));
            throw new RuntimeException(e);
        }
    }

    private Collection<File> listDirectory(String path) {
        File resourceDirectory = new File(path);
        if (!resourceDirectory.getAbsoluteFile().exists()) {
            throw new IllegalArgumentException(String.format("The specified path to resource files does not exist: %s", path));
        }

        if (resourceDirectory.getAbsoluteFile().isDirectory()) {
            return FileUtils.listFiles(resourceDirectory, new String[] { "xml", "json" }, true);
        }
        else if (path.toLowerCase().endsWith("xml") || path.toLowerCase().endsWith("json")) {
            return Collections.singletonList(resourceDirectory);
        }
        else {
            throw new IllegalArgumentException(String.format("path was not a directory or a recognized FHIR file format (XML, JSON) : %s", path));
        }
    }

    private IBaseBundle bundleFiles(String rootPath, Collection<File> files) {
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

        BundleLinks bundleLinks = new BundleLinks(rootPath, null, true, BundleTypeEnum.COLLECTION);

        bundleFactory.addRootPropertiesToBundle("bundled-directory", bundleLinks, resources.size(), null);

        bundleFactory.addResourcesToBundle(resources, BundleTypeEnum.COLLECTION, "",
                BundleInclusionRule.BASED_ON_INCLUDES, null);

        return (IBaseBundle) bundleFactory.getResourceBundle();
    }

    private IBaseResource parseFile(File f) {
        try {
            String resource = FileUtils.readFileToString(f, Charset.forName("UTF-8"));

            IParser selectedParser = this.selectParser(f.getName());
            return selectedParser.parseResource(resource);
        } catch (Exception e) {
            logger.warn("Error parsing resource {}: {}", f.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    private IParser selectParser(String filename) {
        if (filename.toLowerCase().endsWith("json")) {
            if (this.json == null) {
                this.json = this.fhirContext.newJsonParser();
            }
            
            return this.json;
        } else {
            if (this.xml == null) {
                this.xml = this.fhirContext.newXmlParser();
            }

            return this.xml;
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