package org.opencds.cqf.fhir.test;

import ca.uhn.fhir.context.FhirContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class FhirResourceLoader implements ResourceLoader {

    List<IBaseResource> resources;
    FhirContext fhirContext;
    Class<?> relativeToClazz;

    public FhirResourceLoader(FhirContext context, Class<?> clazz, List<String> directoryList, boolean recursive) {
        this.fhirContext = context;
        this.relativeToClazz = clazz;

        this.resources = directoryList.stream()
                .map(this::getDirectoryOrFileLocation)
                .filter(x -> x != null)
                .map(x -> this.getFilePaths(x, recursive))
                .flatMap(Collection::stream)
                .map(this::loadTestResources)
                .collect(Collectors.toList());
    }

    public Class<?> getRelativeClass() {
        return relativeToClazz;
    }

    public List<IBaseResource> getResources() {
        return resources;
    }

    private String getDirectoryOrFileLocation(String relativePath) {
        var resource = this.relativeToClazz.getResource(relativePath);

        if (resource == null) {
            return null;
        }

        String directoryLocationUrl = resource.toString();

        if (directoryLocationUrl.startsWith("file:/")) {
            directoryLocationUrl = directoryLocationUrl.substring("file:/".length() - 1);
        }
        return directoryLocationUrl;
    }

    private List<String> getFilePaths(String directoryPath, boolean recursive) {
        var filePaths = new ArrayList<String>();
        File inputDir = new File(directoryPath);
        var files = inputDir.isDirectory()
                ? new ArrayList<File>(Arrays.asList(
                        Optional.ofNullable(inputDir.listFiles()).orElseThrow(NoSuchElementException::new)))
                : new ArrayList<File>();

        for (File file : files) {
            if (file.isDirectory()) {
                if (recursive) {
                    filePaths.addAll(getFilePaths(file.getPath(), recursive));
                }
            } else {
                if (!file.getPath().endsWith(".cql")) filePaths.add(file.getPath());
            }
        }
        return filePaths;
    }

    private IBaseResource loadTestResources(String location) {
        return readResource(fhirContext, location);
    }
}
