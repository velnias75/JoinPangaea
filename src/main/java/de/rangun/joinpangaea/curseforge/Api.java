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

package de.rangun.joinpangaea.curseforge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author heiko
 *
 */
public final class Api { // NOPMD by heiko on 03.02.23, 03:46

	public final static int OK_RESPONSE = 200;

	private final static String ENDPOINT = "https://api.curseforge.com";

	private static Api instance;
	private String apiKey;

	private Api() {

		try (InputStream keyIn = Api.class.getResourceAsStream("/api.json")) {

			this.apiKey = keyIn != null
					? JsonParser.parseReader(new InputStreamReader(keyIn)).getAsJsonObject().get("CurseforgeAPIKey")
							.getAsString()
					: null;

		} catch (IOException e) {

		}
	}

	/**
	 * @return the instance
	 */
	public static Api getInstance() {

		if (instance == null) { // NOPMD by heiko on 03.02.23, 03:51
			instance = new Api();
		}

		return instance;
	}

	public URL getLatestFileFor(final String version) throws IOException, ApiResponseException {

		final JsonArray latestFiles = getResponse("/v1/mods/396246").getAsJsonArray("latestFilesIndexes");

		final List<JsonElement> forVersions = latestFiles.asList().stream().filter(o -> {
			return version.equals(o.getAsJsonObject().get("gameVersion").getAsString());
		}).toList();

		return !forVersions.isEmpty() // NOPMD by heiko on 03.02.23, 03:50
				? getURLForMod(396_246, forVersions.get(0).getAsJsonObject().get("fileId").getAsInt())
				: null;
	}

	public URL getURLForMod(final int projectID, final int fileID)
			throws MalformedURLException, IOException, ApiResponseException {

		final JsonElement downloadUrl = getResponse("/v1/mods/" + projectID + "/files/" + fileID).getAsJsonObject()
				.get("downloadUrl");

		return !downloadUrl.isJsonNull() ? new URL(downloadUrl.getAsString()) : null; // NOPMD by heiko on 03.02.23,
																						// 03:50
	}

	private JsonObject getResponse(final String req) throws IOException, ApiResponseException {

		final URL obj = new URL(ENDPOINT + req);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.addRequestProperty("x-api-key", apiKey);
		con.setRequestMethod("GET");

		if (con.getResponseCode() != OK_RESPONSE) {
			throw new ApiResponseException(con.getResponseCode());
		}

		return JsonParser.parseReader(new BufferedReader(new InputStreamReader(con.getInputStream()))).getAsJsonObject()
				.getAsJsonObject("data").getAsJsonObject();
	}
}
