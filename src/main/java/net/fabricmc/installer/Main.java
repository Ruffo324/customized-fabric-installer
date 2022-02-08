/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.installer;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.installer.client.ClientHandler;
import net.fabricmc.installer.util.ArgumentParser;
import net.fabricmc.installer.util.CrashDialog;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Reference;

public class Main {

	@SuppressWarnings("SpellCheckingInspection")
	public static class NiemesControllerConstants {
		public static String InstallerTitleOverride = "Niemes Launcher"; // Utils.BUNDLE.getString("installer.title");
		public static String ImageFileName = "placeholder_ruffo.png"; // Utils.BUNDLE.getString("installer.title");

		public static String VersionId = "1.8.6"; // SEE https://launchermeta.mojang.com/mc/game/version_manifest_v2.json

		public static class ShowInUI {
			public static boolean Image = true;
			public static boolean MinecraftVersionDropdown = true;
			public static boolean MinecraftVersionSnapshotsCheckbox = false;
			public static boolean LoaderVersion = false;
			public static boolean InstallationLocation = true;
			public static boolean CreateProfileCheckbox = false;
			public static boolean InstallButton = true;
			public static boolean StatusText = true;
		}
	}


	public static MetaHandler GAME_VERSION_META;
	public static MetaHandler LOADER_META;

	public static final List<Handler> HANDLERS = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		System.out.println("Loading Fabric Installer: " + Main.class.getPackage().getImplementationVersion());

		HANDLERS.add(new ClientHandler());
//		HANDLERS.add(new ServerHandler());

		ArgumentParser argumentParser = ArgumentParser.create(args);
		String command = argumentParser.getCommand().orElse(null);

		//Can be used if you wish to re-host or provide custom versions. Ensure you include the trailing /
		argumentParser.ifPresent("metaurl", s -> Reference.metaServerUrl = s);

		GAME_VERSION_META = new MetaHandler(Reference.getMetaServerEndpoint("v2/versions/game"));
		LOADER_META = new MetaHandler(Reference.getMetaServerEndpoint("v2/versions/loader"));

		//Default to the help command in a headless environment
		if (GraphicsEnvironment.isHeadless() && command == null) {
			command = "help";
		}

		if (command == null) {
			try {
				InstallerGui.start();
			} catch (Exception e) {
				e.printStackTrace();
				new CrashDialog(e);
			}
		} else if (command.equals("help")) {
			System.out.println("help - Opens this menu");
			HANDLERS.forEach(handler -> System.out.printf("%s %s\n", handler.name().toLowerCase(), handler.cliHelp()));

			LOADER_META.load();
			GAME_VERSION_META.load();

			System.out.printf("\nLatest Version: %s\nLatest Loader: %s\n", GAME_VERSION_META.getLatestVersion(argumentParser.has("snapshot")).getVersion(), Main.LOADER_META.getLatestVersion(false).getVersion());
		} else {
			for (Handler handler : HANDLERS) {
				if (command.equalsIgnoreCase(handler.name())) {
					try {
						handler.installCli(argumentParser);
					} catch (Exception e) {
						throw new RuntimeException("Failed to install " + handler.name(), e);
					}

					return;
				}
			}

			//Only reached if a handler is not found
			System.out.println("No handler found for " + args[0] + " see help");
		}
	}
}
