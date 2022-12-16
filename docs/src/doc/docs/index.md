# CQF Clinical Reasoning

Welcome to the CQF Clinical Reasoning Documentation. [Clinical Quality Framework](https://www.cqframework.org) (CQF) is a joint effort by the [Health Level 7 (HL7)](http://hl7.org) [Clinical Decision Support](https://confluence.hl7.org/display/CDS/WorkGroup+Home) and [Clinical Quality Information](https://confluence.hl7.org/display/CQIWC/Clinical+Quality+Information+Home) Work Groups to identify, develop, and harmonize standards that promote integration and reuse between Clinical Decision Support (CDS) and Clinical Quality Measurement (CQM).

This repository contains reference code and documentation for various FHIR clinical reasoning operations in Java. It provides a set of interfaces based upon the [HAPI FHIR](https://hapifhir.io/hapi-fhir/) Java models that allow these operations to be implemented on various Java-based platforms, such as Android, Spark, and the HAPI FHIR server.

There is a particular focus on how CQL relates to FHIR, providing guidance for binding CQL and FHIR for authoring, packaging, publication, and at runtime.

There are also Javascript implementations of [CQL](https://github.com/cqframework/cql-execution) and other FHIR operations, also hosted by CQF.

## Contributing

Developers should see the [Developer Guide](developer-guide.md) for details on setting up a deveopment environment, an overview of architecture and so on.
