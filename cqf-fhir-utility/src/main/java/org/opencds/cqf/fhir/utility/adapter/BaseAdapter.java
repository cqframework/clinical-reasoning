package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeChildChoiceDefinition;
import ca.uhn.fhir.context.RuntimeChildPrimitiveEnumerationDatatypeDefinition;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.exception.DataProviderException;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownType;
import org.opencds.cqf.fhir.utility.Ids;
import java.util.List;

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
