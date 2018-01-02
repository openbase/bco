package org.openbase.bco.manager.agent.core.preset;

/*
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

//package org.openbase.bco.dal.remote.control.agent.preset;
//
//import com.philips.lighting.hue.sdk.PHAccessPoint;
//import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
//import com.philips.lighting.hue.sdk.PHHueSDK;
//import com.philips.lighting.hue.sdk.PHMessageType;
//import com.philips.lighting.hue.sdk.PHSDKListener;
//import com.philips.lighting.model.PHBridge;
//import org.openbase.jul.exception.CouldNotPerformException;
//import java.util.List;
//import rst.domotic.unit.agent.AgentConfigType;
//
///**
// *
// * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
// */
//public class HueTestAgent extends AbstractAgent {
//
//    public HueTestAgent(AgentConfigType.AgentConfig agentConfig) {
//        super(agentConfig);
//    }
//
//    @Override
//    public void activate() throws CouldNotPerformException, InterruptedException {
//        super.activate(); //To change body of generated methods, choose Tools | Templates.
//
//        PHHueSDK phHueSDK = PHHueSDK.getInstance();
//        phHueSDK.setAppName("dal");
//        phHueSDK.setDeviceName("HueTestAgent");
//
//        // Local SDK Listener
//        PHSDKListener listener = new PHSDKListener() {
//
//            @Override
//            public void onAccessPointsFound(List accessPoint) {
//             // Handle your bridge search results here.  Typically if multiple results are returned you will want to display them in a list 
//                // and let the user select their bridge.   If one is found you may opt to connect automatically to that bridge.            
//            }
//
//            @Override
//            public void onCacheUpdated(List cacheNotificationsList, PHBridge bridge) {
//             // Here you receive notifications that the BridgeResource Cache was updated. Use the PHMessageType to   
//                // check which cache was updated, ex.g.
//                if (cacheNotificationsList.contains(PHMessageType.LIGHTS_CACHE_UPDATED)) {
//                    System.out.println("Lights Cache Updated ");
//                }
//            }
//
//            @Override
//            public void onBridgeConnected(PHBridge b, String username) {
//                phHueSDK.setSelectedBridge(b);
//                phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
//            // Here it is recommended to set your connected bridge in your sdk object (as above) and start the heartbeat.
//                // At this point you are connected to a bridge so you should pass control to your main program/activity.
//                // The username is generated randomly by the bridge.
//                // Also it is recommended you store the connected IP Address/ Username in your app here.  This will allow easy automatic connection on subsequent use. 
//            }
//
//            @Override
//            public void onAuthenticationRequired(PHAccessPoint accessPoint) {
//                phHueSDK.startPushlinkAuthentication(accessPoint);
//            // Arriving here indicates that Pushlinking is required (to prove the User has physical access to the bridge).  Typically here
//                // you will display a pushlink image (with a timer) indicating to to the user they need to push the button on their bridge within 30 seconds.
//            }
//
//            @Override
//            public void onConnectionResumed(PHBridge bridge) {
//
//            }
//
//            @Override
//            public void onConnectionLost(PHAccessPoint accessPoint) {
//                // Here you would handle the loss of connection to your bridge.
//            }
//
//            @Override
//            public void onError(int code, final String message) {
//                // Here you can handle events such as Bridge Not Responding, Authentication Failed and Bridge Not Found
//            }
//
//            @Override
//            public void onParsingErrors(List parsingErrorsList) {
//                // Any JSON parsing errors are returned here.  Typically your program should never return these.      
//            }
//        };
//
//        phHueSDK.getNotificationManager().registerSDKListener(listener);
//        
//        PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
//        sm.search(true, true); 
//
//    }
//
//}
