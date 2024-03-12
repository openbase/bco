---
sidebar_position: 1
---

# How to setup BCO on Raspberry PI via Docker

## Hardware Requirements

* Pi 4 with at least 4GB is recommended.
* SD Card with at least 16 GB (In order to store some historical data).
* A direct ethernet connection to your router to reduce delays.
  
:::warning RESTRICTIONS
For ARM architectures only 64-bit BCO docker images are provided. Thus, stick to an 64-bit PI operating system to simplify the PI setup and to enjoy BCO persistance.
:::

## PI Preparation
1. Install a fresh 64-bit Ubuntu Server on your PI [Install Ubuntu Server](https://ubuntu.com/tutorials/how-to-install-ubuntu-on-your-raspberry-pi#1-overview)
2. Login via ssh to your PI (the installation guide includes an description on how to get the ip-address)
   1. Default ```ssh ubuntu@<ip-address>```
3. (Optional) Create a new user:
   1. `sudo adduser <username>`
   2. `sudo usermod -aG sudo <username>`
   3. Close ssh session `CTRL-D` and reconnect `ssh <username>@<ip-address>`

## Next Step

Now, you can follow the [docker setup guide](/user/howto/setup-docker.md).
