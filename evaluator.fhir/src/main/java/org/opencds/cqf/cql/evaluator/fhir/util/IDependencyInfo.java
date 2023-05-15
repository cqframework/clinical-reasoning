package org.opencds.cqf.cql.evaluator.fhir.util;

import org.hl7.fhir.instance.model.api.IPrimitiveType;

public interface IDependencyInfo {

//  // TODO: Need for figuring out how to determine which package the dependency is in.
//  private String referenceSource;
//  private String reference;
//  private String referencePackageId;
//
//  public DependencyInfo() {}
//
//  public DependencyInfo(String referenceSource, String reference) {
//    this.referenceSource = referenceSource;
//    this.reference = reference;
//  }

  public IPrimitiveType<String> getReferenceSource();
//  {
//    return this.referenceSource;
//  }

  public void setReferenceSource(IPrimitiveType<String> referenceSource);
//  {
//    this.referenceSource = referenceSource;
//  }

  public IPrimitiveType<String> getReference();
//  {
//    return this.reference;
//  }

  public void setReference(IPrimitiveType<String> reference);
//  {
//    this.reference = reference;
//  }

  public IPrimitiveType<String> getReferencePackageId();
//  {
//    return this.referencePackageId;
//  }

  public void setReferencePackageId(IPrimitiveType<String> referencePackageId);
//  {
//    this.referencePackageId = referencePackageId;
//  }
}