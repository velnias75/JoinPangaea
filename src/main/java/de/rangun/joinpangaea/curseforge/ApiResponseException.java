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

/**
 * @author heiko
 *
 */
public final class ApiResponseException extends Exception {

	private static final long serialVersionUID = 2543184228432156830L;

	private final int responseCode;

	/**
	 * @param responseCode
	 */
	public ApiResponseException(final int responseCode) { // NOPMD by heiko on 03.02.23, 03:54
		this.responseCode = responseCode;
	}

	/**
	 * @return the code
	 */
	public int getResponseCode() {
		return responseCode;
	}

	@Override
	public String getMessage() {

		String msg;

		switch (responseCode) {
		case 200:
			msg = "OK";
			break;
		case 403:
			msg = "Verboten (fehlt der API-Key?)";
			break;
		case 404:
			msg = "Nicht gefunden";
			break;
		case 500:
			msg = "Intener Server Fehler";
			break;
		default:
			msg = "Unbekannter Fehler";
			break;
		}

		return msg + " (HTTP-Antwort-Code: " + responseCode + ")";
	}
}
