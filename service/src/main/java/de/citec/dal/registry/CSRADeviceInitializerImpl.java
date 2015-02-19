/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.device.philips.PH_Hue_E27Controller;
import de.citec.dal.hal.device.philips.PH_Hue_GU10Controller;
import de.citec.dal.util.DALRegistry;
import de.citec.dal.util.DeviceInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class CSRADeviceInitializerImpl implements DeviceInitializer {

    private static final Logger logger = LoggerFactory.getLogger(CSRADeviceInitializerImpl.class);

    @Override
    public void initDevices(final DALRegistry registry) {
        logger.info("Init devices...");
        Location outdoor = new Location("outdoor");
        Location kitchen = new Location("kitchen");
        Location wardrobe = new Location("wardrobe");
        Location living = new Location("living");
        Location sports = new Location("sports");
        Location bath = new Location("bath");
        Location control = new Location("control");

        try {
//				registry.register(new PW_PowerPlugController("PW_PowerPlug_000", "USBCharger_1", control));
//				registry.register(new PW_PowerPlugController("PW_PowerPlug_001", "USBCharger_2", control));
//				registry.register(new PW_PowerPlugController("PW_PowerPlug_002", "USBCharger_3", control));
//				registry.register(new PW_PowerPlugController("PW_PowerPlug_003", "USBCharger_4", control));
//				registry.register(new PW_PowerPlugController("PW_PowerPlug_004", "Fan", control));
//				registry.register(new PW_PowerPlugController("PW_PowerPlug_005", "", control));
//				registry.register(new PW_PowerPlugController("PW_PowerPlug_006", "", control));
//				registry.register(new PW_PowerPlugController("PW_PowerPlug_007", "", control));
//				registry.register(new PW_PowerPlugController("PW_PowerPlug_008", "", control));

//				registry.register(new HM_ReedSwitchController("HM_ReedSwitch_000", "Entrance", wardrobe));
//				registry.register(new HM_ReedSwitchController("HM_ReedSwitch_001", "1", kitchen));
//				registry.register(new HM_ReedSwitchController("HM_ReedSwitch_002", "2", kitchen));
//				registry.register(new HM_ReedSwitchController("HM_ReedSwitch_003", "3", bath));
//				registry.register(new HM_ReedSwitchController("HM_ReedSwitch_004", "4", sports));
//				registry.register(new HM_ReedSwitchController("HM_ReedSwitch_005", "5", sports));
//				registry.register(new HM_ReedSwitchController("HM_ReedSwitch_006", "6", living));
//				registry.register(new HM_ReedSwitchController("HM_ReedSwitch_007", "7", living));
//				registry.register(new HM_ReedSwitchController("HM_ReedSwitch_008", "8", sports));
//
//				registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_000", "WindowLeft", living));
//				registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_001", "WindowRight", living));
//				registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_002", "Window", sports));
            registry.register(new F_MotionSensorController("F_MotionSensor_000", "Entrance", wardrobe));
            registry.register(new F_MotionSensorController("F_MotionSensor_001", "Hallway", wardrobe));
            registry.register(new F_MotionSensorController("F_MotionSensor_002", "Couch", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_003", "Media", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_004", "Table", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_005", "Control", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_006", "Global", kitchen));
            registry.register(new F_MotionSensorController("F_MotionSensor_007", "Global", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_008", "Entrance", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_009", "Shower", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_010", "Sink", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_011", "Interaction", sports));
            registry.register(new F_MotionSensorController("F_MotionSensor_012", "Pathway", sports));
            registry.register(new F_MotionSensorController("F_MotionSensor_013", "Entrance", control));
            registry.register(new F_MotionSensorController("F_MotionSensor_014", "TestUnit_1", control));
            registry.register(new F_MotionSensorController("F_MotionSensor_015", "Entrance", outdoor));
//
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_000", "Hallway_0", wardrobe));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_001", "Hallway_1", wardrobe));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_002", "Table_0", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_003", "Table_1", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_004", "Couch", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_005", "Media", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_009", "Interaction_0", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_007", "Interaction_1", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_008", "TestUnit_0", control));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_010", "SLamp_Left_Window1", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_011", "SLamp_Left_Window2", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_012", "SLamp_Right_Window1", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_013", "SLamp_Right_Window2", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_014", "SLamp_Right1", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_015", "SLamp_Right2", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_016", "SLamp_Window1", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_017", "SLamp_Window2", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_018", "SLamp_Mirror1", bath));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_019", "SLamp_Mirror2", bath));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_020", "LLamp_Left_Window1", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_021", "LLamp_Left_Window2", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_022", "LLamp_Left_Window3", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_023", "LLamp_Left_Window4", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_024", "LLamp_Left_Window5", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_025", "LLamp_Left_Window6", living));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_026", "LLamp_Entrance1", bath));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_027", "LLamp_Entrance2", bath));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_028", "LLamp_Entrance3", bath));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_029", "LLamp_Entrance4", bath));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_030", "LLamp_Entrance5", bath));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_031", "LLamp_Entrance6", bath));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_032", "LLamp_Entrance1", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_033", "LLamp_Entrance2", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_034", "LLamp_Entrance3", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_035", "LLamp_Entrance4", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_036", "LLamp_Entrance5", sports));
//            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_037", "LLamp_Entrance6", sports));

            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_000", "Global_0", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_001", "Global_1", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_002", "Global_2", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_003", "Global_3", kitchen));
