package org.opencds.cqf.cql.evaluator.fhir.util;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseExtension;

public class DependencyInfo {
  // TODO: Need for figuring out how to determine which package the dependency is in.
  private String referenceSource;
  private String reference;
  private String referencePackageId;
  private List<? extends IBaseExtension> extensionList;

  public DependencyInfo() {}

  public DependencyInfo(String referenceSource, String reference, List<? extends IBaseExtension> extensionList) {
    this.referenceSource = referenceSource;
    this.reference = reference;
    this.extensionList = extensionList;
  }

  public String getReferenceSource() {
    return this.referenceSource;
  }
  public List<? extends IBaseExtension> getExtension() {
    return this.extensionList;
  }

  public void setReferenceSource(String referenceSource) {
    this.referenceSource = referenceSource;
  }

  public String getReference() {
    return this.reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getReferencePackageId() {
    return this.referencePackageId;
  }

  public void setReferencePackageId(String referencePackageId) {
    this.referencePackageId = referencePackageId;
  }
}