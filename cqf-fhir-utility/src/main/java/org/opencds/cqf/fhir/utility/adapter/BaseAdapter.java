package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;

public abstract class BaseAdapter extends AdapterBase implements IAdapter<IBase> {
    //    protected final FhirContext fhirContext;
    //    protected final FhirTerser fhirTerser;
    protected final IBase element;
    //    protected final IAdapterFactory adapterFactory;

    protected BaseAdapter(FhirVersionEnum fhirVersion, IBase element) {
        super(FhirContext.forCached(fhirVersion));
        if (element == null) {
            throw new IllegalArgumentException("element can not be null");
        }
        this.element = element;
        //        fhirContext = FhirContext.forCached(fhirVersion);
        //        fhirTerser = new FhirTerser(fhirContext);
        //        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
    }

    public IAdapter<?> setId(String id) {
        setValue(get(), "id", id);
        return this;
    }
    //    public FhirContext fhirContext() {
    //        return fhirContext;
    //    }
    //
    //    public FhirTerser fhirTerser() {
    //        return fhirTerser;
    //    }
    //
    //    public IAdapterFactory getAdapterFactory() {
    //        return adapterFactory;
    //    }

    //    @SuppressWarnings("unchecked")
    //    @Override
    //    public <E extends IBaseExtension<?, ?>> E addExtension() {
    //        if (get() instanceof IBaseHasExtensions baseHasExtensions) {
    //            return (E) baseHasExtensions.addExtension();
    //        }
    //        return null;
    //    }
}
