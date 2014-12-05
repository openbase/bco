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
import java.util.concurrent.Future;
import rsb.RSBException;
import rsb.Scope;
import rsb.patterns.LocalServer;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand.ExecutionType;

/**
 *
 * @author mpohling
 * @param <B> Type related Builder
 */
public abstract class AbstractUnitController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> {

    protected final String id;
    protected final String label;
    private final HardwareUnit relatedHardwareUnit;

    protected final RSBBindingInterface rsbBinding = RSBBindingConnection.getInstance();

    public AbstractUnitController(final String id, final String label, final HardwareUnit relatedHardwareUnit, final MB builder) throws RSBBindingException {
        super(generateScope(id, label, relatedHardwareUnit), builder);
        this.id = id;
        this.label = label;
        this.relatedHardwareUnit = relatedHardwareUnit;
        setField("id", generateHardwareId());
        setField("label", label);

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
        return label;
    }
    
    public HardwareUnit getRelatedHardwareUnit() {
        return relatedHardwareUnit;
    }

    public final String generateHardwareId() { //TODO impl static const
        return relatedHardwareUnit.getId() + "_" + id;
    }

    @Override
    public void registerMethods(LocalServer server) throws RSBException {
    }
    
    public Future executeCommand(final OpenhabCommand.Builder commandBuilder) throws RSBBindingException {
        return executeCommand(generateHardwareId(), commandBuilder, ExecutionType.SYNCHRONOUS);
    }
    
    public Future executeCommand(final String itemName, final OpenhabCommand.Builder commandBuilder, final ExecutionType type) throws RSBBindingException {
        if(commandBuilder == null) {
            throw new RSBBindingException("Skip sending empty command!", new NullPointerException("Argument command is null!"));
        }
        
        if(rsbBinding == null) {
            throw new RSBBindingException("Skip sending command, binding not ready!", new NullPointerException("Argument rsbBinding is null!"));
        }
        
        if(generateHardwareId() == null) {
            throw new RSBBindingException("Skip sending command, could not generate id!", new NullPointerException("Argument id is null!"));
        }
        
        logger.debug("Execute command: Setting item ["+generateHardwareId()+"] to ["+commandBuilder.getType().toString()+"]");
        commandBuilder.setItem(itemName).setExecutionType(type);
        return rsbBinding.executeCommand(commandBuilder.build());
    }

    public static Scope generateScope(final String id, final String label, final HardwareUnit hardware) {
        return hardware.getLocation().getScope().concat(new Scope(Location.COMPONENT_SEPERATOR + id).concat(new Scope(Location.COMPONENT_SEPERATOR + label)));
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+id+"["+label+"]]";
    }
}
