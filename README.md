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

Make sure [gradle](https://gradle.org/install/) is installed.

Download your `gradle.properties` file by going to [repository browser](http://ci.kbase.us:8081/artifactory/webapp/#/artifacts/browse/tree/General/gradle-dev-local), clicking "Set Me Up" at top right. Place the `gradle.properties` file in the root of the project. It will be gitignored; **do not commit it into the codebase**.

Then run:

```sh
$ gradle wrapper
```

This generates the gradle wrapper files.

Run `gradle build` to install dependencies, run tests, and compile. Run `gradle tasks` to see all available commands.

### Project anatomy

* **Source code** live in `src/main/java/us/kbase/common`
* **Tests** live in `src/test/java`
* **Build files** live in `build/` and jars are built to `build/libs`

### Incrementing version

* Increment the `version` variable in `build.gradle`
* Add a section in `RELEASE_NOTES.txt`
