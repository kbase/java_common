# Java Common

Contains Java code common to many KBase applications

In particular, this repo includes KB-SDK code that is shared between servers and apps.

## Including the client in your build

See https://jitpack.io/#kbase/java_common/ for instructions on how to include JitPack built
dependencies in your build.

## JavaDoc

JavaDoc is available at
```
https://javadoc.jitpack.io/com/github/kbase/java_common/<version>/javadoc/
```

For example:

https://javadoc.jitpack.io/com/github/kbase/java_common/0.3.0/javadoc/

### Adding and releasing code

* Adding code
  * All code additions and updates must be made as pull requests directed at the develop branch.
    * All tests must pass and all new code must be covered by tests.
    * All new code must be documented appropriately
      * Javadoc
      * General documentation if appropriate
      * Release notes
* Releases
  * The main branch is the stable branch. Releases are made from the develop branch to the main
    branch.
  * Tag the version in git and github.
  * Create a github release.
  * Check that the javadoc is appropriately built on JitPack.

### Testing

```
./gradlew test
```

## Known issues:

### UObjects sharing JsonTokenStreams

When a Jackon `ObjectMapper` is configured with a `us.kbase.common.service.JacksonTupleModule`
and a `us.kbase.common.service.JsonTokenStream` is provided as an input to that mapper for
deserialization, any `us.kbase.common.service.UObject`s created as part of the deserialization
process will share a reference to the same `JsonTokenStream`. This means that under these
circumstances 2 `UObjects` cannot be operated on at the same time or the following exception
will occur:

```
Exception in thread "main" java.io.IOException: Inner parser wasn't closed previously
	at us.kbase.common.service.JsonTokenStream.init(JsonTokenStream.java:298)
	at us.kbase.common.service.JsonTokenStream.setRoot(JsonTokenStream.java:263)
	at us.kbase.common.service.UObject.getPlacedStream(UObject.java:100)
```

This assumes single threaded operation. If the objects are operated on in multiple threads, it's
possible data corruption could result.

Some examples in the code base where a user might encounter this behavior are:
- When a `us.kbase.common.JsonClientCaller` (the basis of all SDK generated Java clients)
  is set to use a file to store the next RPC response (e.g. via `_setFileForNextRpcResponse()` on
  a generated client), and the method returns more than 1 `UObject` (whether in a list, embedded
  in another object, etc.).
- When a `us.kbase.common.JsonServerServlet` (the basis of all SDK generated Java servers/apps)
  accepts a parameter list containing more than one `UObject`.

The workaround for the bug is to only start processing one UObject when processing of the
prior UObject is complete. Do not process UObjects resulting from the same JSON string in
different threads.
