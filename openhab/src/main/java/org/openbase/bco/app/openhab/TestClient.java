package org.openbase.bco.app.openhab;

import com.google.gson.Gson;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class);

    public static void main(String[] args) {
        try {
//        try {
//            List<DiscoveryResultDTO> thingsFromInbox = OpenHABRestCommunicator.getInstance().getThingsFromInbox();
//            LOGGER.info("Things in inbox[" + thingsFromInbox.size() + "]");
//            DiscoveryResultDTO thing = thingsFromInbox.get(0);
//            LOGGER.info(thing.thingTypeUID + ", " + ", " + thing.label + ", " + thing.thingUID);
//
//            thing.label = "Motion Sensor Test";
//
//            OpenHABRestCommunicator.getInstance().approve(thing);
//
//
////            for (final EnrichedThingDTO thing : OpenHABRestCommunicator.getInstance().getThings()) {
//////                System.out.println("Thing: " + thing.thingTypeUID);
//////
//////                try {
//////                    registerDeviceForThing(thing);
//////                } catch (CouldNotPerformException ex) {
//////                    ExceptionPrinter.printHistory(ex, LOGGER);
//////                }
////                if (thing.label.startsWith("Motion")) {
//////                    thing.label = "Motion Sensor";
//////                    thing.location = "Home";
//////
//////                    OpenHABRestCommunicator.getInstance().updateThing(thing);
////
////                    OpenHABRestCommunicator.getInstance().deleteThing(thing.UID);
////                }
////            }
//        } catch (CouldNotPerformException ex) {
//            ExceptionPrinter.printHistory(ex, LOGGER);
//        }

//        try {
//            EnrichedThingDTO thingOne = OpenHABRestCommunicator.getInstance().getThings().get(0);
//            EnrichedThingDTO thingTwo = OpenHABRestCommunicator.getInstance().getThings().get(0);
//
//            IdentifiableEnrichedThingDTO one = new IdentifiableEnrichedThingDTO(thingOne);
//            IdentifiableEnrichedThingDTO two = new IdentifiableEnrichedThingDTO(thingTwo);
//
//            System.out.println(one.equals(two));
//
//            thingOne.label = "test";
//
//            System.out.println(one.equals(two));
//        } catch (CouldNotPerformException ex) {
//            ExceptionPrinter.printHistory(ex, LOGGER);
//        }
//
//        System.exit(0);
//        EnrichedItemDTO enrichedItemDTO = gson.fromJson(res, EnrichedItemDTO.class);
//
//        System.out.println("Parsed item: [" + enrichedItemDTO.link + ", " + enrichedItemDTO.state + ", "+enrichedItemDTO.name+"]");

//        try {
//            OpenHABConfigSynchronizer openHABConfigSynchronizer = new OpenHABConfigSynchronizer();
//            openHABConfigSynchronizer.init();
//            openHABConfigSynchronizer.activate();
//        } catch (CouldNotPerformException ex) {
//            ExceptionPrinter.printHistory(ex, LOGGER);
//        }

            final Gson gson = new Gson();
//        List<ItemDTO> itemDTOList = new ArrayList<>();
//
//        ItemDTO itemDTO = new ItemDTO();
//        itemDTO.name = "testName";
//        itemDTO.label = "testLabek";
//        itemDTO.category = "testCategory";
//        itemDTO.type = "testType";
//
//        System.out.println(gson.toJson(itemDTO));
//
//        itemDTOList.add(itemDTO);
//        System.out.println(gson.toJson(itemDTOList));

//        List<Integer> integerList = new ArrayList<>();
//        integerList.add(0);
//        integerList.add(100);
//        integerList.add(100);
//        integerList.add(null);
//        System.out.println(gson.toJson(gson.toJson(integerList)));
//
//        OpenHABRestCommunicator openhabCommunicator = new OpenHABRestCommunicator();
//        try {
//            List<EnrichedItemDTO> items = openhabCommunicator.getItems();
//
//            for (EnrichedItemDTO item : items) {
//                LOGGER.info("Item[" + item.name + "] has state[" + item.state + ", " + item.stateDescription + "]");
//                LOGGER.info(item.type + ", " + item.category);
//
//                OpenHABRestCommunicator.getInstance().postCommand(item.name, integerList);
//            }
//        } catch (CouldNotPerformException ex) {
//            ExceptionPrinter.printHistory(ex, LOGGER);
//        }
//        try {
            OpenHABDeviceManager openHABDeviceManager = new OpenHABDeviceManager();
            openHABDeviceManager.init();
            openHABDeviceManager.activate();
//        } catch (Exception ex) {
//            ExceptionPrinter.printHistory(ex, LOGGER);
//        }
//            OpenHABRestCommunicator.getInstance().postCommand("ColorableLight_4", "0,0,10");

        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }


}
