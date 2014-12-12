/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import de.citec.dal.RSBBindingConnection;
import de.citec.dal.data.Location;
import de.citec.dal.exception.RSBBindingException;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.AbstractUnitController;
import de.citec.dal.hal.devices.fibaro.F_MotionSensorController;
import de.citec.dal.hal.devices.gira.GI_5133Controller;
import de.citec.dal.hal.devices.gira.GI_5142Controller;
import de.citec.dal.hal.devices.hager.HA_TYA606EController;
import de.citec.dal.hal.devices.homematic.HM_ReedSwitchController;
import de.citec.dal.hal.devices.homematic.HM_RotaryHandleSensorController;
import de.citec.dal.hal.devices.philips.PH_Hue_E27Controller;
import de.citec.dal.hal.devices.philips.PH_Hue_GU10Controller;
import de.citec.dal.hal.devices.plugwise.PW_PowerPlugController;
import de.citec.dal.util.NotAvailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class DalRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RSBBindingConnection.class);

    private static final class InstanceHolder {

        static final DalRegistry INSTANCE = new DalRegistry();
    }

    public static DalRegistry getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final TreeMap<String, AbstractDeviceController> deviceRegistry;
    private final TreeMap<String, AbstractUnitController> unitRegistry;

    private final Map<Class<? extends AbstractUnitController>, List<AbstractUnitController>> registeredUnitClasses;

    private DalRegistry() {
        this.deviceRegistry = new TreeMap<>();
        this.unitRegistry = new TreeMap<>();
        this.registeredUnitClasses = new TreeMap<>();
        initDevices();
    }

    public void register(AbstractDeviceController hardware) {
        deviceRegistry.put(hardware.getId(), hardware);
        Collection<AbstractUnitController> units = hardware.getUnits();
        for (AbstractUnitController unit : units) {
            unitRegistry.put(unit.getScope().toString(), unit);
            if (!registeredUnitClasses.containsKey(unit.getClass())) {
                registeredUnitClasses.put(unit.getClass(), new ArrayList<AbstractUnitController>());
            }
            registeredUnitClasses.get(unit.getClass()).add(unit);
        }
    }

    public Collection<AbstractDeviceController> getHardwareCollection() {
        return Collections.unmodifiableCollection(deviceRegistry.values());
    }

    public TreeMap<String, AbstractDeviceController> getDeviceMap() {
        return deviceRegistry;
    }

    public TreeMap<String, AbstractUnitController> getUnitMap() {
        return unitRegistry;
    }

    public Collection<Class<? extends AbstractUnitController>> getRegisteredUnitClasses() {
        return Collections.unmodifiableSet(registeredUnitClasses.keySet());
    }
    
    public Collection<AbstractUnitController> getUnits(final Class<? extends AbstractUnitController> unitClass) throws NotAvailableException {
        if(!registeredUnitClasses.containsKey(unitClass)) {
            throw new NotAvailableException(unitClass.getSimpleName());
        }
        return Collections.unmodifiableCollection(registeredUnitClasses.get(unitClass));
    }

    private void initDevices() {
        logger.info("Init devices...");
        Location outdoor = new Location("Outdoor");
        Location kitchen = new Location("kitchen");
        Location wardrobe = new Location("wardrobe");
        Location living = new Location("living");
        Location sports = new Location("sports");
        Location bath = new Location("bath");
        Location control = new Location("control");

        try {
            register(new PW_PowerPlugController("PW_PowerPlug_000", "USBCharger_1", control));
            register(new PW_PowerPlugController("PW_PowerPlug_001", "USBCharger_2", control));
            register(new PW_PowerPlugController("PW_PowerPlug_002", "USBCharger_3", control));
            register(new PW_PowerPlugController("PW_PowerPlug_003", "USBCharger_4", control));
            register(new PW_PowerPlugController("PW_PowerPlug_004", "Fan", control));
            register(new PW_PowerPlugController("PW_PowerPlug_005", "", control));
            register(new PW_PowerPlugController("PW_PowerPlug_006", "", control));
            register(new PW_PowerPlugController("PW_PowerPlug_007", "", control));
            register(new PW_PowerPlugController("PW_PowerPlug_008", "", control));

            register(new HM_ReedSwitchController("HM_ReedSwitch_000", "Entrance", wardrobe));
            register(new HM_ReedSwitchController("HM_ReedSwitch_001", "1", kitchen));
            register(new HM_ReedSwitchController("HM_ReedSwitch_002", "2", kitchen));
            register(new HM_ReedSwitchController("HM_ReedSwitch_003", "3", bath));
            register(new HM_ReedSwitchController("HM_ReedSwitch_004", "4", sports));
            register(new HM_ReedSwitchController("HM_ReedSwitch_005", "5", sports));
            register(new HM_ReedSwitchController("HM_ReedSwitch_006", "6", living));
            register(new HM_ReedSwitchController("HM_ReedSwitch_007", "7", living));
            register(new HM_ReedSwitchController("HM_ReedSwitch_008", "8", sports));

            register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_000", "WindowLeft", living));
            register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_001", "WindowRight", living));
            register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_002", "Window", sports));

            register(new F_MotionSensorController("F_MotionSensor_000", "Entrance", wardrobe));
            register(new F_MotionSensorController("F_MotionSensor_001", "Hallway", wardrobe));
            register(new F_MotionSensorController("F_MotionSensor_002", "Couch", living));
            register(new F_MotionSensorController("F_MotionSensor_003", "Media", living));
            register(new F_MotionSensorController("F_MotionSensor_004", "Table", living));
            register(new F_MotionSensorController("F_MotionSensor_005", "Control", living));
            register(new F_MotionSensorController("F_MotionSensor_006", "Global", kitchen));
            register(new F_MotionSensorController("F_MotionSensor_007", "Global", bath));
            register(new F_MotionSensorController("F_MotionSensor_008", "Entrance", bath));
            register(new F_MotionSensorController("F_MotionSensor_009", "Shower", bath));
            register(new F_MotionSensorController("F_MotionSensor_010", "Sink", bath));
            register(new F_MotionSensorController("F_MotionSensor_011", "Interaction", sports));
            register(new F_MotionSensorController("F_MotionSensor_012", "Pathway", sports));
            register(new F_MotionSensorController("F_MotionSensor_013", "Entrance", control));
            register(new F_MotionSensorController("F_MotionSensor_014", "TestUnit_1", control));
            register(new F_MotionSensorController("F_MotionSensor_015", "Entrance", outdoor));

            register(new PH_Hue_E27Controller("PH_Hue_E27_000", "Hallway_0", wardrobe));
            register(new PH_Hue_E27Controller("PH_Hue_E27_001", "Hallway_1", wardrobe));
            register(new PH_Hue_E27Controller("PH_Hue_E27_002", "Table_0", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_003", "Table_1", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_004", "Couch", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_005", "Media", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_009", "Interaction_0", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_007", "Interaction_1", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_008", "TestUnit_0", control));
            register(new PH_Hue_E27Controller("PH_Hue_E27_006", "TestUnit_0", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_010", "SLamp_Left_Window1", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_011", "SLamp_Left_Window2", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_012", "SLamp_Right_Window1", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_013", "SLamp_Right_Window2", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_014", "SLamp_Right1", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_015", "SLamp_Right2", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_016", "SLamp_Window1", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_017", "SLamp_Window2", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_018", "SLamp_Mirror1", bath));
            register(new PH_Hue_E27Controller("PH_Hue_E27_019", "SLamp_Mirror2", bath));
            register(new PH_Hue_E27Controller("PH_Hue_E27_020", "LLamp_Left_Window1", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_021", "LLamp_Left_Window2", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_022", "LLamp_Left_Window3", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_023", "LLamp_Left_Window4", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_024", "LLamp_Left_Window5", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_025", "LLamp_Left_Window6", living));
            register(new PH_Hue_E27Controller("PH_Hue_E27_026", "LLamp_Entrance1", bath));
            register(new PH_Hue_E27Controller("PH_Hue_E27_027", "LLamp_Entrance2", bath));
            register(new PH_Hue_E27Controller("PH_Hue_E27_028", "LLamp_Entrance3", bath));
            register(new PH_Hue_E27Controller("PH_Hue_E27_029", "LLamp_Entrance4", bath));
            register(new PH_Hue_E27Controller("PH_Hue_E27_030", "LLamp_Entrance5", bath));
            register(new PH_Hue_E27Controller("PH_Hue_E27_031", "LLamp_Entrance6", bath));
            register(new PH_Hue_E27Controller("PH_Hue_E27_032", "LLamp_Entrance1", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_033", "LLamp_Entrance2", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_034", "LLamp_Entrance3", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_035", "LLamp_Entrance4", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_036", "LLamp_Entrance5", sports));
            register(new PH_Hue_E27Controller("PH_Hue_E27_037", "LLamp_Entrance6", sports));

            register(new PH_Hue_GU10Controller("PH_Hue_GU10_000", "Global_0", kitchen));
            register(new PH_Hue_GU10Controller("PH_Hue_GU10_001", "Global_1", kitchen));
            register(new PH_Hue_GU10Controller("PH_Hue_GU10_002", "Global_2", kitchen));
            register(new PH_Hue_GU10Controller("PH_Hue_GU10_003", "Global_3", kitchen));
            register(new PH_Hue_GU10Controller("PH_Hue_GU10_004", "Global_0", bath));
            register(new PH_Hue_GU10Controller("PH_Hue_GU10_005", "Global_1", bath));
            register(new PH_Hue_GU10Controller("PH_Hue_GU10_006", "Global_2", bath));
            register(new PH_Hue_GU10Controller("PH_Hue_GU10_007", "Global_3", bath));

            register(new HA_TYA606EController("HA_TYA606E_000", "1", control));
            register(new HA_TYA606EController("HA_TYA606E_001", "2", control));
            register(new HA_TYA606EController("HA_TYA606E_002", "3", control));
            register(new HA_TYA606EController("HA_TYA606E_003", "4", control));
            register(new HA_TYA606EController("HA_TYA606E_004", "5", control));
            register(new HA_TYA606EController("HA_TYA606E_005", "6", control));
            register(new HA_TYA606EController("HA_TYA606E_006", "7", control));
            register(new HA_TYA606EController("HA_TYA606E_007", "8", control));
            register(new HA_TYA606EController("HA_TYA606E_008", "9", control));
            register(new HA_TYA606EController("HA_TYA606E_009", "10", control));
            register(new HA_TYA606EController("HA_TYA606E_010", "11", control));

            String[] giraLabel0 = {"Button_1", "Button_2", "Button_3", "Button_4"};
            register(new GI_5142Controller("GI_5142_000", "Entrance", giraLabel0, bath));
            register(new GI_5142Controller("GI_5142_001", "Control", giraLabel0, living));
            register(new GI_5142Controller("GI_5142_002", "Pathway", giraLabel0, sports));

            String[] giraLabel1 = {"Button_1", "Button_2", "Button_3", "Button_4", "Button_5", "Button_6"};
            register(new GI_5133Controller("GI_5133_000", "Door", giraLabel1, kitchen));
            register(new GI_5133Controller("GI_5133_001", "Entrance", giraLabel1, wardrobe));
            register(new GI_5133Controller("GI_5133_002", "Hallway", giraLabel1, wardrobe));
            register(new GI_5133Controller("GI_5133_005", "Media", giraLabel1, living));

            String[] giraLabel2 = {"Button_5", "Button_6", "Button_7", "Button_8", "Button_9", "Button_10"};
            register(new GI_5133Controller("GI_5133_004", "Control", giraLabel2, living));
            register(new GI_5133Controller("GI_5133_006", "Pathway", giraLabel2, sports));

            String[] giraLabel3 = {"Button_7", "Button_8", "Button_9", "Button_10", "Button_11", "Button_12"};
            register(new GI_5133Controller("GI_5133_003", "Hallway", giraLabel3, wardrobe));
        } catch (RSBBindingException ex) {
            logger.warn("Could not initialize devices!", ex);
        }
    }
}
