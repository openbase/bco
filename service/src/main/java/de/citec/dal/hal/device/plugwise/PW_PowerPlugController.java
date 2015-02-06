/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.plugwise;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.data.transform.PowerStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.unit.PowerConsumptionSensorController;
import de.citec.dal.hal.unit.PowerPlugController;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.plugwise.PW_PowerPlugType;
import rst.devices.plugwise.PW_PowerPlugType.PW_PowerPlug;
import rst.homeautomation.openhab.OnOffHolderType.OnOffHolder.OnOff;

/**
 *
 * @author mpohling
 */
public class PW_PowerPlugController extends AbstractOpenHABDeviceController<PW_PowerPlug, PW_PowerPlug.Builder> {

    private final static String COMPONENT_POWER_PLUG = "PowerPlug";
    private final static String COMPONENT_POWER_CONSUMPTION = "PowerConsumption";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PW_PowerPlugType.PW_PowerPlug.getDefaultInstance()));
    }

    private final PowerPlugController powerPlug;
    private final PowerConsumptionSensorController powerConsumption;

    public PW_PowerPlugController(final String id, final String label, final Location location) throws VerificationFailedException, InstantiationException {
        super(id, label, location, PW_PowerPlug.newBuilder());
//        builder.setId(id); //TODO still useful or already setuped in super class?
        this.powerPlug = new PowerPlugController(COMPONENT_POWER_PLUG, label, this, data.getPowerPlugBuilder());
        this.powerConsumption = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION, label, this, data.getPowerConsumptionBuilder());
        this.register(powerPlug);
        this.register(powerConsumption);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_POWER_PLUG, getClass().getMethod("updatePowerPlug", OnOff.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION, getClass().getMethod("updatePowerConsumption", double.class));
    }

    public void updatePowerPlug(OnOff type) throws RSBBindingException {
        try {
            powerPlug.updatePowerState(PowerStateTransformer.transform(type));
        } catch (CouldNotTransformException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }

    public void updatePowerConsumption(double value) {
        powerConsumption.updatePowerConsumption((float) value);
    }
}
