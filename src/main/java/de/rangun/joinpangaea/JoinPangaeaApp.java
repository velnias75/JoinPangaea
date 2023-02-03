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

package de.rangun.joinpangaea; // NOPMD by heiko on 03.02.23, 04:27

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.rangun.joinpangaea.curseforge.Api;
import de.rangun.joinpangaea.curseforge.ApiResponseException;

/**
 * @author heiko
 *
 */
public final class JoinPangaeaApp { // NOPMD by heiko on 03.02.23, 06:48

	private final static Image APPLOGO = Toolkit.getDefaultToolkit()
			.getImage(JoinPangaeaApp.class.getResource("/server-icon.png"));

	private final static String MC_VERSION = "1.19.2";

	private final static int ONLY_ONE_PROFILE = 1;
	private final static int HAS_ERROR = 1;

	private CompletableFuture<Void> installingFuture;

	private JFrame mainWindow;
	private JLabel currentActionLabel; // NOPMD by heiko on 03.02.23, 04:34
	private JProgressBar currentProgress;
	private JTextArea currentDetail;
	private JButton installButton;
	private JButton quitButton;

	private List<URL> modList;
	private final List<Profile> validProfiles = new ArrayList<>();

	private final static class Manifest {

		/* default */ final JsonObject minecraft;
		/* default */ final JsonArray files;

		public Manifest(final JsonObject minecraft, final JsonArray files) {
			this.minecraft = minecraft;
			this.files = files;
		}
	}

