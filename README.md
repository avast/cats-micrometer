# Cats Micrometer

Latest
version: [see TC](https://teamcity.ida.avast.com/buildConfiguration/ThreatLabs_CustomerBackendSystems_cats_micrometer_Publish?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds)

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
[SST](https://avast.github.io/scala-server-toolkit/subprojects/micrometer) but any other way is fine (see
e.g. [official way](https://micrometer.io/docs/registry/statsD)).

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

      catsMeterRegistry.initIfConfigured() >>
        doYourJob() >>
        counter.increment >>
        IO.pure(ExitCode.Success)
    }
  }

  def doYourJob(): IO[Unit] = IO.unit // TODO: do something real
}

```

### Auto-init of metrics

TLDR: use `meterRegistry.initIfConfigured()` at the end of your setup and set `CATS_MICROMETER_INIT=true` env/system property whenever you
want to do the metrics initialization (so that they appear in Grafana).

---

It's often needed to initialize metrics as a part of their setup to see them in a final storage (this lazy model applies e.g. to StatsD
whereas JMX or Prometheus don't need it). The initialization way quite vary for different metrics - e.g. a `1` has to be put into
COUNTER, `0` to TIMER etc.

This library supports the initialization out-of-the-box and offer it as an optional functionality.  
You have to add `meterRegistry.initIfConfigured()` at the place where you want the initialization to be done (usually, it's the last thing
you'd like to do in your application setup) and then you can turn the initialization by setting an _environment variable_ for the app:

  ```bash
  CATS_MICROMETER_INIT=true
  ```

In case of BUTT-deployed app, your `runner.yaml` could look like this then:

```yaml
---
name: ff_app
class: ...
jvm:
  - "-Dconfig.file=config/application.conf"
  - "-Dio.netty.leakDetection.level=advanced"
  - "-XX:OnOutOfMemoryError=kill -9 %p "
env: "CATS_MICROMETER_INIT=true"
  ...
```

## App setup with Avast StatsD

In case you use SST stack (means `MonixServerApp`, Micrometer+StatsD+Pureconfig
bundle, [http4s server module](https://avast.github.io/scala-server-toolkit/subprojects/http4s), ...), you'll have a similar code in your
app setup:

```scala
val program: Resource[Task, Server[Task]] = for {
  executorModule <- ExecutorModule.makeDefault[Task]
  _ <- Resource.eval(logger.info("=============== LAUNCH APPLICATION ==============="))
  config <- ConfigModule.load
  meterRegistry <-
    MicrometerStatsDModule
      .make[Task](config.statsd, namingConvention = Some(NamingConvention.dot))
      .map(CatsMicrometer.wrapRegistry(_, executorModule.blocker))
  _ <- Resource.eval(MicrometerJvmModule.make[Task](meterRegistry.underlying))
  clock = Clock.create[Task]
  serverMetricsModule <- Resource.eval(MicrometerHttp4sServerMetricsModule.make[Task](meterRegistry.underlying, clock))
  // TODO rest of your app setup ;-)
  routes = Http4sRouting.make(serverMetricsModule.serverMetrics(???))
  server <- Http4sBlazeServerModule.make[Task](config.http4s, routes, executorModule.executionContext)
  _ <- Resource.eval(meterRegistry.initIfConfigured()) // see above!
  _ <- Resource.eval(logger.info("=============== APPLICATION STARTED ==============="))
} yield server
```

where:

```scala
object ConfigModule {
  def load: Resource[Task, AppConfiguration] =
    Resource.eval(Task {
      ConfigSource.default.loadOrThrow[AppConfiguration]
    })
}

final case class AppConfiguration(
                                   statsd: MicrometerStatsDConfig,
                                   http4s: Http4sBlazeServerConfig
                                   // TODO rest of you app configuration ;-)
                                 )

```

...and in your `reference.conf`:

- BUTT deployed app:
  ```hocon
  statsd {
    host = "localhost"
    prefix = "app_name." // may be ff_* for older apps ;-)
  }
  ```
- LUFT deployed app:
  See [CML (section "Foreign host metrics")](https://cml.avast.com/pages/viewpage.action?pageId=19797802).
  ```hocon
  statsd {
    host = "statsd.prg5.ff.avast.com"
    port = 8126
    prefix = "test.app_name." // or prod!
  }
  ```

Few things worth explicit mentioning:

- `namingConvention = Some(NamingConvention.dot)` - without it, a camelCase conversion is applied to the metrics - you don't want that
- `prefix = "app_name."` - as [described on CML](https://cml.avast.com/pages/viewpage.action?pageId=19797802), mind the dot at the end!

