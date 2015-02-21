/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.hager;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.PowerConsumptionSensorController;
import de.citec.dal.hal.unit.PowerPlugController;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.devices.hager.HA_TYA606EType;

/**
 *
 * @author mpohling
 */
public class HA_TYA606EController extends AbstractOpenHABDeviceController<HA_TYA606EType.HA_TYA606E, HA_TYA606EType.HA_TYA606E.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HA_TYA606EType.HA_TYA606E.getDefaultInstance()));
    }

    public HA_TYA606EController(final String id, final String label, final Location location) throws VerificationFailedException, InstantiationException {
        super(id, label, location, HA_TYA606EType.HA_TYA606E.newBuilder());

        this.registerUnit(new PowerPlugController(label, this, data.getPowerPlug0Builder(), getDefaultServiceFactory()));
        this.registerUnit(new PowerPlugController(label, this, data.getPowerPlug1Builder(), getDefaultServiceFactory()));
        this.registerUnit(new PowerPlugController(label, this, data.getPowerPlug2Builder(), getDefaultServiceFactory()));
        this.registerUnit(new PowerPlugController(label, this, data.getPowerPlug3Builder(), getDefaultServiceFactory()));
        this.registerUnit(new PowerPlugController(label, this, data.getPowerPlug4Builder(), getDefaultServiceFactory()));
        this.registerUnit(new PowerPlugController(label, this, data.getPowerPlug5Builder(), getDefaultServiceFactory()));
        this.registerUnit(new PowerConsumptionSensorController(label, this, data.getPowerConsumptionSensor0Builder()));
        this.registerUnit(new PowerConsumptionSensorController(label, this, data.getPowerConsumptionSensor1Builder()));
        this.registerUnit(new PowerConsumptionSensorController(label, this, data.getPowerConsumptionSensor2Builder()));
        this.registerUnit(new PowerConsumptionSensorController(label, this, data.getPowerConsumptionSensor3Builder()));
        this.registerUnit(new PowerConsumptionSensorController(label, this, data.getPowerConsumptionSensor4Builder()));
        this.registerUnit(new PowerConsumptionSensorController(label, this, data.getPowerConsumptionSensor5Builder()));
    }
}
