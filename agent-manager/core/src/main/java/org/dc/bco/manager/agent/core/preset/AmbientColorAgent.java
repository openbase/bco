/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core.preset;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.dc.bco.dal.remote.service.ColorServiceRemote;
import org.dc.bco.manager.agent.core.AbstractAgent;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.rst.processing.MetaConfigVariableProvider;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine
 * Threepwood</a>
 */
public class AmbientColorAgent extends AbstractAgent {

//    List<HSVColorType.HSVColor> colorList = new ArrayList<>();
//        colorList.add(HSVColorType.HSVColor.newBuilder().setHue(0).setSaturation(100).setValue(20).build());
//        colorList.add(HSVColorType.HSVColor.newBuilder().setHue(300).setSaturation(100).setValue(20).build());
//        colorList.add(HSVColorType.HSVColor.newBuilder().setHue(256).setSaturation(100).setValue(20).build());
//
//        PowerControl powerControl = new PowerControl("Chillerstrasse", PowerStateType.PowerState.State.ON);
//        powerControl.activate();
////        ColorControl colorControl = new ColorControl("Home");
////
////        while (true) {
////            System.out.println("RED");
////            colorControl.execute(Color.RED).get();
////            System.out.println("BLUE");
////            colorControl.execute(Color.BLUE).get();
////            System.out.println("YELLOW");
////            colorControl.execute(Color.YELLOW).get();
////            Thread.sleep(30000);
////        }
//
//        ColorLoopControl colorControlX = new ColorLoopControl("Chillerstrasse", colorList, 1000);
//        colorControlX.activate();
//        ColorLoopControl colorControlXX = new ColorLoopControl("Kueche", colorList, 1000);
//        colorControlXX.activate();
//        ColorLoopControl colorControl2 = new ColorLoopControl("Kueche", colorList, 1000);
//        colorControl2.activate();
//        ColorLoopControl colorControl3 = new ColorLoopControl("Wohnzimmer", colorList, 1000);
//        colorControl3.activate();
//        ColorLoopControl colorControl4 = new ColorLoopControl("Bad", colorList, 1000);
//        colorControl4.activate();
////        ColorLoopControl colorControlC = new ColorLoopControl("Control", colorList);
////        colorControlC.activate();
////        ColorControl colorControl3 = new ColorControl("Kitchen", colorList);
////        colorControl3.activate();
////        ColorControl colorControl4 = new ColorControl("Bath", colorList);
////        colorControl4.activate();
////        ColorControl colorControl5 = new ColorControl("Living", colorList);
////        colorControl5.activate();
////        ColorControl colorControl6 = new ColorControl("Control", colorList);
////        colorControl6.activate();
////        Thread.sleep(60000);
//
////        PowerServiceControl powerServiceControl = new PowerServiceControl("Home", PowerStateType.PowerState.State.OFF);
////        powerServiceControl.activate();
//    private static int globalId = 1;
//    private final int localId;
    /**
     * Key to identify a color from the meta configuration.
     */
    private static final String COLOR_KEY = "COLOR";
    /**
     * Key to identify a unit from the meta configuration.
     */
    private static final String UNIT_KEY = "UNIT";
    /**
     * Key to identify the holding time from the meta configuration.
     */
    private static final String HOLDING_TIME_KEY = "HOLDING_TIME";
    /**
     * Key to identify the strategy from the meta configuration.
     */
    private static final String STRATEGY_KEY = "STRATEGY";
    /**
     * Separator to get the hue,saturation and brightness values out of one
     * color string.
     */
    private static final String SEPERATOR = ";";

    /**
     * The strategy how the agent will change the color of the lights.
     */
    public enum ColoringStrategy {

        /**
         * After the holding time one random light which differs from the last
         * will be changed to a random different color.
         */
        ONE,
        /**
         * After the holding time all lights are change their color to a random
         * different one.
         */
        ALL;
    }

    private ColoringStrategy strategy;
    private Thread thread;
    private long holdingTime;
    private final List<ColorServiceRemote> colorRemotes = new ArrayList<>();
    private final List<HSVColor> colors = new ArrayList<>();
    private final Random random;

    public AmbientColorAgent() throws InstantiationException, InterruptedException, CouldNotPerformException {
        super(false);
        logger.info("Creating AmbienColorAgent");
        random = new Random();
    }

