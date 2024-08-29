package org.opencds.cqf.fhir.cr.inputparameters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;

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
            List<IBase> context,
            List<IBaseExtension<?, ?>> launchContext) {
        checkNotNull(repository, "expected non-null value for repository");
        var fhirVersion = repository.fhirContext().getVersion().getVersion();
        switch (fhirVersion) {
            case DSTU3:
                return (T) new org.opencds.cqf.fhir.cr.inputparameters.dstu3.InputParameterResolver(
                        repository,
                        subjectId,
                        encounterId,
                        practitionerId,
                        parameters,
                        useServerData,
                        data,
                        context,
                        launchContext);
            case R4:
                return (T) new org.opencds.cqf.fhir.cr.inputparameters.r4.InputParameterResolver(
                        repository,
                        subjectId,
                        encounterId,
                        practitionerId,
                        parameters,
                        useServerData,
                        data,
                        context,
                        launchContext);
            case R5:
                return (T) new org.opencds.cqf.fhir.cr.inputparameters.r5.InputParameterResolver(
                        repository,
                        subjectId,
                        encounterId,
                        practitionerId,
                        parameters,
                        useServerData,
                        data,
                        context,
                        launchContext);
            default:
                return null;
        }
    }
}
