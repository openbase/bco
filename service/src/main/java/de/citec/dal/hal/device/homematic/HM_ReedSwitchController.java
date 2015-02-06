/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.homematic;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.data.transform.OpenClosedStateTransformer;
import de.citec.dal.exception.DALException;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.unit.BatteryController;
import de.citec.dal.hal.unit.ReedSwitchController;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.homematic.HM_ReedSwitchType;
import rst.devices.homematic.HM_ReedSwitchType.HM_ReedSwitch;
import rst.homeautomation.openhab.OpenClosedHolderType.OpenClosedHolder.OpenClosed;

/**
 *
 * @author mpohling
 */
public class HM_ReedSwitchController extends AbstractOpenHABDeviceController<HM_ReedSwitch, HM_ReedSwitch.Builder> {

    private final static String COMPONENT_REED_SWITCH = "ReedSwitch";
    private final static String COMPONENT_BATTERY = "Battery";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HM_ReedSwitchType.HM_ReedSwitch.getDefaultInstance()));
    }

    private final ReedSwitchController reedSwitch;
    private final BatteryController battery;

    public HM_ReedSwitchController(final String id, final String label, final Location location) throws VerificationFailedException, InstantiationException {
        super(id, label, location, HM_ReedSwitch.newBuilder());

        data.setId(id);
        this.reedSwitch = new ReedSwitchController(COMPONENT_REED_SWITCH, label, this, data.getReedSwitchBuilder());
        this.battery = new BatteryController(COMPONENT_BATTERY, label, this, data.getBatteryBuilder());
        this.register(reedSwitch);
        this.register(battery);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_REED_SWITCH, getClass().getMethod("updateReedSwitch", OpenClosed.class));
        halFunctionMapping.put(COMPONENT_BATTERY, getClass().getMethod("updateBatteryLevel", double.class));
    }

    public void updateReedSwitch(OpenClosed type) {
        try {
            reedSwitch.updateOpenClosedState(OpenClosedStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OpenClosedType(openhab) to OpenClosedState!", ex);
        }
    }

    public void updateBatteryLevel(double value) {
        battery.updateBatteryLevel((float) value);
    }
}
