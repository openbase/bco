/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.fibaro;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.unit.AbstractUnitController;
import de.citec.dal.hal.unit.ButtonController;
import de.citec.dal.hal.unit.LightController;
import de.citec.dal.hal.unit.Unit;
import de.citec.dal.transform.UnitConfigToUnitClassTransformer;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.fibaro.F_FGS_221Type;
import rst.homeautomation.device.fibaro.F_MotionSensorType;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.rsb.processing.StringProcessor;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.LightType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author mpohling
 */
public class F_FGS221Controller extends AbstractOpenHABDeviceController<F_FGS_221Type.F_FGS_221, F_FGS_221Type.F_FGS_221.Builder> {

	static {
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(F_MotionSensorType.F_MotionSensor.getDefaultInstance()));
	}

	public F_FGS221Controller(final DeviceConfig config) throws InstantiationException, CouldNotTransformException {
		super(config, F_FGS_221Type.F_FGS_221.newBuilder());
		try {

            config.get
            for(UnitConfig unitConfig : config.getUnitConfigList()) {
//                unitConfig.getTemplate().ge
                String unitTypeName = StringProcessor.transformUpperCaseToCamelCase(unitConfig.getTemplate().getType().name());
                getClass().getClassLoader().loadClass("rst.homeautomation.unit."+unitTypeName+"Type."+unitTypeName);
                data.addRepeatedField(data.getDescriptorForType().findFieldByName("unit_"+unitConfig.getTemplate().getType().name().toLowerCase()), );
                UnitConfigToUnitClassTransformer.transform(unitConfig).getConstructor(UnitConfigType.UnitConfig.class, Device.class, LightType.Light.Builder.class);
                unitConfig.getLabel();
                Unit unit;
                registerUnit(unit);
            }

            data.get

            config.getUnitConfigList().get(0).
			registerUnit(new LightController(unitlabel[0], this, data.getLight0Builder()));
			registerUnit(new LightController(unitlabel[1], this, data.getLight1Builder()));
			registerUnit(new ButtonController(unitlabel[2], this, data.getButton0Builder()));
			registerUnit(new ButtonController(unitlabel[3], this, data.getButton1Builder()));
		} catch (CouldNotPerformException ex) {
			throw new InstantiationException(this, ex);
		}
	}


}
