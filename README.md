# Base Cube One

This is the official repository of the smart environment framework Base Cube One.

* [Documentation](https://basecubeone.org)  
* [User Installation](https://basecubeone.org/user/installation.html)  
* [Contribution](https://basecubeone.org/developer/contribution.html)  
* [Community](https://openbase.org)

## How to build BCO

* [Please follow our developer tool chain setup guide.](https://basecubeone.org/developer/)  

## Update Gradle Dependencies

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
