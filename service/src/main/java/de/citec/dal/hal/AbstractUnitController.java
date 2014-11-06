/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.RSBBindingConnection;
import de.citec.dal.RSBBindingInterface;
import de.citec.dal.data.Location;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.al.HardwareUnit;
import de.citec.dal.service.RSBCommunicationService;
import org.openhab.core.types.Command;
import rsb.RSBException;
import rsb.Scope;
import rsb.patterns.LocalServer;

/**
 *
 * @author mpohling
 * @param <B> Type related Builder
 */
public abstract class AbstractUnitController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> {

    protected final String id;
    protected final String lable;
    private final HardwareUnit relatedHardwareUnit;

    protected RSBBindingInterface rsbBinding = RSBBindingConnection.getInstance();

    public AbstractUnitController(final String id, final String lable, final HardwareUnit relatedHardwareUnit, final MB builder) throws RSBBindingException {
        super(generateScope(id, lable, relatedHardwareUnit), builder);
        this.id = id;
        this.lable = lable;
        this.relatedHardwareUnit = relatedHardwareUnit;
        super.builder.setField(builder.getDescriptorForType().findFieldByName("id"), relatedHardwareUnit.getInstance_id());

        try {
            init();
        } catch (RSBException ex) {
            throw new RSBBindingException("Could not init RSBCommunicationService!", ex);
        }
    }

    public String getId() {
        return id;
    }

    public String getLable() {
        return lable;
    }
    
    public HardwareUnit getRelatedHardwareUnit() {
        return relatedHardwareUnit;
    }

    public String generateHardwareId() { //TODO impl static const
        return relatedHardwareUnit.getId() + "_" + id;
    }

    @Override
    public void registerMethods(LocalServer server) throws RSBException {
    }

    public void postCommand(Command command) throws RSBBindingException {
        rsbBinding.postCommand(generateHardwareId(), command);
    }

    public void sendCommand(Command command) throws RSBBindingException {
        logger.debug("Send command: Setting item ["+generateHardwareId()+"] to ["+command.toString()+"]");
        rsbBinding.sendCommand(generateHardwareId(), command);
    }

    public static Scope generateScope(final String id, final String lable, final HardwareUnit hardware) {
        return hardware.getLocation().getScope().concat(new Scope(Location.COMPONENT_SEPERATOR + id).concat(new Scope(Location.COMPONENT_SEPERATOR + lable)));
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+id+"["+lable+"]]";
    }
}
