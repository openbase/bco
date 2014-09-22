package de.citec.csra.rsbbindingtester;

import de.citec.csra.rsbbindingtester.view.GUI;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rsb.Event;
import rsb.Factory;
import rsb.Handler;
import rsb.InitializeException;
import rsb.Listener;
import rsb.RSBException;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.RemoteServer;
import rst.homeautomation.MotionSensorType;
import rst.homeautomation.MotionSensorType.MotionSensor;
import rst.homeautomation.PowerPlugType;
import rst.homeautomation.PowerPlugType.PowerPlug;
import rst.homeautomation.ReedSwitchType.ReedSwitch;
import static rst.homeautomation.states.MotionType.Motion.MotionState.MOVEMENT;
import static rst.homeautomation.states.MotionType.Motion.MotionState.NO_MOVEMENT;
import rst.homeautomation.states.PowerType;
import static rst.homeautomation.states.PowerType.Power.*;
import rst.homeautomation.states.PowerType.Power.PowerState;
import rst.vision.HSVColorType;

public class Controller implements Handler {

    private Listener listener;
    private final PropertyChangeSupport pcs;
    private Scope listener_scope;
    private RemoteServer server;

    public Controller() {
        pcs = new PropertyChangeSupport(this);

        listener_scope = new Scope("/home/kitchen/powerplug/000/status");

        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerPlugType.PowerPlug.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HSVColorType.HSVColor.getDefaultInstance()));

        try {
            System.out.println("createListener");
            listener = Factory.getInstance().createListener(listener_scope);
            System.out.println("listener.activate();");
            listener.activate();
            System.out.println("listener.addHandler(this, true);");
            listener.addHandler(this, true);
            System.out.println("done.");

        } catch (InitializeException ex) {
            //Logger.getLogger(RSBClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Listener could not be initialized!");
        } catch (RSBException ex) {
            //Logger.getLogger(RSBClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Listener could not be activated!");
        } catch (InterruptedException ex) {
            //Logger.getLogger(RSBClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Coul not add Handler!");
        }

    }


    public<T extends Object> void callMethod(String methodName, T type, final String scope, boolean async) {
        callMethod(methodName, type, new Scope(scope), async);
    }
    public<T extends Object> void callMethod(String methodName, T type, final Scope scope, boolean async) {
        try {
            System.out.println("Calling method [" + methodName + "] on scope: " + scope.toString());
            server = Factory.getInstance().createRemoteServer(scope);
            try {
                server.activate();
            } catch (RSBException ex) {
                System.out.println("Server could not be activated on scope [" + scope.toString() + "] !");
            }
            if(async) {
                server.callAsync(methodName, type);
            } else {
                server.call(methodName, type);
            }
            server.deactivate();
        } catch (RSBException | ExecutionException | TimeoutException | InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Could not call setColor from server!");
        }
    }

    public void update() {
        try {
            server.call("update");
        } catch (RSBException | ExecutionException | TimeoutException ex) {
            //Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Could not call update from server!");
        }
    }

    PowerType.Power.PowerState lastPowerState = PowerState.UNKNOWN;

    public PowerType.Power.PowerState getLastPowerState() {
        return lastPowerState;
    }

    @Override
    public void internalNotify(Event event) {
        System.out.println("Event incoming!:" + event.getType().getName());
        if (event.getType().equals(PowerPlug.class)) {
            System.out.println("PowerPlugType");
            PowerPlug pp = (PowerPlug) event.getData();
            System.out.println("State: " + pp.getState().getState());
            lastPowerState = pp.getState().getState();
            switch (pp.getState().getState()) {
                case ON:
                    pcs.firePropertyChange("ON", null, null);
                    break;
                case OFF:
                    pcs.firePropertyChange("OFF", 0, 1);
                    break;
                case UNKNOWN:
                    pcs.firePropertyChange("PP_UNKNOWN", 0, 1);
                    break;
                default:
                    System.out.println("Unknown power plug state!");
            }
        } else if (event.getType().equals(MotionSensor.class) && event.getScope().isSubScopeOf(new Scope("/home/wardrobe"))) {
            MotionSensor ms = (MotionSensor) event.getData();
            switch (ms.getState().getState()) {
                case MOVEMENT:
                    pcs.firePropertyChange("MV", 0, 1);
                    break;
                case NO_MOVEMENT:
                    pcs.firePropertyChange("NMV", 0, 1);
                    break;
                case UNKNOWN:
                    pcs.firePropertyChange("M_UNKNOWN", 0, 1);
                    break;
                default:
                    System.out.println("Unknown motion sensor state!");
            }
        } else if (event.getType().equals(ReedSwitch.class)) {
            ReedSwitch swt = (ReedSwitch) event.getData();
            switch (swt.getState().getState()) {
                case OPEN:
                    pcs.firePropertyChange("OPEN", 0, 1);
                    break;
                case CLOSED:
                    pcs.firePropertyChange("CLOSED", 0, 1);
                    break;
                case UNKNOWN:
                    pcs.firePropertyChange("RS_UNKNOWN", 0, 1);
                    break;
                default:
                    System.out.println("Unknown reed switch state!");
            }
        } else {
            System.out.println("Unknown data type!");
        }
    }

    public static void main(String[] args) {
        GUI.initGui(new Controller());
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        System.out.println("add listener: " + listener.getClass().getName());
        pcs.addPropertyChangeListener(listener);
    }
}
