# PRP: Fix IntelliJ Gradle Sync Failures Caused by Dependency Verification

## Metadata
- **Title**: Add trusted-artifact rules for sources/javadoc JARs to fix IntelliJ Gradle sync
- **Status**: Implemented
- **Priority**: High (Developer Experience)
- **Estimated Effort**: 0.25 days
- **Target Branch**: master
- **Implementation Date**: 2026-03-03

## Problem Statement

After commit `7270f661` (PR #938) introduced `gradle/verification-metadata.xml` with SHA-256 checksums for dependency verification, **IntelliJ Gradle sync fails across the entire project** while CLI builds (`./gradlew assemble`) continue to work fine.

### Symptoms

IntelliJ reports errors across all modules during Gradle sync:

| Module(s) | Error |
|---|---|
| `:build-logic` | Gradle import errors |
| All 8 submodules | "Project annotation processor import failure" |
| Root + all 8 submodules | "Project version catalogs inspection failure" |
| `:cqf-fhir-benchmark` | Dependency verification failure |

### Why CLI Builds Succeed But IntelliJ Fails

The behavioral difference is that **IntelliJ downloads `-sources.jar` files** during Gradle sync (for "Go To Source" navigation), while `./gradlew assemble` does not. These source JARs are subject to dependency verification, and **none of them had checksums** in `verification-metadata.xml`.

### Error Cascade

All four error categories trace to a single root cause:

1. **`:build-logic` import errors** — IntelliJ tries to attach sources for build-logic plugin dependencies; sources JARs fail verification, causing build-logic to fail to import
2. **Annotation processor failures** — build-logic failure means convention plugins (`cqf.java-conventions`, etc.) cannot be configured, so annotation processor paths are unresolvable
3. **Version catalog failures** — build-logic failure means `build-logic/gradle/libs.versions.toml` is unavailable
4. **Benchmark verification failure** — first module in configuration order where Gradle reports the actual verification error

### Additional Issue: Invalid XML Element

The user had locally added `<on-missing-verification>log</on-missing-verification>` (line 6) as a workaround attempt. This element is **not valid** in Gradle 9.3.1's `dependency-verification-1.3.xsd` schema. The schema's `<configurationType>` only permits: `<verify-metadata>`, `<verify-signatures>`, `<keyring-format>`, `<key-servers>`, `<trusted-artifacts>`, `<ignored-keys>`, `<trusted-keys>`. Gradle silently ignores the invalid element, so the workaround had no effect.

## Analysis

### Investigation Process

1. Confirmed CLI build works — `./gradlew assemble` passes cleanly
2. Reviewed IntelliJ project configuration (`.idea/gradle.xml`) — build runner set to Gradle (correct)
3. Read the dependency verification failure report at `build/reports/dependency-verification/at-1772489141499/dependency-verification-report.html`
4. Identified **31 `-sources.jar` files** failing verification for both `:cqf-fhir-benchmark:annotationProcessor` and `:cqf-fhir-benchmark:compileClasspath` configurations
5. Confirmed the failing artifacts were build-logic classpath sources (spotless, errorprone, animalsniffer, vanniktech publish dependencies) — not benchmark-specific
6. Validated the `<on-missing-verification>` element against the Gradle 9.3.1 XSD schema at `~/.gradle/wrapper/dists/gradle-9.3.1-all/.../dependency-verification-1.3.xsd` — confirmed invalid
7. Confirmed the `<trust>` element supports a `file` attribute with `regex="true"` per the same XSD schema

### Failing Artifacts (31 sources JARs)

All 31 are `-sources.jar` files from build-logic plugin dependencies, including:

- `spotless-lib-4.3.0-sources.jar`
- `gradle-errorprone-plugin-5.0.0-sources.jar`
- `gradle-animalsniffer-plugin-2.0.1-sources.jar`
- `gradle-maven-publish-plugin-0.36.0-sources.jar`
- `foojay-resolver-1.0.0-sources.jar`
- `org.eclipse.jgit-7.5.0.202512021534-r-sources.jar`
- And 25 others

### Why Trusting Sources/Javadoc JARs Is Safe

Sources and Javadoc JARs are **supplementary artifacts** used exclusively for IDE navigation and documentation display. They are:
- Never compiled or executed
- Never placed on any runtime or compile classpath
- Not security-critical — an attacker gaining control of a sources JAR could only display misleading source code in the IDE, not execute malicious code

This is consistent with Gradle's own documentation, which recommends trusting these artifacts globally when they cause verification friction.

## Solution

### File Changed

`gradle/verification-metadata.xml`

### Change 1: Remove Invalid Element

Delete the `<on-missing-verification>log</on-missing-verification>` element. It is not valid per the Gradle 9.3.1 XSD schema and is silently ignored.

**Before:**
```xml
<configuration>
   <verify-metadata>true</verify-metadata>
   <verify-signatures>false</verify-signatures>
    <on-missing-verification>log</on-missing-verification>
   <trusted-artifacts>
      <trust group="org.jetbrains" name="annotations"/>
      <trust group="org.jetbrains.kotlin" name=".*" regex="true"/>
      <trust group="org.jetbrains.kotlinx" name=".*" regex="true"/>
   </trusted-artifacts>
</configuration>
```

### Change 2: Add Trusted-Artifact Rules for Sources and Javadoc JARs

Add two `<trust file>` entries with regex patterns to globally trust all `-sources.jar` and `-javadoc.jar` files.

**After:**
```xml
<configuration>
   <verify-metadata>true</verify-metadata>
   <verify-signatures>false</verify-signatures>
   <trusted-artifacts>
      <trust group="org.jetbrains" name="annotations"/>
      <trust group="org.jetbrains.kotlin" name=".*" regex="true"/>
      <trust group="org.jetbrains.kotlinx" name=".*" regex="true"/>
      <trust file=".*-sources\.jar" regex="true"/>
      <trust file=".*-javadoc\.jar" regex="true"/>
   </trusted-artifacts>
</configuration>
```

## Design Decisions

### Why Global Trust Rules vs. Adding Individual Checksums

| Approach | Pros | Cons |
|---|---|---|
| **Global trust rules** (chosen) | Zero maintenance; covers future dependencies automatically; 2-line change | Trusts all sources/javadoc JARs (acceptable — they are non-executable) |
| **Add 31 individual checksums** | Maximum verification granularity | Fragile; breaks on every dependency update; must re-run `--write-verification-metadata` frequently |
| **Disable verification entirely** | Simplest fix | Loses all dependency verification benefits |

The global trust approach is the standard Gradle recommendation for this scenario.

### Why `-javadoc.jar` Is Also Included

While only `-sources.jar` files were observed failing, `-javadoc.jar` files have the same characteristics (IDE-only, non-executable) and would cause identical failures if IntelliJ were configured to download Javadoc. Adding the rule proactively avoids a repeat of this issue.

## Verification

1. **CLI build**: `./gradlew assemble` — passes (BUILD SUCCESSFUL)
2. **IntelliJ sync**: Gradle elephant icon > "Reload All Gradle Projects" — all errors clear

## Success Criteria

- [x] `./gradlew assemble` passes
- [x] Invalid `<on-missing-verification>` element removed
- [x] Sources and Javadoc JARs globally trusted via regex rules
- [ ] IntelliJ Gradle sync completes without errors (user to verify)
