/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.devices.plugwise;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.PowerStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHardwareController;
import de.citec.dal.hal.al.PowerConsumptionSensorController;
import de.citec.dal.hal.al.PowerPlugController;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.plugwise.PW_PowerPlugType;
import rst.devices.plugwise.PW_PowerPlugType.PW_PowerPlug;

/**
 *
 * @author mpohling
 */
public class PW_PowerPlugController extends AbstractHardwareController<PW_PowerPlug, PW_PowerPlug.Builder> {

    private final static String COMPONENT_POWER_PLUG = "PowerPlug";
    private final static String COMPONENT_POWER_CONSUMPTION_SENSOR = "PowerConsumptionSensor";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PW_PowerPlugType.PW_PowerPlug.getDefaultInstance()));
    }

    private final PowerPlugController powerPlug;
    private final PowerConsumptionSensorController powerConsumptionSensor;

    public PW_PowerPlugController(final String id, final Location location) throws RSBBindingException {
        super(id, location, PW_PowerPlug.newBuilder());

        builder.setId(id);
        this.powerPlug = new PowerPlugController(COMPONENT_POWER_PLUG, this, builder.getPowerPlugBuilder());
        this.powerConsumptionSensor = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR, this, builder.getPowerConsumptionSensorBuilder());
        this.register(powerPlug);
        this.register(powerConsumptionSensor);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_POWER_PLUG, getClass().getMethod("updatePowerPlug", OnOffType.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR, getClass().getMethod("getPowerConsumption"));
    }

    public void updatePowerPlug(OnOffType type) throws RSBBindingException {
        try {
            powerPlug.updatePowerState(PowerStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
    
    public void updatePowerConsumptionSensor(DecimalType value) {
        powerConsumptionSensor.updatePowerConsumption(value.floatValue());
    }
}
