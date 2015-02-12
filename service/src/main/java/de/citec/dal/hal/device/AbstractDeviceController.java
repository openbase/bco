/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import com.google.protobuf.GeneratedMessage;
import java.util.HashMap;
import java.util.Map;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.AbstractUnitController;
import de.citec.dal.hal.service.Service;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.rsb.RSBCommunicationService;
import de.citec.jul.rsb.RSBInformerInterface.InformerType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import rsb.RSBException;
import rsb.patterns.LocalServer;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractDeviceController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> implements DeviceInterface {

    public final static String ID_SEPERATOR = "_";
    public final static String TYPE_FILED_ID = "id";
    public final static String TYPE_FILED_LABEL = "label";

    protected final String id;
    protected final String label;
    protected final String hardware_id;
    protected final String instance_id;
    protected final Location location;
//    protected final Map<String, Method> halFunctionMapping;
    protected final Map<String, AbstractUnitController> unitMap;
    protected Map<AbstractUnitController, Collection<Service>> unitServiceHardwareMap;

    public AbstractDeviceController(final String id, final String label, final Location location, final MB builder) throws InstantiationException {
        super(generateScope(id, location), builder);
        this.id = id;
        this.label = label;
        this.location = location;
        this.unitMap = new HashMap<>();
//        this.halFunctionMapping = new HashMap<>();
        this.unitServiceHardwareMap = new HashMap<>();

        try {
            this.hardware_id = parseDeviceId(id, getClass());
            this.instance_id = parseInstanceId(id);
        } catch (VerificationFailedException ex) {
            throw new InstantiationException("Unable to generate id fields.", this, ex);
        }

        setField(TYPE_FILED_ID, id);
        setField(TYPE_FILED_LABEL, label);

        try {
            init(InformerType.Distributed);
        } catch (RSBException ex) {
            throw new InstantiationException("Could not init RSBCommunicationService!", ex);
        }

//        try {
//            initHardwareMapping();
//        } catch (Exception ex) {
//            throw new InstantiationException("Could not apply hardware mapping for " + getClass().getSimpleName() + "!", ex);
//        }
    }

    public final static String parseDeviceId(String id, Class<? extends AbstractDeviceController> hardware) throws VerificationFailedException {
        assert id != null;
        assert hardware != null;
        String hardwareId = hardware.getSimpleName().replace("Controller", "");

        /* verify given id */
        if (!id.startsWith(hardwareId)) {
            throw new VerificationFailedException("Given id [" + id + "] does not start with prefix [" + hardwareId + "]!");
        }

        return hardwareId;
    }

    public final static String parseInstanceId(String id) throws VerificationFailedException {
        String[] split = id.split(ID_SEPERATOR);
        String instanceId;

        try {
            instanceId = split[split.length - 1];
        } catch (IndexOutOfBoundsException ex) {
            throw new VerificationFailedException("Given id [" + id + "] does not contain saperator [" + ID_SEPERATOR + "]");
        }

        /* verify instance id */
        try {
            Integer.parseInt(instanceId);
        } catch (NumberFormatException ex) {
            throw new VerificationFailedException("Given id [" + id + "] does not end with a instance nubmer!");
        }
        return instanceId;
    }

    protected <U extends AbstractUnitController> void registerUnit(final U unit) {
        unitMap.put(unit.getName(), unit);
    }


	//TODO mpohling: how to handle multi unit with same name.
    public AbstractUnitController getUnitByName(final String name) throws NotAvailableException {
        if(!unitMap.containsKey(name)) {
            throw new NotAvailableException("Unit["+name+"]", this+" has no registed unit with given name!");
        }
        return unitMap.get(name);
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public void registerService(Service service, AbstractUnitController unit) {
        if (!unitServiceHardwareMap.containsKey(unit)) {
            unitServiceHardwareMap.put(unit, new ArrayList<Service>());
        }
        unitServiceHardwareMap.get(unit).add(service);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getLable() {
        return label;
    }

    @Override
    public void activate() {
        super.activate();

        for (AbstractUnitController controller : unitMap.values()) {
            controller.activate();
        }
    }

    @Override
    public void deactivate() throws InterruptedException {
        super.deactivate();

        for (AbstractUnitController controller : unitMap.values()) {
            controller.deactivate();
        }
    }

    @Override
    public void registerMethods(LocalServer server) {
        // dummy construct: For registering methods overwrite this method.
    }

    @Override
    public String getHardware_id() {
        return hardware_id;
    }

    @Override
    public String getInstance_id() {
        return instance_id;
    }

//    protected abstract void initHardwareMapping() throws NoSuchMethodException, SecurityException;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id:" + id + "|scope:" + getLocation().getScope() + "]";
    }

    public Collection<AbstractUnitController> getUnits() {
        return Collections.unmodifiableCollection(unitMap.values());
    }
}
