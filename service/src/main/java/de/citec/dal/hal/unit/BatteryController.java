/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.exception.DALException;
import de.citec.dal.hal.AbstractUnitController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
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

    public BatteryController(String id, final String label, DeviceInterface hardwareUnit, Battery.Builder builder) throws DALException {
        super(id, label, hardwareUnit, builder);
    }

    public void updateBatteryLevel(final double batteryState) {
        data.setBatteryState(data.getBatteryStateBuilder().setLevel(batteryState));
        notifyChange();
    }
}
