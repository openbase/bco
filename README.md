# Base Cube One

[![Dev](https://github.com/openbase/bco/actions/workflows/build-and-test.yml/badge.svg?branch=dev)](https://github.com/openbase/bco/actions/workflows/build-and-test.yml)
[![Latest Version](https://img.shields.io/maven-central/v/org.openbase/bco.dal.remote?label=Latest%20Version)](https://central.sonatype.com/artifact/org.openbase/bco.dal.remote)

Smart environment automation featured by [openbase.org](https://openbase.org).

## Features
- Install smart home apps in your rooms with just a few clicks to add new features
- A next-generation automation system that goes beyond traditional rules
  - Adapts to your goals and preferences
  - Smart priority handling for smooth interactions
- Automatic conflict resolution so your smart home always behaves the way you expect
- Understands your home environment to enable more intuitive automation

## Integrate BCO into your Home Assistant instance with our add-on collection:
[![Open your Home Assistant instance and show the add add-on repository dialog with a specific repository URL pre-filled.](https://my.home-assistant.io/badges/supervisor_add_addon_repository.svg)](https://my.home-assistant.io/redirect/supervisor_add_addon_repository/?repository_url=https%3A%2F%2Fgithub.com%2Fopenbase%2Fhomeassistant.addons.bco)

## Hands on

* [Documentation](https://basecubeone.org)
* [Installation](https://basecubeone.org/docs/user/installatio)

## Contribution

Feel free to report feature requests and discovered bugs via [github](https://github.com/openbase/bco/issues/new).
- If you want to contribute to bco, just fork the repositories, apply your changes and create a new pull request.
- For long term contribution you are welcome to apply for an openbase membership via support@openbase.org or by joining our [Discord Server](https://discord.com/invite/M48eh76f?utm_source=Discord%20Widget&utm_medium=Connect).

## Development

### How to build BCO

* [Please follow our developer tool chain setup guide.](https://basecubeone.org/docs/developer)

### Update Gradle Dependencies

We are using a plugin called `Gradle refreshVersions` to manage all our backend dependencies. Thus, all dependencies
declared within `build.gradle.kts` provide a placeholder `_` for their version while each version is maintained within
the `versions.properties`.

```
testImplementation("io.mockk:mockk:_")
```

In order to check for updates just execute `gradle refreshVersions`. Afterwards, you will find all latest versions
within the `versions.properties` file.

```
version.mockk=1.11.0
### available=1.12.0
```

In order to update a dependency, just add the version you prefer to the version declaration in `versions.properties`.

```
version.mockk=1.12.0
```

The next gradle build will use the new dependency version without any further steps being required. Don't forget to sync
your gradle settings within IntelliJ in case you are not using the gradle `auto-reload setting` feature.

Further details about the plugin can be found at: https://jmfayard.github.io/refreshVersions/
