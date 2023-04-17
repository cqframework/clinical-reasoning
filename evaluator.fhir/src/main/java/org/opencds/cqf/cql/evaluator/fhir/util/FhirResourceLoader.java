package org.opencds.cqf.cql.evaluator.fhir.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBaseResource;

import com.google.common.io.Files;

import ca.uhn.fhir.context.FhirContext;

public class FhirResourceLoader implements ResourceLoader {

  List<IBaseResource> resources;
  FhirContext fhirContext;
  Class<?> relativeToClazz;


  public FhirResourceLoader(FhirContext context, Class<?> clazz, List<String> directoryList,
      boolean recursive) {
    this.fhirContext = context;
    this.resources = new ArrayList<>();
    this.relativeToClazz = clazz;
    List<String> locations = new ArrayList<>();

    directoryList.forEach(dir -> {
      locations.addAll(getFilePaths(getDirectoryOrFileLocation(dir), recursive));
    });

    locations.forEach(item -> {
      IBaseResource resource = loadTestResources(item);
      resources.add(resource);
    });
  }

  public Class<?> getRelativeClass() {
    return relativeToClazz;
  }

  public List<IBaseResource> getResources() {
    if (resources == null) {
      resources = new ArrayList<>();
    }
    return resources;
  }

  private String getDirectoryOrFileLocation(String relativePath) {
    String directoryLocationUrl = this.relativeToClazz.getResource(relativePath).toString();


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
        if (!file.getPath().endsWith(".cql"))
          filePaths.add(file.getPath());
      }
    }
    return filePaths;
  }

  private String getCqlContent(String rootPath, String relativePath) {
    var p = Paths.get(rootPath).getParent().resolve(relativePath).normalize();
    try {
      return Files.asCharSource(p.toFile(), StandardCharsets.UTF_8).read();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private IBaseResource loadTestResources(String location) {

    IBaseResource resource = readResource(fhirContext, location);
    if (resource.fhirType().equals("Library")) {
      String cqlLocation;
      switch (fhirContext.getVersion().getVersion()) {
        case DSTU3:
          cqlLocation =
              org.opencds.cqf.cql.evaluator.fhir.util.dstu3.AttachmentUtil.getCqlLocation(resource);
          if (cqlLocation != null) {
            resource = org.opencds.cqf.cql.evaluator.fhir.util.dstu3.AttachmentUtil
                .addData(resource, getCqlContent(location, cqlLocation));
          }
          break;
        case R4:
          cqlLocation =
              org.opencds.cqf.cql.evaluator.fhir.util.r4.AttachmentUtil.getCqlLocation(resource);
          if (cqlLocation != null) {
            resource = org.opencds.cqf.cql.evaluator.fhir.util.r4.AttachmentUtil.addData(resource,
                getCqlContent(location, cqlLocation));
          }
          break;
        case R5:
          cqlLocation =
              org.opencds.cqf.cql.evaluator.fhir.util.r5.AttachmentUtil.getCqlLocation(resource);
          if (cqlLocation != null) {
            resource = org.opencds.cqf.cql.evaluator.fhir.util.r5.AttachmentUtil.addData(resource,
                getCqlContent(location, cqlLocation));
          }
          break;
        default:
          throw new IllegalArgumentException(
              String.format("unsupported FHIR version: %s", fhirContext));
      }
    }
    return resource;
  }
}
