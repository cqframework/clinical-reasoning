package org.opencds.cqf.fhir.utility.operation;

enum Scope {
    // Operation requires an instance of the resource (e.g. a specific Patient)
    // i.e invoked by id on the resource
    INSTANCE, 
    // Operation applies to a type of resource (e.g. all Patients)
    TYPE,
    // Operation is server-wide
    SERVER
}
