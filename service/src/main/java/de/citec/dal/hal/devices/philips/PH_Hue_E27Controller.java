/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.devices.philips;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.HSVColorTransformer;
import de.citec.dal.data.transform.PowerStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.al.AmbientLightController;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.philips.PH_Hue_GU10Type;
import rst.devices.philips.PH_Hue_GU10Type.PH_Hue_GU10;

/**
 *
 * @author mpohling
 */
public class PH_Hue_E27Controller extends AbstractDeviceController<PH_Hue_GU10, PH_Hue_GU10.Builder> {

    private final static String COMPONENT_AMBIENT_LIGHT = "AmbientLight";
    private final static String COMPONENT_POWER_SWITCH = "PowerSwitch";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PH_Hue_GU10Type.PH_Hue_GU10.getDefaultInstance()));
    }

    private final AmbientLightController ambientLight;

    public PH_Hue_E27Controller(final String id, final String lable, final Location location) throws RSBBindingException {
        super(id, lable, location, PH_Hue_GU10.newBuilder());
        super.builder.setId(id);
        this.ambientLight = new AmbientLightController(COMPONENT_AMBIENT_LIGHT, lable, this, builder.getAmbientLightBuilder());
        this.register(ambientLight);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_AMBIENT_LIGHT, getClass().getMethod("updateAmbientLight", HSBType.class));
        halFunctionMapping.put(COMPONENT_POWER_SWITCH, getClass().getMethod("updatePowerSwitch", OnOffType.class));
    }

    public void updateAmbientLight(final HSBType type) throws RSBBindingException {
        try {
            ambientLight.updateColor(HSVColorTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not updateAmbientLight!", ex);
        }
    }

    public void updatePowerSwitch(final OnOffType type) throws RSBBindingException {
        try {
            ambientLight.updatePowerState(PowerStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
}
