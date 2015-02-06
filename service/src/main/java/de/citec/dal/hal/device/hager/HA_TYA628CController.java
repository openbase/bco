/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.hager;

import de.citec.dal.data.Location;
import de.citec.dal.exception.DALException;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.unit.RollershutterController;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.hager.HA_TYA628CType;

/**
 *
 * @author mpohling
 */
public class HA_TYA628CController extends AbstractDeviceController<HA_TYA628CType.HA_TYA628C, HA_TYA628CType.HA_TYA628C.Builder> {

    private final static String COMPONENT_ROLLERSHUTTER_0 = "Rollershutter_0";
    private final static String COMPONENT_ROLLERSHUTTER_1 = "Rollershutter_1";
    private final static String COMPONENT_ROLLERSHUTTER_2 = "Rollershutter_2";
    private final static String COMPONENT_ROLLERSHUTTER_3 = "Rollershutter_3";
    private final static String COMPONENT_ROLLERSHUTTER_4 = "Rollershutter_4";
    private final static String COMPONENT_ROLLERSHUTTER_5 = "Rollershutter_5";
    private final static String COMPONENT_ROLLERSHUTTER_6 = "Rollershutter_6";
    private final static String COMPONENT_ROLLERSHUTTER_7 = "Rollershutter_7";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HA_TYA628CType.HA_TYA628C.getDefaultInstance()));
    }

    private final RollershutterController rollershutter_0;
    private final RollershutterController rollershutter_1;
    private final RollershutterController rollershutter_2;
    private final RollershutterController rollershutter_3;
    private final RollershutterController rollershutter_4;
    private final RollershutterController rollershutter_5;
    private final RollershutterController rollershutter_6;
    private final RollershutterController rollershutter_7;

    public HA_TYA628CController(final String id, final String label, final String[] unitLabel, final Location location) throws VerificationFailedException, DALException {
        super(id, label, location, HA_TYA628CType.HA_TYA628C.newBuilder());

        data.setId(id);
        this.rollershutter_0 = new RollershutterController(COMPONENT_ROLLERSHUTTER_0, unitLabel[0], this, data.getRollershutter0Builder());
        this.rollershutter_1 = new RollershutterController(COMPONENT_ROLLERSHUTTER_1, unitLabel[1], this, data.getRollershutter1Builder());
        this.rollershutter_2 = new RollershutterController(COMPONENT_ROLLERSHUTTER_2, unitLabel[2], this, data.getRollershutter2Builder());
        this.rollershutter_3 = new RollershutterController(COMPONENT_ROLLERSHUTTER_3, unitLabel[3], this, data.getRollershutter3Builder());
        this.rollershutter_4 = new RollershutterController(COMPONENT_ROLLERSHUTTER_4, unitLabel[4], this, data.getRollershutter4Builder());
        this.rollershutter_5 = new RollershutterController(COMPONENT_ROLLERSHUTTER_5, unitLabel[5], this, data.getRollershutter5Builder());
        this.rollershutter_6 = new RollershutterController(COMPONENT_ROLLERSHUTTER_6, unitLabel[6], this, data.getRollershutter6Builder());
        this.rollershutter_7 = new RollershutterController(COMPONENT_ROLLERSHUTTER_7, unitLabel[7], this, data.getRollershutter7Builder());
        this.register(rollershutter_0);
        this.register(rollershutter_1);
        this.register(rollershutter_2);
        this.register(rollershutter_3);
        this.register(rollershutter_4);
        this.register(rollershutter_5);
        this.register(rollershutter_6);
        this.register(rollershutter_7);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_ROLLERSHUTTER_0, getClass().getMethod("updateRollershutter_0", double.class));
        halFunctionMapping.put(COMPONENT_ROLLERSHUTTER_1, getClass().getMethod("updateRollershutter_1", double.class));
        halFunctionMapping.put(COMPONENT_ROLLERSHUTTER_2, getClass().getMethod("updateRollershutter_2", double.class));
        halFunctionMapping.put(COMPONENT_ROLLERSHUTTER_3, getClass().getMethod("updateRollershutter_3", double.class));
        halFunctionMapping.put(COMPONENT_ROLLERSHUTTER_4, getClass().getMethod("updateRollershutter_4", double.class));
        halFunctionMapping.put(COMPONENT_ROLLERSHUTTER_5, getClass().getMethod("updateRollershutter_5", double.class));
        halFunctionMapping.put(COMPONENT_ROLLERSHUTTER_6, getClass().getMethod("updateRollershutter_6", double.class));
        halFunctionMapping.put(COMPONENT_ROLLERSHUTTER_7, getClass().getMethod("updateRollershutter_7", double.class));
    }

    public void updateRollershutter_0(double type) throws RSBBindingException {
        rollershutter_0.updatePosition((float) type);
    }

    public void updateRollershutter_1(double type) throws RSBBindingException {
        rollershutter_1.updatePosition((float) type);
    }

    public void updateRollershutter_2(double type) throws RSBBindingException {
        rollershutter_2.updatePosition((float) type);
    }

    public void updateRollershutter_3(double type) throws RSBBindingException {
        rollershutter_3.updatePosition((float) type);
    }

    public void updateRollershutter_4(double type) throws RSBBindingException {
        rollershutter_4.updatePosition((float) type);
    }

    public void updateRollershutter_5(double type) throws RSBBindingException {
        rollershutter_5.updatePosition((float) type);
    }

    public void updateRollershutter_6(double type) throws RSBBindingException {
        rollershutter_6.updatePosition((float) type);
    }

    public void updateRollershutter_7(double type) throws RSBBindingException {
        rollershutter_7.updatePosition((float) type);
    }
}
