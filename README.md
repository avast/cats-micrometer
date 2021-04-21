# Cats Micrometer

Latest version: [see TC](https://teamcity.ida.avast.com/buildConfiguration/ThreatLabs_CustomerBackendSystems_cats_micrometer_Publish?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds)

This project is an FP wrapper over Micrometer library, adjusted to the needs of CBS team but available to anyone.  
It's currently released only for Scala 2.13.

## Usage

### SBT import

Most of the time you need the `core` module:

```scala
libraryDependencies += "com.avast.cbs" %% "cats-micrometer-core" % "latestVersion"
```

There also exists the API module which contains just trait (suitable for as a library dependency):

```scala
libraryDependencies += "com.avast.cbs" %% "cats-micrometer-api" % "latestVersion"
```

### Code example

This library is a wrapper of standard Micrometer `MeterRegistry` - so it's up to you how/where you get its instance. We can recommend using
[SST](https://avast.github.io/scala-server-toolkit/subprojects/micrometer) but any other way is fine (see e.g. [official way](https://micrometer.io/docs/registry/statsD)).

```scala
import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.syntax.flatMap._
import com.avast.micrometer.CatsMicrometer
import com.avast.micrometer.api.{CatsMeterRegistry, Tag}
import io.micrometer.core.instrument.MeterRegistry

object Readme extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    Blocker[IO].use { blocker =>
      val meterRegistry: MeterRegistry = ??? // TODO create registry
      val catsMeterRegistry: CatsMeterRegistry[IO] = CatsMicrometer.wrapRegistry(meterRegistry, blocker)

      val counter = catsMeterRegistry.counter("theCounter", Tag("fruitType", "apples"))
      val timer = catsMeterRegistry.timer("theTimer", Tag("type", "requests"), Tag("service", "Filerep"))
      val histogram = catsMeterRegistry.summary("processing.summary")

      // you won't probably ever need to touch this...
      val _ = catsMeterRegistry.gauge("theGauge")(IO.pure(42))

      doYourJob() >>
        counter.increment >>
        IO.pure(ExitCode.Success)
    }
  }

  def doYourJob(): IO[Unit] = IO.unit // TODO: do something real
}
```

### Auto-init of metrics

It's often needed to initialize metrics as a part of their setup to see them in a final storage (this lazy model applies e.g. to StatsD
whereas JMX or Prometheus don't need it). The initialization way quite vary for different metrics - e.g. a `1` has to be put into 
COUNTER, `0` to TIMER etc.

This library supports the initialization out-of-the-box and offer it as an optional functionality, so you can use it just on-demand, e.g.
when starting the app in testing environment.  
The default way how to turn it on is to set an _environment variable_ for the app:

```bash
CATS_MICROMETER_INIT=true
```

Another way is to use optional argument of `CatsMicrometer.wrapRegistry` to either force the initialization or, on the other hand, disabling
it for good:
```scala
val catsMeterRegistry: CatsMeterRegistry[IO] = CatsMicrometer.wrapRegistry(meterRegistry, blocker, initStrategy = InitStrategy.DoInit)
```
