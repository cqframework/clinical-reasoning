# CQL Evaluator

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf.cql/evaluator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf.cql/evaluator) [![Build Status](https://www.travis-ci.com/DBCG/cql-evaluator.svg?branch=master)](https://www.travis-ci.com/DBCG/cql-evaluator) [![project chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://chat.fhir.org/#narrow/stream/179220-cql)

This project provides a Java-based [CQL](https://cql.hl7.org/) evaluation environment and related components.

The front-end of the evaluator is the [cql-to-elm translator](https://github.com/cqframework/clinical_quality_language/tree/master/Src/java) (which provides translation of CQL to [ELM](https://cql.hl7.org/elm.html)) and the backend is the [cql-engine](https://github.com/DBCG/cql_engine) (which provides ELM execution). The evaluator glues those two components together and provides DataProvider and TerminologyProvider implementations that allow execution of CQL against a filesystem, FHIR server, and in-process FHIR resources.

## Prerequisites

[Java 8+](https://adoptopenjdk.net/)

[Maven 3.5+](https://maven.apache.org/index.html)

## Usage

The cql-evaluator project includes a cli tool that allows execution of CQL from the command line. You can build this tool with:

```bash
mvn package
```

Then you can run the cli with:

```bash
java -jar ./evaluator.cli/target/evaluator.cli-1.4.3-SNAPSHOT-shaded.jar --help
```

That will list the command line options. There is some CQL content available in the [Connectathon IG](https://github.com/DBCG/connectathon) repository. If you have that repository checked out as a sibling repo to this one, you can execute a sample Measure test case by running:

```bash
java -jar ./evaluator.cli/target/evaluator.cli-1.4.3-SNAPSHOT-shaded.jar \
--lu ../connectathon/fhir4/input/pagecontent/cql \
--ln EXM104_FHIR4 \
-m FHIR=../connectathon/fhir4/input/tests/EXM104_FHIR4-8.1.000 \
-t ../connectathon/fhir4/input/vocabulary/valueset \
-c Patient=numer-EXM104-FHIR4
-fv R4
```

## Repository Structure

```bash
.
|-- evaluator            # core cql-evaluator classes, code to glue the engine/translator together
|   |-- src
|   |   |-- main
|   |   `-- test
|-- evaluator.builder    # factories for constructing a cql-evaluator
|   |-- src
|   |   |-- main
|   |   `-- test
|-- evaluator.cli        # cli tool for running the cql-evaluator
|   |-- src
|   |   |-- main
|   |   `-- test
|-- evaluator.expression # functionality for evaluating CQL snippets
|   |-- src
|   |   |-- main
|   |   `-- test
|-- evaluator.fhir       # FHIR Resource adapters and operations
|   |-- src
|   |   |-- main
|   |   `-- test
|-- evaluator.library    # functionality for evaluating CQL/FHIR Libraries
|   |-- src
|   |   |-- main
|   |   `-- test
`-- evaluator.measure    # functionality for evaluating FHIR Measures
    `-- src
        |-- main
        `-- test
```

## Getting Help

Bugs and feature requests can be filed with [Github Issues](https://github.com/DBCG/cql-evaluator/issues).

The implementers are active on the official FHIR [Zulip chat for CQL](https://chat.fhir.org/#narrow/stream/179220-cql).

Inquires for commercial support can be directed to [info@alphora.com](info@alphora.com).

## Related Projects

[Clinical Quality Language](https://github.com/cqframework/clinical_quality_language) - Tooling in support of the CQL specification, including the CQL verifier/translator used in this project.

[CQL Engine](https://github.com/DBCG/cql_engine) - The open source Java-based ELM evaluation engine used in this project.

[CQL Support for Atom](https://atom.io/packages/language-cql) - Open source CQL IDE with syntax highlighting, linting, and local CQL evaluation provided by this project.

[CQF Ruler](https://github.com/DBCG/cqf-ruler) - Integrates this project into the HAPI FHIR server, providing CQL Library evaluation among other functionality.

## License

Copyright 2019+ Dynamic Content Group, LLC (dba Alphora)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
