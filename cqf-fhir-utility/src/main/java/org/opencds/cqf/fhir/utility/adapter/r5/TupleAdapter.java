package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.LinkedHashMap;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.Tuple;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.ITupleAdapter;

public class TupleAdapter extends BaseAdapter implements ITupleAdapter {

    public TupleAdapter(IBase tuple) {
        super(FhirVersionEnum.R5, tuple);
        if (!(tuple instanceof Tuple)) {
            throw new IllegalArgumentException("object passed as tuple argument is not a Tuple data type");
        }
    }

    @Override
    public Tuple get() {
        return (Tuple) element;
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var properties = new LinkedHashMap<String, Object>();
        get().children().forEach(c -> properties.put(c.getName(), c.getValues()));
        return properties;
    }
}
