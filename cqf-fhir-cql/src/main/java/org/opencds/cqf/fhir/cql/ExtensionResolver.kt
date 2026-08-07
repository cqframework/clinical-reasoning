package org.opencds.cqf.fhir.cql

import java.util.*
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.CqfExpression

/** This class is used to resolve any CQFExpression extensions that exist on an extension. */
class ExtensionResolver(
    private val subjectId: IIdType,
    private val parameters: IBaseParameters?,
    private val bundle: IBaseBundle?,
    private val libraryEngine: LibraryEngine,
) {
    fun <E : IBaseExtension<*, *>?> resolveExtensions(
        resource: IBase?,
        extensions: MutableList<E?>,
        referencedLibraries: MutableMap<String?, String?>?,
    ) {
        for (extension in extensions) {
            val nestedExtensions = extension!!.getExtension()
            if (nestedExtensions != null && !nestedExtensions.isEmpty()) {
                @Suppress("UNCHECKED_CAST")
                resolveExtensions(
                    resource,
                    nestedExtensions as MutableList<IBaseExtension<*, *>?>,
                    referencedLibraries,
                )
            }
            val value = extension.value
            if (value is IBaseHasExtensions) {
                val valueExtensions = value.extension
                if (valueExtensions != null) {
                    val expressionExtensions: Optional<out IBaseExtension<*, *>?> =
                        valueExtensions
                            .stream()
                            .filter { e -> e!!.url != null && e.url == Constants.CQF_EXPRESSION }
                            .findFirst()
                    if (expressionExtensions.isPresent) {
                        val result =
                            getExpressionResult(
                                expressionExtensions.get(),
                                referencedLibraries,
                                resource,
                            )
                        if (result != null) {
                            extension.value = result
                        }
                    }
                }
            }
        }
    }

    protected fun <E : IBaseExtension<*, *>?> getExpressionResult(
        expressionExtension: E?,
        referencedLibraries: MutableMap<String?, String?>?,
        resource: IBase?,
    ): IBaseDatatype? {
        val result =
            libraryEngine.resolveExpression(
                subjectId.idPart,
                CqfExpression.of(expressionExtension, referencedLibraries),
                parameters,
                null,
                bundle,
                resource,
                null,
            )

        return if (!result.isNullOrEmpty()) result[0] as IBaseDatatype? else null
    }
}
