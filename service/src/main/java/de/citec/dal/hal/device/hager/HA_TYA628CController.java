/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.hager;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.RollershutterController;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.devices.hager.HA_TYA628CType;

/**
 *
 * @author mpohling
 */
public class HA_TYA628CController extends AbstractOpenHABDeviceController<HA_TYA628CType.HA_TYA628C, HA_TYA628CType.HA_TYA628C.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HA_TYA628CType.HA_TYA628C.getDefaultInstance()));
    }

    public HA_TYA628CController(final String id, final String label, final String[] unitLabel, final Location location) throws InstantiationException {
        super(id, label, location, HA_TYA628CType.HA_TYA628C.newBuilder());
        this.registerUnit(new RollershutterController(unitLabel[0], this, data.getRollershutter0Builder(), getDefaultServiceFactory()));
        this.registerUnit(new RollershutterController(unitLabel[1], this, data.getRollershutter1Builder(), getDefaultServiceFactory()));
        this.registerUnit(new RollershutterController(unitLabel[2], this, data.getRollershutter2Builder(), getDefaultServiceFactory()));
        this.registerUnit(new RollershutterController(unitLabel[3], this, data.getRollershutter3Builder(), getDefaultServiceFactory()));
        this.registerUnit(new RollershutterController(unitLabel[4], this, data.getRollershutter4Builder(), getDefaultServiceFactory()));
        this.registerUnit(new RollershutterController(unitLabel[5], this, data.getRollershutter5Builder(), getDefaultServiceFactory()));
        this.registerUnit(new RollershutterController(unitLabel[6], this, data.getRollershutter6Builder(), getDefaultServiceFactory()));
        this.registerUnit(new RollershutterController(unitLabel[7], this, data.getRollershutter7Builder(), getDefaultServiceFactory()));
    }
}
