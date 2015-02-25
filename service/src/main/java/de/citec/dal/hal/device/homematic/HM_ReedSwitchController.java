/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.homematic;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.BatteryController;
import de.citec.dal.hal.unit.ReedSwitchController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.homematic.HM_ReedSwitchType;
import rst.homeautomation.device.homematic.HM_ReedSwitchType.HM_ReedSwitch;

/**
 *
 * @author mpohling
 */
public class HM_ReedSwitchController extends AbstractOpenHABDeviceController<HM_ReedSwitch, HM_ReedSwitch.Builder> {

	static {
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HM_ReedSwitchType.HM_ReedSwitch.getDefaultInstance()));
	}

	public HM_ReedSwitchController(final String label, final Location location) throws InstantiationException {
		super(label, location, HM_ReedSwitch.newBuilder());
		try {
			this.registerUnit(new ReedSwitchController(label, this, data.getReedSwitchBuilder()));
			this.registerUnit(new BatteryController(label, this, data.getBatteryBuilder()));
		} catch (CouldNotPerformException ex) {
			throw new InstantiationException(this, ex);
		}
	}
}
