/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.gira;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.ButtonController;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.gira.GI_5133Type;

/**
 *
 * @author mpohling
 */
public class GI_5133Controller extends AbstractOpenHABDeviceController<GI_5133Type.GI_5133, GI_5133Type.GI_5133.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(GI_5133Type.GI_5133.getDefaultInstance()));
    }

    public GI_5133Controller(final String id, final String label, final String[] unitLabel, final Location location) throws VerificationFailedException, InstantiationException {
        super(id, label, location, GI_5133Type.GI_5133.newBuilder());
        this.registerUnit(new ButtonController(unitLabel[0], this, data.getButton0Builder()));
        this.registerUnit(new ButtonController(unitLabel[1], this, data.getButton1Builder()));
        this.registerUnit(new ButtonController(unitLabel[2], this, data.getButton2Builder()));
        this.registerUnit(new ButtonController(unitLabel[3], this, data.getButton3Builder()));
        this.registerUnit(new ButtonController(unitLabel[4], this, data.getButton4Builder()));
        this.registerUnit(new ButtonController(unitLabel[5], this, data.getButton5Builder()));
    }
}
