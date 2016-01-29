///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package org.dc.bco.manager.scene.core;
//
//import org.dc.bco.manager.scene.lib.Scene;
//import org.dc.jul.exception.CouldNotPerformException;
//import org.dc.jul.exception.printer.ExceptionPrinter;
//import org.dc.jul.exception.InstantiationException;
//import org.dc.jul.exception.NotAvailableException;
//import org.dc.jul.pattern.Observable;
//import org.dc.jul.pattern.Observer;
//import org.dc.bco.registry.scene.remote.SceneRegistryRemote;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.concurrent.ExecutionException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
//import rst.homeautomation.control.scene.SceneRegistryType;
//
///**
// *
// * @author mpohling
// */
//public class SceneLoader {
//    
//    private static final Logger logger = LoggerFactory.getLogger(SceneLoader.class);
//
//    private final HashMap<String, Scene> agentMap;
//    private final SceneRegistryRemote agentRegistryRemote;
//
//
//    public SceneLoader() throws InstantiationException, InterruptedException {
//        try {
//            agentMap = new HashMap<>();
//
//            agentRegistryRemote = new SceneRegistryRemote();
//            agentRegistryRemote.addObserver(new Observer<SceneRegistryType.SceneRegistry>() {
//
//                @Override
//                public void update(Observable<SceneRegistryType.SceneRegistry> source, SceneRegistryType.SceneRegistry data) throws Exception {
//                    updateScenes(data);
//                }
//            });
//            agentRegistryRemote.init();
//            agentRegistryRemote.activate();
//            System.out.println("waiting for agents...");
//
//        } catch (CouldNotPerformException ex) {
//            throw new InstantiationException(this, ex);
//        }
//    }
//
//    private void updateScenes(final SceneRegistryType.SceneRegistry data) throws InterruptedException {
//
//        // add new agents
//        for (SceneConfig config : data.getSceneConfigList()) {
//
//            if (!agentMap.containsKey(config.getId())) {
//                try {
//                    agentMap.put(config.getId(), createScene(config));
//                    
//                } catch (CouldNotPerformException ex) {
//                    ExceptionPrinter.printHistory(ex, logger);
//                }
//            }
//        }
//
//        boolean found;
//        // remove outdated agents
//        for (Scene scene : new ArrayList<>(agentMap.values())) {
//            found = false;
//            for (SceneConfig config : data.getSceneConfigList()) {
//                try {
//                    if (scene.getConfig().getId().equals(config.getId())) {
//                        found = true;
//                        break;
//                    }
//                } catch (NotAvailableException ex) {
//                    continue;
//                }
//            }
//
//            if (!found) {
//                try {
//                    agentMap.remove(scene.getConfig().getId()).deactivate();
//                } catch (CouldNotPerformException ex) {
//                    ExceptionPrinter.printHistory(ex, logger);
//                }
//            }
//        }
//    }
//
//    public Scene createScene(final SceneConfig config) throws CouldNotPerformException {
//        Scene scene = new SceneController(config);
//        try {
//            scene.activate();
//        } catch (InterruptedException ex) {
//            ExceptionPrinter.printHistory(ex, logger);
//        }
//        return scene;
//    }
//
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) throws org.dc.jul.exception.InstantiationException, InterruptedException, CouldNotPerformException, ExecutionException {
//        new SceneLoader();
//    }
//}
