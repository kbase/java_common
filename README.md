# KBase Java Common

A collection of generic, common utility classes used throughout the KBase stack.

## Installation

Simple set up: add the maven repository URL to your `build.gradle`:

```
repositories {
    maven {
        url 'http://ci.kbase.us:8081/artifactory/gradle-dev'
    }
}

dependencies {
    // Artifactory dependencies
    implementation 'kbase:java_common:0.5.0'
}
```

## API

Packages:

* `us.kbase.common.exceptions`
* `us.kbase.common.utils`
* `us.kbase.common.mongo`
* `us.kbase.common.performance`
* `us.kbase.common.service`

TODO

## Development

Make sure [gradle](https://gradle.org/install/) is installed. Then run:

```sh
$ gradle wrapper
```

This generates the gradle wrapper files.

Run `./gradlew build` to install dependencies, run tests, and compile. Run `gradle tasks` to see all available commands.

### Building

Run `gradle build` and deploy the file in `build/libs/java-common-X.Y.Z.jar` into the KBase Artifactory.

### Project anatomy

* **Source code** live in `src/main/java/us/kbase/common`
* **Tests** live in `src/test/java`
* **Build files** live in `build/` and jars are built to `build/libs`

### Incrementing version

* Increment the `version` variable in `build.gradle`
* Add a section in `RELEASE_NOTES.txt`
