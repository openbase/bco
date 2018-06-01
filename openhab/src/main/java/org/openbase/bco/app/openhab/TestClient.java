package org.openbase.bco.app.openhab;

import com.google.gson.Gson;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.function.Consumer;

public class TestClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class);

    public static void main(String[] args) {
        try {
            final Gson gson = new Gson();

            Client client = ClientBuilder.newClient();
            WebTarget webTarget = client.target("http://localhost:8080/rest/events");

            SseEventSource sseEventSource = SseEventSource.target(webTarget).build();
            sseEventSource.register(inboundSseEvent -> {
                String id = inboundSseEvent.getId();
                String name = inboundSseEvent.getName();
                String payload = inboundSseEvent.readData();

                System.out.printf("Received event[" + id + ", " + name + "] with payload[" + payload + "]");
            });
            sseEventSource.open();
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }
}
