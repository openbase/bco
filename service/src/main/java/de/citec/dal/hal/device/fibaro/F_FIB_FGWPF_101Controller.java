/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.fibaro;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.PowerConsumptionSensorController;
import de.citec.dal.hal.unit.PowerPlugController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.fibaro.F_FIB_FGWPF_101Type;

/**
 *
 * @author thuxohl
 */
public class F_FIB_FGWPF_101Controller extends AbstractOpenHABDeviceController<F_FIB_FGWPF_101Type.F_FIB_FGWPF_101 , F_FIB_FGWPF_101Type.F_FIB_FGWPF_101.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(F_FIB_FGWPF_101Type.F_FIB_FGWPF_101.getDefaultInstance()));
    }

    public F_FIB_FGWPF_101Controller(final String name, String label, final Location location) throws de.citec.jul.exception.InstantiationException {
        super(name, label, location, F_FIB_FGWPF_101Type.F_FIB_FGWPF_101.newBuilder());
        registerUnit(new PowerPlugController(label, this, data.getPowerPlugBuilder()));
        registerUnit(new PowerConsumptionSensorController(label, this, data.getPowerConsumptionBuilder()));
    }
}
