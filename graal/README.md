Some day I will win...

## Configure the project for GraalVM

Add `sbt-assembly` to the `plugins.sbt` file:

```sbt
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
```

Add this to `build.sbt` to configure `assembly` and `graalvm-native`:

```sbt
lazy val `train-station` = project
  // ...
  .enablePlugins(JavaAppPackaging, GraalVMNativeImagePlugin)
  .settings(
    mainClass in Compile := Some("es.eriktorr.train_station.TrainControlPanelApp"),
    graalVMNativeImageCommand := "/Library/Java/JavaVirtualMachines/graalvm-ce-java11-21.0.0.2/Contents/Home/lib/svm/bin/native-image",
    graalVMNativeImageOptions ++= Seq(
      "--verbose",
      "--no-fallback",
      "--allow-incomplete-classpath",
      "--initialize-at-build-time",
      "--report-unsupported-elements-at-runtime",
      "--initialize-at-run-time=org.postgresql.sspi.SSPIClient",
      "-H:+ReportExceptionStackTraces",
      s"-H:Class=es.eriktorr.train_station.TrainControlPanelApp",
      s"-H:JNIConfigurationFiles=${baseDirectory.value}/graal/native-image/jni-config.json",
      s"-H:DynamicProxyConfigurationFiles=${baseDirectory.value}/graal/native-image/proxy-config.json",
      s"-H:ReflectionConfigurationFiles=${baseDirectory.value}/graal/native-image/reflect-config.json",
      s"-H:ResourceConfigurationFiles=${baseDirectory.value}/graal/native-image/resource-config.json",
      s"-H:SerializationConfigurationFiles=${baseDirectory.value}/graal/native-image/serialization-config.json"
    ),
    mainClass in assembly := Some("es.eriktorr.train_station.TrainControlPanelApp"),
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
```

Add this to `Settings`:

```sbt
private[this] def commonSettings(projectName: String): Def.SettingsDefinition =
    Seq(
      // ...
      test in assembly := {},
      assemblyMergeStrategy in assembly := {
        case "module-info.class" => MergeStrategy.discard
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      }
    )
```

## Build Native Image

Build the assembly file and execute it with the `native-image-agent`:

```commandline
jenv exec java -agentlib:native-image-agent=config-output-dir=.,access-filter-file=access-filter.json 
               -jar train-station-assembly-1.0.0.jar
```

This will produce the following files:

```text
jni-config.json              reflect-config.json             serialization-config.json
proxy-config.json            resource-config.json
```

Copy them into the `graal/native-image` directory of the project and build the native image with:

```commandline
jenv exec graalvm-native-image:packageBin
```