    @Override
    public void init(final AgentConfigType.AgentConfig config) throws InitializationException, InterruptedException {
        try {
            super.init(config);

            DeviceRegistryRemote deviceRegistryRemote = new DeviceRegistryRemote();
            deviceRegistryRemote.init();
            deviceRegistryRemote.activate();

            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("AmbientColorAgent", config.getMetaConfig());

            int i = 1;
            String unitId;
            try {
                while (!(unitId = configVariableProvider.getValue(UNIT_KEY + "_" + i)).isEmpty()) {
                    logger.info("Found unit id [" + unitId + "] with key [" + UNIT_KEY + "_" + i + "]");
                    ColorServiceRemote remote = new ColorServiceRemote();
                    remote.init(deviceRegistryRemote.getUnitConfigById(unitId));
                    colorRemotes.add(remote);
                    i++;
                }
            } catch (NotAvailableException ex) {
                i--;
                logger.info("Found [" + i + "] unit/s");
            }
            i = 1;
            String colorString;
            try {
                while (!(colorString = configVariableProvider.getValue(COLOR_KEY + "_" + i)).isEmpty()) {
                    logger.info("Found color [" + colorString + "] with key [" + COLOR_KEY + "_" + i + "]");
                    String[] split = colorString.split(SEPERATOR);
                    double hue = Double.parseDouble(split[0]);
                    double saturation = Double.parseDouble(split[1]);
                    double brightness = Double.parseDouble(split[2]);
                    colors.add(HSVColor.newBuilder().setHue(hue).setSaturation(saturation).setValue(brightness).build());
                    i++;
                }
            } catch (NotAvailableException ex) {
                i--;
                logger.info("Found [" + i + "] color/s");
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                logger.warn("Error while parsing color. Use following patter [KEY,VALUE] => [" + COLOR_KEY + ",<hue>;<saturation>;<brightness>]", ex);
            }

            holdingTime = Long.parseLong(configVariableProvider.getValue(HOLDING_TIME_KEY));
            strategy = ColoringStrategy.valueOf(configVariableProvider.getValue(STRATEGY_KEY));
            deviceRegistryRemote.shutdown();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getClass().getSimpleName() + "]");
        for (ColorServiceRemote colorRemote : colorRemotes) {
            colorRemote.activate();
        }
        super.activate();
    }

    @Override
    public void execute() throws CouldNotPerformException {
        initColorStates();
        setExecutionThread();
        thread.start();
    }

    @Override
    public void stop() throws CouldNotPerformException, InterruptedException {
        thread.interrupt();
        thread.join(10000);
        if (thread.isAlive()) {
            throw new CouldNotPerformException("Fatal error: Could not stop " + this + "!");
        }
    }

    private void initColorStates() throws CouldNotPerformException {
        HSVColor color;
        for (ColorServiceRemote colorRemote : colorRemotes) {
            if (!colors.contains(colorRemote.getColor())) {
                color = colors.get(random.nextInt(colors.size()));
                colorRemote.setColor(color);
            }
        }
    }

    private void setExecutionThread() {
        switch (strategy) {
        case ALL:
            thread = new AllStrategyThread();
            break;
        case ONE:
            thread = new OneStrategyThread();
            break;
        default:
            thread = new OneStrategyThread();
        }
    }

    public String stringHSV(HSVColor color) {
        return color.getHue() + SEPERATOR + color.getSaturation() + SEPERATOR + color.getValue();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        super.deactivate();
        for (ColorServiceRemote colorRemote : colorRemotes) {
            colorRemote.deactivate();
        }
    }

    private class AllStrategyThread extends Thread {

        @Override
        public void run() {
            try {
                if (colorRemotes.isEmpty()) {
                    throw new InvalidStateException("No service remote available!");
                }
                final long delay = holdingTime / colorRemotes.size();
                while (executing && Thread.interrupted()) {
                    for (ColorServiceRemote colorRemote : colorRemotes) {
                        try {
                            colorRemote.setColor(choseDifferentElem(colors, colorRemote.getColor()));
                        } catch (CouldNotPerformException ex) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not set/get color of [" + colorRemote.getClass().getName() + "]", ex), logger);
                        }
                        Thread.sleep(delay);
                    }
                }
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Execution thread canceled!", ex), logger);
            }
        }
    }

    private class OneStrategyThread extends Thread {

        @Override
        public void run() {
            ColorServiceRemote remote = null;
            try {
                if (colorRemotes.isEmpty()) {
                    throw new InvalidStateException("No service remote available!");
                }

                while (executing && Thread.interrupted()) {
                    try {
                        choseDifferentElem(colorRemotes, remote).setColor(choseDifferentElem(colors, remote.getColor()));
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not set/get color of [" + remote + "]", ex), logger);
                    }
                    Thread.sleep(holdingTime);
                }
                logger.info("Execution thread finished.");
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Execution thread canceled!", ex), logger);
            }
        }
    }

    /**
     * Get a random element out of the list that differs from currentElem. If
     * the list only contains one element then this element is returned despite
     * being possibly the same as currentElem. If currentElem is not contained
     * by the list a random element from the list is returned.
     *
     * @param <T> the type of list elements
     * @param list the list containing elements of type T
     * @param currentElem the currently hold element out of the list
     * @return a different element from the list than currentElem
     */
    private <T> T choseDifferentElem(List<T> list, T currentElem) {
        if (currentElem == null || list.size() == 1) {
            return list.get(0);
        }

        int oldIndex = list.indexOf(currentElem);
        if (oldIndex == -1) {
            return list.get(random.nextInt(list.size()));
        }

        int newIndex = random.nextInt(list.size());
        while (newIndex == oldIndex) {
            newIndex = random.nextInt(list.size());
        }
        return list.get(newIndex);
    }
}

//TODO Tamino: hierüber müssen wir unbedingt mal reden :D
//                    if (lastModification + holdingTime < System.currentTimeMillis()) {
//                        for (ColorServiceRemote colorRemote : colorRemotes) {
//                            try {
//                                colorRemote.setColor(choseDifferentElem(colors, colorRemote.getColor()));
//                            } catch (CouldNotPerformException ex) {
//                                logger.warn("Could not set/get color of [" + colorRemote.getClass().getName() + "]", ex);
//                            }
//                        }
//                        lastModification = System.currentTimeMillis();
//                    }
//                    Thread.yield();
