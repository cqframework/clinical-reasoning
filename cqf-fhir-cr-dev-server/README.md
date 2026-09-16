# cqf-fhir-cr-dev-server

A self-contained CQF Clinical Reasoning FHIR development server. Wraps HAPI's `RestfulServer`
on Spring Boot's embedded Tomcat, registers the operation providers from `cqf-fhir-cr-hapi`
(`$evaluate-measure`, `$apply`, etc.), and bridges plain CRUD to an `IRepository`. The
default build wires an in-memory repository — swap it out for a JPA, REST, or IG-backed one
in `ServerR4Config` when integrating elsewhere.

## Configuration

Spring Boot config under `application.yml`:

```yaml
server:
  port: 8080            # HTTP port

cqf:
  server:
    base-path: /fhir    # servlet mount path
    fhir-version: R4    # R4 (DSTU3 not yet wired)
```

Override on the command line: `--server.port=9090 --cqf.server.base-path=/r4`.

## Run from source

```bash
# Build the fat JAR
./gradlew :cqf-fhir-cr-dev-server:bootJar

# Run it
java -jar cqf-fhir-cr-dev-server/build/libs/cqf-fhir-cr-dev-server-*.jar

# Smoke test
curl http://localhost:8080/fhir/metadata
```

`./gradlew :cqf-fhir-cr-dev-server:bootRun` works too for live-reload development.
