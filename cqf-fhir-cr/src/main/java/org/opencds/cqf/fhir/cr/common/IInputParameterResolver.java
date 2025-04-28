package org.opencds.cqf.fhir.cr.common;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;

/**
 * This interface exposes functionality to resolve parameters passed into an operation as CQL Resource parameters
 * for evaluation. e.g. "%subject"
 */
public interface IInputParameterResolver {
    public IBaseParameters getParameters();

    public <T extends ICompositeType> IBaseParameters resolveInputParameters(List<T> dataRequirement);

    public static <T extends IInputParameterResolver> T createResolver(
            IRepository repository,
            IIdType subjectId,
            IIdType encounterId,
            IIdType practitionerId,
            IBaseParameters parameters,
            IBaseBundle data) {
        return createResolver(repository, subjectId, encounterId, practitionerId, parameters, data, null, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IInputParameterResolver> T createResolver(
            IRepository repository,
            IIdType subjectId,
            IIdType encounterId,
            IIdType practitionerId,
            IBaseParameters parameters,
            IBaseBundle data,
            List<IParametersParameterComponentAdapter> context,
            List<IBaseExtension<?, ?>> launchContext) {
        checkNotNull(repository, "expected non-null value for repository");
        return (T) new InputParameterResolver(
                repository, subjectId, encounterId, practitionerId, parameters, data, context, launchContext);
    }
}
