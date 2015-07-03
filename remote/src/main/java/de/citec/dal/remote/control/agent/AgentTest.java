/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent;

import de.citec.dal.remote.control.ColorLoopControl;
import de.citec.dal.remote.control.PowerControl;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import rst.homeautomation.state.PowerStateType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class AgentTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InstantiationException, InterruptedException, CouldNotPerformException, ExecutionException {

        List<HSVColor> colorList = new ArrayList<>();
        colorList.add(HSVColor.newBuilder().setHue(0).setSaturation(100).setValue(100).build());
        colorList.add(HSVColor.newBuilder().setHue(300).setSaturation(100).setValue(100).build());
        colorList.add(HSVColor.newBuilder().setHue(73).setSaturation(100).setValue(100).build());

        PowerControl powerControl = new PowerControl("Home", PowerStateType.PowerState.State.ON);
        powerControl.activate();
//        ColorControl colorControl = new ColorControl("Home");
//
//        while (true) {
//            System.out.println("RED");
//            colorControl.execute(Color.RED).get();
//            System.out.println("BLUE");
//            colorControl.execute(Color.BLUE).get();
//            System.out.println("YELLOW");
//            colorControl.execute(Color.YELLOW).get();
//            Thread.sleep(30000);
//        }

        ColorLoopControl colorControlX = new ColorLoopControl("Home", colorList, 1000);
        colorControlX.activate();
        ColorLoopControl colorControlXX = new ColorLoopControl("Kitchen", colorList, 1000);
        colorControlXX.activate();
        ColorLoopControl colorControl2 = new ColorLoopControl("Kitchen", colorList, 1000);
        colorControl2.activate();
        ColorLoopControl colorControl3 = new ColorLoopControl("Living", colorList, 1000);
        colorControl3.activate();
        ColorLoopControl colorControl4 = new ColorLoopControl("Bath", colorList, 1000);
        colorControl4.activate();
//        ColorLoopControl colorControlC = new ColorLoopControl("Control", colorList);
//        colorControlC.activate();
//        ColorControl colorControl3 = new ColorControl("Kitchen", colorList);
//        colorControl3.activate();
//        ColorControl colorControl4 = new ColorControl("Bath", colorList);
//        colorControl4.activate();
//        ColorControl colorControl5 = new ColorControl("Living", colorList);
//        colorControl5.activate();
//        ColorControl colorControl6 = new ColorControl("Control", colorList);
//        colorControl6.activate();
//        Thread.sleep(60000);
        
        
//        PowerServiceControl powerServiceControl = new PowerServiceControl("Home", PowerStateType.PowerState.State.OFF);
//        powerServiceControl.activate();
    }

}
