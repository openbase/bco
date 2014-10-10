/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHALController;
import rsb.Event;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.devices.generic.BatteryStateType;
import rst.devices.generic.BatteryStateType.BatteryState;

/**
 *
 * @author thuxohl
 */
public class BatteryStateController extends AbstractHALController<BatteryState, BatteryState.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(BatteryStateType.BatteryState.getDefaultInstance()));
    }

    public BatteryStateController(String id, HardwareUnit hardwareUnit, BatteryState.Builder builder) throws RSBBindingException {
        super(id, hardwareUnit, builder);
    }

    public void updateBatteryLevel(final float batteryState) {
        builder.setChargeLevel(batteryState);
        notifyChange();
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("getBatteryLevel", new GetBatteryLevelCallback());
    }

    public float getBatteryLevel() {
        logger.debug("Getting [" + id + "] BatteryChargeLevel: [" + builder.getChargeLevel() + "]");
        return builder.getChargeLevel();
    }

    public class GetBatteryLevelCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(Float.class, BatteryStateController.this.getBatteryLevel());
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + BatteryStateController.this.getId() + "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }
}
