package org.opencds.cqf.cql.evaluator.fhir;

import org.hl7.fhir.instance.model.api.IBaseBundle;
/**
 * This interface takes a directory and bundles all FHIR resources found in it
 * recursively.
 */
public interface DirectoryBundler {
    public  IBaseBundle bundle(String path); 
}