	/* default */ final static class Profile {

		/* default */ final String identifier;
		/* default */ final String name;
		/* default */ final String gameDir;

		public Profile(final String identifier, final String name, final String gameDir) {

			this.identifier = identifier;
			this.name = name;
			this.gameDir = gameDir;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {

		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					final JoinPangaeaApp window = new JoinPangaeaApp();
					window.mainWindow.setVisible(true);
				} catch (Exception e) { // NOPMD by heiko on 03.02.23, 04:31
					e.printStackTrace(); // NOPMD by heiko on 03.02.23, 04:28
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public JoinPangaeaApp() {

		initialize();

		currentActionLabel.setText("Abholen der Mod-Liste …");
		currentProgress.setMinimum(0);
		currentProgress.setMaximum(4);
		currentProgress.setValue(0);

		CompletableFuture.supplyAsync(() -> {

			try {

				appendDetail("Lade Manifest für Minecraft-Version " + MC_VERSION + " herunter …");

				final URL manifestURL = Api.getInstance().getLatestFileFor(MC_VERSION);
				currentProgress.setValue(1);

				appendDetail("Analysiere das Manifest …");

				final Manifest manifest = readManifest(manifestURL);
				currentProgress.setValue(2);

				final File launcherProfiles = new File(
						getMinecraftDir() + File.separatorChar + "launcher_profiles.json");

				if (launcherProfiles.exists() && launcherProfiles.isFile()) {

					appendDetail("\nErmittle geeignete Profile …");

					final JsonObject profiles = JsonParser.parseReader(new FileReader(launcherProfiles)) // NOPMD by
																											// heiko on
																											// 03.02.23,
																											// 04:44
							.getAsJsonObject().get("profiles").getAsJsonObject();

					profiles.asMap().forEach((id, entry) -> {

						final JsonObject jsonEntry = entry.getAsJsonObject();
						final String lastVersionId = jsonEntry.get("lastVersionId").getAsString();

						if (lastVersionId.contains(manifest.minecraft.get("version").getAsString())
								&& lastVersionId.contains("fabric")
								&& lastVersionId.contains(manifest.minecraft.get("modLoaders").getAsJsonArray().asList()
										.get(0).getAsJsonObject().get("id").getAsString().split("\\-")[1])
								&& jsonEntry.has("gameDir")) {

							final Profile profile = new Profile(id, jsonEntry.get("name").getAsString(),
									jsonEntry.get("gameDir").getAsString());

							validProfiles.add(profile);

							appendDetail("Geeignetes Profil:\n  Name = " + profile.name + "\n  Spielverzeichnis = "
									+ profile.gameDir);
						}
					});

					currentProgress.setValue(3);

					if (!validProfiles.isEmpty()) { // NOPMD by heiko on 03.02.23, 04:31

						appendDetail("\nErmittle Mods zum downloaden …");

						modList = getModDownloadURLList(manifest);
						currentProgress.setValue(4);

					} else {
						error("Kein nutzbares Profil gefunden.");
					}
				} else {
					error("Kann " + launcherProfiles.getPath() + " nicht öffnen."); // NOPMD by heiko on 03.02.23, 04:27
				}

			} catch (IOException | ApiResponseException e) {
				error(e);
				throw new CompletionException(e);
			}

			appendDetail(Integer.toString(modList.size()) + " Mods gefunden.\n");

			currentProgress.setValue(0);
			currentActionLabel.setText("Bereit zur Installation.");
			installButton.setEnabled(true);

			return modList;
		});
	}

	private void appendDetail(final String detailText) {
		currentDetail.append(detailText + "\n");
		currentDetail.setCaretPosition(currentDetail.getDocument().getLength());
	}

	private void error(final Exception e) { // NOPMD by heiko on 03.02.23, 04:33
		error(e.getLocalizedMessage()); // NOPMD by heiko on 03.02.23, 04:27
	}

	private void error(final String msg) {

		final Font font = currentActionLabel.getFont();

		currentActionLabel.setText("Fehler");
		currentActionLabel.setForeground(Color.RED);
		currentActionLabel.setFont(font.deriveFont(font.getStyle() | Font.BOLD));

		appendDetail(msg);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() { // NOPMD by heiko on 03.02.23, 04:31

		mainWindow = new JFrame();
		mainWindow.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) { // NOPMD by heiko on 03.02.23, 06:49

				if (!(installingFuture != null && !installingFuture.isDone())) {

					mainWindow.dispose();
					System.exit(0); // NOPMD by heiko on 03.02.23, 06:47
				}
			}
		});

		mainWindow.setResizable(false);
		mainWindow.setIconImage(APPLOGO);
		mainWindow.setTitle("JoinPangaea");
		mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainWindow.getContentPane().setLayout(new BoxLayout(mainWindow.getContentPane(), BoxLayout.X_AXIS));

		final JPanel logoPanel = new JPanel();
		mainWindow.getContentPane().add(logoPanel);
		final GridBagLayout gbl_logoPanel = new GridBagLayout();
		gbl_logoPanel.rowWeights = new double[] { 1.0, 0.0 };
		gbl_logoPanel.rowHeights = new int[] { 0, 0 };
		gbl_logoPanel.columnWidths = new int[] { 0 };
		logoPanel.setLayout(gbl_logoPanel);

		final JLabel lblNewLabel = new JLabel();
		lblNewLabel.setVerticalAlignment(SwingConstants.TOP);
		final GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel.insets = new Insets(5, 5, 5, 8);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		logoPanel.add(lblNewLabel, gbc_lblNewLabel);
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setIcon(new ImageIcon(APPLOGO));

		final JLabel lblNewLabel_1 = new JLabel("<html><body>© 2023 by<br>Velnias75</body></html>");
		lblNewLabel_1.setVerticalAlignment(SwingConstants.BOTTOM);
		final GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.insets = new Insets(5, 5, 5, 8);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		logoPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);

		final JPanel mainPanel = new JPanel();
		mainWindow.getContentPane().add(mainPanel);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		mainPanel.add(tabbedPane);

		final JPanel installPanel = new JPanel();
		tabbedPane.addTab("Installation", null, installPanel, null);
		final GridBagLayout gbl_installPanel = new GridBagLayout();
		gbl_installPanel.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0 };
		gbl_installPanel.columnWeights = new double[] { 0.0 };
		gbl_installPanel.rowHeights = new int[] { 0, 0, 300, 0 };
		gbl_installPanel.columnWidths = new int[] { 500 };

		installPanel.setLayout(gbl_installPanel);

		currentActionLabel = new JLabel();
		final GridBagConstraints gbc_currentActionLabel = new GridBagConstraints();
		gbc_currentActionLabel.insets = new Insets(5, 5, 5, 5);

		gbc_currentActionLabel.gridx = 0;
		gbc_currentActionLabel.gridy = 0;
		gbc_currentActionLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_currentActionLabel.anchor = GridBagConstraints.NORTH;

		installPanel.add(currentActionLabel, gbc_currentActionLabel);

		currentProgress = new JProgressBar();
		currentProgress.setToolTipText("Fortschritt …");
		final GridBagConstraints gbc_currentProgress = new GridBagConstraints();
		gbc_currentProgress.insets = new Insets(5, 5, 5, 5);

		gbc_currentProgress.gridx = 0;
		gbc_currentProgress.gridy = 1;
		gbc_currentProgress.fill = GridBagConstraints.HORIZONTAL;

		installPanel.add(currentProgress, gbc_currentProgress);

		currentDetail = new JTextArea();
		currentDetail.setWrapStyleWord(true);
		currentDetail.setFont(new Font("Monospaced", Font.PLAIN, 12));
		currentDetail.setColumns(1);
		currentDetail.setToolTipText("Aktuelle Aktion …");
		currentDetail.setEditable(false);

