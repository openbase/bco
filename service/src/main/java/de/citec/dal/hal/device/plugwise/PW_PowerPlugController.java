/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.plugwise;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.PowerConsumptionSensorController;
import de.citec.dal.hal.unit.PowerPlugController;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.devices.plugwise.PW_PowerPlugType;
import rst.homeautomation.devices.plugwise.PW_PowerPlugType.PW_PowerPlug;

/**
 *
 * @author mpohling
 */
public class PW_PowerPlugController extends AbstractOpenHABDeviceController<PW_PowerPlug, PW_PowerPlug.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PW_PowerPlugType.PW_PowerPlug.getDefaultInstance()));
    }

    public PW_PowerPlugController(final String id, final String label, final Location location) throws VerificationFailedException, InstantiationException {
        super(id, label, location, PW_PowerPlug.newBuilder());
        this.registerUnit(new PowerPlugController(label, this, data.getPowerPlugBuilder(), getDefaultServiceFactory()));
        this.registerUnit(new PowerConsumptionSensorController(label, this, data.getPowerConsumptionBuilder()));
    }
}
