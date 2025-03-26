package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.ICompositeType;
import java.util.Date;

public interface IPeriodAdapter extends IAdapter<ICompositeType> {

    Date getStart();

    Date getEnd();

    IPeriodAdapter setStart(Date start);

    IPeriodAdapter setEnd(Date start);
}
