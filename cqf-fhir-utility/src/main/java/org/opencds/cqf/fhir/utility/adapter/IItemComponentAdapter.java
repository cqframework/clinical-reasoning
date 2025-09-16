package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;

public interface IItemComponentAdapter extends IAdapter<IBase> {

    String getLinkId();

    boolean hasDefinition();

    String getDefinition();

    boolean hasItem();

    List<? extends IItemComponentAdapter> getItem();

    void setItem(List<? extends IItemComponentAdapter> item);

    void addItem(IItemComponentAdapter item);

    void addItems(List<IItemComponentAdapter> items);
}
