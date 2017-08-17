package net.spals.appbuilder.app.finatra

import com.google.common.collect.ImmutableSet
import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import net.spals.appbuilder.annotations.service.AutoBindSingleton
import net.spals.appbuilder.config.service.ServiceScan
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.mockito.ArgumentMatchers.{eq => m_eq}
import org.mockito.Mockito.mock
import org.mockito.Mockito.when
import org.reflections.Reflections
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

/**
  * Functional tests for binding overrides within a [[FinatraWebApp]]
  *
  * @author tkral
  */
class FinatraWebAppOverridesFTest {

  @DataProvider
  def bindingOverridesProvider: Array[Array[AnyRef]] = {
    val customModuleAppAB = new FinatraWebApp {
      addModule(createCustomModule(new MyFinatraSingletonServiceA))
      addModule(createCustomModule(new MyFinatraSingletonServiceB))
      enableBindingOverrides()
      build()
    }

    val customModuleAppBA = new FinatraWebApp {
      addModule(createCustomModule(new MyFinatraSingletonServiceB))
      addModule(createCustomModule(new MyFinatraSingletonServiceA))
      enableBindingOverrides()
      build()
    }

    val customModuleAppSA = new FinatraWebApp {
      setServiceScan(createServiceScan())
      addModule(createCustomModule(new MyFinatraSingletonServiceA))
      enableBindingOverrides()
      build()
    }

    val customModuleAppAS = new FinatraWebApp {
      addModule(createCustomModule(new MyFinatraSingletonServiceA))
      setServiceScan(createServiceScan())
      enableBindingOverrides()
      build()
    }

    val customModuleAppASB = new FinatraWebApp {
      addModule(createCustomModule(new MyFinatraSingletonServiceA))
      setServiceScan(createServiceScan())
      addModule(createCustomModule(new MyFinatraSingletonServiceB))
      enableBindingOverrides()
      build()
    }

    Array(
      Array(customModuleAppAB, classOf[MyFinatraSingletonServiceB]),
      Array(customModuleAppBA, classOf[MyFinatraSingletonServiceA]),
      Array(customModuleAppSA, classOf[MyFinatraSingletonServiceA]),
      Array(customModuleAppAS, classOf[MyFinatraSingleTonServiceAutoBind]),
      Array(customModuleAppASB, classOf[MyFinatraSingletonServiceB])
    )
  }

  @Test(dataProvider = "bindingOverridesProvider")
  def testBindingOverrides(app: FinatraWebApp,
                           expectedServiceClass: Class[MyFinatraSingletonService]) {
    val testServerWrapper = new EmbeddedHttpServer(
      twitterServer = app,
      stage = Stage.PRODUCTION
    )

    try {
      testServerWrapper.start()

      val serviceInjector = app.getServiceInjector
      assertThat(serviceInjector.getInstance(classOf[MyFinatraSingletonService]),
        instanceOf[MyFinatraSingletonService](expectedServiceClass))
    } finally {
      testServerWrapper.close()
    }
  }

  private def createCustomModule(service: MyFinatraSingletonService): Module = {
    new Module() {
      override def configure(binder: Binder): Unit =
        binder.bind(classOf[MyFinatraSingletonService]).toInstance(service)
    }
  }

  private def createServiceScan(): ServiceScan = {
    val reflections = mock(classOf[Reflections])
    when(reflections.getTypesAnnotatedWith(m_eq(classOf[AutoBindSingleton])))
      .thenReturn(ImmutableSet.of[Class[_]](classOf[MyFinatraSingleTonServiceAutoBind]))

    new ServiceScan.Builder().setReflections(reflections).build
  }
}

private trait MyFinatraSingletonService {  }

private class MyFinatraSingletonServiceA extends MyFinatraSingletonService {  }

private class MyFinatraSingletonServiceB extends MyFinatraSingletonService {  }

@AutoBindSingleton(baseClass = classOf[MyFinatraSingletonService])
private class MyFinatraSingleTonServiceAutoBind extends MyFinatraSingletonService {  }
