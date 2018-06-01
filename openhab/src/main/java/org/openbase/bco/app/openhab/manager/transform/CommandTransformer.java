package org.openbase.bco.app.openhab.manager.transform;

import com.google.protobuf.Message;
import org.eclipse.smarthome.core.library.types.*;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.concurrent.TimeUnit;

public class CommandTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandTransformer.class);

    public static Message getServiceData(final String state, ServiceType serviceType) throws CouldNotPerformException {
        Message serviceData;

        try {
            switch (serviceType) {
                case COLOR_STATE_SERVICE:
                    // will receive OnOff, IncreaseDecrease, Percent, HSB
                    // ignore everything that is not hsb
                    try {
                        serviceData = ColorStateTransformer.transform(HSBType.valueOf(state));
                    } catch (IllegalArgumentException ex) {
                        LOGGER.debug("Ignore color state service update[" + state + "] because it is not a valid HSBType");
                        return null;
                    }
                    break;
                case POWER_STATE_SERVICE:
                    serviceData = PowerStateTransformer.transform(OnOffType.valueOf(state));
                    break;
                case BRIGHTNESS_STATE_SERVICE:
                    // will receive OnOff, IncreaseDecrease, Percent
                    // ignore everything that is not percent
                    try {
                        serviceData = BrightnessStateTransformer.transform(PercentType.valueOf(state));
                    } catch (IllegalArgumentException ex) {
                        LOGGER.debug("Ignore brightness state service update[" + state + "] because it is not a valid PercentType");
                        return null;
                    }
                    break;
                case POWER_CONSUMPTION_STATE_SERVICE:
                    serviceData = PowerConsumptionStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case TEMPERATURE_STATE_SERVICE:
                    serviceData = TemperatureStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case MOTION_STATE_SERVICE:
                    serviceData = MotionStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case TAMPER_STATE_SERVICE:
                    serviceData = TamperStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case BATTERY_STATE_SERVICE:
                    serviceData = BatteryStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case SMOKE_ALARM_STATE_SERVICE:
                    serviceData = AlarmStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case SMOKE_STATE_SERVICE:
                    serviceData = SmokeStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case TEMPERATURE_ALARM_STATE_SERVICE:
                    serviceData = AlarmStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case TARGET_TEMPERATURE_STATE_SERVICE:
                    serviceData = TemperatureStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case ILLUMINANCE_STATE_SERVICE:
                    serviceData = IlluminanceStateTransformer.transform(DecimalType.valueOf(state));
                    break;
                case BLIND_STATE_SERVICE:
                    // will receive UpDown, StopMove, Percent
                    try {
                        serviceData = UpDownStateTransformer.transform(UpDownType.valueOf(state));
                    } catch (IllegalArgumentException ex) {
                        try {
                            serviceData = StopMoveStateTransformer.transform(StopMoveType.valueOf(state));
                        } catch (IllegalArgumentException exx) {
                            try {
                                serviceData = OpeningRatioTransformer.transform(PercentType.valueOf(state));
                            } catch (IllegalArgumentException exxx) {
                                throw new CouldNotPerformException("Could parse[" + state + "] as UpDown, StopMove, or Percent for roller shutter");
                            }
                        }
                    }
                    break;
                case BUTTON_STATE_SERVICE:
                    serviceData = ButtonStateTransformer.transform(OnOffType.valueOf(state));
                    break;
                case CONTACT_STATE_SERVICE:
                    serviceData = OpenClosedStateTransformer.transform(OpenClosedType.valueOf(state));
                    break;
                case HANDLE_STATE_SERVICE:
                    serviceData = HandleStateTransformer.transform(StringType.valueOf(state));
                    break;
                case STANDBY_STATE_SERVICE:
                    serviceData = StandbyStateTransformer.transform(OnOffType.valueOf(state));
                default:
                    throw new CouldNotTransformException("Unknown how to transform[" + state + "] into serviceType[" + serviceType + "].");
            }
        } catch (IllegalArgumentException ex) {
            throw new CouldNotPerformException("Could not parse stateString[" + state + "] for serviceType[" + serviceType + "]");
        }

        return TimestampProcessor.updateTimestamp(System.currentTimeMillis(), serviceData, TimeUnit.MICROSECONDS);
    }
}
