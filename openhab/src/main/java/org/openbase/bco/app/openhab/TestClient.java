package org.openbase.bco.app.openhab;

import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.List;
import java.util.Random;

public class TestClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class);

    public static void main(String[] args) {
        try {
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

            Random random = new Random();
            Registries.waitForData();
            List<UnitConfig> unitConfigs = Registries.getUnitRegistry().getUnitConfigs();
            Stopwatch stopwatch = new Stopwatch();
            stopwatch.start();
            for (int i = 0; i < 1000; i++) {
                final UnitConfig unitConfig = unitConfigs.get(random.nextInt(unitConfigs.size()));
                final String alias = unitConfig.getAlias(random.nextInt(unitConfig.getAliasCount()));
                Registries.getUnitRegistry().getUnitConfigByAlias(alias);
            }
            long stop = stopwatch.stop();
            LOGGER.info("Getting 1000 units by alias took: " + stop + "ms");

            stopwatch.restart();
            for(int i = 0; i < 1000; i++) {
                final UnitConfig unitConfig = unitConfigs.get(random.nextInt(unitConfigs.size()));
                final String alias = unitConfig.getAlias(random.nextInt(unitConfig.getAliasCount()));

                for (UnitConfig config : Registries.getUnitRegistry().getUnitConfigs()) {
                    for(String unitAlias : config.getAliasList()) {
                        if(unitAlias.equals(alias)) {
                            continue;
                        }
                    }
                }
            }
            stop = stopwatch.stop();
            LOGGER.info("Getting 1000 units by alias took: " + stop + "ms");

        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }
}
