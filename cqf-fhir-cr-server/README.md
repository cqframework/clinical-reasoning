# cqf-fhir-cr-server

A self-contained CQF Clinical Reasoning FHIR server. Wraps HAPI's `RestfulServer` on Spring
Boot's embedded Tomcat, registers the operation providers from `cqf-fhir-cr-hapi`
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
./gradlew :cqf-fhir-cr-server:bootJar

# Run it
java -jar cqf-fhir-cr-server/build/libs/cqf-fhir-cr-server-*.jar

# Smoke test
curl http://localhost:8080/fhir/metadata
```

`./gradlew :cqf-fhir-cr-server:bootRun` works too for live-reload development.

## Docker

Three image variants are available; pick based on what you need:

| Variant | How to build | Notes |
| --- | --- | --- |
| Buildpacks | `./gradlew :cqf-fhir-cr-server:bootBuildImage` | No Dockerfile, OCI image via Spring Boot. |
| Standard Dockerfile | `./gradlew :cqf-fhir-cr-server:dockerBuild` | `Dockerfile`, full JRE base. |
| jlink (slim) | `./gradlew :cqf-fhir-cr-server:dockerBuildJlink` | `Dockerfile.jlink`, custom JRE on distroless (~40% smaller). |

Run any of them with:

```bash
docker run --rm -p 8080:8080 cqf-fhir-cr-server:latest
```

## Published images (GHCR)

Tagged releases (`v*`) automatically publish to GitHub Container Registry via
[`.github/workflows/release-image.yml`](../.github/workflows/release-image.yml).

Image: `ghcr.io/cqframework/clinical-reasoning/cqf-fhir-cr-server`

Tags published per release `vX.Y.Z`:

- `X.Y.Z`         — standard image
- `X.Y.Z-jlink`   — jlink slim image
- `latest`        — moved to the standard image only for stable releases (skipped for `-rc`/`-alpha`/`-beta`)

Pull and run:

```bash
docker pull ghcr.io/cqframework/clinical-reasoning/cqf-fhir-cr-server:latest
docker run --rm -p 8080:8080 \
  ghcr.io/cqframework/clinical-reasoning/cqf-fhir-cr-server:latest
```

To cut a release image, push a `v*` tag:

```bash
git tag v4.7.0
git push origin v4.7.0
```

The workflow builds the `bootJar`, builds the jlink Dockerfile, tags `X.Y.Z` /
`X.Y.Z-jlink` / (conditionally) `latest`, and pushes them to GHCR.
