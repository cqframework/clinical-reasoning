package org.opencds.cqf.fhir.cr.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import org.hl7.fhir.instance.model.api.*;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;

public interface IInputParameterResolver {
    public IBaseParameters getParameters();

    public <T extends ICompositeType> IBaseParameters resolveInputParameters(List<T> dataRequirement);

    public static <T extends IInputParameterResolver> T createResolver(
            Repository repository,
            IIdType subjectId,
            IIdType encounterId,
            IIdType practitionerId,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data) {
        return createResolver(
                repository, subjectId, encounterId, practitionerId, parameters, useServerData, data, null, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IInputParameterResolver> T createResolver(
            Repository repository,
            IIdType subjectId,
            IIdType encounterId,
            IIdType practitionerId,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            List<IParametersParameterComponentAdapter> context,
            List<IBaseExtension<?, ?>> launchContext) {
        checkNotNull(repository, "expected non-null value for repository");
        return (T) new InputParameterResolver(
                repository,
                subjectId,
                encounterId,
                practitionerId,
                parameters,
                useServerData,
                data,
                context,
                launchContext);
    }
}
