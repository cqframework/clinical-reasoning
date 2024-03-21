package org.opencds.cqf.fhir.cql;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlParameterDefinition;
import org.opencds.cqf.fhir.utility.FhirPathCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryConstructor {

    private static final Logger logger = LoggerFactory.getLogger(LibraryConstructor.class);

    protected FhirContext fhirContext;
    protected IFhirPath fhirPath;

    public LibraryConstructor(FhirContext fhirContext) {

        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    }

    public String constructCqlLibrary(
            String expression, List<Pair<String, String>> libraries, List<CqlParameterDefinition> parameters) {
        logger.debug("Constructing expression for local evaluation");

        StringBuilder sb = new StringBuilder();

        constructHeader(sb);
        constructUsings(sb);
        constructIncludes(sb, libraries);
        constructParameters(sb, parameters);
        constructExpression(sb, expression);

        String cql = sb.toString();

        logger.debug(cql);
        return cql;
    }

    private void constructExpression(StringBuilder sb, String expression) {
        sb.append(String.format("%ndefine \"return\":%n       %s", expression));
    }

    private void constructIncludes(StringBuilder sb, List<Pair<String, String>> libraries) {
        sb.append(String.format(
                "include FHIRHelpers version '%s' called FHIRHelpers%n",
                fhirContext.getVersion().getVersion().getFhirVersionString()));

        if (libraries != null) {
            for (Pair<String, String> library : libraries) {
                VersionedIdentifier vi = getVersionedIdentifier(library.getLeft());
                sb.append(String.format("include \"%s\"", vi.getId()));
                if (vi.getVersion() != null) {
                    sb.append(String.format(" version '%s'", vi.getVersion()));
                }
                if (library.getRight() != null) {
                    sb.append(String.format(" called \"%s\"", library.getRight()));
                }
                sb.append("\n");
            }
        }
    }

    private void constructParameters(StringBuilder sb, List<CqlParameterDefinition> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        for (CqlParameterDefinition cpd : parameters) {
            sb.append("parameter \"")
                    .append(cpd.getName())
                    .append("\" ")
                    .append(this.getTypeDeclaration(cpd.getType(), cpd.getIsList()))
                    .append(String.format("%n"));
        }
    }

    private String getTypeDeclaration(String type, Boolean isList) {
        // TODO: Handle "FHIR" and "System" prefixes
        // Should probably mark system types in the CqlParameterDefinition?
        if (Boolean.TRUE.equals(isList)) {
            return "List<" + type + ">";
        } else {
            return type;
        }
    }

    private void constructUsings(StringBuilder sb) {
        sb.append(String.format(
                "using FHIR version '%s'%n",
                fhirContext.getVersion().getVersion().getFhirVersionString()));
    }

    private void constructHeader(StringBuilder sb) {
        sb.append(String.format("library expression version '1.0.0'%n%n"));
    }

    protected VersionedIdentifier getVersionedIdentifier(String url) {
        if (!url.contains("/Library/")) {
            throw new IllegalArgumentException(
                    "Invalid resource type for determining library version identifier: Library");
        }
        String[] urlSplit = url.split("/Library/");
        if (urlSplit.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid url, Library.url SHALL be <CQL namespace url>/Library/<CQL library name>");
        }

        // TODO: Use the namespace manager here to do the mapping?
        // String cqlNamespaceUrl = urlSplit[0];

        String cqlName = urlSplit[1];
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        if (cqlName.contains("|")) {
            String[] nameVersion = cqlName.split("\\|");
            String name = nameVersion[0];
            String version = nameVersion[1];
            versionedIdentifier.setId(name);
            versionedIdentifier.setVersion(version);
        } else {
            versionedIdentifier.setId(cqlName);
        }
        return versionedIdentifier;
    }
}
