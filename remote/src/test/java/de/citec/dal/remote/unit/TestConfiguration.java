/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_FGS221Controller;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.device.gira.GI_5142Controller;
import de.citec.dal.hal.device.hager.HA_TYA628CController;
import de.citec.dal.hal.device.homematic.HM_ReedSwitchController;
import de.citec.dal.hal.device.homematic.HM_RotaryHandleSensorController;
import de.citec.dal.hal.device.philips.PH_Hue_E27Controller;
import de.citec.dal.hal.device.plugwise.PW_PowerPlugController;
import de.citec.dal.registry.DeviceRegistry;
import de.citec.jul.exception.CouldNotPerformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thuxohl
 */
public class TestConfiguration {

//	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestConfiguration.class);
//
//	public static final Location LOCATION = new Location("paradise");
//
//	public TestConfiguration() {
////		DALRegistry.destroy();
//	}
//
//	@Override
//	public void initDevices(final DeviceRegistry registry) {
//		try {
//			registry.register(new PH_Hue_E27Controller(AmbientLightRemoteTest.LABEL, LOCATION));
//			registry.register(new F_MotionSensorController(BatteryRemoteTest.LABEL, LOCATION));
//			registry.register(new F_MotionSensorController(BrightnessSensorRemoteTest.LABEL, LOCATION));
//			registry.register(new GI_5142Controller(ButtonRemoteTest.LABEL, ButtonRemoteTest.BUTTONS, LOCATION));
//			registry.register(new HM_RotaryHandleSensorController(HandleSensorRemoteTest.LABEL, LOCATION));
//			registry.register(new F_MotionSensorController(MotionSensorRemoteTest.LABEL, LOCATION));
//			registry.register(new PW_PowerPlugController(PowerConsumptionSensorRemoteTest.LABEL, LOCATION));
//			registry.register(new PW_PowerPlugController(PowerPlugRemoteTest.LABEL, LOCATION));
//			registry.register(new HM_ReedSwitchController(ReedSwitchRemoteTest.LABEL, LOCATION));
//			registry.register(new HA_TYA628CController(RollershutterRemoteTest.LABEL, RollershutterRemoteTest.ROLLERSHUTTER, LOCATION));
//			registry.register(new F_MotionSensorController(TamperSwitchRemoteTest.LABEL, LOCATION));
//			registry.register(new F_MotionSensorController(TemperatureSensorRemoteTest.LABEL, LOCATION));
//			registry.register(new F_FGS221Controller(LightRemoteTest.LABEL, LightRemoteTest.UNITS, LOCATION));
//		} catch (CouldNotPerformException ex) {
//			logger.warn("Could not register unit test device!", ex);
//		}
//	}
}
