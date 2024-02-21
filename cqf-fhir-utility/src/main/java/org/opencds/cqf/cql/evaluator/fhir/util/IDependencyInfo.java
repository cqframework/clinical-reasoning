package org.opencds.cqf.cql.evaluator.fhir.util;

import org.hl7.fhir.instance.model.api.IPrimitiveType;

public interface IDependencyInfo {
  public IPrimitiveType<String> getReferenceSource();

  public void setReferenceSource(IPrimitiveType<String> referenceSource);

  public IPrimitiveType<String> getReference();

  public void setReference(IPrimitiveType<String> reference);

  public IPrimitiveType<String> getReferencePackageId();

  public void setReferencePackageId(IPrimitiveType<String> referencePackageId);
}