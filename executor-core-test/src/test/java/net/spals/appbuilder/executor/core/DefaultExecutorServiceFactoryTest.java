package net.spals.appbuilder.executor.core;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
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

    private static final Key KEY_1 = new Key.Builder(DefaultExecutorServiceFactoryTest.class)
        .addTags("1")
        .build();
    private static final Key KEY_2 = new Key.Builder(DefaultExecutorServiceFactoryTest.class)
        .addTags("2")
        .build();

    @Test
    public void testCreateFixedThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService = executorServiceFactory.createFixedThreadPool(2, KEY_1);
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

        final ExecutorService executorService = executorServiceFactory.createCachedThreadPool(KEY_1);
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

        final ExecutorService executorService = executorServiceFactory.createSingleThreadExecutor(KEY_1);
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
            executorServiceFactory.createSingleThreadScheduledExecutor(KEY_1);
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
            executorServiceFactory.createScheduledThreadPool(2, KEY_1);
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

        final ExecutorService executorService = executorServiceFactory.createFixedThreadPool(2, KEY_1);

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(KEY_1), sameInstance(executorService))
        );
    }

    @Test
    public void testRegisterCachedThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService = executorServiceFactory.createCachedThreadPool(KEY_1);

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(KEY_1), sameInstance(executorService))
        );
    }

    @Test
    public void testRegisterSingleThreadExecutor() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService = executorServiceFactory.createSingleThreadExecutor(KEY_1);

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(KEY_1), sameInstance(executorService))
        );
    }

    @Test
    public void testRegisterSingleThreadScheduledExecutor() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ScheduledExecutorService scheduledExecutorService =
            executorServiceFactory.createSingleThreadScheduledExecutor(KEY_1);

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(KEY_1), sameInstance(scheduledExecutorService))
        );
    }

    @Test
    public void testRegisterScheduledThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ScheduledExecutorService scheduledExecutorService =
            executorServiceFactory.createScheduledThreadPool(2, KEY_1);

        assertThat(
            executorServiceFactory.getExecutorServices(),
            hasEntry(is(KEY_1), sameInstance(scheduledExecutorService))
        );
    }

    @Test
    public void testStopAll() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());
        final ExecutorService executorService1 = mock(ExecutorService.class);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService1);
        final ExecutorService executorService2 = mock(ExecutorService.class);
        executorServiceFactory.getExecutorServices().put(KEY_2, executorService2);

        executorServiceFactory.stopAll();

        verify(executorService1).shutdown();
        verify(executorService2).shutdown();
    }

    @Test
    public void testStop() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());
        final ExecutorService executorService = mock(ExecutorService.class);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService);

        executorServiceFactory.stop(KEY_1);
        verify(executorService).shutdown();
    }

    @Test
    public void testStopMissingKey() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));

        catchException(() -> executorServiceFactory.stop(KEY_1));
        assertThat(caughtException(), nullValue());
    }

    @Test
    public void testGetWith() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));
        final ExecutorService executorService = mock(ExecutorService.class);
        executorServiceFactory.getExecutorServices().put(KEY_1, executorService);

        final Optional<ExecutorService> actual = executorServiceFactory.get(KEY_1);

        assertThat(actual, is(Optional.of(executorService)));
    }

    @Test
    public void testGetMissingKey() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));

        final Optional<ExecutorService> actual = executorServiceFactory.get(KEY_1);

        assertThat(actual, is(Optional.empty()));
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
