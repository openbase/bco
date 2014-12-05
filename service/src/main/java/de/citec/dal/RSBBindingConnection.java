/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.data.Location;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.devices.fibaro.F_MotionSensorController;
import de.citec.dal.hal.devices.gira.GI_5133Controller;
import de.citec.dal.hal.devices.gira.GI_5142Controller;
import de.citec.dal.hal.devices.hager.HA_TYA606EController;
import de.citec.dal.hal.devices.homematic.HM_ReedSwitchController;
import de.citec.dal.hal.devices.homematic.HM_RotaryHandleSensorController;
import de.citec.dal.hal.devices.philips.PH_Hue_E27Controller;
import de.citec.dal.hal.devices.philips.PH_Hue_GU10Controller;
import de.citec.dal.hal.devices.plugwise.PW_PowerPlugController;
import de.citec.dal.service.HardwareManager;
import de.citec.dal.service.HardwareRegistry;
import de.citec.dal.service.rsb.RSBCommunicationService;
import de.citec.dal.service.rsb.RSBRemoteService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.RSBException;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.openhab.DALBindingType;
import rst.homeautomation.openhab.DALBindingType.DALBinding;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.openhab.RSBBindingType;
import rst.homeautomation.openhab.RSBBindingType.RSBBinding;

/**
 *
 * @author thuxohl
 */
public class RSBBindingConnection implements RSBBindingInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(OpenhabCommandType.OpenhabCommand.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(RSBBindingType.RSBBinding.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(DALBindingType.DALBinding.getDefaultInstance()));
    }
    
    private static final Logger logger = LoggerFactory.getLogger(RSBBindingConnection.class);

    private static RSBBindingConnection instance;
