/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.dal;

import de.citec.dal.data.Location;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.devices.fibaro.F_MotionSensorController;
import de.citec.dal.hal.devices.homematic.HM_ReedSwitchController;
import de.citec.dal.hal.devices.homematic.HM_RotaryHandleSensorController;
import de.citec.dal.hal.devices.philips.PH_Hue_E27Controller;
import de.citec.dal.hal.devices.philips.PH_Hue_GU10Controller;
import de.citec.dal.hal.devices.plugwise.PW_PowerPlugController;
import de.citec.dal.service.HardwareManager;
import de.citec.dal.service.HardwareRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thuxohl
 */
public class RSBBindingConnection implements RSBBindingInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(RSBBindingConnection.class);
    
    private static RSBBindingConnection instance;
    private final RSBBindingInterface binding;
    
    private final HardwareRegistry registry;
    private final HardwareManager hardwareManager;

    public RSBBindingConnection(RSBBindingInterface binding) {
        this.binding = binding;
        this.registry = HardwareRegistry.getInstance();
        this.hardwareManager = HardwareManager.getInstance();
        this.initDevices();
        this.instance = this;
    }
    
    private void initDevices() {
        logger.info("Init devices...");
        Location kitchen = new Location("kitchen");
        Location wardrobe = new Location("wardrobe");
        Location living = new Location("living");
        Location sports = new Location("sports");
        Location bath = new Location("bath");
        Location controlroom = new Location("controlroom");
        
        try {
            registry.register(new PW_PowerPlugController("PW_PowerPlug_000", kitchen));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_001", kitchen));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_002", kitchen));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_003", living));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_004", living));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_005", living));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_006", wardrobe));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_007", wardrobe));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_008", bath));
            
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_000", wardrobe));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_001", kitchen));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_002", kitchen));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_003", bath));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_004", sports));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_005", sports));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_006", living));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_007", living));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_008", sports));
            
            registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_000", living));
            registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_001", living));
            registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_002", sports));
            
            registry.register(new F_MotionSensorController("F_MotionSensor_000", wardrobe));
            registry.register(new F_MotionSensorController("F_MotionSensor_001", wardrobe));
            registry.register(new F_MotionSensorController("F_MotionSensor_002", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_003", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_004", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_005", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_006", kitchen));
            registry.register(new F_MotionSensorController("F_MotionSensor_007", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_008", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_009", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_010", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_011", sports));
            registry.register(new F_MotionSensorController("F_MotionSensor_012", sports));

            registry.register(new F_MotionSensorController("F_MotionSensor_013", sports));
            registry.register(new F_MotionSensorController("F_MotionSensor_014", kitchen));
            registry.register(new F_MotionSensorController("F_MotionSensor_015", kitchen));
            
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_000", wardrobe));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_001", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_002", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_003", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_004", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_005", controlroom));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_006", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_007", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_008", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_009", sports));
            
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_000", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_001", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_002", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_003", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_004", bath));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_005", bath));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_006", bath));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_007", bath));
        } catch (RSBBindingException ex) {
            logger.warn("Could not initialize devices!", ex);
        }
    }
    
    public void activate() {
        logger.info("Activate " + getClass().getSimpleName() + "...");
        hardwareManager.activate();
    }

    public void deactivate() {
        logger.info("Deactivate " + getClass().getSimpleName() + "...");
        hardwareManager.deactivate();
    }
    
    @Override
    public void internalReceiveCommand(String itemName, Command command) {
//        hardwareManager.internalReceiveCommand(itemName, command);
    }

    @Override
    public void internalReceiveUpdate(String itemName, State newState) {
        hardwareManager.internalReceiveUpdate(itemName, newState);
    }

    @Override
    public void postCommand(String itemName, Command command) throws RSBBindingException {
        binding.postCommand(itemName, command);
    }

    @Override
    public void sendCommand(String itemName, Command command) throws RSBBindingException {
        binding.sendCommand(itemName, command);
    }
    
    public static RSBBindingInterface getInstance() {
        return instance;
    }

    
    
}