		final GridBagConstraints gbc_currentDetail = new GridBagConstraints();
		gbc_currentDetail.fill = GridBagConstraints.BOTH;
		gbc_currentDetail.insets = new Insets(5, 5, 5, 5);
		gbc_currentDetail.weighty = 1.0;

		gbc_currentDetail.gridx = 0;
		gbc_currentDetail.gridy = 2;

		final JScrollPane scrollPane = new JScrollPane(currentDetail);

		installPanel.add(scrollPane, gbc_currentDetail);

		final JPanel panel = new JPanel();
		final GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(5, 5, 5, 5);
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 3;
		installPanel.add(panel, gbc_panel);

		installButton = new JButton("Installiere");
		installButton.setMnemonic('I');
		installButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) { // NOPMD by heiko on 03.02.23, 04:33

				installButton.setEnabled(false);

				installingFuture = CompletableFuture.runAsync(new Runnable() {

					@Override
					public void run() { // NOPMD by heiko on 03.02.23, 13:10

						quitButton.setEnabled(false);

						final int[] hasError = { 0 }; // NOPMD by heiko on 03.02.23, 04:26

						String gameDir;

						currentActionLabel.setText("Wähle in zu installierendes Profil aus …");

						if (validProfiles.size() == ONLY_ONE_PROFILE) {
							gameDir = validProfiles.get(0).gameDir;
						} else {

							final ProfileChooserDialog profileChooserDlg = new ProfileChooserDialog(mainWindow,
									validProfiles);
							profileChooserDlg.setVisible(true);

							gameDir = profileChooserDlg.gameDir;
						}

						if (gameDir != null) {

							currentActionLabel.setText("Installiere …");

							currentProgress.setMinimum(0);
							currentProgress.setMaximum(modList.size() + 1);

							modList.forEach(mod -> {

								final String fileName = mod.getFile().substring(mod.getFile().lastIndexOf('/') + 1);

								final File destFile = new File(
										gameDir + File.separatorChar + "mods" + File.separatorChar + fileName);

								if (!destFile.exists()) { // NOPMD by heiko on 03.02.23, 04:31

									destFile.getParentFile().mkdirs();

									try (FileOutputStream fileOutputStream = new FileOutputStream(destFile)) { // NOPMD
																												// by
																												// heiko
																												// on
																												// 03.02.23,
																												// 04:44

										appendDetail("Lade herunter: " + fileName + " …");

										final ReadableByteChannel readableByteChannel = Channels // NOPMD by heiko on
																									// 03.02.23, 04:34
												.newChannel(mod.openStream());

										final FileChannel fileChannel = fileOutputStream.getChannel();
										fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

									} catch (IOException e) {

										error("Fehler beim Herunterladen: " + e); // NOPMD by heiko on 03.02.23, 13:06
										hasError[0] = 1;
										throw new CompletionException(e);

									} finally {
										installButton.setEnabled(hasError[0] == HAS_ERROR);
										quitButton.setEnabled(true);
									}

								} else {
									appendDetail("Überspringe: " + fileName + " …");
								}

								currentProgress.setValue(currentProgress.getValue() + 1);

							});

							appendDetail("\nErzeuge Mod-Konfiguration …");

							URI resource;

							try {

								resource = getClass().getResource("").toURI();

								try (FileSystem fileSystem = FileSystems.newFileSystem(resource,
										Collections.<String, String>emptyMap(),
										Thread.currentThread().getContextClassLoader())) {

									final Path jarPath = fileSystem.getPath("/config");

									Files.walkFileTree(jarPath, new SimpleFileVisitor<Path>() {

										private Path currentTarget;

										@Override
										public FileVisitResult preVisitDirectory(final Path dir,
												final BasicFileAttributes attrs) throws IOException {

											currentTarget = Paths.get(gameDir + File.separatorChar + "config")
													.resolve(jarPath.relativize(dir).toString());
											Files.createDirectories(currentTarget);

											return FileVisitResult.CONTINUE;
										}

										@Override
										public FileVisitResult visitFile(final Path file,
												final BasicFileAttributes attrs) throws IOException {

											final Path target = Paths.get(gameDir + File.separatorChar + "config")
													.resolve(jarPath.relativize(file).toString());

											if (!(new File(target.toUri())).exists()) { // NOPMD by heiko on 03.02.23,
																						// 13:07
												Files.copy(file,
														Paths.get(gameDir + File.separatorChar + "config")
																.resolve(jarPath.relativize(file).toString()),
														StandardCopyOption.REPLACE_EXISTING);
											} else {
												appendDetail(target.toString()
														+ " übersprungen, da Datei bereits existiert");
											}

											return FileVisitResult.CONTINUE;
										}
									});

									final Path sources[] = {
											Paths.get(getMinecraftDir() + File.separatorChar + "servers.dat"),
											Paths.get(getMinecraftDir() + File.separatorChar + "options.txt") };

									for (final Path source : sources) {

										final Path target = Paths.get(gameDir).resolve(source.getFileName());

										if (!(new File(target.toUri())).exists()) { // NOPMD by heiko on 03.02.23, 13:50
											Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
										} else {
											appendDetail(target + " übersprungen, da Datei bereits existiert");
										}
									}

								} catch (IOException e) {

									error("Fehler beim Erzeugen der Konfiguration:" + e); // NOPMD by heiko on 03.02.23,
																							// 13:06
									hasError[0] = 1;
									throw new CompletionException(e);

								} finally {
									installButton.setEnabled(hasError[0] == HAS_ERROR);
									quitButton.setEnabled(true);
								}

							} catch (URISyntaxException e1) {
								error("Fehler beim Erzeugen der Konfiguration:" + e); // NOPMD by heiko on 03.02.23,
								// 13:06
								hasError[0] = 1;
								throw new CompletionException(e1);

							} finally {
								installButton.setEnabled(hasError[0] == HAS_ERROR);
								quitButton.setEnabled(true);
							}

							currentProgress.setValue(currentProgress.getValue() + 1);

							if (hasError[0] == HAS_ERROR) {
								installButton.setEnabled(true);
							} else {

								final String instFinished = "Installation beendet."; // NOPMD by heiko on 03.02.23,
																						// 04:36

								appendDetail("\n" + instFinished);
								currentActionLabel.setText(instFinished);
							}

						} else {

							final String instCancelled = "Installation abgebrochen."; // NOPMD by heiko on 03.02.23,
																						// 04:36

							installButton.setEnabled(true);

							appendDetail(instCancelled);
							currentActionLabel.setText(instCancelled);
						}

						quitButton.setEnabled(true);
					}
				});
			}
		});

		installButton.setEnabled(false);
		panel.add(installButton);

		quitButton = new JButton("Beenden");
		quitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) { // NOPMD by heiko on 03.02.23, 06:49
				mainWindow.dispose();
			}
		});
		quitButton.setMnemonic('B');
		panel.add(quitButton);

		final JPanel settingsPanel = new JPanel();
		tabbedPane.addTab("Einstellungen", null, settingsPanel, null);

		mainWindow.pack();
		mainWindow.setLocationRelativeTo(null);
	}

	private List<URL> getModDownloadURLList(final Manifest manifest)
			throws IOException, ApiResponseException, ProtocolException {

		final List<URL> urls = new ArrayList<>();

		manifest.files.forEach(file -> {

			if (!file.isJsonNull()) {

				URL url = null;

				try {
					url = Api.getInstance().getURLForMod(file.getAsJsonObject().get("projectID").getAsInt(),
							file.getAsJsonObject().get("fileID").getAsInt());
				} catch (IOException | ApiResponseException e1) {
				}

				if (url != null) {
					urls.add(url);
				}
			}

		});

		return urls;
	}

	private Manifest readManifest(final URL zipURL) throws IOException, ApiResponseException, ProtocolException {

		final HttpURLConnection con = (HttpURLConnection) zipURL.openConnection();

		con.setRequestMethod("GET");

		try (ZipInputStream zis = new ZipInputStream(con.getInputStream())) {

			ZipEntry entry;

			final byte[] buffer = new byte[1024];
			final List<Byte> manifest = new ArrayList<>();

			while ((entry = zis.getNextEntry()) != null) {

				if ("manifest.json".equals(entry.getName()) && !entry.isDirectory()) { // NOPMD by heiko on 03.02.23,
																						// 04:41

					int len;

					while ((len = zis.read(buffer)) > 0) {
						for (int i = 0; i < len; ++i) {
							manifest.add(buffer[i]);
						}
					}
				}
			}

			final StringBuilder stringBuilder = new StringBuilder(manifest.size());

			for (final Byte b : manifest.toArray(new Byte[0])) {
				stringBuilder.append((char) b.byteValue());
			}

			final JsonObject jsonManifest1 = JsonParser.parseString(stringBuilder.toString()).getAsJsonObject();

			return new Manifest(jsonManifest1.get("minecraft").getAsJsonObject(),
					jsonManifest1.get("files").getAsJsonArray());
		}
	}

	private String getMinecraftDir() {

		final String OS = (System.getProperty("os.name")).toUpperCase(Locale.ROOT); // NOPMD by heiko on 03.02.23, 04:34
		String dir;

		if (OS.contains("WIN")) {
			dir = System.getenv("AppData");
		} else {
			dir = System.getProperty("user.home");
		}

		return dir + File.separatorChar + ".minecraft";
	}
}
