## Troubleshooting

### How to remove all item entries

Enter the openHAB client console of your openHAB docker container
```
docker exec -it openhab /openhab/runtime/bin/client
```
:::tip
The default password for the login is: `habopen`
:::

Use the following command within the openHAB console in order to remove all items: 
```
smarthome:links clear
smarthome:items clear
```
Afterwards, just restart the BCO openHAB Device Manager container in order to recreate all items.