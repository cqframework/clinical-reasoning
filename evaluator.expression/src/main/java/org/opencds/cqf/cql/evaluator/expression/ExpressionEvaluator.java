package org.opencds.cqf.cql.evaluator.expression;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DataRequirement;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.CqlParameterDefinition;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;

@SuppressWarnings("unused")
@Named
public class ExpressionEvaluator {

    private static Logger logger = LoggerFactory.getLogger(ExpressionEvaluator.class);

    protected FhirContext fhirContext;
    protected CqlFhirParametersConverter cqlFhirParametersConverter;
    protected LibraryContentProviderFactory libraryContentProviderFactory;
    protected DataProviderFactory dataProviderFactory;
    protected TerminologyProviderFactory terminologyProviderFactory;
    protected EndpointConverter endpointConverter;
    protected CqlEvaluatorBuilder cqlEvaluatorBuilder;
    protected IFhirPath fhirPath;
    protected LibraryProcessor libraryProcessor;
    protected ModelResolverFactory fhirModelResolverFactory;
    protected Supplier<CqlEvaluatorBuilder> cqlEvaluatorSupplier;

    @Inject
    public ExpressionEvaluator(FhirContext fhirContext, CqlFhirParametersConverter cqlFhirParametersConverter,
            LibraryContentProviderFactory libraryContentProviderFactory, DataProviderFactory dataProviderFactory,
            TerminologyProviderFactory terminologyProviderFactory, EndpointConverter endpointConverter,
            ModelResolverFactory fhirModelResolverFactory,
            Supplier<CqlEvaluatorBuilder> cqlEvaluatorBuilderSupplier) {

        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.fhirPath = fhirContext.newFhirPath();
        this.cqlFhirParametersConverter = requireNonNull(cqlFhirParametersConverter, "cqlFhirParametersConverter");
        this.libraryContentProviderFactory = requireNonNull(libraryContentProviderFactory,
                "libraryLoaderFactory can not be null");
        this.dataProviderFactory = requireNonNull(dataProviderFactory, "dataProviderFactory can not be null");
        this.terminologyProviderFactory = requireNonNull(terminologyProviderFactory,
                "terminologyProviderFactory can not be null");

        this.cqlEvaluatorSupplier = requireNonNull(cqlEvaluatorBuilderSupplier,
                "cqlEvaluatorBuilderSupplier can not be null");
        this.endpointConverter = requireNonNull(endpointConverter, "endpointConverter can not be null");

        this.fhirModelResolverFactory = requireNonNull(fhirModelResolverFactory, "fhirModelResolverFactory can not be null");

        if (!this.fhirModelResolverFactory.getModelUri().equals(Constants.FHIR_MODEL_URI)) {
            throw new IllegalArgumentException("fhirModelResolverFactory was a FHIR modelResolverFactory");
        }
    }

    /**
     * Evaluates a CQL expression and returns the results as a Parameters resource.
     * 
     * @param expression          Expression to be evaluated. Note that this is an
     *                            expression of CQL, not the text of a library with
     *                            definition statements.
     * @param parameters          Any input parameters for the expression.
     *                            Parameters defined in this input will be made
     *                            available by name to the CQL expression. Parameter
     *                            types are mapped to CQL as specified in the Using
     *                            CQL section of the cpg implementation guide. If a
     *                            parameter appears more than once in the input
     *                            Parameters resource, it is represented with a List
     *                            in the input CQL. If a parameter has parts, it is
     *                            represented as a Tuple in the input CQL.
     * @return IBaseParameters The result of evaluating the given expression,
     *         returned as a FHIR type, either a resource, or a FHIR-defined type
     *         corresponding to the CQL return type, as defined in the Using CQL
     *         section of the cpg implementation guide. If the result is a List of
     *         resources, the result will be a Bundle. If the result is a CQL
     *         system-defined or FHIR-defined type, the result is returned as a
     *         Parameters resource
     */
    public IBaseParameters evaluate(String expression, IBaseParameters parameters) {
        return this.evaluate(expression, parameters, null, null, null, null, null, null, null, null);
    }

