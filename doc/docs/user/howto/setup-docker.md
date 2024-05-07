---
sidebar_position: 2
---

# How to setup BCO via Docker

Follow the instructions step by step in order to setup your own home cube.
  
## Define Default User
If you want to use the current user as the default one, just execute the following command. Otherwise store the name of the default user in the following variable:

```bash
export DEFAULT_USER=$(whoami)
```

## Setup Docker Environment

1. Follow the official setup guidelines at [Docker Docs](https://docs.docker.com/engine/install/debian/)
   * Short summary for the pi:
     1. `curl -fsSL https://get.docker.com -o get-docker.sh`
     2. `sudo sh get-docker.sh`
2. Add default user to docker group
```bash
sudo usermod -aG docker ${DEFAULT_USER}
```

:::warning HINT
All following docker commands are performed as root via the `sudo` prefix since this is the default for the PI. In case you setup BCO in a rootless docker environment, then you have to remove the `sudo` prefix before performing each docker command. Otherwise it could happen that all containers are deployed to the docker v1 environment of the root user. 
:::

### Setup Network
Setup a dedicated bco network for security reasons.
```bash
sudo docker network create bco-net
```

## MQTT Broker Setup

### Create Docker Container
```bash
echo -e "allow_anonymous true\nlistener 1883" > $HOME/.mosquitto.conf && \
sudo docker run \
    --name mqtt-broker \
    --network=bco-net \
    --publish 1883:1883 \
    --volume $HOME/.mosquitto.conf:/mosquitto/config/mosquitto.conf \
    --restart=always \
    --log-driver=local \
    --detach \
    eclipse-mosquitto
```

:::tip HINT
At this stage, you can follow the [bco demo setup](/user/howto/bco-demo.md) in case you just want to play around with BCO without any smart home devices being involved.
:::

## Openhab Setup

Create a new openHAB user + group
```bash
sudo adduser --system --shell /usr/sbin/nologin openhab
sudo addgroup --system openhab
sudo usermod -a -G openhab openhab
```
Enable the openHAB user to access usb gateways such as zwave or zigbee sticks
```bash
sudo usermod -a -G dialout openhab
sudo usermod -a -G tty openhab
```

### Add the default user to the openHAB group
```bash
sudo usermod -a -G openhab ${DEFAULT_USER}
```

### Setup Z-Wave USB Stick
:::info INFO
Skip this subsection if you do not have a Z-Wave USB Stick!
:::
At this point, make sure your Z-Wave USB Stick is plugged in and that it is available under the following path:
```bash
export ZWAVE_STICK=--device=/dev/ttyACM0
```

### Deploy the openHAB Docker
```bash
sudo docker run \
    --name openhab \
    --network=bco-net \
    --publish 8080:8080 \
    --publish 8443:8443 \
    --volume /etc/localtime:/etc/localtime:ro \
    --volume /etc/timezone:/etc/timezone:ro \
    --volume openhab_conf:/openhab/conf \
    --volume openhab_userdata:/openhab/userdata \
    --volume openhab_addons:/openhab/addons \
    --detach \
    --env USER_ID=$(id -u openhab) \
    --env GROUP_ID=$(getent group openhab | cut -d: -f3) \
    --env CRYPTO_POLICY=unlimited \
    --restart=always \
    --log-driver=local \
    $ZWAVE_STICK \
    openhab/openhab:4.0.4
```

## BCO Setup

### Create BCO user and group
```bash
sudo adduser --system --shell /usr/sbin/nologin bco
sudo addgroup --system bco
sudo usermod -a -G bco bco
```

### Add default user to bco group
```bash
sudo usermod -a -G bco ${DEFAULT_USER}
```

### Deploy the BCO Core Docker
```bash
sudo docker run \
    --name bco \
    --network=bco-net \
    --publish 13781:13781 \
    --volume /etc/localtime:/etc/localtime:ro \
    --volume /etc/timezone:/etc/timezone:ro \
    --volume bco_data:/home/bco/data \
    --detach \
    --env USER_ID=$(id -u bco) \
    --env GROUP_ID=$(getent group bco | cut -d: -f3) \
    --env BCO_OPTIONS='--host mqtt-broker' \
    --restart=always \
    --log-driver=local \
    --tty \
    openbaseorg/bco:stable
```

### Deploy the BCO Device Manager openHAB Docker
```bash
sudo docker run \
    --name bco-device-manager-openhab \
    --network=bco-net \
    --volume /etc/localtime:/etc/localtime:ro \
    --volume /etc/timezone:/etc/timezone:ro \
    --volume bco_data:/home/bco/data \
    --volume openhab_conf:/etc/openhab2 \
    --detach \
    --env USER_ID=$(id -u bco) \
    --env GROUP_ID=$(getent group bco | cut -d: -f3) \
    --env OPENHAB_GROUP_ID=$(getent group openhab | cut -d: -f3) \
    --env BCO_OPTIONS='--host mqtt-broker --openhab-url http://openhab:8080' \
    --restart=always \
    --log-driver=local \
    --tty \
    openbaseorg/bco-device-manager-openhab:stable
```

### Deploy the BCO Webapp Docker
The docker deploys a local webserver thats serving the BCO webapp within your local network.
This supports an installation free control of your home environment on any device thats offers a web browser.
:::info INFO
If port `80` is already in use you can alter the `X` in `--publish X:80` in order to refer
to another port (e. g. `--publish 8282:80`).
:::
```bash
sudo docker run \
    --name bco-webapp \
    --publish 80:80 \
    --volume /etc/localtime:/etc/localtime:ro \
    --volume /etc/timezone:/etc/timezone:ro \
    --detach \
    --restart=always \
    --log-driver=local \
    -t \
    opaal/bco-ui-albiorix:latest
```

### Enable BCO to access the openHAB sitemap directory in order to generate or update sitemaps
Make BCO user a member of the openHAB group
```bash
sudo chmod -R g+rwX /var/lib/docker/volumes/openhab_conf/_data/sitemaps
sudo chgrp bco /var/lib/docker/volumes/openhab_conf/_data/sitemaps
```

## Setup Portainer as Docker Management Tool

The official installation can be found at: [Portainer Doc](https://www.portainer.io/installation/)
or use this shortcut: 
```bash
sudo docker volume create portainer_data
sudo docker run \
    -d -p 8000:8000 \
    -p 9000:9000 \
    --name portainer \
    --restart=always \
    --log-driver=local \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v portainer_data:/data portainer/portainer-ce
```
After the installation has finished, you can access portainer via port ```9000```
for example: [http://yourhostname:9000](http://yourhostname:9000)

## Setup auto update service with watchtower

Watchtower can be used to automatically keep your docker containers up-to-date.
The following modifications has been applied:
* ```WATCHTOWER_CLEANUP``` cleans up images that has been replaced by newer ones.
* ```WATCHTOWER_INCLUDE_STOPPED``` even update stopped containers.
* ```WATCHTOWER_TIMEOUT``` give components more time to perform a proper container shutdown.
* ```-v /etc/timezone:/etc/timezone:ro``` use the timezone of the hosts system.

Checkout the [watchtower documentation](https://containrrr.dev/watchtower/) for more details.

### Setup Watchtower as a daemon service

Once watchtower is deployed as a daemon service, it will check every 300 seconds for new container images.
In case a new image is available, the container will be stopped, updated and started after the update has finished.

Use the following command to setup watchtower as a daemon:
```bash
sudo docker run -d \
    --name watchtower \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /etc/timezone:/etc/timezone:ro \
    --restart=always \
    --log-driver=local \
    --env WATCHTOWER_CLEANUP=true \
    --env WATCHTOWER_INCLUDE_STOPPED=true \
    --env WATCHTOWER_TIMEOUT=60 \
    containrrr/watchtower:latest
```

### Setup Watchtower as a one shot service

In case you still want to be the one who is in change when updates should be performed,
you can setup watchtower as a one shot service. Therefore, updates are only checked in case you manually start the watchtower container.
Once started, watchtower will update all containers and stop itself afterwards again.

Use the following command to setup watchtower as aon shot service.
```bash
sudo docker run -d \
    --name watchtower \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /etc/timezone:/etc/timezone:ro \
    --log-driver=local \
    --env WATCHTOWER_CLEANUP=true \
    --env WATCHTOWER_INCLUDE_STOPPED=true \
    --env WATCHTOWER_TIMEOUT=60 \
    --env WATCHTOWER_RUN_ONCE=true \
    containrrr/watchtower:latest
```

## Next Step

Now, the docker setup is finished and you can continue with the [initial openHAB setup](/user/howto/setup-openhab.md).
