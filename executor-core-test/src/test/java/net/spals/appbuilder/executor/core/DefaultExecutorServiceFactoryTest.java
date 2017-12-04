package net.spals.appbuilder.executor.core;

import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockTracer;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultExecutorServiceFactory}
 *
 * @author tkral
 */
public class DefaultExecutorServiceFactoryTest {

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

        assertThat(executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(executorService)));
    }

    @Test
    public void testRegisterCachedThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
            executorServiceFactory.createCachedThreadPool(this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass()).build();

        assertThat(executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(executorService)));
    }

    @Test
    public void testRegisterSingleThreadExecutor() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
            executorServiceFactory.createSingleThreadExecutor(this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass()).build();

        assertThat(executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(executorService)));
    }

    @Test
    public void testRegisterSingleThreadScheduledExecutor() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ScheduledExecutorService scheduledExecutorService =
            executorServiceFactory.createSingleThreadScheduledExecutor(this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass()).build();

        assertThat(executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(scheduledExecutorService)));
    }

    @Test
    public void testRegisterScheduledThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ScheduledExecutorService scheduledExecutorService =
            executorServiceFactory.createScheduledThreadPool(2, this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass()).build();

        assertThat(executorServiceFactory.getExecutorServices(),
            hasEntry(is(expectedKey), sameInstance(scheduledExecutorService)));
    }

    @Test
    public void testStop() {
        final DefaultExecutorServiceFactory executorServiceFactory = spy(new DefaultExecutorServiceFactory(new MockTracer()));

        final ExecutorService executorService1 = mock(ExecutorService.class);
        final ExecutorServiceFactory.Key key1 = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass())
            .addTags("1")
            .build();
        final ExecutorService executorService2 = mock(ExecutorService.class);
        final ExecutorServiceFactory.Key key2 = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass())
            .addTags("2")
            .build();

        executorServiceFactory.getExecutorServices().put(key1, executorService1);
        executorServiceFactory.getExecutorServices().put(key2, executorService2);

        executorServiceFactory.stop();
        verify(executorServiceFactory).stopExecutorService(eq(key1), eq(executorService1));
        verify(executorServiceFactory).stopExecutorService(eq(key2), eq(executorService2));
    }

    private ExecutorService getTracedDelegate(final TracedExecutorService tracedExecutorService) {
        try {
            final Field delegateField = TracedExecutorService.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);
            return (ExecutorService) delegateField.get(tracedExecutorService);
        } catch (NoSuchFieldException|IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
