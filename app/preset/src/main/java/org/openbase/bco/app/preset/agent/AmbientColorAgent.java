package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.service.ColorStateServiceRemote;
import org.openbase.bco.dal.control.layer.unit.agent.AbstractAgentController;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.vision.HSBColorType.HSBColor;
import retrofit2.Callback;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
@Deprecated
public class AmbientColorAgent extends AbstractAgentController {

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
        ALL
    }

    private ColoringStrategy strategy;
    private Thread thread;
    private long holdingTime;
    private final List<ColorableLightRemote> colorableLightRemotes = new ArrayList<>();
    private final List<HSBColor> colors = new ArrayList<>();
    private final Random random;

    public AmbientColorAgent() throws InterruptedException, CouldNotPerformException {
        random = new Random();

        System.out.println("construct color agent");
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            System.out.println("init color agent");
            super.init(config);
            System.out.println("2 color agent");
            Registries.getUnitRegistry().waitForData();

            System.out.println("3 color agent");
            // load remotes
            final LocationRemote locationRemote = Units.getUnit(config.getPlacementConfig().getLocationId(), true, Units.LOCATION);
            colorableLightRemotes.clear();
            colorableLightRemotes.addAll(locationRemote.getUnits(UnitType.COLORABLE_LIGHT, false, Units.COLORABLE_LIGHT, true));

            System.out.println("4 color agent");

            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("AmbientColorAgent", config.getMetaConfig());

            // load colors
            int i = 1;
            String colorString;
            try {
                while (!(colorString = configVariableProvider.getValue(COLOR_KEY + "_" + i)).isEmpty()) {
                    logger.debug("Found color [" + colorString + "] with key [" + COLOR_KEY + "_" + i + "]");
                    String[] split = colorString.split(SEPERATOR);
                    double hue = Double.parseDouble(split[0]);
                    double saturation = Double.parseDouble(split[1]);
                    double brightness = Double.parseDouble(split[2]);
                    colors.add(HSBColor.newBuilder().setHue(hue).setSaturation(saturation).setBrightness(brightness).build());
                    i++;
                }
            } catch (NotAvailableException ex) {
                i--;
                logger.debug("Found [" + i + "] color/s");
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Error while parsing color. Use following patter [KEY,VALUE] => [" + COLOR_KEY + ",<hue>;<saturation>;<brightness>]", ex), logger, LogLevel.WARN);
            }

            if (colors.isEmpty()) {
                colors.add(HSBColor.newBuilder().setHue(0).setSaturation(1d).setBrightness(0.5d).build());
                colors.add(HSBColor.newBuilder().setHue(50).setSaturation(1d).setBrightness(0.5d).build());
                colors.add(HSBColor.newBuilder().setHue(100).setSaturation(1d).setBrightness(0.5d).build());
                colors.add(HSBColor.newBuilder().setHue(150).setSaturation(1d).setBrightness(0.5d).build());
                colors.add(HSBColor.newBuilder().setHue(200).setSaturation(1d).setBrightness(0.5d).build());
                colors.add(HSBColor.newBuilder().setHue(250).setSaturation(1d).setBrightness(0.5d).build());
                colors.add(HSBColor.newBuilder().setHue(300).setSaturation(1d).setBrightness(0.5d).build());
            }

            System.out.println("5 color agent");
            holdingTime = Long.parseLong(configVariableProvider.getValue(HOLDING_TIME_KEY, "500"));
            strategy = ColoringStrategy.valueOf(configVariableProvider.getValue(STRATEGY_KEY, "ONE"));

            System.out.println("init color agent done");

        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public ActionDescription execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        System.out.println("execute color agent");
        initColorStates();
        setExecutionThread(() -> new RemoteAction(activationState.getResponsibleAction()).isValid());
        thread.start();
        return activationState.getResponsibleAction();
    }

    @Override
    public void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        if (thread != null) {
            thread.interrupt();
            thread.join(10000);
            if (thread.isAlive()) {
                throw new CouldNotPerformException("Could not stop " + this + "!");
            }
        }
        super.stop(activationState);
    }

    private void initColorStates() throws CouldNotPerformException {
        HSBColor color;
        for (ColorableLightRemote colorRemote : colorableLightRemotes) {
            if (!colorRemote.isDataAvailable()) {
                continue;
            }
            if (!colors.contains(colorRemote.getColorState().getColor().getHsbColor())) {
                color = colors.get(random.nextInt(colors.size()));
                System.out.println("set color");
                observe(colorRemote.setColor(color, getDefaultActionParameter(60000)));
            }
        }
    }

    private void setExecutionThread(final Callable<Boolean> validation) {
        switch (strategy) {
            case ALL:
                thread = new AllStrategyThread(validation);
                break;
            case ONE:
            default:
                thread = new OneStrategyThread(validation);
                break;
        }
    }

    public String stringHSV(HSBColor color) {
        return color.getHue() + SEPERATOR + color.getSaturation() + SEPERATOR + color.getBrightness();
    }

    private class AllStrategyThread extends Thread {

        private final Callable<Boolean> validation;

        AllStrategyThread(final Callable<Boolean> validation) {
            this.validation = validation;
        }

        @Override
        public void run() {
            try {
                if (colorableLightRemotes.isEmpty()) {
                    throw new InvalidStateException("No service remote available!");
                }
                final long delay = holdingTime / colorableLightRemotes.size();
                while (validation.call() && !Thread.interrupted()) {
                    for (ColorableLightRemote colorRemote : colorableLightRemotes) {
                        try {
                            observe(colorRemote.setColor(choseDifferentElem(colors, colorRemote.getHSBColor()), getDefaultActionParameter()));
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

        private final Callable<Boolean> validation;

        OneStrategyThread(final Callable<Boolean> validation) {
            this.validation = validation;
        }

        @Override
        public void run() {
            ColorableLightRemote remote = null;
            try {
                if (colorableLightRemotes.isEmpty()) {
                    throw new InvalidStateException("No service remote available!");
                }

                System.out.println("start main thread color agent");
                while (validation.call() && !Thread.interrupted()) {
                    try {
                        remote = choseDifferentElem(colorableLightRemotes, remote);
                        remote.setColor(choseDifferentElem(colors, remote.getHSBColor()));
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not set/get color of [" + remote + "]", ex), logger);
                    }
                    Thread.sleep(holdingTime);
                }
                logger.debug("Execution thread finished.");
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
     * @param <T>         the type of list elements
     * @param list        the list containing elements of type T
     * @param currentElem the currently hold element out of the list
     *
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
