package org.opencds.cqf.cql.evaluator.fhir.util;

public class DependencyInfo {
  // TODO: Need for figuring out how to determine which package the dependency is in.
  private String referenceSource;
  private String reference;
  private String referencePackageId;

  public DependencyInfo() {}

  public DependencyInfo(String referenceSource, String reference) {
    this.referenceSource = referenceSource;
    this.reference = reference;
  }

  public String getReferenceSource() {
    return this.referenceSource;
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