/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.exception.DALException;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.unit.DeviceInterface;
import de.citec.jul.rsb.RSBCommunicationService;
import de.citec.jul.rsb.RSBInformerInterface;
import de.citec.jul.rsb.ScopeProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import rsb.RSBException;
import rsb.Scope;

/**
 *
 * @author mpohling
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractUnitController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> {

    public final static String TYPE_FILED_ID = "id";
    public final static String TYPE_FILED_LABEL = "label";

    protected final String id;
    protected final String label;
    private final DeviceInterface device;
    private List<Service> serviceList;

    public AbstractUnitController(final String id, final String label, final DeviceInterface device, final MB builder) throws DALException {
        super(generateScope(id, label, device), builder);
        this.id = id; // TODO mpohling: Still necessary?
        this.label = label;
        this.device = device;
        this.serviceList = new ArrayList<>();
        
        setField(TYPE_FILED_ID, generateScope(id, null)); // TODO mpohling: Still necessary?
        setField(TYPE_FILED_LABEL, label);

        try {
            init(RSBInformerInterface.InformerType.Distributed);
        } catch (RSBException ex) {
            throw new DALException("Could not init RSBCommunicationService!", ex);
        }
    }

    public String getId() {
        return id;
    }

    public String getLable() {
        return label;
    }

    public DeviceInterface getDevice() {
        return device;
    }
    
    public Collection<Service> getServices() {
        return Collections.unmodifiableList(serviceList);
    }
    
    public void registerService(final Service service) {
        serviceList.add(service);
    }
    
    public Scope generateScope() {
        return generateScope(id, label, device);
    }

    public static Scope generateScope(final String id, final String label, final DeviceInterface device) {
        return device.getLocation().getScope().concat(new Scope(ScopeProvider.SEPARATOR + id).concat(new Scope(ScopeProvider.SEPARATOR + label)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "[" + label + "]]";
    }
}
