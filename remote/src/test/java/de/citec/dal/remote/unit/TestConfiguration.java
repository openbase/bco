/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.device.gira.GI_5142Controller;
import de.citec.dal.hal.device.hager.HA_TYA628CController;
import de.citec.dal.hal.device.homematic.HM_ReedSwitchController;
import de.citec.dal.hal.device.homematic.HM_RotaryHandleSensorController;
import de.citec.dal.hal.device.philips.PH_Hue_E27Controller;
import de.citec.dal.hal.device.plugwise.PW_PowerPlugController;
import de.citec.dal.util.DALRegistry;
import de.citec.jul.exception.VerificationFailedException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thuxohl
 */
public class TestConfiguration implements de.citec.dal.util.DeviceInitializer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestConfiguration.class);

    public static final Location LOCATION = new Location("paradise");

    @Override
    public void initDevices(final DALRegistry registry) {

        try {
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_000", AmbientLightRemoteTest.LABEL, LOCATION));
            registry.register(new F_MotionSensorController("F_MotionSensor_001", BatteryRemoteTest.LABEL, LOCATION));
            registry.register(new F_MotionSensorController("F_MotionSensor_002", BrightnessSensorRemoteTest.LABEL, LOCATION));
            registry.register(new GI_5142Controller("GI_5142_003", ButtonRemoteTest.LABEL, ButtonRemoteTest.BUTTONS, LOCATION));
            registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_004", HandleSensorRemoteTest.LABEL, LOCATION));
            registry.register(new F_MotionSensorController("F_MotionSensor_005", MotionSensorRemoteTest.LABEL, LOCATION));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_006", PowerConsumptionSensorRemoteTest.LABEL, LOCATION));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_007", PowerPlugRemoteTest.LABEL, LOCATION));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_008", ReedSwitchRemoteTest.LABEL, LOCATION));
            registry.register(new HA_TYA628CController("HA_TYA628C_009", RollershutterRemoteTest.LABEL, RollershutterRemoteTest.ROLLERSHUTTER, LOCATION));
            registry.register(new F_MotionSensorController("F_MotionSensor_010", TamperSwitchRemoteTest.LABEL, LOCATION));
            registry.register(new F_MotionSensorController("F_MotionSensor_011", TemperatureSensorRemoteTest.LABEL, LOCATION));
        } catch (de.citec.jul.exception.InstantiationException ex) {
            logger.warn("Could not initialize unit test device!", ex);
        } catch (VerificationFailedException ex) {
            Logger.getLogger(TestConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
