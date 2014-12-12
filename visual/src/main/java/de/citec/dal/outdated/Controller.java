package de.citec.dal.outdated;

import de.citec.dal.outdated.GUI;
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
import rst.homeautomation.ButtonType;
import rst.homeautomation.ButtonType.Button;
import rst.homeautomation.MotionSensorType;
import rst.homeautomation.PowerPlugType;
import rst.homeautomation.PowerPlugType.PowerPlug;
import rst.homeautomation.states.PowerType;
import rst.homeautomation.states.PowerType.Power.PowerState;
import rst.vision.HSVColorType;

@Deprecated
public class Controller implements Handler {

    private final PropertyChangeSupport pcs;

    private Listener listenerPowerPlug;
    private Listener listenerButton;
    private RemoteServer server;

    private final String STATUS_ENDING = "/status";
    private final String CTRL_ENDING = "/ctrl";

    private PowerType.Power.PowerState lastPowerState = PowerState.UNKNOWN;

    public Controller() {
        pcs = new PropertyChangeSupport(this);

        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerPlugType.PowerPlug.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HSVColorType.HSVColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(ButtonType.Button.getDefaultInstance()));
    }

    public <T extends Object> void callMethod(String methodName, T type, final String scope, boolean async) {
        callMethod(methodName, type, new Scope(scope), async);
    }

    public <T extends Object> void callMethod(String methodName, T type, final Scope scope, boolean async) {
        try {
            System.out.println("Calling method [" + methodName + "] on scope: " + scope.toString());
            server = Factory.getInstance().createRemoteServer(scope);
            try {
                server.activate();
            } catch (RSBException ex) {
                System.out.println("Server could not be activated on scope [" + scope.toString() + "] !");
            }
            if (async) {
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

    public void activateListener(Scope scope, Listener listener) {
        if( listener != null && listener.isActive() ) {
            try {
                listener.deactivate();
            } catch (RSBException ex) {
                System.out.println("Listener could not be deactivated!");
            } catch (InterruptedException ex) {
                System.out.println("Interrupdet while deactivating listener!");
            }
        }
        try {
            listener = Factory.getInstance().createListener(scope);
            listener.activate();
            listener.addHandler(this, true);
            System.out.println("Creating Listener on Scope [" + scope.toString() + "] was succesfull!");

        } catch (InitializeException ex) {
            System.out.println("Listener could not be initialized!");
        } catch (RSBException ex) {
            System.out.println("Listener could not be activated!");
        } catch (InterruptedException ex) {
            System.out.println("Coul not add Handler to Listener!");
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

    public PowerType.Power.PowerState getLastPowerState() {
        return lastPowerState;
    }

    public Listener getButtonListener() {
        return listenerButton;
    }

    public Listener getPowerPlugListener() {
        return listenerPowerPlug;
    }

    @Override
    public void internalNotify(Event event) {
        System.out.println("Event incoming!:" + event.getType().getName());
        if (event.getType().equals(PowerPlug.class)) {
            PowerPlug pp = (PowerPlug) event.getData();
            System.out.println("Revieved PowerPlugType with State: " + pp.getState().getState());
            lastPowerState = pp.getState().getState();
            switch (pp.getState().getState()) {
                case ON:
                    pcs.firePropertyChange("ON", 0, 1);
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
        } else if (event.getType().equals(Button.class)) {
            Button bs = (Button) event.getData();
            System.out.println("Recieved ButtonType witch ClickState: " + bs.getState().getState());
            switch (bs.getState().getState()) {
                case CLICKED:
                    pcs.firePropertyChange("CLICK", 0, 1);
                    break;
                case DOUBLE_CLICKED:
                    pcs.firePropertyChange("DCLICK", 0, 1);
                    break;
                case RELEASED:
                    pcs.firePropertyChange("RELEASE", 0, 1);
                    break;
                case UNKNOWN:
                    pcs.firePropertyChange("B_UNKNOWN", 0, 1);
                    break;
                default:
                    System.out.println("Unknown motion sensor state!");
            }
//        } else if (event.getType().equals(MotionSensor.class) && event.getScope().isSubScopeOf(new Scope("/home/wardrobe"))) {
//            MotionSensor ms = (MotionSensor) event.getData();
//            switch (ms.getState().getState()) {
//                case MOVEMENT:
//                    pcs.firePropertyChange("MV", 0, 1);
//                    break;
//                case NO_MOVEMENT:
//                    pcs.firePropertyChange("NMV", 0, 1);
//                    break;
//                case UNKNOWN:
//                    pcs.firePropertyChange("M_UNKNOWN", 0, 1);
//                    break;
//                default:
//                    System.out.println("Unknown motion sensor state!");
//            }
//        } else if (event.getType().equals(ReedSwitch.class)) {
//            ReedSwitch swt = (ReedSwitch) event.getData();
//            switch (swt.getState().getState()) {
//                case OPEN:
//                    pcs.firePropertyChange("OPEN", 0, 1);
//                    break;
//                case CLOSED:
//                    pcs.firePropertyChange("CLOSED", 0, 1);
//                    break;
//                case UNKNOWN:
//                    pcs.firePropertyChange("RS_UNKNOWN", 0, 1);
//                    break;
//                default:
//                    System.out.println("Unknown reed switch state!");
//            }
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

    public Scope createStatusScope(String scopeBasis) {
        return new Scope(scopeBasis.concat(STATUS_ENDING));
    }

    public Scope createCTRLScope(String scopeBasis) {
        return new Scope(scopeBasis.concat(CTRL_ENDING));
    }
}