//    private final RSBBindingInterface binding;

    private final Scope dalRemoteScope = new Scope("/dal");
    private final Scope openhabBindingScope = new Scope("/openhab");
    private final RSBRemoteService<DALBinding> dalRemoteService;
    private final RSBCommunicationService<RSBBinding, RSBBinding.Builder> communicationService;

    private final HardwareRegistry registry;
    private final HardwareManager hardwareManager;

    public RSBBindingConnection() {
//        this.binding = binding;
        this.instance = this;
        this.registry = HardwareRegistry.getInstance();
        this.hardwareManager = HardwareManager.getInstance();
        this.initDevices();

        dalRemoteService = new RSBRemoteService<DALBinding>(dalRemoteScope) {

            @Override
            public void notifyUpdated(DALBinding data) {
                RSBBindingConnection.this.notifyUpdated(data);
            }
        };
        dalRemoteService.init(dalRemoteScope);

        communicationService = new RSBCommunicationService<RSBBinding, RSBBinding.Builder>(openhabBindingScope, RSBBinding.newBuilder()) {

            @Override
            public void registerMethods(LocalServer server) throws RSBException {
                RSBBindingConnection.this.registerMethods(server);
            }
        };
        try {
            communicationService.init();
        } catch (RSBException ex) {
            logger.warn("Unable to initialize the communication service in [" + getClass().getSimpleName() + "]");
        }

        try {
            this.hardwareManager.activate();
        } catch (Exception ex) {
            logger.warn("Hardware manager could not be activated!", ex);
        }

        dalRemoteService.activate();
        {
            try {
                communicationService.activate();
            } catch (Exception ex) {
                logger.warn("Unable to activate the communication service in [" + getClass().getSimpleName() + "]", ex);
            }
        }
    }

    private void initDevices() {
        logger.info("Init devices...");
        Location outdoor = new Location("Outdoor");
        Location kitchen = new Location("kitchen");
        Location wardrobe = new Location("wardrobe");
        Location living = new Location("living");
        Location sports = new Location("sports");
        Location bath = new Location("bath");
        Location control = new Location("control");

        try {
            registry.register(new PW_PowerPlugController("PW_PowerPlug_000", "USBCharger_1", control));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_001", "USBCharger_2", control));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_002", "USBCharger_3", control));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_003", "USBCharger_4", control));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_004", "Fan", control));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_005", "", control));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_006", "", control));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_007", "", control));
            registry.register(new PW_PowerPlugController("PW_PowerPlug_008", "", control));

            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_000", "Entrance", wardrobe));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_001", "1", kitchen));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_002", "2", kitchen));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_003", "3", bath));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_004", "4", sports));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_005", "5", sports));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_006", "6", living));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_007", "7", living));
            registry.register(new HM_ReedSwitchController("HM_ReedSwitch_008", "8", sports));

            registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_000", "WindowLeft", living));
            registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_001", "WindowRight", living));
            registry.register(new HM_RotaryHandleSensorController("HM_RotaryHandleSensor_002", "Window", sports));

            registry.register(new F_MotionSensorController("F_MotionSensor_000", "Entrance", wardrobe));
            registry.register(new F_MotionSensorController("F_MotionSensor_001", "Hallway", wardrobe));
            registry.register(new F_MotionSensorController("F_MotionSensor_002", "Couch", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_003", "Media", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_004", "Table", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_005", "Control", living));
            registry.register(new F_MotionSensorController("F_MotionSensor_006", "Global", kitchen));
            registry.register(new F_MotionSensorController("F_MotionSensor_007", "Global", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_008", "Entrance", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_009", "Shower", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_010", "Sink", bath));
            registry.register(new F_MotionSensorController("F_MotionSensor_011", "Interaction", sports));
            registry.register(new F_MotionSensorController("F_MotionSensor_012", "Pathway", sports));
            registry.register(new F_MotionSensorController("F_MotionSensor_013", "Entrance", control));
            registry.register(new F_MotionSensorController("F_MotionSensor_014", "TestUnit_1", control));
            registry.register(new F_MotionSensorController("F_MotionSensor_015", "Entrance", outdoor));

            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_000", "Hallway_0", wardrobe));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_001", "Hallway_1", wardrobe));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_002", "Table_0", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_003", "Table_1", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_004", "Couch", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_005", "Media", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_006", "Interaction_0", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_007", "Interaction_1", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_008", "TestUnit_0", control));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_009", "TestUnit_1", control));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_010", "SLamp_Left_Window1", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_011", "SLamp_Left_Window2", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_012", "SLamp_Right_Window1", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_013", "SLamp_Right_Window2", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_014", "SLamp_Right1", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_015", "SLamp_Right2", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_016", "SLamp_Window1", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_017", "SLamp_Window2", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_018", "SLamp_Mirror1", bath));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_019", "SLamp_Mirror2", bath));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_020", "LLamp_Left_Window1", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_021", "LLamp_Left_Window2", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_022", "LLamp_Left_Window3", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_023", "LLamp_Left_Window4", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_024", "LLamp_Left_Window5", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_025", "LLamp_Left_Window6", living));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_026", "LLamp_Entrance1", bath));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_027", "LLamp_Entrance2", bath));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_028", "LLamp_Entrance3", bath));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_029", "LLamp_Entrance4", bath));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_030", "LLamp_Entrance5", bath));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_031", "LLamp_Entrance6", bath));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_032", "LLamp_Entrance1", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_033", "LLamp_Entrance2", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_034", "LLamp_Entrance3", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_035", "LLamp_Entrance4", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_036", "LLamp_Entrance5", sports));
            registry.register(new PH_Hue_E27Controller("PH_Hue_E27_037", "LLamp_Entrance6", sports));

            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_000", "Global_0", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_001", "Global_1", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_002", "Global_2", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_003", "Global_3", kitchen));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_004", "Global_0", bath));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_005", "Global_1", bath));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_006", "Global_2", bath));
            registry.register(new PH_Hue_GU10Controller("PH_Hue_GU10_007", "Global_3", bath));

            registry.register(new HA_TYA606EController("HA_TYA606E_000", "1", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_001", "2", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_002", "3", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_003", "4", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_004", "5", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_005", "6", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_006", "7", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_007", "8", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_008", "9", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_009", "10", control));
            registry.register(new HA_TYA606EController("HA_TYA606E_010", "11", control));

            String[] giraLabel0 = {"Button_1", "Button_2", "Button_3", "Button_4"};
            registry.register(new GI_5142Controller("GI_5142_000", "Entrance", giraLabel0, bath));
            registry.register(new GI_5142Controller("GI_5142_001", "Control", giraLabel0, living));
            registry.register(new GI_5142Controller("GI_5142_002", "Pathway", giraLabel0, sports));

            String[] giraLabel1 = {"Button_1", "Button_2", "Button_3", "Button_4", "Button_5", "Button_6"};
            registry.register(new GI_5133Controller("GI_5133_000", "Door", giraLabel1, kitchen));
            registry.register(new GI_5133Controller("GI_5133_001", "Entrance", giraLabel1, wardrobe));
            registry.register(new GI_5133Controller("GI_5133_002", "Hallway", giraLabel1, wardrobe));
            registry.register(new GI_5133Controller("GI_5133_005", "Media", giraLabel1, living));

            String[] giraLabel2 = {"Button_5", "Button_6", "Button_7", "Button_8", "Button_9", "Button_10"};
            registry.register(new GI_5133Controller("GI_5133_004", "Control", giraLabel2, living));
            registry.register(new GI_5133Controller("GI_5133_006", "Pathway", giraLabel2, sports));

            String[] giraLabel3 = {"Button_7", "Button_8", "Button_9", "Button_10", "Button_11", "Button_12"};
            registry.register(new GI_5133Controller("GI_5133_003", "Hallway", giraLabel3, wardrobe));
        } catch (RSBBindingException ex) {
            logger.warn("Could not initialize devices!", ex);
        }
    }

    public final void notifyUpdated(DALBinding data) {
        switch (data.getState().getState()) {
            case ACTIVE:
                logger.debug("Active rsb binding state!");
                break;
            case DEACTIVE:
                logger.debug("Deactive rsb binding state!");
                break;
            case UNKNOWN:
                logger.debug("Unkown rsb binding state!");
                break;
        }
    }

    public final void registerMethods(LocalServer server) {
        try {
            server.addMethod("internalReceiveUpdate", new InternalReceiveUpdateCallback());
        } catch (RSBException ex) {
            logger.warn("Could not add methods to local server in [" + getClass().getSimpleName() + "]", ex);
        }
    }

    @Override
    public void internalReceiveUpdate(OpenhabCommand command) {
        hardwareManager.internalReceiveUpdate(command);
    }

    public static class InternalReceiveUpdateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            instance.internalReceiveUpdate((OpenhabCommand) request.getData());
            return new Event(String.class, "Ok");
        }

    }

    @Override
    public Future executeCommand(OpenhabCommandType.OpenhabCommand command) throws RSBBindingException {
        dalRemoteService.callMethod("executeCommand", command, true);
        return null; // TODO: mpohling implement future handling.
    }

    public static RSBBindingInterface getInstance() {
        while (instance == null) {
            logger.warn("WARN: Binding not ready yet!");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(RSBBindingConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return instance;
    }
}