//            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_004", "Global_0", bath));
//            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_005", "Global_1", bath));
//            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_006", "Global_2", bath));
//            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_007", "Global_3", bath));

//				registry.register(new HA_TYA606EController("HA_TYA606E_000", "1", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_001", "2", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_002", "3", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_003", "4", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_004", "5", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_005", "6", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_006", "7", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_007", "8", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_008", "9", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_009", "10", control));
//				registry.register(new HA_TYA606EController("HA_TYA606E_010", "11", control));
//            String[] giraLabel0 = {"Button_1", "Button_2", "Button_3", "Button_4"};
//            registry.register(new GI_5142Controller("GI_5142_000", "Entrance", giraLabel0, bath));
//            registry.register(new GI_5142Controller("GI_5142_001", "Control", giraLabel0, living));
//            registry.register(new GI_5142Controller("GI_5142_002", "Pathway", giraLabel0, sports));
//
//            String[] giraLabel1 = {"Button_1", "Button_2", "Button_3", "Button_4", "Button_5", "Button_6"};
//            registry.register(new GI_5133Controller("GI_5133_000", "Door", giraLabel1, kitchen));
//            registry.register(new GI_5133Controller("GI_5133_001", "Entrance", giraLabel1, wardrobe));
//            registry.register(new GI_5133Controller("GI_5133_002", "Hallway", giraLabel1, wardrobe));
//            registry.register(new GI_5133Controller("GI_5133_005", "Media", giraLabel1, living));
//
//            String[] giraLabel2 = {"Button_5", "Button_6", "Button_7", "Button_8", "Button_9", "Button_10"};
//            registry.register(new GI_5133Controller("GI_5133_004", "Control", giraLabel2, living));
//            registry.register(new GI_5133Controller("GI_5133_006", "Pathway", giraLabel2, sports));
//
//            String[] giraLabel3 = {"Button_7", "Button_8", "Button_9", "Button_10", "Button_11", "Button_12"};
//            registry.register(new GI_5133Controller("GI_5133_003", "Hallway", giraLabel3, wardrobe));
//
//            String[] hagerLabel1 = {"Rollershutter_1", "Rollershutter_2", "Rollershutter_3", "Rollershutter_4", "Rollershutter_5", "Rollershutter_6", "Rollershutter_7", "Rollershutter_8"};
//            registry.register(new HA_TYA628CController("HA_TYA628C_000", "Control", hagerLabel1, control));
        } catch (de.citec.jul.exception.InstantiationException ex) {
            logger.warn("Could not initialize devices!", ex);
        }
    }
}
