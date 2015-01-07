/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.RSBBindingConnection;
import de.citec.dal.RSBBindingInterface;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import de.citec.dal.data.Location;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.VerificatioinFailedException;
import de.citec.dal.hal.al.HardwareUnit;
import de.citec.dal.service.rsb.RSBCommunicationService;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;
import rsb.RSBException;
import rsb.patterns.LocalServer;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand.ExecutionType;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <B>
 */
public abstract class AbstractDeviceController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> implements HardwareUnit {

    public final static String ID_SEPERATOR = "_";

    protected final String id;
    protected final String label;
    protected final String hardware_id;
    protected final String instance_id;
    protected final Location location;
    protected final Map<String, Method> halFunctionMapping;
    protected final Map<String, AbstractUnitController> unitMap;

    protected RSBBindingInterface rsbBinding = RSBBindingConnection.getInstance();

    public AbstractDeviceController(final String id, final String label, final Location location, final MB builder) throws RSBBindingException {
        super(generateScope(id, location), builder);
        this.id = id;
        this.label = label;
        this.hardware_id = parseHardwareId(id, getClass());
        this.instance_id = parseInstanceId(id);
        this.location = location;
        this.unitMap = new HashMap<>();
        this.halFunctionMapping = new HashMap<>();
        setField("id", id);
//        super.builder.setField(builder.getDescriptorForType().findFieldByName("label"), label); //TODO: Activate after rst integration

        try {
            init();
        } catch (RSBException ex) {
            throw new RSBBindingException("Could not init RSBCommunicationService!", ex);
        }

        try {
            initHardwareMapping();
        } catch (Exception ex) {
            throw new RSBBindingException("Could not apply hardware mapping for " + getClass().getSimpleName() + "!", ex);
        }
    }

    public final static String parseHardwareId(String id, Class<? extends AbstractDeviceController> hardware) throws VerificatioinFailedException {
        assert id != null;
        assert hardware != null;
        String hardwareId = hardware.getSimpleName().replace("Controller", "");

        /* verify given id */
        if (!id.startsWith(hardwareId)) {
            throw new VerificatioinFailedException("Given id [" + id + "] does not start with prefix [" + hardwareId + "]!");
        }

        return hardwareId;
    }

    public final static String parseInstanceId(String id) throws VerificatioinFailedException {
        String[] split = id.split(ID_SEPERATOR);
        String instanceId;

        try {
            instanceId = split[split.length - 1];
        } catch (IndexOutOfBoundsException ex) {
            throw new VerificatioinFailedException("Given id [" + id + "] does not contain saperator [" + ID_SEPERATOR + "]");
        }

        /* verify instance id */
        try {
            Integer.parseInt(instanceId);
        } catch (NumberFormatException ex) {
            throw new VerificatioinFailedException("Given id [" + id + "] does not end with a instance nubmer!");
        }
        return instanceId;
    }

    protected <U extends AbstractUnitController> void register(final U hardware) {
        unitMap.put(hardware.getId(), hardware);
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public void internalReceiveUpdate(String itemName, OpenhabCommand command) {
        logger.debug("internalReceiveUpdate [" + itemName + "=" + command.getType() + "]");

        String id_suffix = itemName.replaceFirst(id + "_", "");
        Method relatedMethod = halFunctionMapping.get(id_suffix);

        if (relatedMethod == null) {
            logger.warn("Could not apply update: Related Method unknown!");
            return;
        }

        try {
            switch (command.getType()) {
                case DECIMAL:
                    relatedMethod.invoke(this, command.getDecimal().getValue());
                    break;
                case HSB:
                    relatedMethod.invoke(this, command.getHsb());
                    break;
                case INCREASEDECREASE:
                    relatedMethod.invoke(this, command.getIncreaseDecrease());
                    break;
                case ONOFF:
                    relatedMethod.invoke(this, command.getOnOff().getState());
                    break;
                case OPENCLOSED:
                    relatedMethod.invoke(this, command.getOpenClosed().getState());
                    break;
                case PERCENT:
                    relatedMethod.invoke(this, command.getPercent().getValue());
                    break;
                case STOPMOVE:
                    relatedMethod.invoke(this, command.getStopMove().getState());
                    break;
                case STRING:
                    relatedMethod.invoke(this, command.getText());
                    break;
                case UPDOWN:
                    relatedMethod.invoke(this, command.getUpDown().getState());
                    break;
                default:
                    logger.warn("No corresponding Openhab command type found. Could not invoke method!");
                    break;
            }
        } catch (IllegalAccessException ex) {
            logger.error("Cannot acces related Method [" + relatedMethod.getName() + "]", ex);
        } catch (IllegalArgumentException ex) {
            //logger.error("The given argument is [" + newState.getClass().getName() + "]!");
            logger.error("Does not match [" + relatedMethod.getParameterTypes()[0].getName() + "] which is needed by [" + relatedMethod.getName() + "]!", ex);
        } catch (InvocationTargetException ex) {
            logger.error("The related method [" + relatedMethod.getName() + "] throws an exceptioin!", ex);
        } catch (Exception ex) {
            logger.error("Fatal invokation error!", ex);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public String getLable() {
        return label;
    }

    @Override
    public void activate() throws RSBBindingException {
        try {
            super.activate();

            for (AbstractUnitController controller : unitMap.values()) {
                controller.activate();
            }
        } catch (Exception ex) {
            throw new RSBBindingException(ex);
        }
    }

    @Override
    public void deactivate() throws RSBBindingException {
        try {
            super.deactivate();

            for (AbstractUnitController controller : unitMap.values()) {
                controller.deactivate();
            }
        } catch (Exception ex) {
            throw new RSBBindingException(ex);
        }
    }

    @Override
    public void registerMethods(LocalServer server) {
        // dummy construct: For registering methods overwrite this method.
    }

    public Future executeCommand(final String itemName, final OpenhabCommandType.OpenhabCommand.Builder commandBuilder, final ExecutionType type) throws RSBBindingException {
        commandBuilder.setItem(itemName).setExecutionType(type);
        return rsbBinding.executeCommand(commandBuilder.build());
    }

    @Override
    public String getHardware_id() {
        return hardware_id;
    }

    @Override
    public String getInstance_id() {
        return instance_id;
    }

    protected abstract void initHardwareMapping() throws NoSuchMethodException, SecurityException;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id:" + id + "|scope:" + getLocation().getScope() + "]";
    }

    public Collection<AbstractUnitController> getUnits() {
        return Collections.unmodifiableCollection(unitMap.values());
    }
}
