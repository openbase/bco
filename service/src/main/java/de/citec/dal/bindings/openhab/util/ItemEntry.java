/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util;

import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceTypeHolderType;

/**
 *
 * @author mpohling
 */
public class ItemEntry {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ItemEntry.class);
    
    
//    
//    public String generateStringRep() {
//        String stringRep = "";
//                    stringRep += getCommand(serviceConfig.getType());
//                    stringRep += "   ";
//
//                    // id !?
//                    stringRep += openhabBindingServiceConfig.getItemId();
//                    stringRep += "   ";
//
//                    // label
//                    stringRep += "\"" + unitConfig.getLabel() + "\"";
//                    stringRep += "   ";
//
//                    // icon !?
////                    serviceEntry += "<" + unitConfig. + ">";
//                    stringRep += "   ";
//                    // groups
//                    stringRep += "(" + unitConfig.getTemplate().getType().name().toLowerCase() + "," + unitConfig.getPlacementConfig().getLocationConfig().getLabel().toLowerCase() + ")";
//                    stringRep += "   ";
//
//                    // hardware
//                    stringRep += "{ " + openhabBindingServiceConfig.getItemHardwareConfig() + " }";
//                    return s
//    }
    
    private String getCommand(ServiceTypeHolderType.ServiceTypeHolder.ServiceType type) {
        switch (type) {
            case COLOR_SERVICE:
                return "Color";
            case OPENING_RATIO_PROVIDER:
                return "Number";
            case BATTERY_PROVIDER:
            case SHUTTER_SERVICE:
                return "Percent";
            case POWER_SERVICE:
                return "Switch";
            case TEMPERATURE_PROVIDER:
            case MOTION_PROVIDER:
            case TAMPER_PROVIDER:
                return "Number";
            case BRIGHTNESS_PROVIDER:
            case BRIGHTNESS_SERVICE:
            case DIM_PROVIDER:
            case DIM_SERVICE:
                return "Dimmer";
            default:
//                throw new AssertionError("Unkown Service Type: " + type);
                logger.warn("Unkown Service Type: " + type);
                return "";

        }
    }
}
