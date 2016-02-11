package org.dc.bco.manager.agent.core.preset;

/*
 * #%L
 * COMA AgentManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.pattern.Observable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dc.bco.dal.lib.layer.service.provider.BrightnessProvider;
import org.dc.bco.dal.remote.unit.BrightnessSensorRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.unit.BrightnessSensorType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class BrightnessStateFutionProvider extends Observable<Double> implements BrightnessProvider {
//
//    /**
//     * Default 3 minute window of no movement unit the state switches to NO_MOTION.
//     */
//    public static final long MOTION_TIMEOUT = 900;

    /**
     * Measurement time window within the max, min and average updates are performed.
     */
    public static final long DEFAULT_MEASUREMENT_TIME_WINDOW = 900;

    public static final double UNKNOWN_VALUE = -1;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Double brightnessMinState, brightnessMaxState, brightnessAverageState;
    /**
     * Contains pairs of timestamp as key and a brightness value as entry.
     */
    private final Map<Long, Double> brightnessLastStates;
    private final long measurementTimeWindow;

    private final List<BrightnessSensorRemote> brightnessSensorList;

    public BrightnessStateFutionProvider(Collection<UnitConfig> brightnessUnitConfigs) throws InstantiationException, InterruptedException {
        this(brightnessUnitConfigs, DEFAULT_MEASUREMENT_TIME_WINDOW);
    }

    public BrightnessStateFutionProvider(final Collection<UnitConfig> brightnessUnitConfigs, final long measurementTimeWindow) throws InstantiationException, InterruptedException {
        try {
            this.brightnessSensorList = new ArrayList<>();
            this.brightnessLastStates = new HashMap<>();
            this.measurementTimeWindow = measurementTimeWindow;
            this.brightnessMinState = UNKNOWN_VALUE;
            this.brightnessMaxState = UNKNOWN_VALUE;
            this.brightnessAverageState = UNKNOWN_VALUE;

//            this.brightnessTimeout = new Timeout(brightnessTimeout) {
//
//                @Override
//                public void expired() {
//                    updateBrightnessState(Brightnes.BrightnessState.newBuilder().setValue(BrightnessStateType.BrightnessState.State.NO_MOVEMENT));
//                }
//            };
            BrightnessSensorRemote brightnessSensorRemote;
            for (UnitConfigType.UnitConfig unitConfig : brightnessUnitConfigs) {
                if (unitConfig.getType() != UnitTemplateType.UnitTemplate.UnitType.MOTION_SENSOR) {
                    logger.warn("Skip Unit[" + unitConfig.getId() + "] because its not of Type[" + UnitTemplateType.UnitTemplate.UnitType.MOTION_SENSOR + "]!");
                    continue;
                }

                brightnessSensorRemote = new BrightnessSensorRemote();
                brightnessSensorRemote.init(unitConfig);
                brightnessSensorList.add(brightnessSensorRemote);
                brightnessSensorRemote.addObserver((Observable<BrightnessSensorType.BrightnessSensor> source, BrightnessSensorType.BrightnessSensor data) -> {
                    updateBrightnessState(data.getBrightness());
                });
            }
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private synchronized void updateBrightnessState(final Double brightnessState) {
//        Long currentTimeMillis = System.currentTimeMillis();
//
//        if(brightnessState <= UNKNOWN_VALUE) {
//            throw new VerificationFailedException("Given brightness Value["+brightnessState+"] is invalid!");
//        }
//        // save value
//        brightnessLastStates.put(currentTimeMillis, brightnessState);
//
//
    }

    @Override
    public Double getBrightness() throws CouldNotPerformException {
        try {
            if (brightnessAverageState == UNKNOWN_VALUE) {
                if (brightnessLastStates.isEmpty()) {
                    throw new NotAvailableException("brightness");
                }
                return brightnessLastStates.values().stream().findFirst().get();
            }
            return brightnessAverageState;
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not return brightness!", ex);
        }
    }

    public Double getBrightnessMinState() {
        return brightnessMinState;
    }

    public Double getBrightnessMaxState() {
        return brightnessMaxState;
    }

    public Double getBrightnessAverageState() {
        return brightnessAverageState;
    }

    public Map<Long, Double> getBrightnessLastStates() {
        return brightnessLastStates;
    }

    public long getMeasurementTimeWindow() {
        return measurementTimeWindow;
    }

    public List<BrightnessSensorRemote> getBrightnessSensorList() {
        return brightnessSensorList;
    }
}
