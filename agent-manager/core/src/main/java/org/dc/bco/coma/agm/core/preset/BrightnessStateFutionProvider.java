package org.dc.bco.coma.agm.core.preset;

import de.citec.dal.hal.provider.BrightnessProvider;
import de.citec.dal.hal.service.ServiceType;
import de.citec.dal.remote.unit.BrightnessSensorRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.pattern.Observable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceConfigType;
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

    public BrightnessStateFutionProvider(Collection<UnitConfig> brightnessUnitConfigs) throws InstantiationException {
        this(brightnessUnitConfigs, DEFAULT_MEASUREMENT_TIME_WINDOW);
    }

    public BrightnessStateFutionProvider(final Collection<UnitConfig> brightnessUnitConfigs, final long measurementTimeWindow) throws InstantiationException {
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

    @Override
    public ServiceType getServiceType() {
        return ServiceType.BRIGHTNESS;
    }

    @Override
    public ServiceConfigType.ServiceConfig getServiceConfig() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
