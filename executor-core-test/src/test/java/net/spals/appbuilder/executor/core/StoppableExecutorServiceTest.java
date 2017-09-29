package net.spals.appbuilder.executor.core;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StoppableExecutorService}.
 *
 * @author tkral
 */
public class StoppableExecutorServiceTest {

    @DataProvider
    Object[][] loggerNameProvider() {
        return new Object[][] {
                // Case: No tags
                {new ExecutorServiceFactory.Key.Builder()
                        .setParentClass(this.getClass())
                        .build(),
                    "net.spals.appbuilder.executor.core.StoppableExecutorServiceTest[]"},
                // Case: Single tag
                {new ExecutorServiceFactory.Key.Builder()
                        .setParentClass(this.getClass())
                        .addTags("myTag")
                        .build(),
                        "net.spals.appbuilder.executor.core.StoppableExecutorServiceTest[myTag]"},
                // Case: Multiple tags
                {new ExecutorServiceFactory.Key.Builder()
                        .setParentClass(this.getClass())
                        .addTags("myTag1", "myTag2")
                        .build(),
                        "net.spals.appbuilder.executor.core.StoppableExecutorServiceTest[myTag1,myTag2]"},
        };
    }

    @Test(dataProvider = "loggerNameProvider")
    public void testLoggerName(final ExecutorServiceFactory.Key executorServiceKey,
                               final String expectedLoggerName) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final StoppableExecutorService stoppableExecutorService =
            new StoppableExecutorService(executorService, executorServiceKey, 1L, TimeUnit.SECONDS);

        assertThat(stoppableExecutorService.getLogger().getName(), is(expectedLoggerName));
    }

    @Test
    public void testStopAlreadyShutdown() {
        final ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.isShutdown()).thenReturn(true);

        final ExecutorServiceFactory.Key executorServiceKey = new ExecutorServiceFactory.Key.Builder()
            .setParentClass(this.getClass())
            .build();
        final StoppableExecutorService stoppableExecutorService =
             new StoppableExecutorService(executorService, executorServiceKey, 1L, TimeUnit.SECONDS);

        stoppableExecutorService.stop();
        verify(executorService, never()).shutdown();
        verify(executorService, never()).shutdownNow();
    }

    @Test
    public void testStopNoTermination() throws InterruptedException {
        final ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.isShutdown()).thenReturn(false);
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false,true);

        final ExecutorServiceFactory.Key executorServiceKey = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass())
                .build();
        final StoppableExecutorService stoppableExecutorService =
            new StoppableExecutorService(executorService, executorServiceKey, 1L, TimeUnit.SECONDS);

        stoppableExecutorService.stop();
        verify(executorService).shutdown();
        verify(executorService).shutdownNow();
    }

    @Test
    public void testStopErrorTermination() throws InterruptedException {
        final ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.isShutdown()).thenReturn(false);
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        final ExecutorServiceFactory.Key executorServiceKey = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass())
                .build();
        final StoppableExecutorService stoppableExecutorService =
            new StoppableExecutorService(executorService, executorServiceKey, 1L, TimeUnit.SECONDS);

        stoppableExecutorService.stop();
        verify(executorService).shutdown();
        verify(executorService).shutdownNow();
    }

    @Test
    public void testExecute() throws InterruptedException {
        final ExecutorService delegate = mock(ExecutorService.class);

        final ExecutorServiceFactory.Key executorServiceKey = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass())
                .build();
        final StoppableExecutorService stoppableExecutorService =
                new StoppableExecutorService(delegate, executorServiceKey, 1L, TimeUnit.SECONDS);

        final Runnable r = () -> {};
        stoppableExecutorService.execute(r);
        verify(delegate).execute(same(r));
    }

    @Test
    public void testSubmitCallable() {
        final ExecutorService delegate = mock(ExecutorService.class);

        final ExecutorServiceFactory.Key executorServiceKey = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass())
                .build();
        final StoppableExecutorService stoppableExecutorService =
                new StoppableExecutorService(delegate, executorServiceKey, 1L, TimeUnit.SECONDS);

        final Callable<Void> c = () -> (Void)null;
        stoppableExecutorService.submit(c);
        verify(delegate).submit(same(c));
    }

    @Test
    public void testSubmitRunnable() {
        final ExecutorService delegate = mock(ExecutorService.class);

        final ExecutorServiceFactory.Key executorServiceKey = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass())
                .build();
        final StoppableExecutorService stoppableExecutorService =
                new StoppableExecutorService(delegate, executorServiceKey, 1L, TimeUnit.SECONDS);

        final Runnable r = () -> {};
        stoppableExecutorService.submit(r);
        verify(delegate).submit(same(r));
    }

    @Test
    public void testSubmitRunnableResult() {
        final ExecutorService delegate = mock(ExecutorService.class);

        final ExecutorServiceFactory.Key executorServiceKey = new ExecutorServiceFactory.Key.Builder()
                .setParentClass(this.getClass())
                .build();
        final StoppableExecutorService stoppableExecutorService =
                new StoppableExecutorService(delegate, executorServiceKey, 1L, TimeUnit.SECONDS);

        final Runnable r = () -> {};
        stoppableExecutorService.submit(r, 1L);
        verify(delegate).submit(same(r), eq(1L));
    }
}
