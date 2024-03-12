---
---
# BCO Eveson

In smart environments, a myriad of sensors and processing components produce a very large amount of data every second. We implemented the framework Eveson which supports the monitoring through the generation of pleasant ambient soundscapes based on such complex data streams.

![LocationGraphStructure](/img/bco/eveson/EvesonLogo.jpg)

Eveson supports the individual selection of data streams and types and the configuration of how they should be sonified. The bundle of all data to sound mappings is stored in a theme configuration. For your prototype we especially selected only sounds of one natural environment (the German forest) to generate a familiar soundscape for the user.

## Repository

[Github: openbase/bco.eveson](https://github.com/openbase/bco.eveson)

## How to Start the Application

Since Eveson is a part of the default distribution of bco, you can simply start Eveson by executing:
```
bco-eveson
```

for more details checkout:

```
bco-eveson --help
```