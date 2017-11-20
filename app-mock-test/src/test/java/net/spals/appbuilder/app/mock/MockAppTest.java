package net.spals.appbuilder.app.mock;

import com.google.inject.Injector;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.config.service.ServiceScan;
import org.mockito.internal.util.MockUtil;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MockApp}.
 *
 * @author tkral
 */
public class MockAppTest {

    @Test
    public void testAddMockitoSingleton() {
        final AutoBindService mockitoService = mock(AutoBindService.class);
        final MockApp app = new MockApp.Builder(MockAppTest.class)
            .addMockSingleton(mockitoService, AutoBindService.class)
            .build();

        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(AutoBindService.class), sameInstance(mockitoService));
    }

    @Test
    public void testAddMockitoOverride() {
        final MockApp app = new MockApp.Builder(MockAppTest.class)
            .addMockSingleton(mock(AutoBindService.class), AutoBindService.class)
            .setServiceScan(new ServiceScan.Builder()
                .addServicePackages("net.spals.appbuilder.app.mock")
                .build())
            .build();

        final Injector serviceInjector = app.getServiceInjector();
        final AutoBindService service = serviceInjector.getInstance(AutoBindService.class);

        assertThat(MockUtil.isMock(service), is(true));
    }

    @Test
    public void testAddManualMockSingleton() {
        final MockAutoBindService manualMock = new MockAutoBindService();
        final MockApp app = new MockApp.Builder(MockAppTest.class)
            .addMockSingleton(manualMock)
            .build();

        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(AutoBindService.class), sameInstance(manualMock));
    }

    @Test
    public void testAddManualMockOverride() {
        final MockAutoBindService manualMock = new MockAutoBindService();
        final MockApp app = new MockApp.Builder(MockAppTest.class)
            .addMockSingleton(manualMock)
            .setServiceScan(new ServiceScan.Builder()
                .addServicePackages("net.spals.appbuilder.app.mock")
                .build())
            .build();

        final Injector serviceInjector = app.getServiceInjector();
        final AutoBindService service = serviceInjector.getInstance(AutoBindService.class);

        assertThat(service, sameInstance(manualMock));
    }

    private interface AutoBindService {  }

    @AutoBindSingleton(baseClass = AutoBindService.class)
    private static class TestAutoBindService implements AutoBindService {  }

    private static class MockAutoBindService implements AutoBindService, MockSingleton<AutoBindService> {

        @Override
        public Class<AutoBindService> baseClass() {
            return AutoBindService.class;
        }
    }
}


