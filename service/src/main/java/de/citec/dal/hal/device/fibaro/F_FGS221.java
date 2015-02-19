/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.fibaro;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.ButtonController;
import de.citec.dal.hal.unit.LightController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.fibaro.F_FGS_221Type;
import rst.devices.fibaro.F_MotionSensorType;

/**
 *
 * @author mpohling
 */
public class F_FGS221 extends AbstractOpenHABDeviceController<F_FGS_221Type.F_FGS_221, F_FGS_221Type.F_FGS_221.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(F_MotionSensorType.F_MotionSensor.getDefaultInstance()));
    }

    public F_FGS221(final String id, String label, String[] unitlabel, final Location location) throws de.citec.jul.exception.InstantiationException {
        super(id, label, location, F_FGS_221Type.F_FGS_221.newBuilder());
        registerUnit(new LightController(unitlabel[0], this, data.getLight0Builder()));
        registerUnit(new LightController(unitlabel[1], this, data.getLight1Builder()));
        registerUnit(new ButtonController(unitlabel[0], this, data.getButton0Builder()));
        registerUnit(new ButtonController(unitlabel[1], this, data.getButton1Builder()));
    }
}
