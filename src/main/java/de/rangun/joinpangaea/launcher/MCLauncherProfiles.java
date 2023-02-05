/*
 * Copyright 2023 by Heiko Schäfer <heiko@rangun.de>
 *
 * This file is part of JoinPangaea.
 *
 * JoinPangaea is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * JoinPangaea is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JoinPangaea.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.rangun.joinpangaea.launcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.rangun.joinpangaea.utils.Utils;

/**
 * @author heiko
 *
 */
public final class MCLauncherProfiles {

	private static MCLauncherProfiles instance;

	private final static String PATH = Utils.getMinecraftDir() + File.separatorChar + "launcher_profiles.json";

	private JsonObject launcherJson;
	private Map<String, JsonObject> profilesMap;

	private final boolean existing;

	private MCLauncherProfiles() throws IOException {

		boolean exists = false;

		try (Reader reader = new InputStreamReader(new FileInputStream(PATH))) { // NOPMD by heiko on 05.02.23, 02:02

			launcherJson = JsonParser.parseReader(reader).getAsJsonObject();
			final JsonObject profiles = launcherJson.get("profiles").getAsJsonObject();

			profilesMap = new HashMap<>(profiles.size());

			profiles.entrySet().forEach((entry) -> {
				profilesMap.put(entry.getKey(), entry.getValue().getAsJsonObject());
			});

			profilesMap = Collections.unmodifiableMap(profilesMap);
			exists = true;

		} catch (FileNotFoundException e) {

			profilesMap = Collections.emptyMap();

		} finally {
			existing = exists;
		}
	}

	/**
	 * @return the instance
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static MCLauncherProfiles getInstance() throws IOException {

		if (instance == null) { // NOPMD by heiko on 05.02.23, 02:02
			instance = new MCLauncherProfiles();
		}

		return instance;
	}

	public Map<String, JsonObject> getProfiles() {
		return profilesMap;
	}

	public boolean isExisting() {
		return existing;
	}

	/**
	 * @return
	 */
	public String getPath() {
		return PATH;
	}

	/**
	 * @param identifier
	 */
	public void setIcon(final String identifier) {

		final JsonObject icon = profilesMap.get(identifier);

		if (!icon.get("icon").getAsString().contains("base64")) {

			try (InputStream inputStream = MCLauncherProfiles.class.getResourceAsStream("/server-icon.png")) {

				int avail;

				final ByteArrayOutputStream bos = new ByteArrayOutputStream();

				while ((avail = inputStream.available()) != 0) {

					final byte[] buf = new byte[avail]; // NOPMD by heiko on 05.02.23, 03:50
					bos.write(buf, 0, inputStream.read(buf, 0, avail));
				}

				final String image = "data:image/png;base64,"
						+ (new String(Base64.getEncoder().encode(bos.toByteArray())));

				icon.addProperty("icon", image);

				try (FileWriter fileWriter = new FileWriter(PATH)) { // NOPMD by heiko on 05.02.23, 03:50
					fileWriter.write(launcherJson.toString());
				}

			} catch (IOException e) {
			}
		}
	}
}
