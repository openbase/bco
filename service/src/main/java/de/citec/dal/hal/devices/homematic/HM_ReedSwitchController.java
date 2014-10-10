/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.devices.homematic;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.OpenClosedStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHardwareController;
import de.citec.dal.hal.al.BatteryStateController;
import de.citec.dal.hal.al.ReedSwitchController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.homematic.HM_ReedSwitchType;
import rst.devices.homematic.HM_ReedSwitchType.HM_ReedSwitch;

/**
 *
 * @author mpohling
 */
public class HM_ReedSwitchController extends AbstractHardwareController<HM_ReedSwitch, HM_ReedSwitch.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HM_ReedSwitchType.HM_ReedSwitch.getDefaultInstance()));
    }

    private final ReedSwitchController reedSwitch;
    private final BatteryStateController batteryState;

    public HM_ReedSwitchController(final String id, final Location location) throws RSBBindingException {
        super(id, location, HM_ReedSwitch.newBuilder());

        builder.setId(id);
        this.reedSwitch = new ReedSwitchController("ReedSwitch", this, builder.getReedSwitchBuilder());
        this.batteryState = new BatteryStateController("BatteryState", this, builder.getBatteryStateBuilder());
        this.register(reedSwitch);
        this.register(batteryState);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put("ReedSwitch", getClass().getMethod("updateReedSwitch", org.openhab.core.library.types.OpenClosedType.class));
        halFunctionMapping.put("BatteryState", getClass().getMethod("updateBatteryState", org.openhab.core.library.types.DecimalType.class));
    }

    public void updateReedSwitch(org.openhab.core.library.types.OpenClosedType type) {
        try {
            reedSwitch.updateOpenClosedState(OpenClosedStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OpenClosedType(openhab) to OpenClosedState!", ex);
        }
    }

    public void updateBatteryState(org.openhab.core.library.types.DecimalType value) {
        batteryState.updateBatteryLevel(value.floatValue());
    }
}
