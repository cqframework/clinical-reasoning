package org.opencds.cqf.cql.evaluator.fhir.adapter;

import org.hl7.fhir.instance.model.api.IBase;

/** Marker interface for HL7 Structure adapters
 * @param <T> An HL7 Structure Type
 */
public interface Adapter <T extends IBase> {
    /**
     * @return returns the underlying HL7 Structure for this adapter
     */
    T get();
}