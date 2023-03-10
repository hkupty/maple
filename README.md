# Maple

[![version](https://img.shields.io/maven-central/v/com.hkupty.maple/maple-core?style=flat-square)](https://mvnrepository.com/artifact/com.hkupty.maple)

Maple is an opinionated backend for [slf4j](https://github.com/qos-ch/slf4j/) that focuses on doing one thing right: Logging structured logs in json format to the console.

## Warning!

Maple is currently in alpha and, while usable, *has not been tested in production yet*.

Please use with caution. Feedback, however, is very welcome.

## Why use maple?

Maple presents itself as an alternative to [logback](https://logback.qos.ch/).
It is designed for a specific use case: When you want to have [structured logging](https://stackify.com/what-is-structured-logging-and-why-developers-need-it/), straight to the console.
This might be a common use-case for jvm apps running in kubernetes.
If that is your use case, you might prefer maple over logback because:

- Maple is specialized for this use-case, working out of the box with sane defaults;
- If you already have [jackson](https://github.com/FasterXML/jackson-core/), [jackson-jr](https://github.com/FasterXML/jackson-jr), [gson](https://github.com/google/gson) or any [jakarta/json-p](https://github.com/jakartaee/jsonp-api) compliant library, maple will use it to write json logs, so no extra dependencies needed;
- It is very optimized, with impressive performance when compared to logback;
- It is also designed not consume almost any runtime memory, so it won't cause GC pressure;
- If you want to configure, the extension config library [maple-yaml-config](maple-yaml-config/README.md) allows you to configure maple in yaml,
which might be a more native configuration format for its runtime environment (i.e. kubernetes);

However, maple doesn't try to replace logback for all its use cases. If you have to log in multiple formats, to a file or any other target, logback might still be your tool of choice.


## Usage

Maple is a backend for slf4j, so you don't need to interact with it directly.

In order to use it, add it to the [build manager of your preference](https://mvnrepository.com/artifact/com.hkupty.maple/maple-core/0.4), for example:

```groovy
// gradle

runtimeOnly 'com.hkupty.maple:maple-core:0.4'

// Maple doesn't have any strict dependencies aside from slf4j.
implementation 'org.slf4j:slf4j-api:2.0.6'

// But for rendering the json messages, you might be using jackson, gson or a JSON-P/JSR-353 compatible library.
// If that's the case, you don't need to add any dependencies because maple will automatically select the one available.
// If you don't have any, we recommend using jackson-core, at least version 2.12:
// runtimeOnly 'com.fasterxml.jackson.core:jackson-core:2.12.0'
```

:warning: Note that maple is built targeting JVM 17+.

By default, you will get log level `INFO` enabled as well as the following fields:
- `timestamp`
- `level`
- `message`
- `logger`
- `thread`
- `mdc`
- `markers`
- `data` (slf4j's 2.0 `.addKeyValue()`)
- `throwable`

Maple has support for logging also a `Counter` to each message, individually marking each message with a monotonically increasing
`long` counting from process startup, but that is disabled by default.

If you want to configure it, maple provides a separate convenience library for configuring your log levels in yaml files:
```yaml
# resources/maple.yaml

# This is for configuring the root level
maple:
  # We don't need to set level because by default it is set to INFO
  fields:
    # Not that it matter for json, but the key-value pairs below will be rendered in this order.
    # So, for human readability in the console, one can tweak the position of the fields:
    - level
    - logger
    - thread
    - data
    - mdc
    - markers
    - message
    - throwable
  loggers:
    # This map will match the logger with the same literal name and all its children loggers, so
    # com.mycompany.myapp as well as com.mycompany.myapp.controllers.MyGreatController and so on..
    com.mycompany.myapp:
      level: debug
      # There's no need to set fields here since it will inherit from the root logger
    com.vendor.noisylib:
      level: warn
      fields:
        # USE WITH CAUTION! You can opt to remove/add fields to the message in different loggers
        # In this example, we're removing `thread`, `data`, `mdc` and `markers` and adding the `counter` field.
        # This means the log messages from `com.vendor.noisylib` will be rendered differently.
        - level
        - logger
        - message
        - throwable
        - counter
```

If you want to use [maple-yaml-config](maple-yaml-config/README.md), you have to add it as a dependency:

```groovy
runtimeOnly 'com.hkupty.maple:maple-yaml-config:0.4'

// We have to add a yaml parser to the classpath for `maple-yaml-config` to work properly.
// Currently we only support `jackson-dataformat-yaml`, but we plan on adding support for other libraries.
runtimeOnly 'com.fasterxml.jackson.core:jackson-core:2.14.2'
runtimeOnly 'com.fasterxml.jackson.core:jackson-databind:2.14.2'
runtimeOnly 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.2'
```

## Principles

### Structured logging

Logging is supposed to provide meaningful information and, with the evolution of log processing platforms,
it is oftentimes difficult to convey the right information as written, natural text, in a way that it
both makes sense for humans and is easy for machines to process.

Instead, we should embrace the notion that logs are effectively data and should be treated as such.

### Lightweight configuration

Maple comes packed with a sane defaults configuration that allows one to plug it and start using immediately.
Although configuration is possible, by rolling with the shipped defaults one can already reap the benefits of structured
logging without having to set up any configuration.

### Unobtrusiveness

The logging framework should not draw out much attention. It should just work.
With that in mind, maple tries to be a simple yet effective component in your architecture.
It should not require you to add in more dependencies. Instead, it should work with whatever you have available.
Also, it should make its best effort to consume the fewer resources as possible, being efficient and sparing your app of GC pauses
when under heavy load.

## Roadmap

- 1.0
  - [x] Stable API
  - [x] Configuration mechanism for fine-tuning logs based on logger name;
  - [x] Json sinks:
    - [x] Jackson
    - [x] Gson
    - [x] Jakarta
  - [ ] 40-50% Test coverage
  - [maple-yaml-config's 1.0 goals](maple-yaml-config/README.md#roadmap)
- 2.0
  - [ ] Tests
    - [ ] Add [arch unit](https://www.archunit.org/) tests
    - [ ] Add generative tests
    - [ ] 70-80% test coverage
  - [ ] Transform log messages:
    - [ ] Allow for custom (through configuration) transformation of log before it is rendered
  - [ ] Native implementation:
    - [ ] MDC
    - [ ] Markers
