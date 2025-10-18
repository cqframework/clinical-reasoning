package org.opencds.cqf.fhir.cql;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlParameterDefinition;
import org.opencds.cqf.fhir.utility.fhirpath.FhirPathCache;
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
            String name,
            String version,
            String expression,
            Map<String, String> libraries,
            List<CqlParameterDefinition> parameters) {
        logger.debug("Constructing expression for local evaluation");
        return constructCqlLibrary(
                name, version, Set.of("define \"return\":%n  %s".formatted(expression)), libraries, parameters);
    }

    public String constructCqlLibrary(
            String name,
            String version,
            Set<String> expressions,
            Map<String, String> libraries,
            List<CqlParameterDefinition> parameters) {

        StringBuilder sb = new StringBuilder();

        constructHeader(sb, name, version);
        constructUsings(sb);
        constructIncludes(sb, libraries);
        constructParameters(sb, parameters);
        constructContext(sb, null);
        for (var expression : expressions) {
            sb.append("%s%n%n".formatted(expression));
        }

        String cql = sb.toString();

        logger.debug(cql);
        return cql;
    }

    private String getFhirVersionString(FhirVersionEnum fhirVersion) {
        // The version of the DSTU3 enum is 3.0.2 which the CQL Engine does not support.
        return fhirVersion == FhirVersionEnum.DSTU3 ? "3.0.1" : fhirVersion.getFhirVersionString();
    }

    private void constructIncludes(StringBuilder sb, Map<String, String> libraries) {
        sb.append("include FHIRHelpers version '%s' called FHIRHelpers%n"
                .formatted(getFhirVersionString(fhirContext.getVersion().getVersion())));

        if (libraries != null) {
            for (var library : libraries.entrySet()) {
                var vi = VersionedIdentifiers.forUrl(library.getValue());
                sb.append("include \"%s\"".formatted(vi.getId()));
                if (vi.getVersion() != null) {
                    sb.append(" version '%s'".formatted(vi.getVersion()));
                }
                sb.append(" called \"%s\"".formatted(library.getKey()));
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
                    .append("%n".formatted());
        }
        sb.append("%n".formatted());
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
        sb.append("using FHIR version '%s'%n%n"
                .formatted(getFhirVersionString(fhirContext.getVersion().getVersion())));
    }

    private void constructHeader(StringBuilder sb, String name, String version) {
        sb.append("library %s version '%s'%n%n".formatted(name, version));
    }

    private void constructContext(StringBuilder sb, String contextType) {
        sb.append("context %s%n%n"
                .formatted(StringUtils.isBlank(contextType) ? "Patient" : contextType)
                .formatted());
    }
}
