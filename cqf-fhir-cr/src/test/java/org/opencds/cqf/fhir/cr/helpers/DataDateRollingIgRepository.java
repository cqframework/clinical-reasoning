package org.opencds.cqf.fhir.cr.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.util.BundleUtil;
import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * An {@link IgRepository} that applies the CDC opioid-cds {@code dataDateRoller}
 * extension to resources on read/search. See {@link DataDateRollerHelper}.
 *
 * <p>Test-only: lets opioid-cds fixtures with hard-coded anchor dates stay
 * "current" relative to today's wall clock, so that CQL windows like
 * {@code 2 years or less on or before Today()} remain satisfied without
 * yearly fixture date bumps.
 */
public class DataDateRollingIgRepository extends IgRepository {

    private final LocalDate today;

    public DataDateRollingIgRepository(FhirContext fhirContext, Path root, LocalDate today) {
        super(fhirContext, root);
        this.today = today;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {
        T resource = super.read(resourceType, id, headers);
        DataDateRollerHelper.rollIfAnnotated(resource, today, fhirContext());
        return resource;
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Multimap<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        B bundle = super.search(bundleType, resourceType, searchParameters, headers);
        for (var entry : BundleUtil.toListOfResources(fhirContext(), bundle)) {
            DataDateRollerHelper.rollIfAnnotated(entry, today, fhirContext());
        }
        return bundle;
    }
}
