# CQL Evaluator

This project provides a Java-based [CQL](https://cql.hl7.org/) evaluation service and related components.

The front-end of the evaluator is the [cql-to-elm translator](https://github.com/cqframework/clinical_quality_language/tree/master/Src/java) (which provides translation of CQL to [ELM](https://cql.hl7.org/elm.html)) and the backend is the [cql-engine](https://github.com/DBCG/cql_engine) (which provides ELM execution). The evaluator glues those two components together and provides DataProvider and TerminologyProvider implementations that allow execution of CQL against a filesystem, FHIR server, and in-process FHIR resources.

## Prerequisites

Java 11+

Maven 3.5+

## Usage

## License

Copyright 2020 Dynamic Content Group

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
