/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractUnitController;
import rsb.Event;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.BatteryType;
import rst.homeautomation.BatteryType.Battery;

/**
 *
 * @author thuxohl
 */
public class BatteryController extends AbstractUnitController<Battery, Battery.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(BatteryType.Battery.getDefaultInstance()));
    }

    public BatteryController(String id, final String label, HardwareUnit hardwareUnit, Battery.Builder builder) throws RSBBindingException {
        super(id, label, hardwareUnit, builder);
    }

    public void updateBatteryLevel(final double batteryState) {
        builder.setState(builder.getStateBuilder().setLevel(batteryState));
        notifyChange();
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("getBatteryLevel", new GetBatteryLevelCallback());
    }

    public double getBatteryLevel() {
        logger.debug("Getting [" + id + "] BatteryChargeLevel: [" + builder.getState().getLevel() + "]");
        return builder.getState().getLevel();
    }

    public class GetBatteryLevelCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(Float.class, BatteryController.this.getBatteryLevel());
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + BatteryController.this.getId() + "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }
}
