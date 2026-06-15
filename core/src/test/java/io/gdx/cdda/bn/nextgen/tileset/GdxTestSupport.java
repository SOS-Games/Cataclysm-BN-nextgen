package io.gdx.cdda.bn.nextgen.tileset;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Initializes LibGDX with OpenGL for unit tests that need {@code Texture}. */
public final class GdxTestSupport {

    private static boolean initialized;

    private GdxTestSupport() {}

    public static synchronized void initIfNeeded() {
        if (initialized) {
            return;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final Thread thread = new Thread(() -> {
            final Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
            config.setWindowedMode(1, 1);
            config.disableAudio(true);
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override
                public void create() {
                    latch.countDown();
                }
            }, config);
        }, "gdx-test");
        thread.setDaemon(true);
        thread.start();
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for LibGDX test context");
            }
            while (Gdx.app == null) {
                Thread.sleep(10L);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for LibGDX test context", e);
        }
        initialized = true;
    }

    public static void runOnGdxThread(final ThrowingRunnable action) throws Exception {
        initIfNeeded();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        Gdx.app.postRunnable(() -> {
            try {
                action.run();
            } catch (final Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for LibGDX runnable");
        }
        final Throwable failure = error.get();
        if (failure != null) {
            if (failure instanceof Exception) {
                throw (Exception) failure;
            }
            if (failure instanceof Error) {
                throw (Error) failure;
            }
            throw new Exception(failure);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
