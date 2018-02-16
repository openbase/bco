package org.openbase.bco.app.openhab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTO;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public class TestClient {

    public static void main(String[] args) {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target("http://localhost:8080/rest");

        WebTarget thingsWebTarget = webTarget.path("items/MotionDetectorAlarm");
        Invocation.Builder invocationBuilder = thingsWebTarget.request();

        Response response = invocationBuilder.get();

        System.out.println("Response status: " + response.getStatus());
        String res = response.readEntity(String.class);
        System.out.println("Response: " + res);

        Gson gson = new GsonBuilder().create();
        EnrichedItemDTO enrichedItemDTO = gson.fromJson(res, EnrichedItemDTO.class);

        System.out.println("Parsed item: [" + enrichedItemDTO.link + ", " + enrichedItemDTO.state + ", "+enrichedItemDTO.name+"]");
    }
}
