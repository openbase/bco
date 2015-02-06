/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.philips;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.HSVColorTransformer;
import de.citec.dal.data.transform.PowerStateTransformer;
import de.citec.dal.exception.DALException;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.unit.AmbientLightController;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.philips.PH_Hue_GU10Type;
import rst.devices.philips.PH_Hue_GU10Type.PH_Hue_GU10;
import rst.homeautomation.openhab.HSBType.HSB;
import rst.homeautomation.openhab.OnOffHolderType.OnOffHolder.OnOff;

/**
 *
 * @author mpohling
 */
public class PH_Hue_GU10Controller extends AbstractDeviceController<PH_Hue_GU10, PH_Hue_GU10.Builder> {

    private final static String COMPONENT_AMBIENT_LIGHT = "AmbientLight";
    private final static String COMPONENT_POWER_SWITCH = "PowerSwitch";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PH_Hue_GU10Type.PH_Hue_GU10.getDefaultInstance()));
    }

    private final AmbientLightController ambientLight;

    public PH_Hue_GU10Controller(final String id, final String label,final Location location) throws VerificationFailedException, DALException {
        super(id, label,location, PH_Hue_GU10.newBuilder());
        super.data.setId(id);
        this.ambientLight = new AmbientLightController(COMPONENT_AMBIENT_LIGHT, label,this, data.getAmbientLightBuilder());
        this.register(ambientLight);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_AMBIENT_LIGHT, getClass().getMethod("updateAmbientLight", HSB.class));
        halFunctionMapping.put(COMPONENT_POWER_SWITCH, getClass().getMethod("updatePowerSwitch", OnOff.class));
    }

    public void updateAmbientLight(final HSB type) throws RSBBindingException {
        try {
            ambientLight.updateColor(HSVColorTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not updateAmbientLight!", ex);
        }
    }

    public void updatePowerSwitch(final OnOff type) throws RSBBindingException {
        try {
            ambientLight.updatePowerState(PowerStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
}
