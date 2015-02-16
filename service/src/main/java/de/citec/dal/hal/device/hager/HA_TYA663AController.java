/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.hager;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.hager.HA_TYA663AType;

/**
 *
 * @author mpohling
 */
public class HA_TYA663AController extends AbstractOpenHABDeviceController<HA_TYA663AType.HA_TYA663A, HA_TYA663AType.HA_TYA663A.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HA_TYA663AType.HA_TYA663A.getDefaultInstance()));
    }

    public HA_TYA663AController(final String id, final String label, final String[] unitLabel, final Location location) throws de.citec.jul.exception.InstantiationException {
        super(id, label, location, HA_TYA663AType.HA_TYA663A.newBuilder());
        // TODO tamino: implement dimmer unit.
    }
}
