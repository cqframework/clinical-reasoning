package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirContext;
import java.util.Date;
import org.hl7.fhir.r4.model.Period;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IPeriodAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class PeriodAdapter implements IPeriodAdapter {

    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final ModelResolver modelResolver =
            FhirModelResolverCache.resolverForVersion(fhirContext.getVersion().getVersion());
    private final Period period = new Period();

    @Override
    public Period get() {
        return period;
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    @Override
    public Date getStart() {
        return period.getStart();
    }

    @Override
    public Date getEnd() {
        return period.getEnd();
    }

    @Override
    public IPeriodAdapter setStart(Date start) {
        period.setStart(start);
        return this;
    }

    @Override
    public IPeriodAdapter setEnd(Date end) {
        period.setEnd(end);
        return this;
    }
}
