package net.spals.appbuilder.executor.core;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DefaultExecutorServiceFactory}
 *
 * @author tkral
 */
public class DefaultExecutorServiceFactoryTest {

    @Test
    public void testCreateFixedThreadPool() {
        final ExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
                executorServiceFactory.createFixedThreadPool(2 /*nThreads*/, this.getClass());
        assertThat(executorService, instanceOf(StoppableExecutorService.class));

        final StoppableExecutorService stoppableExecutorService =
                (StoppableExecutorService)executorService;
        assertThat(stoppableExecutorService.getDelegate(), instanceOf(TraceableExecutorService.class));
        assertThat(stoppableExecutorService.getShutdown(), is(1000L));
        assertThat(stoppableExecutorService.getShutdownUnit(), is(TimeUnit.MILLISECONDS));

        final TraceableExecutorService traceableExecutorService =
                (TraceableExecutorService)stoppableExecutorService.getDelegate();
        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)traceableExecutorService.getDelegate();
        assertThat(threadPoolExecutor.getCorePoolSize(), is(2));
        assertThat(threadPoolExecutor.getMaximumPoolSize(), is(2));
        assertThat(threadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS), is(0L));
    }

    @Test
    public void testCreateCachedThreadPool() {
        final ExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
                executorServiceFactory.createCachedThreadPool(this.getClass());
        assertThat(executorService, instanceOf(StoppableExecutorService.class));

        final StoppableExecutorService stoppableExecutorService =
                (StoppableExecutorService)executorService;
        assertThat(stoppableExecutorService.getDelegate(), instanceOf(TraceableExecutorService.class));
        assertThat(stoppableExecutorService.getShutdown(), is(1000L));
        assertThat(stoppableExecutorService.getShutdownUnit(), is(TimeUnit.MILLISECONDS));

        final TraceableExecutorService traceableExecutorService =
                (TraceableExecutorService)stoppableExecutorService.getDelegate();
        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)traceableExecutorService.getDelegate();
        assertThat(threadPoolExecutor.getCorePoolSize(), is(0));
        assertThat(threadPoolExecutor.getMaximumPoolSize(), is(Integer.MAX_VALUE));
        assertThat(threadPoolExecutor.getKeepAliveTime(TimeUnit.SECONDS), is(60L));
    }

    @Test
    public void testCreateSingleThreadExecutor() {
        final ExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
                executorServiceFactory.createSingleThreadExecutor(this.getClass());
        assertThat(executorService, instanceOf(StoppableExecutorService.class));

        final StoppableExecutorService stoppableExecutorService =
                (StoppableExecutorService)executorService;
        assertThat(stoppableExecutorService.getShutdown(), is(1000L));
        assertThat(stoppableExecutorService.getShutdownUnit(), is(TimeUnit.MILLISECONDS));

        // Unable to add more assertions because single thread executor uses a private type
    }

    @Test
    public void testRegisterFixedThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
            executorServiceFactory.createFixedThreadPool(2 /*nThreads*/, this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass()).build();

        assertThat(executorServiceFactory.getStoppableExecutorServices(),
                hasEntry(is(expectedKey), sameInstance(executorService)));
    }

    @Test
    public void testRegisterCachedThreadPool() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
                executorServiceFactory.createCachedThreadPool(this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass()).build();

        assertThat(executorServiceFactory.getStoppableExecutorServices(),
                hasEntry(is(expectedKey), sameInstance(executorService)));
    }

    @Test
    public void testRegisterSingleThreadExecutor() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final ExecutorService executorService =
                executorServiceFactory.createSingleThreadExecutor(this.getClass());
        final ExecutorServiceFactory.Key expectedKey = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass()).build();

        assertThat(executorServiceFactory.getStoppableExecutorServices(),
                hasEntry(is(expectedKey), sameInstance(executorService)));
    }

    @Test
    public void testStop() {
        final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(new MockTracer());

        final StoppableExecutorService executorService1 = mock(StoppableExecutorService.class);
        final ExecutorServiceFactory.Key key1 = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass())
                .addTags("1")
                .build();
        final StoppableExecutorService executorService2 = mock(StoppableExecutorService.class);
        final ExecutorServiceFactory.Key key2 = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass())
                .addTags("2")
                .build();

        executorServiceFactory.getStoppableExecutorServices().put(key1, executorService1);
        executorServiceFactory.getStoppableExecutorServices().put(key2, executorService2);

        executorServiceFactory.stop();
        verify(executorService1).stop();
        verify(executorService2).stop();
    }
}
