package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;

public class ItemPair
        extends ImmutablePair<IQuestionnaireItemComponentAdapter, IQuestionnaireResponseItemComponentAdapter> {

    public ItemPair(FhirVersionEnum fhirVersion, IItemComponentAdapter item, IBaseBackboneElement responseItem) {
        this(item, (IQuestionnaireResponseItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(fhirVersion, responseItem));
    }

    public ItemPair(FhirVersionEnum fhirVersion, IBaseBackboneElement item, IBaseBackboneElement responseItem) {
        this(
                (IQuestionnaireItemComponentAdapter) IAdapterFactory.createAdapterForBase(fhirVersion, item),
                (IQuestionnaireResponseItemComponentAdapter)
                        IAdapterFactory.createAdapterForBase(fhirVersion, responseItem));
    }

    public <T extends IItemComponentAdapter> ItemPair(T item, T responseItem) {
        super((IQuestionnaireItemComponentAdapter) item, (IQuestionnaireResponseItemComponentAdapter) responseItem);
    }

    public IQuestionnaireItemComponentAdapter getItem() {
        return left;
    }

    public IQuestionnaireResponseItemComponentAdapter getResponseItem() {
        return right;
    }
}
