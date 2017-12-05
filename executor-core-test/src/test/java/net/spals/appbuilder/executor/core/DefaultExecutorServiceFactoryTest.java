package net.spals.appbuilder.executor.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.*;

import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockTracer;
import org.testng.annotations.Test;

import net.spals.appbuilder.executor.core.ExecutorServiceFactory.Key;

/**
 * Unit tests for {@link DefaultExecutorServiceFactory}
 *
 * @author tkral
 */
public class DefaultExecutorServiceFactoryTest {

    private static final ExecutorServiceFactory.Key KEY_1 = new ExecutorServiceFactory.Key.Builder()
        .setParentClass(DefaultExecutorServiceFactoryTest.class)
        .addTags("1")
        .build();
    private static final ExecutorServiceFactory.Key KEY_2 = new ExecutorServiceFactory.Key.Builder()
        .setParentClass(DefaultExecutorServiceFactoryTest.class)
        .addTags("2")
        .build();

    @Test
    public void testCreateFixedThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
            executorServiceFactory.createFixedThreadPool(2, this.getClass());
        assertThat(executorService, instanceOf(TracedExecutorService.class));

        final TracedExecutorService tracedExecutorService = (TracedExecutorService) executorService;
        final ExecutorService delegate = getTracedDelegate(tracedExecutorService);
        assertThat(delegate, instanceOf(ThreadPoolExecutor.class));

        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) delegate;
        assertThat(threadPoolExecutor.getCorePoolSize(), is(2));
        assertThat(threadPoolExecutor.getMaximumPoolSize(), is(2));
        assertThat(threadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS), is(0L));
    }

    @Test
    public void testCreateCachedThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
            executorServiceFactory.createCachedThreadPool(this.getClass());
        assertThat(executorService, instanceOf(TracedExecutorService.class));

        final TracedExecutorService tracedExecutorService = (TracedExecutorService) executorService;
        final ExecutorService delegate = getTracedDelegate(tracedExecutorService);
        assertThat(delegate, instanceOf(ThreadPoolExecutor.class));

        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) delegate;
        assertThat(threadPoolExecutor.getCorePoolSize(), is(0));
        assertThat(threadPoolExecutor.getMaximumPoolSize(), is(Integer.MAX_VALUE));
        assertThat(threadPoolExecutor.getKeepAliveTime(TimeUnit.SECONDS), is(60L));
    }

    @Test
    public void testCreateSingleThreadExecutor() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
            executorServiceFactory.createSingleThreadExecutor(this.getClass());
        assertThat(executorService, instanceOf(TracedExecutorService.class));

        final TracedExecutorService tracedExecutorService = (TracedExecutorService) executorService;
        final ExecutorService delegate = getTracedDelegate(tracedExecutorService);
        assertThat(delegate.getClass().getSimpleName(), is("FinalizableDelegatedExecutorService"));

        // Unable to add more assertions because single thread executor uses a private type
    }

    @Test
    public void testCreateSingleThreadScheduledExecutor() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ScheduledExecutorService scheduledExecutorService =
            executorServiceFactory.createSingleThreadScheduledExecutor(this.getClass());
        assertThat(scheduledExecutorService, instanceOf(TracedScheduledExecutorService.class));

        final TracedScheduledExecutorService tracedScheduledExecutorService = (TracedScheduledExecutorService) scheduledExecutorService;
        final ScheduledExecutorService delegate = tracedScheduledExecutorService.getDelegate();
        assertThat(delegate.getClass().getSimpleName(), is("DelegatedScheduledExecutorService"));

        // Unable to add more assertions because single thread executor uses a private type
    }

    @Test
    public void testCreateScheduledThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ScheduledExecutorService scheduledExecutorService =
            executorServiceFactory.createScheduledThreadPool(2, this.getClass());
        assertThat(scheduledExecutorService, instanceOf(TracedScheduledExecutorService.class));

        final TracedScheduledExecutorService tracedScheduledExecutorService = (TracedScheduledExecutorService) scheduledExecutorService;
        final ScheduledExecutorService delegate = tracedScheduledExecutorService.getDelegate();
        assertThat(delegate, instanceOf(ScheduledThreadPoolExecutor.class));

        final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) delegate;
        assertThat(scheduledThreadPoolExecutor.getCorePoolSize(), is(2));
        assertThat(scheduledThreadPoolExecutor.getMaximumPoolSize(), is(Integer.MAX_VALUE));
        assertThat(scheduledThreadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS), is(0L));
    }

    @Test
    public void testRegisterFixedThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
            executorServiceFactory.createFixedThreadPool(2, this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass()).build();

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(executorService))
        );
    }

    @Test
    public void testRegisterCachedThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
            executorServiceFactory.createCachedThreadPool(this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass()).build();

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(executorService))
        );
    }

    @Test
    public void testRegisterSingleThreadExecutor() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
            executorServiceFactory.createSingleThreadExecutor(this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass()).build();

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(executorService))
        );
    }

    @Test
    public void testRegisterSingleThreadScheduledExecutor() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ScheduledExecutorService scheduledExecutorService =
            executorServiceFactory.createSingleThreadScheduledExecutor(this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass()).build();

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(scheduledExecutorService))
        );
    }

    @Test
    public void testRegisterScheduledThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ScheduledExecutorService scheduledExecutorService =
            executorServiceFactory.createScheduledThreadPool(2, this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass()).build();

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(scheduledExecutorService))
        );
    }

    @Test
    public void testAllStop() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));
        final ExecutorService executorService1 = mock(ExecutorService.class);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService1);
        final ExecutorService executorService2 = mock(ExecutorService.class);
        executorServiceFactory.getExecutorServices().put(KEY_2, executorService2);

        executorServiceFactory.stop();

        verify(executorServiceFactory).stopExecutorService(eq(KEY_1), eq(executorService1));
        verify(executorServiceFactory).stopExecutorService(eq(KEY_2), eq(executorService2));
    }

    @Test
    public void testStop() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));
        final ExecutorService executorService = mock(ExecutorService.class);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService);

        final Optional<Boolean> stopped = executorServiceFactory.stop(KEY_1);

        assertThat(stopped, is(Optional.of(true)));
        verify(executorServiceFactory).stopExecutorService(eq(KEY_1), eq(executorService));
    }

    @Test
    public void testStopAlreadyStopped() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));
        final ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.isShutdown()).thenReturn(true);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService);

        final Optional<Boolean> stopped = executorServiceFactory.stop(KEY_1);

        assertThat(stopped, is(Optional.of(false)));
        verify(executorServiceFactory, never()).stopExecutorService(any(Key.class), any(ExecutorService.class));
    }

    @Test
    public void testStopMissingKey() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));

        final Optional<Boolean> stopped = executorServiceFactory.stop(KEY_1);

        assertThat(stopped, is(Optional.empty()));
        verify(executorServiceFactory, never()).stopExecutorService(any(Key.class), any(ExecutorService.class));
    }

    @Test
    public void testIsTerminatedNotTerminated() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));
        final ExecutorService executorService = mock(ExecutorService.class);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService);

        final Optional<Boolean> terminated = executorServiceFactory.isTerminated(KEY_1);

        assertThat(terminated, is(Optional.of(false)));
    }

    @Test
    public void testIsTerminated() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));
        final ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.isTerminated()).thenReturn(true);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService);

        final Optional<Boolean> terminated = executorServiceFactory.isTerminated(KEY_1);

        assertThat(terminated, is(Optional.of(true)));
    }

    @Test
    public void testIsTerminatedMissingKey() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));

        final Optional<Boolean> terminated = executorServiceFactory.isTerminated(KEY_1);

        assertThat(terminated, is(Optional.empty()));
    }

    @Test
    public void testIsShutdownNotShutdown() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));
        final ExecutorService executorService = mock(ExecutorService.class);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService);

        final Optional<Boolean> shutdown = executorServiceFactory.isShutdown(KEY_1);

        assertThat(shutdown, is(Optional.of(false)));
    }

    @Test
    public void testIsShutdown() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));
        final ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.isShutdown()).thenReturn(true);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService);

        final Optional<Boolean> shutdown = executorServiceFactory.isShutdown(KEY_1);

        assertThat(shutdown, is(Optional.of(true)));
    }

    @Test
    public void testIsShutdownMissingKey() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));

        final Optional<Boolean> shutdown = executorServiceFactory.isShutdown(KEY_1);

        assertThat(shutdown, is(Optional.empty()));
    }

    private ExecutorService getTracedDelegate(final TracedExecutorService tracedExecutorService) {
        try {
            final Field delegateField = TracedExecutorService.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);
            return (ExecutorService) delegateField.get(tracedExecutorService);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
