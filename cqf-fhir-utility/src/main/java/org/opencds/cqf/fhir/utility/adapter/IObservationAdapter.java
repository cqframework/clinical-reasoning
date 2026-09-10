package org.opencds.cqf.fhir.utility.adapter;

import static org.opencds.cqf.fhir.utility.VersionUtilities.dateTimeTypeForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.instantTypeForVersion;

import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public interface IObservationAdapter extends IResourceAdapter {
    default IObservationAdapter setBasedOn(List<IBaseReference> basedOn) {
        setValue("basedOn", basedOn);
        return this;
    }

    default IObservationAdapter setPartOf(List<IBaseReference> partOf) {
        setValue("partOf", partOf);
        return this;
    }

    default IObservationAdapter setDerivedFrom(List<IBaseReference> derivedFrom) {
        setValue("derivedFrom", derivedFrom);
        return this;
    }

    default IObservationAdapter setStatus(String status) {
        setValue("status", status);
        return this;
    }

    default String getStatus() {
        return resolvePathString("status");
    }

    default IObservationAdapter setCode(ICompositeType codeableConcept) {
        setValue("code", codeableConcept);
        return this;
    }

    default ICodeableConceptAdapter getCode() {
        var code = resolvePath("code", IBase.class);
        return code == null ? null : getAdapterFactory().createCodeableConcept(code);
    }

    default IObservationAdapter setCategory(List<ICompositeType> category) {
        setValue("category", category);
        return this;
    }

    default IObservationAdapter setSubject(IBaseReference subject) {
        setValue("subject", subject);
        return this;
    }

    default IObservationAdapter setEncounter(IBaseReference encounter) {
        setValue("encounter", encounter);
        return this;
    }

    default IObservationAdapter setEffective(String effective) {
        return setEffective(dateTimeTypeForVersion(fhirVersion(), effective));
    }

    default IObservationAdapter setEffective(IBaseDatatype effective) {
        if (effective instanceof IPrimitiveType<?> primitive && primitive.getValue() instanceof Date) {
            setValue("effectiveDateTime", primitive);
        }
        return this;
    }

    default IObservationAdapter setEffectivePeriod(ICompositeType period) {
        setValue(get(), "effectivePeriod", period);
        return this;
    }

    default IObservationAdapter setIssued(String issued) {
        return setIssued(instantTypeForVersion(fhirVersion(), issued));
    }

    default IObservationAdapter setIssued(IBaseDatatype issued) {
        if (issued instanceof IPrimitiveType<?> primitive
                && primitive.fhirType().equals("instant")
                && primitive.getValue() instanceof Date) {
            setValue("issued", primitive);
        }
        return this;
    }

    default IObservationAdapter setPerformer(List<IBaseReference> performer) {
        setValue("performer", performer);
        return this;
    }

    default IObservationAdapter setValue(IBaseDatatype value) {
        setValue("value", value);
        return this;
    }
}
