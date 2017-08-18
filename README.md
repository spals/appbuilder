[![][Build Status img]][Build Status]
[![][License img]][License]
[![][Maven Central img]][Maven Central]
 
[Build Status]:https://travis-ci.org/spals/appbuilder
[Build Status img]:https://travis-ci.org/spals/appbuilder.svg?branch=master

[License]:LICENSE
[License img]:https://img.shields.io/badge/license-BSD3-blue.svg

[Maven Central]:https://maven-badges.herokuapp.com/maven-central/net.spals.appbuilder/spals-appbuilder-bom
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/net.spals.appbuilder/spals-appbuilder-bom/badge.svg

# AppBuilder

A framework for building Java and Scala applications with micro-services

# Introduction

The Spals AppBuilder is a framework for constructing applications in Java or Scala using micro-services. Here are some important definitions which will help with understanding this concept:

- _micro-service_: A modular unit of functionality contained within a single Java or Scala class. In practice, micro-services are usually defined by an interface contract and implemented by a class.
- _module_: A group of related micro-services that are tested together and can be shared together within a build system.
- _application_: A container for a set of micro-services which together form a complete solution to a business problem.
- _web application_: An application which requires a web server. Usually, this is to support an HTTP-based API, such as [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) or [GraphQL](https://en.wikipedia.org/wiki/GraphQL). 
- _worker application_: An application which does not require a web server.

Note that a full business solution need not be limited to a single application. In some cases, multiple applications may be created and state shared among them via a syncronizing data store or an asynchronous API (such as one implemented over a pub-sub message queue).

The Spals AppBuilder framework attempts to achieve 3 basic goals (in no particular order):

1. Define and implement a set of micro-services that are common among many applications.
2. Make it easy to define and implement custom micro-services.
3. Make it easy to inject runtime configuration into both pre-defined and custom micro-services.

# Quickstart

This quickstart imagines that we would like to create a calculator application. A natural part of such an application would be a micro-service which performs basic arithmetic functions.

So we are going to define a `ArithmeticCalculator` micro-service to handle this piece of the application. We will then implement the `ArithmeticCalculator` micro-service.

_NOTE_: These are not complete examples. Some parts of the quickstart are left as an exercise for the reader. However, the Spals AppBuilder test suite contains the following complete examples:
 
- A minimally viable [Java application](https://github.com/spals/appbuilder/blob/master/app-dropwizard-test/src/test/java/net/spals/appbuilder/app/dropwizard/minimal/MinimalDropwizardWebApp.java)
- A sample [Java application](https://github.com/spals/appbuilder/blob/master/app-dropwizard-test/src/test/java/net/spals/appbuilder/app/dropwizard/sample/SampleDropwizardWebApp.java) which uses pre-defined micro-services, configures their default implementations, and defines custom micro-services
- A sample [Java application](https://github.com/spals/appbuilder/blob/master/app-dropwizard-test/src/test/java/net/spals/appbuilder/app/dropwizard/plugins/PluginsDropwizardWebApp.java) which uses pre-defined micro-services and configures their alternate implementations (plugins)
- A minimally viable [Scala application](https://github.com/spals/appbuilder/blob/master/app-finatra-test/src/test/scala/net/spals/appbuilder/app/finatra/minimal/MinimalFinatraWebApp.scala)
- A sample [Scala application](https://github.com/spals/appbuilder/blob/master/app-finatra-test/src/test/scala/net/spals/appbuilder/app/finatra/sample/SampleFinatraWebApp.scala)  which uses pre-defined micro-services, configures their default implementations, and defines custom micro-services
- A sample [Scala application](https://github.com/spals/appbuilder/blob/master/app-finatra-test/src/test/scala/net/spals/appbuilder/app/finatra/plugins/PluginsFinatraWebApp.scala) which uses pre-defined micro-services and configures their alternate implementations (plugins)

## Installation

All installation examples within this README show how to add Spals AppBuilder to a [Maven](http://maven.apache.org/) build. However, this is not a pre-requisite for using the AppBuilder framework. All Spals AppBuilder artifacts are published to Maven Central and should be able to be used with any build system which integrates with it.

Whether we're using Java or Scala, we'll want to include the Spals AppBuilder BOM which defines core pieces of the framework as well as all pre-defined micro-services:
```xml
<dependencyManagement>
    <dependencies>
        ...
        <dependency>
            <groupId>net.spals.appbuilder</groupId>
            <artifactId>spals-appbuilder-bom</artifactId>
            <version>${appbuilder.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        ...
    </dependencies>
</dependencyManagement>
```

## Java

Spals AppBuilder integrates with [Dropwizard](http://www.dropwizard.io/1.1.0/docs/) to create Java web applications.

### Installation

In addition to the Spals AppBuilder BOM, we'll add the plugins specifically for Dropwizard:
```xml
<dependencies>
    <dependency>
        <groupId>net.spals.appbuilder.plugins</groupId>
        <artifactId>spals-appbuilder-app-dropwizard</artifactId>
        <version>${appbuilder.version}</version>
    </dependency>
</dependencies>
```

### Define a Micro-Service

Micro-service definitions are made via Java interface contracts.
```java
package com.example.calculator.arithmetic;

/**
* A mciro-service definition for an arthimetic calculator.
*/
public interface ArithmeticCalculator {
    double add(double a, double b);
    
    double divide(double a, double b);
    
    double multiply(double a, double b);
    
    double subtract(double a, double b);
}
```

### Implement a Micro-Service

Micro-services are implemented via Java classes.
```java
package com.example.calculator.arithmetic;

import net.spals.appbuilder.annotations.service.AutoBindSingleton;

/**
* A default implementation of the ArithmeticCalculator micro-service.
*/
@AutoBindSingleton(baseClass = ArithmeticCalculator.class)
class DefaultArithmeticCalculator implements ArithmeticCalculator {
    
    @Override
    public double add(final double a, final double b) {
        return a + b;
    }
    
    @Override
    public double divide(final double a, final double b) {
        return a / b;
    }
    
    ...
}
```

### Use Micro-Service

Let's expose our `ArithmeticCalculator` micro-service in a RESTful API endpoint.
```java
package com.example.calculator.api;

import com.google.inject.Inject;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@AutoBindSingleton
@Path("calculator")
@Produces(MediaType.TEXT_PLAIN)
public class CalculatorResource {
    
    private final ArithmeticCalculator arithmeticCalculator;
    
    @Inject
    DefaultPaymentService(final ArithmeticCalculator arithmeticCalculator) {
        this.arithmeticCalculator = arithmeticCalculator;
    }
    
    @GET
    @Path("add/{a}/{b}")
    public Response add(final double a, final double b) {
        final double result = arithmeticCalculator.add(a, b);
        return Response.ok(result).build();
    }

    @GET
    @Path("divide/{a}/{b}")
    public Response divide(final double a, final double b) {
        final double result = arithmeticCalculator.divide(a, b);
        return Response.ok(result).build();
    }
    
    ...
}
```

### Define Application

Finally, let's tie all of our micro-services together into a Dropwizard application.
```java
package com.example.calculator.app;

public class CalculatorWebApp extends Application<Configuration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorWebApp.class);

    private static final String APP_CONFIG_FILE_NAME = "config/calculator-app.yml";

    public static void main(final String[] args) throws Throwable {
        new CalculatorWebApp().run("server", APP_CONFIG_FILE_NAME);
    }

    private DropwizardWebApp.Builder webAppDelegateBuilder;
    private DropwizardWebApp webAppDelegate;

    @Override
    public void initialize(final Bootstrap<Configuration> bootstrap) {
        this.webAppDelegateBuilder = new DropwizardWebApp.Builder(bootstrap, LOGGER)
            .setServiceScan(new ServiceScan.Builder()
                // Have the Appbuilder framework scan the com.example.calculator
                // package for micro-services
                .addServicePackages("com.example.calculator")
                .build());
    }

    @Override
    public void run(final Configuration configuration, final Environment env) throws Exception {
        this.webAppDelegate = webAppDelegateBuilder.setEnvironment(env).build();
    }
}
```

## Scala

It is possible to translate the Dropwizard application code above into Scala, however the framework also integrates with [Finatra](https://twitter.github.io/finatra/) for more native Scala support.

### Installation

In addition to the Spals AppBuilder BOM, we'll add the plugins specifically for Finatra:
```xml
<dependencies>
    <dependency>
        <groupId>net.spals.appbuilder.plugins</groupId>
        <artifactId>spals-appbuilder-app-finatra</artifactId>
        <version>${appbuilder.version}</version>
    </dependency>
</dependencies>
```

### Define a Micro-Service

Micro-service definitions are made via Scala traits.
```scala
package com.example.calculator.arithmetic

/**
* A mciro-service definition for an arthimetic calculator.
*/
trait ArithmeticCalculator {
    def add(a: Double, b: Double): Double
    
    def divide(a: Double, b: Double): Double
    
    def multiply(a: Double, b: Double): Double
    
    def subtract(a: Double, b: Double): Double
}
```

### Implement a Micro-Service

Micro-services are implemented via Scala classes.
```scala
package com.example.calculator.arithmetic

import net.spals.appbuilder.annotations.service.AutoBindSingleton

/**
* A default implementation of the ArithmeticCalculator micro-service.
*/
@AutoBindSingleton(baseClass = classOf[ArithmeticCalculator])
private[arithmetic] class DefaultArithmeticCalculator extends ArithmeticCalculator {
    
    override def add(a: Double, b: Double): Double = a + b
    
    override def divide(a: Double, b: Double): Double = a / b
    
    ...
}
```

### Use Micro-Service

Let's expose our `ArithmeticCalculator` micro-service in a RESTful API endpoint.
```scala
package com.example.calculator.api

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import net.spals.appbuilder.annotations.service.AutoBindSingleton

@AutoBindSingleton
private[finatra] class CalculatorController @Inject() (
  arithmeticCalculator: ArithmeticCalculator
) extends Controller {

  get("/add/:a/:b") { request: Request =>
    val result = arithmeticCalculator.add(
      request.params("a").toDouble, request.params("b").toDouble)
    response.ok.body(result)
  }

  get("/divide/:a/:b") { request: Request =>
    val result = arithmeticCalculator.divide(
      request.params("a").toDouble, request.params("b").toDouble)
    response.ok.body(result)
  }

  ...
}

```

### Define Application

Finally, let's tie all of our micro-services together into a Finatra application.
```scala
package com.example.calculator.app

import net.spals.appbuilder.app.finatra.FinatraWebApp

class CalculatorWebApp extends FinatraWebApp {

  setServiceScan(new ServiceScan.Builder()
    // Have the Appbuilder framework scan the com.example.calculator
    // package for micro-services
    .addServicePackages("com.example.calculator")
    .build())
  build()
}

```

## Notes

- It is important to keep package names organized because the `ServiceScan` works by scanning for package prefixes. It is recommended that all package names at least start with [com|org|net].[organizationName].[applicationName]
- Support for custom micro-services is complete, however certain predefined services are still in Beta. In particular, the asynchronous message producer and consumer services have not been fully tested.
- This README does not discuss all aspects of the Appbuilder framework. There is a TODO for a wiki which will go into greater detail about individual pieces of the framework 
