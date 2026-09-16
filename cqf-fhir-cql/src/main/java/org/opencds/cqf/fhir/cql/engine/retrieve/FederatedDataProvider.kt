package org.opencds.cqf.fhir.cql.engine.retrieve

import com.google.common.collect.Iterables
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.model.ModelResolver
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.runtime.Interval
import org.opencds.cqf.cql.engine.runtime.Value

class FederatedDataProvider(
    modelResolver: ModelResolver?,
    protected val retrieveProviders: MutableList<RetrieveProvider>,
) : CompositeDataProvider(modelResolver, null) {
    /**
     * @param context - resource type
     * @param contextPath -
     * @param contextValue - id
     * @param dataType - resource type
     * @param templateId
     * @param codePath
     * @param codes
     * @param valueSet
     * @param datePath - the SP name to use
     * @param dateLowPath - high path (period.start)
     * @param dateHighPath - low path (period.end)
     * @param dateRange - date range to search within
     * @return
     */
    override fun retrieve(
        context: String?,
        contextPath: String?,
        contextValue: String?,
        dataType: String,
        templateId: String?,
        codePath: String?,
        codes: Iterable<Code>?,
        valueSet: String?,
        datePath: String?,
        dateLowPath: String?,
        dateHighPath: String?,
        dateRange: Interval?,
    ): Iterable<Value?>? {
        val results = mutableListOf<Iterable<Value?>?>()
        for (provider in this.retrieveProviders) {
            results.add(
                provider.retrieve(
                    context,
                    contextPath,
                    contextValue,
                    dataType,
                    templateId,
                    codePath,
                    codes,
                    valueSet,
                    datePath,
                    dateLowPath,
                    dateHighPath,
                    dateRange,
                )
            )
        }

        return Iterables.concat(results)
    }
}
