# Initial openHAB Setup

## OpenHAB 3.x.x

Access the OpenHAB UI over a webbrowser at `http://<OpenHAB_IP>:8080`.
Setup an initial user account and perform the other steps of the initial setup.

Afterwards you have to create an API Token so that BCO can authenticate its communication with OpenHAB.
To do this, access `http://<OPENHAB_IP>:8080/createApiToken`.
Enter the username and password of the user account you created and set "BCO" as the token name.
Leave the token scope field empty and click on the "Create API Token" button.
Then your generated API token of the form `oh.bco.<token>` should be displayed.
Copy this token and add the following entry to the meta config of the OpenHAB gateway config with the registry editor: 
`TOKEN=oh.bco.<token>`

## Next Step

Setup recording of historical data via the [influxdb setup](/developer/addon/bco-persistence.md).



