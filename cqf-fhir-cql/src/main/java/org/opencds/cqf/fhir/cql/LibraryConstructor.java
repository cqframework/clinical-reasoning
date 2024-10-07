package org.opencds.cqf.fhir.cql;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
        return constructCqlLibrary(
                "expression",
                "1.0.0",
                Arrays.asList(String.format("%ndefine \"return\":%n       %s", expression)),
                libraries,
                parameters);
    }

    public String constructCqlLibrary(
            String name,
            String version,
            List<String> expressions,
            List<Pair<String, String>> libraries,
            List<CqlParameterDefinition> parameters) {

        StringBuilder sb = new StringBuilder();

        constructHeader(sb, name, version);
        constructUsings(sb);
        constructIncludes(sb, libraries);
        constructParameters(sb, parameters);
        constructContext(sb, null);
        for (var expression : expressions) {
            sb.append(String.format("%s%n%n", expression));
        }

        String cql = sb.toString();

        logger.debug(cql);
        return cql;
    }

    private String getFhirVersionString(FhirVersionEnum fhirVersion) {
        // The version of the DSTU3 enum is 3.0.2 which the CQL Engine does not support.
        return fhirVersion == FhirVersionEnum.DSTU3 ? "3.0.1" : fhirVersion.getFhirVersionString();
    }

    private void constructIncludes(StringBuilder sb, List<Pair<String, String>> libraries) {
        sb.append(String.format(
                "include FHIRHelpers version '%s' called FHIRHelpers%n",
                getFhirVersionString(fhirContext.getVersion().getVersion())));

        if (libraries != null) {
            for (Pair<String, String> library : libraries) {
                var vi = VersionedIdentifiers.forUrl(library.getLeft());
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
        sb.append("\n");
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
                "using FHIR version '%s'%n%n",
                getFhirVersionString(fhirContext.getVersion().getVersion())));
    }

    private void constructHeader(StringBuilder sb, String name, String version) {
        sb.append(String.format("library %s version '%s'%n%n", name, version));
    }

    private void constructContext(StringBuilder sb, String contextType) {
        sb.append(String.format(
                String.format("context %s%n%n", StringUtils.isBlank(contextType) ? "Patient" : contextType)));
    }
}
