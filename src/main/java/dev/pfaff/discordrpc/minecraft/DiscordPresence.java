package dev.pfaff.discordrpc.minecraft;

import dev.pfaff.discordrpc.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class DiscordPresence implements ClientModInitializer, PreLaunchEntrypoint {
	private static final Logger LOGGER = LogManager.getLogger();

	public static final int PID = Math.toIntExact(ProcessHandle.current().pid());

	private static final Activity activity = new Activity("Launching", null, System.currentTimeMillis());

	private static final DiscordRPC client = new DiscordRPC("1321685210915536977", 0, DiscordPresence::sendActivity);

	private static final Object sendLock = new Object();

	@Override
	public void onPreLaunch() {
		sendActivity();
	}

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
			activity.details("Playing");
			String name;
			if (mc.isConnectedToLocalServer()) {
				assert mc.getServer() != null;
				name = mc.getServer().getSaveProperties().getLevelName();
			} else {
				assert mc.getCurrentServerEntry() != null;
				name = mc.getCurrentServerEntry().address;
			}
			activity.state("on " + name);
			activity.isInstance(true);
//				activity.timestampStart = System.currentTimeMillis();
			sendActivity();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
			sendMainMenuActivity();
		});
		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
			sendMainMenuActivity();
		});
	}

	private static void sendMainMenuActivity() {
		activity.details("Idling");
		activity.state("on the main menu");
//		activity.timestampStart = System.currentTimeMillis();
		sendActivity();
	}

	private static void sendActivity() {
		Thread.startVirtualThread(() -> {
			synchronized (sendLock) {
				try {
					client.setActivity(PID, activity);
					LOGGER.debug("Set activity");
				} catch (NotConnectedException ignored) {
				} catch (IOException e) {
					LOGGER.error("Caught error while setting activity", e);
				}
			}
		});
	}
}
