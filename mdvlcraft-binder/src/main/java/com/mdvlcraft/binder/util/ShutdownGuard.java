package com.mdvlcraft.binder.util;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * After {@code stop}, a dedicated server saves the world and ends its main thread, but the process only
 * exits once every non-daemon thread has finished. A mod in the pack leaves a non-daemon thread pool
 * running once a player has joined, so the process never exits and restart scripts hang. Once the server
 * has fully stopped (everything is saved by then), give other threads a grace period, then exit.
 */
@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class ShutdownGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long GRACE_MILLIS = 15_000L;

    private ShutdownGuard() {
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        if (!event.getServer().isDedicatedServer()) {
            return;
        }
        Thread guard = new Thread(() -> {
            try {
                Thread.sleep(GRACE_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
            List<String> lingering = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && !t.isDaemon() && t != Thread.currentThread() && !"DestroyJavaVM".equals(t.getName()))
                .map(Thread::getName)
                .toList();
            LOGGER.warn("Server stopped but the process is still running; threads keeping it alive: {}. Exiting.", lingering);
            // exit runs the shutdown hooks (log flushing); halt if one of them hangs
            Thread fallback = new Thread(() -> {
                try {
                    Thread.sleep(GRACE_MILLIS);
                } catch (InterruptedException ignored) {
                }
                Runtime.getRuntime().halt(0);
            }, "MDVLCraft shutdown guard fallback");
            fallback.setDaemon(true);
            fallback.start();
            System.exit(0);
        }, "MDVLCraft shutdown guard");
        guard.setDaemon(true);
        guard.start();
    }
}
