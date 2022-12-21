# CQF Clinical Reasoning

Welcome to the CQF Clinical Reasoning on FHIR Documentation. [Clinical Quality Framework](https://www.cqframework.org) (CQF) is a joint effort by the [Health Level 7 (HL7)](http://hl7.org) [Clinical Decision Support](https://confluence.hl7.org/display/CDS/WorkGroup+Home) and [Clinical Quality Information](https://confluence.hl7.org/display/CQIWC/Clinical+Quality+Information+Home) Work Groups to identify, develop, and harmonize standards that promote integration and reuse between Clinical Decision Support (CDS) and Clinical Quality Measurement (CQM). [Fast Healthcare Interoperability Resouces](https://hl7.org/fhir/) (FHIR) is a standard for healthcare data. CQF uses FHIR as the standard for representing healthcare data and defines standards for Clinical Reasoning operations using FHIR. [Clinical Reasoning](http://hl7.org/fhir/clinicalreasoning-module.html) could be broadly described as "business logic for clinical settings" and includes things like quality measure reporting, clinical decision support, and disease sureveillance.

This repository contains reference code and documentation for various clinical reasoning operations on FHIR in Java. It provides a set of interfaces based upon the [HAPI FHIR](https://hapifhir.io/hapi-fhir/) Java models that allow these operations to be implemented on various Java-based platforms, such as Android, Spark, and the HAPI FHIR server.

There is a particular focus on how [Clinical Quality Language](https://cql.hl7.org/) (CQL) relates to FHIR. CQL is a domain specific language targeted at authors of quality measure and clinical support logic. Much like FHIR standardizes the data format, CQL provides a standard logic representation.

CQF also hosts Javascript implementations of [CQL](https://github.com/cqframework/cql-execution) and other FHIR operations.

## Contributing

Developers should see the [Developer Guide](developer-guide.md) for details on setting up a deveopment environment, an overview of architecture and so on.
