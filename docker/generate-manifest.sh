docker manifest create openbaseorg/bco:experimental openbaseorg/bco:experimental-amd64 openbaseorg/bco:experimental-arm32
docker manifest annotate openbaseorg/bco:experimental openbaseorg/bco:experimental-arm32 --arch arm
docker manifest push --purge openbaseorg/bco:experimental
docker manifest create openbaseorg/bco-device-manager-openhab:experimental openbaseorg/bco-device-manager-openhab:experimental-amd64 openbaseorg/bco-device-manager-openhab:experimental-arm32
docker manifest annotate openbaseorg/bco-device-manager-openhab:experimental openbaseorg/bco-device-manager-openhab:experimental-arm32 --arch arm
docker manifest push --purge openbaseorg/bco-device-manager-openhab:experimental

docker manifest create openbaseorg/spread:experimental openbaseorg/spread:experimental-amd64 openbaseorg/spread:experimental-arm32
docker manifest annotate openbaseorg/spread:experimental openbaseorg/spread:experimental-arm32 --arch arm
docker manifest push --purge openbaseorg/spread:experimental
docker manifest create openbaseorg/spread:latest openbaseorg/spread:latest-amd64 openbaseorg/spread:latest-arm32
docker manifest annotate openbaseorg/spread:latest openbaseorg/spread:latest-arm32 --arch arm
docker manifest push --purge openbaseorg/spread:latest
