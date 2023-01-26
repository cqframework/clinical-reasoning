package org.opencds.cqf.cql.evaluator.fhir.util;


import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class FhirResourceLoader implements ResourceLoader{

    List<IBaseResource> resources;
    FhirContext fhirContext;

    public FhirResourceLoader(FhirContext context, Class<?> clazz, List<String> directoryList, boolean recursive) {
        this.fhirContext = context;
        this.resources = new ArrayList<>();
        List<String> locations = new ArrayList<>();

        directoryList.forEach(dir -> {
            locations.addAll(getFilePaths(getDirectoryLocation(clazz, dir), recursive));
        });

        locations.forEach(item -> {
            IBaseResource resource = loadTestResources(item);
            resources.add( resource);
        });
    }

    public List<IBaseResource> getResources() {
        if(resources == null) {
            resources = new ArrayList<>();
        }
        return resources;
    }

    private String getDirectoryLocation(Class<?> clazz, String relativePath) {
        String directoryLocationUrl = clazz.getResource(relativePath).toString();
        if (directoryLocationUrl.startsWith("file:/")) {
            directoryLocationUrl = directoryLocationUrl.substring("file:/".length() - 1);
        }
        return directoryLocationUrl;
    }

    private List<String> getFilePaths(String directoryPath, Boolean recursive) {
        List<String> filePaths = new ArrayList<String>();
        File inputDir = new File(directoryPath);
        ArrayList<File> files = inputDir.isDirectory() ? new ArrayList<File>(Arrays.asList(Optional.ofNullable(inputDir.listFiles()).<NoSuchElementException>orElseThrow(() -> new NoSuchElementException()))) : new ArrayList<File>();

        for (File file : files) {
            if (file.isDirectory()) {
                if (recursive) {
                    filePaths.addAll(getFilePaths(file.getPath(), recursive));
                }
            } else {
                if (!file.getPath().endsWith(".cql"))
                    filePaths.add(file.getPath());
            }
        }
        return filePaths;
    }

    private IBaseResource loadTestResources(String location) {
        IBaseResource resource = readResource(fhirContext, location);
        return resource;
    }
}
