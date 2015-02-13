/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.BatteryType;
import rst.homeautomation.BatteryType.Battery;

/**
 *
 * @author thuxohl
 */
public class BatteryController extends AbstractUnitController<Battery, Battery.Builder> implements BatteryInterface {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BatteryType.Battery.getDefaultInstance()));
    }
    
    public BatteryController(final String label, DeviceInterface device, Battery.Builder builder) throws InstantiationException {
        super(BatteryController.class, label, device, builder);
    }
    
    public void updateBattery(final double batteryState) {
        logger.debug("Updating Battery level of [" + this.getClass().getSimpleName() + "] to [" + batteryState + "]");
        data.setBatteryState(data.getBatteryStateBuilder().setLevel(batteryState));
        notifyChange();
    }
    
    @Override
    public double getBattery() throws CouldNotPerformException {
        return data.getBatteryState().getLevel();
    }
}
