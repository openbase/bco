# BCO Persistence

This section introduces how [InfluxDB](https://docs.influxdata.com/influxdb/v2.2/get-started/) and the ```BCO Influxdb Connector``` app can be used to store the history of unit service state changes. This can for example be useful to persist and monitor the current economy level of your smart environment.

## How to setup InfluxDB via docker
Download and run the docker container via:
```bash
sudo docker run \
  --name influxdb \
  --network=bco-net \
  --publish 8086:8086 \
  --volume influx_data:/var/lib/influxdb2 \
  --volume influx_config:/etc/influxdb2 \
  --restart=always \
  --log-driver=local \
  --detach \
  influxdb:latest
```
:::tip HINT
Choose ```openbase``` as organization name and ```bco-persistence``` as bucket name to simplify this setup.
:::
![add_unit](/img/bco/persistence/influxdb_welcome.png)
After the container spins up, please follow the onboarding via [homecube](http://homecube:8086) by 
setup your initial user, bucket and organization.
![add_unit](/img/bco/persistence/influxdb_onbording.png)
Once done, influxdb is up and running and you can press `Configure Later` to continue with the bco setup.
![add_unit](/img/bco/persistence/influxdb_onbording_done.png)

##  How to setup the BCO Influxdb Connector App.

The ```BCO Influxdb Connector``` is a BCO app that persists all unit changes into influxdb.

### 1. Register the new App via the UnitRegistry  
To install the InfluxDbConnector you need to register it by using the ```bco-registry-editor```.
So please make sure you are connected to your BCO instance and start the ```bco-registry-editor --host homecube```.
Than, you need to navigate to: UnitRegistry → App  

![add_unit](/img/bco/persistence/add_unit.png)

Now add a new unit with right click → Add

To add the InfluxDB connector class to the new unit, select InfluxDB Connector as AppClassId and press apply.

![add_unit_class](/img/bco/persistence/new_unit.png)
   
### 2. Authenticate and Configure the App via Meta Configs

Switch to the token overview and create a new (READ/WRITE) API token to grant BCO write access.

![influxdb_create_token](/img/bco/persistence/influxdb_create_token.png)
![influxdb_config_token](/img/bco/persistence/influxdb_config_token.png)

Don't forget to select the `bco-persistence` buckets.

![influx_config_token_permission](/img/bco/persistence/influx_config_token_permission.png)

Finally open the token menu...

![influxdb_select_token](/img/bco/persistence/influxdb_select_token.png)

... and copy the key

![influxdb_copy_token](/img/bco/persistence/influxdb_copy_token.png)
Transfer the token to a new MetaConfig entry of the ```BCO Influxdb Connector``` via the ```bco-registry-editor``` e.g. ```INFLUXDB_TOKEN = <PASTE_TOKEN_HERE>```

:::info INFO
In case you choose the default values during the influxdb setup and you run influxdb on the same host as influxdb is running, all values except ```INFLUXDB_TOKEN``` are optionally.
:::

Further configurable meta config entries are:
  * ```INFLUXDB_URL``` → Url of your InfluxDB  
       DEFAULT: ```INFLUXDB_URL = http://influxdb:8086```
  * ```INFLUXDB_BUCKET``` → Name of the bucket where your data will be stored  
       DEFAULT: ```INFLUXDB_BUCKET = bco-persistence```
  * ```INFLUXDB_BATCH_TIME``` → Time limit(ms) after your batch is written to the database  
       DEFAULT: ```INFLUXDB_BATCH_TIME = 1000```
  * ```INFLUXDB_BATCH_LIMIT``` → Max size of your batch  
       DEFAULT: ```INFLUXDB_BATCH_LIMIT = 100```
  * ```INFLUXDB_ORG``` → Org for the bucket  
       DEFAULT: ```INFLUXDB_ORG = openbase```
  * ```INFLUXDB_TOKEN``` → Token with read and write access to your database  

## How to query influx db.
InfluxDB 2 uses Flux as a functional data scripting language.
A good guide how to get started with Flux is provided by the official [Influxdb Documentation](https://v2.docs.influxdata.com/v2.0/query-data/get-started/).

## How to create a Query 
Chronograf is the user interface and administrative component of the InfluxDB platform.
It is already included in influxdb 2
With Chronograf you can quickly see your data and build dashboards.  

Therefore, you need to log in into the Chronograf webview and select the Data Explorer.

If you have run ```bco-test --simulate``` and collected some data in your bucket, you should see some measurements.
![query_data](/img/bco/persistence/chronograf_explorer.png)

This query selects from the measurement ```power_consumption_state_service``` the field ```consumption``` data from the tag alias ```PowerConsumptionSensor-11```.  
It creates this query in Flux (you can see the query when you select the 'Script Editor'):
![flux-query](/img/bco/persistence/flux_query.png)

There are more options to visualize the data like raw_data, histogram table etc.
You can also save your graphs into dashboards.

If you want know about the possibilities of chronograf you can have a look at the official documentation here [Chronograf Documentation](https://docs.influxdata.com/chronograf/v1.7/)
   
## Heartbeat
The database contains a measurement 'heartbeat' with the field 'alive'. If the influxdb connector app is started, the value one is written into this field. Every 15 minutes the value one is written into the field again. When the app is closed a zero value is written into the field. This can be used to check whether the database was functional and stored data.
So you can consider possible downtimes during queries and calculations. 

[Source Code](https://github.com/openbase/bco.app/tree/master/influxdbconnector)

## Service Aggregation
It is possible to perform a service aggregation via the function ```queryAggregatedServiceState``` of a unit. This function needs a ```QueryType``` as a parameter.
Important attributes of the ```QueryType``` for the service aggregation are:

  * measurement 
  * service_type
  * time_range_stop
  * time_range_stop
  * aggregation_window

To get an overview of the ```QueryType``` look here: [Query](https://github.com/openbase/type/blob/master/src/main/proto/openbase/type/domotic/database/Query.proto)

The method returns an [AggregatedServiceState](https://github.com/openbase/type/blob/master/src/main/proto/openbase/type/domotic/state/AggregatedServiceState.proto).
Which contains the service_type, the query and the aggregated_service_type.
Depending on whether the requested service_type is an enum or not, the aggregated_service_type consists of percentages of how often which status was active, or of the average values.

```
// Query for continuous data
Query query = Query.newBuilder()
     .setMeasurement("power_consumption_state_service")
     .setServiceType(ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE)
     .setTimeRangeStart(TimestampType.Timestamp.newBuilder().setTime(time - 3600).build())
     .setTimeRangeStop(TimestampType.Timestamp.newBuilder().setTime(time).build())
     .setAggregatedWindow("1m")
     .build();
AggregatedServiceStateType.AggregatedServiceState aggregatedServiceState = testLocation.queryAggregatedServiceState(query).get();
```

```
// Query for enum data
Query enumQuery = Query.newBuilder()
     .setMeasurement("button_state_service")
     .setServiceType(ServiceTemplateType.ServiceTemplate.ServiceType.BUTTON_STATE_SERVICE)
     .setTimeRangeStart(TimestampType.Timestamp.newBuilder().setTime(time - 3600).build())
     .setTimeRangeStop(TimestampType.Timestamp.newBuilder().setTime(time).build())
     .setAggregatedWindow("1m")
     .build();

AggregatedServiceStateType.AggregatedServiceState aggregatedEnumServiceState = testLocation.queryAggregatedServiceState(enumQuery).ge();
```

The full example how to query an aggregated service state is available over here: [HowToQueryAggregatedState](https://github.com/openbase/bco.dal/blob/master/example/src/main/java/org/openbase/bco/dal/example/HowToQueryAggregatedState.java)

## Query Database
You can also send raw queries to the database via the units with the ```queryRecord``` function.
This function needs also a [QueryType](https://github.com/openbase/type/blob/master/src/main/proto/openbase/type/domotic/database/Query.proto) as a parameter. However, the only attribute that must be filled is the raw_query.
The method returns an [RecordCollection](https://github.com/openbase/type/blob/master/src/main/proto/openbase/type/domotic/database/RecordCollection.proto)  which consists of [Records](https://github.com/openbase/type/blob/master/src/main/proto/openbase/type/domotic/database/Record.proto).
In the [Chronograf](#how-to-create-a-chronograf-widget) and the [Query-Section](#how-to-query-influx-db) it is explained how a raw query looks like and how it can be built.

An example request looks like:

```
String query = "from(bucket: \"bco-persistence\")\n" +
     "  |> range(start:" + (time - 3600) + ", stop: " + time + ")\n" +
     "  |> filter(fn: (r) => r._measurement == \"power_consumption_state_service\")";
RecordCollectionType.RecordCollection recordCollection = testLocation.queryRecord(Query.newBuilder().setRawQuery(query).build()).get();
```

The full example how to query a database record is available over here: [HowToQueryUnitLongTermStateUpdates](https://github.com/openbase/bco.dal/blob/master/example/src/main/java/org/openbase/bco/dal/example/HowToQueryUnitLongTermStateUpdates.java).
