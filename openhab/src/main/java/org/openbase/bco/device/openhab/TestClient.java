package org.openbase.bco.device.openhab;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.eclipse.smarthome.core.types.Command;
import org.openbase.bco.device.openhab.manager.transform.ServiceTypeCommandMapping;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;

public class TestClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class);

    public static void main(String[] args) {
        try {
//            Registries.getTemplateRegistry().waitForData();
            Registries.getTemplateRegistry().addDataObserver((provider, data) -> {
                for (ServiceTemplate serviceTemplate : data.getServiceTemplateList()) {
                    String commandClasses = "";
                    try {
                        for (Class<? extends Command> commandClass : ServiceTypeCommandMapping.getCommandClasses(serviceTemplate.getServiceType())) {
                            commandClasses += commandClass.getSimpleName() + ", ";
                        }
                        LOGGER.info(serviceTemplate.getServiceType().name() + ": " + commandClasses);
                    } catch (NotAvailableException ex) {
                        // do nothing
                    }
                }
            });

//            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
//            final JsonParser jsonParser = new JsonParser();
//
//            System.out.println("Start...");
//            Client client = ClientBuilder.newClient();
//            WebTarget webTarget = client.target("http://localhost:8080/rest/events");
//
//            SseEventSource sseEventSource = SseEventSource.target(webTarget).build();
//            sseEventSource.register(inboundSseEvent -> {
//                final String payload = inboundSseEvent.readData();
//                final JsonElement payloadAsJson = jsonParser.parse(payload);
//
//                System.out.printf("Received event:\n" + gson.toJson(payloadAsJson));
//            });
//            sseEventSource.open();
//            System.out.println("Opened sse source!");
//
//            for (DiscoveryResultDTO discoveryResultDTO : OpenHABRestCommunicator.getInstance().getDiscoveryResults()) {
//                LOGGER.info(discoveryResultDTO.label);
//                LOGGER.info(discoveryResultDTO.thingTypeUID);
//                LOGGER.info(discoveryResultDTO.bridgeUID);
//                LOGGER.info(discoveryResultDTO.representationProperty);
//                LOGGER.info(discoveryResultDTO.thingUID);
//                LOGGER.info(discoveryResultDTO.flag.name());
//                for (Entry<String, Object> stringObjectEntry : discoveryResultDTO.properties.entrySet()) {
//                    LOGGER.info(stringObjectEntry.getKey() + ": " + stringObjectEntry.getValue());
//                }
//            }

//            final String preset = "smarthome/inbox/hue:bridge:00178821671c/added";
//            System.out.println(preset.matches("smarthome/inbox/(.+)/added"));
//            System.out.println(preset.matches("smarthome/inbox/(.+)/added"));
//            System.out.println(preset.matches("smarthome/items/(.+)/state"));

//            OpenHABRestCommunicator.getInstance().addSSEObserver(new Observer<JsonObject>() {
//                @Override
//                public void update(Observable<JsonObject> source, JsonObject data) throws Exception {
//                    LOGGER.info(gson.toJson(data));
//                }
//            });
//
//            Thread.sleep(1000000);
//            Random random = new Random();
//            Registries.waitForData();
//            List<UnitConfig> unitConfigs = Registries.getUnitRegistry().getUnitConfigs();
//            Stopwatch stopwatch = new Stopwatch();
//            stopwatch.start();
//            for (int i = 0; i < 1000; i++) {
//                final UnitConfig unitConfig = unitConfigs.get(random.nextInt(unitConfigs.size()));
//                final String alias = unitConfig.getAlias(random.nextInt(unitConfig.getAliasCount()));
//                Registries.getUnitRegistry().getUnitConfigByAlias(alias);
//            }
//            long stop = stopwatch.stop();
//            LOGGER.info("Getting 1000 units by alias took: " + stop + "ms");
//
//            stopwatch.restart();
//            for(int i = 0; i < 1000; i++) {
//                final UnitConfig unitConfig = unitConfigs.get(random.nextInt(unitConfigs.size()));
//                final String alias = unitConfig.getAlias(random.nextInt(unitConfig.getAliasCount()));
//
//                for (UnitConfig config : Registries.getUnitRegistry().getUnitConfigs()) {
//                    for(String unitAlias : config.getAliasList()) {
//                        if(unitAlias.equals(alias)) {
//                            continue;
//                        }
//                    }
//                }
//            }
//            stop = stopwatch.stop();
//            LOGGER.info("Getting 1000 units by alias took: " + stop + "ms");

        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }
}
