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

package de.rangun.joinpangaea.utils;

import java.io.File;
import java.util.Locale;

/**
 * @author heiko
 *
 */
public final class Utils {

	private Utils() {
	}

	public static String getMinecraftDir() {

		final String OS = (System.getProperty("os.name")).toUpperCase(Locale.ROOT); // NOPMD by heiko on 05.02.23, 02:00
		String dir;

		if (OS.contains("WIN")) {
			dir = System.getenv("AppData");
		} else {
			dir = System.getProperty("user.home");
		}

		return dir + File.separatorChar + ".minecraft";
	}
}