    /**
     * Evaluates a CQL expression and returns the results as a Parameters resource.
     * 
     * @param expression          Expression to be evaluated. Note that this is an
     *                            expression of CQL, not the text of a library with
     *                            definition statements.
     * @param parameters          Any input parameters for the expression.
     *                            Parameters defined in this input will be made
     *                            available by name to the CQL expression. Parameter
     *                            types are mapped to CQL as specified in the Using
     *                            CQL section of the cpg implementation guide. If a
     *                            parameter appears more than once in the input
     *                            Parameters resource, it is represented with a List
     *                            in the input CQL. If a parameter has parts, it is
     *                            represented as a Tuple in the input CQL.
     * @param subject             Subject for which the expression will be
     *                            evaluated. This corresponds to the context in
     *                            which the expression will be evaluated and is
     *                            represented as a relative FHIR id (e.g.
     *                            Patient/123), which establishes both the context
     *                            and context value for the evaluation
     * @param libraries           The list of libraries to be included in the evaluation context.
     * @param useServerData       Whether to use data from the server performing the
     *                            evaluation. If this parameter is true (the
     *                            default), then the operation will use data first
     *                            from any bundles provided as parameters (through
     *                            the data and prefetch parameters), second data
     *                            from the server performing the operation, and
     *                            third, data from the dataEndpoint parameter (if
     *                            provided). If this parameter is false, the
     *                            operation will use data first from the bundles
     *                            provided in the data or prefetch parameters, and
     *                            second from the dataEndpoint parameter (if
     *                            provided).
     * @param bundle              Data to be made available to the library
     *                            evaluation. This parameter is exclusive with the
     *                            prefetchData parameter (i.e. either provide all
     *                            data as a single bundle, or provide data using
     *                            multiple bundles with prefetch descriptions).
     * @param prefetchData        Data to be made available to the library
     *                            evaluation, organized as prefetch response
     *                            bundles. Each prefetchData parameter specifies
     *                            either the name of the prefetchKey it is
     *                            satisfying, a DataRequirement describing the
     *                            prefetch, or both.
     * @param dataEndpoint        An endpoint to use to access data referenced by
     *                            retrieve operations in the library. If provided,
     *                            this endpoint is used after the data or
     *                            prefetchData bundles, and the server, if the
     *                            useServerData parameter is true.
     * @param contentEndpoint     An endpoint to use to access content (i.e.
     *                            libraries) referenced by the library. If no
     *                            content endpoint is supplied, the evaluation will
     *                            attempt to retrieve content from the server on
     *                            which the operation is being performed.
     * @param terminologyEndpoint An endpoint to use to access terminology (i.e.
     *                            valuesets, codesystems, and membership testing)
     *                            referenced by the library. If no terminology
     *                            endpoint is supplied, the evaluation will attempt
     *                            to use the server on which the operation is being
     *                            performed as the terminology server.
     * @return IBaseParameters The result of evaluating the given expression,
     *         returned as a FHIR type, either a resource, or a FHIR-defined type
     *         corresponding to the CQL return type, as defined in the Using CQL
     *         section of the cpg implementation guide. If the result is a List of
     *         resources, the result will be a Bundle. If the result is a CQL
     *         system-defined or FHIR-defined type, the result is returned as a
     *         Parameters resource
     */
    // Canonical is not a canonical data type.
    public IBaseParameters evaluate(String expression, IBaseParameters parameters, String subject,
            List<Pair<String, String>> libraries, Boolean useServerData, IBaseBundle bundle,
            List<Triple<String, DataRequirement, IBaseBundle>> prefetchData, IBaseResource dataEndpoint,
            IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {

        String cql = constructCqlLibrary(expression, libraries, parameters);

        LibraryContentProvider contentProvider = new InMemoryLibraryContentProvider(Lists.newArrayList(cql));
        CqlEvaluatorBuilder builder = this.cqlEvaluatorSupplier.get();
        builder.withLibraryContentProvider(contentProvider);

        Set<String> expressions = new HashSet<String>();
        expressions.add("return");

        libraryProcessor = new LibraryProcessor(fhirContext, cqlFhirParametersConverter, libraryContentProviderFactory,
                dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, () -> builder);

        return libraryProcessor.evaluate(new VersionedIdentifier().withId("expression").withVersion("1.0.0"), subject,
                parameters, contentEndpoint, terminologyEndpoint, dataEndpoint, bundle, expressions);
    }

    private String constructCqlLibrary(String expression, List<Pair<String, String>> libraries,
            IBaseParameters parameters) {
        String cql = null;
        logger.debug("Constructing expression for local evaluation");

        StringBuilder sb = new StringBuilder();

        constructHeader(sb);
        constructUsings(sb, parameters);
        constructIncludes(sb, parameters, libraries);
        constructParameters(sb, parameters);
        constructExpression(sb, expression);

        cql = sb.toString();

        logger.debug(cql);
        return cql;
    }

    private void constructExpression(StringBuilder sb, String expression) {
        sb.append(String.format("\ndefine \"return\":\n       %s", expression));
    }

    private void constructIncludes(StringBuilder sb, IBaseParameters parameters, List<Pair<String, String>> libraries) {
        String fhirVersion = getFhirVersion(parameters);
        if (fhirVersion != null) {
            sb.append(String.format("include FHIRHelpers version \'%s\' called FHIRHelpers\n", fhirVersion));
        }

        if (libraries != null) {
            for (Pair<String, String> library : libraries) {
                VersionedIdentifier vi = getVersionedIdentifer(library.getLeft());
                sb.append(String.format("include \"%s\"", vi.getId()));
                if (vi.getVersion() != null) {
                    sb.append(String.format(" version \'%s\'", vi.getVersion()));
                }
                if (library.getRight() != null) {
                    sb.append(String.format(" called \"%s\"", library.getRight()));
                }
                sb.append("\n");
            }
        }
    }

    private void constructParameters(StringBuilder sb, IBaseParameters parameters) {
        if (parameters == null) {
            return;
        }

        // TODO: Can we consolidate this logic in the Library evaluator somehow? Then we
        // don't have to do this conversion twice
        List<CqlParameterDefinition> cqlParameters = this.cqlFhirParametersConverter.toCqlParameterDefinitions(parameters);
        if (cqlParameters.size() == 0) {
            return;
        }

        for (CqlParameterDefinition cpd : cqlParameters) {
            sb.append("parameter \"" + cpd.getName() + "\" " + this.getTypeDeclaration(cpd.getType(), cpd.getIsList()) + "\n");
        }
    }

    private String getTypeDeclaration(String type, Boolean isList) {
        // TODO: Handle "FHIR" and "System" prefixes
        // Should probably mark system types in the CqlParameterDefinition?
        if (isList) {
            return "List<" + type + ">";
        }
        else {
            return type;
        }
    }

    private void constructUsings(StringBuilder sb, IBaseParameters parameters) {
        String fhirVersion = getFhirVersion(parameters);
        if (fhirVersion != null) {
            sb.append(String.format("using FHIR version \'%s\'\n", fhirVersion));
        }
    }

    private void constructHeader(StringBuilder sb) {
        sb.append("library expression version \'1.0.0\'\n\n");
    }

    private String getFhirVersion(IBaseParameters parameters) {
        if (parameters == null) {
            return null;
        }

        switch (parameters.getStructureFhirVersionEnum()) {
            case DSTU3:
                return "3.0.1";
            case R4:
                return "4.0.1";
            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            case R5:
            default:
                throw new IllegalArgumentException(String.format("Unsupported version of FHIR: %s",
                        parameters.getStructureFhirVersionEnum().getFhirVersionString()));
        }
    }

    protected VersionedIdentifier getVersionedIdentifer(String url) {
        if (!url.contains("/Library/")) {
            throw new IllegalArgumentException(
                    "Invalid resource type for determining library version identifier: Library");
        }
        String[] urlSplit = url.split("/Library/");
        if (urlSplit.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid url, Library.url SHALL be <CQL namespace url>/Library/<CQL library name>");
        }
        String cqlNamespaceUrl = urlSplit[0];

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
