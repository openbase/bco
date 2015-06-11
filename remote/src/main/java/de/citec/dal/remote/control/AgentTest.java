/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import rst.homeautomation.state.PowerStateType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class AgentTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InstantiationException, InterruptedException, CouldNotPerformException, ExecutionException {

        List<HSVColor> colorList = new ArrayList<>();
        colorList.add(HSVColor.newBuilder().setHue(259).setSaturation(100).setValue(100).build());
        colorList.add(HSVColor.newBuilder().setHue(290).setSaturation(100).setValue(100).build());
        colorList.add(HSVColor.newBuilder().setHue(120).setSaturation(100).setValue(100).build());

//        PowerControl powerControl = new PowerControl("Home", PowerStateType.PowerState.State.ON);
//        powerControl.activate();
        ColorControl colorControl = new ColorControl("Living");
        
        colorControl.execute(Color.RED).get();
        colorControl.execute(Color.BLUE).get();
        colorControl.execute(Color.ORANGE).get();
        
//        ColorLoopControl colorControlX = new ColorLoopControl("Home", colorList);
//        colorControlX.activate();
//        ColorLoopControl colorControlXX = new ColorLoopControl("Kitchen", colorList);
//        colorControlXX.activate();
//        ColorLoopControl colorControl2 = new ColorLoopControl("Kitchen", colorList);
//        colorControl2.activate();
//        ColorLoopControl colorControl3 = new ColorLoopControl("Living", colorList);
//        colorControl3.activate();
//        ColorLoopControl colorControl4 = new ColorLoopControl("Bath", colorList);
//        colorControl4.activate();
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

    }

}
