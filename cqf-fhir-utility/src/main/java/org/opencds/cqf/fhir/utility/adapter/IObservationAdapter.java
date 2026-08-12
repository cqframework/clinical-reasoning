package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.ICompositeType;

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

    IObservationAdapter setEffective(String effective);

    IObservationAdapter setEffective(IBaseDatatype effective);

    IObservationAdapter setIssued(String effective);

    IObservationAdapter setIssued(IBaseDatatype effective);

    default IObservationAdapter setPerformer(List<IBaseReference> performer) {
        setValue("performer", performer);
        return this;
    }

    default IObservationAdapter setValue(IBaseDatatype value) {
        setValue("value", value);
        return this;
    }
}
