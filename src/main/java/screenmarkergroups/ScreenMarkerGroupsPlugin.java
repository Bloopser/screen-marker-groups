/*
 * Copyright (c) 2025, Bloopser <https://github.com/Bloopser>
 * Copyright (c) 2018, Kamiel, <https://github.com/Kamielvf>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package screenmarkergroups;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import screenmarkergroups.ui.ScreenMarkerGroupsPluginPanel;
import screenmarkergroups.ui.ScreenMarkerGroupsCreationPanel;

@PluginDescriptor(name = "Screen Marker Groups", description = "Enable drawing of screen markers on top of the client, organized into groups", tags = {
		"boxes", "overlay", "panel", "group", "organize" })
public class ScreenMarkerGroupsPlugin extends Plugin {
	@Provides
	ScreenMarkerGroupsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ScreenMarkerGroupsConfig.class);
	}

	private static final String PLUGIN_NAME = "Screen Marker Groups";
	private static final String CONFIG_GROUP = "screenmarkergroups";
	private static final String CONFIG_KEY_MARKERS = "markerGroups";
	private static final String CONFIG_KEY_ORDER = "groupOrder";
	private static final String CONFIG_KEY_VISIBILITY = "groupVisibility"; // New config key
	private static final String ICON_FILE = "panel_icon.png";
	private static final String DEFAULT_MARKER_NAME = "Marker";
	public static final Dimension DEFAULT_SIZE = new Dimension(2, 2);
	public static final String UNASSIGNED_GROUP = "Unassigned";
	public static final String IMPORTED_GROUP = "Imported";

	@Getter
	private final Map<String, List<ScreenMarkerOverlay>> markerGroups = new ConcurrentHashMap<>();

	@Getter
	private final List<String> groupOrderList = new ArrayList<>();

	// Map to store visibility state for each group
	private final Map<String, Boolean> groupVisibilityStates = new ConcurrentHashMap<>();

	@Inject
	private ConfigManager configManager;

	@Inject
	private ScreenMarkerGroupsConfig config;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	@Getter
	private ScreenMarkerCreationOverlay overlay;

	@Inject
	private Gson gson;

	@Getter
	@Inject
	private ColorPickerManager colorPickerManager;

	@Inject
	private ScreenMarkerWidgetHighlightOverlay widgetHighlight;

	private ScreenMarkerMouseListener mouseListener;
	@Getter
	private ScreenMarkerGroupsPluginPanel pluginPanel;
	private NavigationButton navigationButton;

	@Getter(AccessLevel.PACKAGE)
	private ScreenMarker currentMarker;

	@Getter
	@Setter
	private boolean creatingScreenMarker = false;

	@Getter
	@Setter
	private boolean drawingScreenMarker = false;

	@Getter
	@Setter
	private Rectangle selectedWidgetBounds = null;
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private Point startLocation = null;
	@Getter
	private String targetGroupNameForCreation = null;

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);
		overlayManager.add(widgetHighlight);
		loadGroupsConfig();
		markerGroups.values().stream().flatMap(List::stream).forEach(overlayManager::add);
		pluginPanel = new ScreenMarkerGroupsPluginPanel(this);
		pluginPanel.rebuild();
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/" + ICON_FILE);
		navigationButton = NavigationButton.builder()
				.tooltip(PLUGIN_NAME)
				.icon(icon)
				.priority(5)
				.panel(pluginPanel)
				.build();
		clientToolbar.addNavigation(navigationButton);
		mouseListener = new ScreenMarkerMouseListener(this);
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		overlayManager.remove(widgetHighlight);
		overlayManager.removeIf(ScreenMarkerOverlay.class::isInstance);
		markerGroups.clear();
		groupOrderList.clear();
		groupVisibilityStates.clear(); // Clear visibility state
		clientToolbar.removeNavigation(navigationButton);
		setMouseListenerEnabled(false);
		creatingScreenMarker = false;
		drawingScreenMarker = false;
		pluginPanel = null;
		currentMarker = null;
		mouseListener = null;
		navigationButton = null;
		selectedWidgetBounds = null;
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged profileChanged) {
		overlayManager.removeIf(ScreenMarkerOverlay.class::isInstance);
		markerGroups.clear();
		groupOrderList.clear();
		groupVisibilityStates.clear(); // Clear visibility state
		loadGroupsConfig();
		// Re-add overlays respecting group visibility
		markerGroups.forEach((groupName, overlays) -> {
			if (isGroupVisible(groupName)) {
				overlays.forEach(overlayManager::add);
			}
		});
		if (pluginPanel != null) {
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	public void setMouseListenerEnabled(boolean enabled) {
		if (enabled) {
			mouseManager.registerMouseListener(mouseListener);
		} else {
			mouseManager.unregisterMouseListener(mouseListener);
		}
	}

	/**
	 * Starts the marker creation process, targeting a specific group.
	 * Called primarily from the UI (+ button).
	 */
	public void startCreation(Point location, Dimension size, String groupName) {
		this.targetGroupNameForCreation = markerGroups.containsKey(groupName) ? groupName : UNASSIGNED_GROUP;
		this.creatingScreenMarker = true;
		this.setMouseListenerEnabled(true);
		this.currentMarker = null;
		this.startLocation = null;
		this.drawingScreenMarker = false;

		overlay.setPreferredLocation(null);
		overlay.setPreferredSize(null);

		if (location != null) {
			initializeMarkerCreation(location, size);
		}

		if (pluginPanel != null) {
			pluginPanel.setCreation(true);
		}
	}

	public void startCreation(Point location) {
		startCreation(location, DEFAULT_SIZE, UNASSIGNED_GROUP);
	}

	/**
	 * Initializes the actual ScreenMarker object and overlay state.
	 * Called by the mouse listener upon the first click/drag in the game window
	 * after creation mode is enabled.
	 */
	public void initializeMarkerCreation(Point location, Dimension size) {
		if (currentMarker != null || location == null)
			return;

		long nextMarkerId = markerGroups.values().stream().mapToLong(List::size).sum() + 1;
		currentMarker = new ScreenMarker(
				Instant.now().toEpochMilli(),
				DEFAULT_MARKER_NAME + " " + nextMarkerId,
				ScreenMarkerGroupsPluginPanel.SELECTED_BORDER_THICKNESS,
				ScreenMarkerGroupsPluginPanel.SELECTED_COLOR,
				ScreenMarkerGroupsPluginPanel.SELECTED_FILL_COLOR,
				true,
				false);
		startLocation = location;
		overlay.setPreferredLocation(location);
		overlay.setPreferredSize(size != null ? size : DEFAULT_SIZE);
		drawingScreenMarker = true;
	}

	public void finishCreation(boolean aborted) {
		ScreenMarker marker = currentMarker;
		String targetGroup = targetGroupNameForCreation != null ? targetGroupNameForCreation : UNASSIGNED_GROUP;
		Rectangle overlayBounds = overlay.getBounds();

		if (!aborted && marker != null && overlayBounds != null && overlayBounds.width > 0
				&& overlayBounds.height > 0) {
			// Pass plugin instance 'this' to the constructor
			final ScreenMarkerOverlay screenMarkerOverlay = new ScreenMarkerOverlay(marker, this);
			screenMarkerOverlay.setPreferredLocation(overlayBounds.getLocation());
			screenMarkerOverlay.setPreferredSize(overlayBounds.getSize());

			List<ScreenMarkerOverlay> groupList = markerGroups.computeIfAbsent(targetGroup, k -> new ArrayList<>());
			groupList.add(screenMarkerOverlay);

			if (!groupOrderList.contains(targetGroup)) {
				int insertIndex = groupOrderList.size();
				if (groupOrderList.contains(IMPORTED_GROUP)) {
					insertIndex = groupOrderList.indexOf(IMPORTED_GROUP);
				}
				if (groupOrderList.contains(UNASSIGNED_GROUP)) {
					insertIndex = Math.min(insertIndex, groupOrderList.indexOf(UNASSIGNED_GROUP));
				}
				if (!targetGroup.equals(UNASSIGNED_GROUP) && !targetGroup.equals(IMPORTED_GROUP)) {
					groupOrderList.add(insertIndex, targetGroup);
				} else if (!groupOrderList.contains(targetGroup)) {
					groupOrderList.add(targetGroup);
				}
			}
			overlayManager.saveOverlay(screenMarkerOverlay);
			// Only add overlay if group is visible
			if (isGroupVisible(targetGroup)) {
				overlayManager.add(screenMarkerOverlay);
			}
			updateGroupsConfig(); // This will also save visibility config now
		} else {
			aborted = true;
		}

		creatingScreenMarker = false;
		drawingScreenMarker = false;
		selectedWidgetBounds = null;
		startLocation = null;
		currentMarker = null;
		targetGroupNameForCreation = null;
		setMouseListenerEnabled(false);

		if (pluginPanel != null) {
			pluginPanel.setCreation(false);
			if (!aborted) {
				SwingUtilities.invokeLater(pluginPanel::rebuild);
			}
		}
	}

	public void completeSelection() {
		if (pluginPanel != null && targetGroupNameForCreation != null) {
			ScreenMarkerGroupsCreationPanel creationPanel = pluginPanel.getCreationPanelsMap()
					.get(targetGroupNameForCreation);
			if (creationPanel != null) {
				creationPanel.unlockConfirm();
			}
		}
	}

	public void deleteMarker(final ScreenMarkerOverlay markerToDelete) {
		boolean removed = false;
		for (List<ScreenMarkerOverlay> groupList : markerGroups.values()) {
			if (groupList.remove(markerToDelete)) {
				removed = true;
				break;
			}
		}
		if (removed) {
			overlayManager.remove(markerToDelete);
			overlayManager.resetOverlay(markerToDelete);
			updateGroupsConfig(); // This will also save visibility config now
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	void resizeMarker(Point point) {
		if (startLocation == null) {
			initializeMarkerCreation(point, DEFAULT_SIZE);
			return;
		}
		drawingScreenMarker = true;
		Rectangle bounds = new Rectangle(startLocation);
		bounds.add(point);
		overlay.setPreferredLocation(bounds.getLocation());
		overlay.setPreferredSize(bounds.getSize());
	}

	public void updateGroupsConfig() {
		if (markerGroups.isEmpty()) {
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY_MARKERS);
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY_ORDER);
			// Don't unset visibility here, let updateVisibilityConfig handle it
			updateVisibilityConfig(); // Save potentially empty visibility map
			return;
		} else {
			Map<String, List<ScreenMarker>> groupsToSave = new HashMap<>();
			markerGroups.forEach((groupName, overlayList) -> {
				List<ScreenMarker> markerList = overlayList.stream()
						.map(ScreenMarkerOverlay::getMarker)
						.collect(Collectors.toList());
				groupsToSave.put(groupName, markerList);
			});
			final String markersJson = gson.toJson(groupsToSave);
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_MARKERS, markersJson);
		}

		// Save group order
		List<String> orderToSave = groupOrderList.stream()
				.filter(markerGroups::containsKey)
				.collect(Collectors.toList());

		if (orderToSave.isEmpty()) {
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY_ORDER);
		} else {
			final String orderJson = gson.toJson(orderToSave);
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_ORDER, orderJson);
		}

		// Save visibility states separately
		updateVisibilityConfig();
	}

	/**
	 * Saves the current group visibility states to the config manager.
	 */
	private void updateVisibilityConfig() {
		// Clean up visibility states for groups that no longer exist
		groupVisibilityStates.keySet().retainAll(markerGroups.keySet());

		if (groupVisibilityStates.isEmpty()) {
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY_VISIBILITY);
		} else {
			final String visibilityJson = gson.toJson(groupVisibilityStates);
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_VISIBILITY, visibilityJson);
		}
	}

	private void loadGroupsConfig() {
		markerGroups.clear();
		groupOrderList.clear();
		groupVisibilityStates.clear(); // Clear before loading

		final String markersJson = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_MARKERS);
		if (!Strings.isNullOrEmpty(markersJson)) {
			try {
				final Map<String, List<ScreenMarker>> loadedGroups = gson.fromJson(markersJson,
						new TypeToken<HashMap<String, List<ScreenMarker>>>() {
						}.getType());

				if (loadedGroups != null) {
					loadedGroups.forEach((groupName, markerList) -> {
						List<ScreenMarkerOverlay> overlayList = markerList.stream()
								.filter(Objects::nonNull)
								// Pass plugin instance 'this' using a lambda
								.map(marker -> new ScreenMarkerOverlay(marker, this))
								.collect(Collectors.toList());
						markerGroups.put(groupName, new ArrayList<>(overlayList));
					});
				}
			} catch (Exception e) {
				System.err.println("Error parsing marker groups JSON: " + e.getMessage());
				markerGroups.clear();
			}
		}
		markerGroups.computeIfAbsent(UNASSIGNED_GROUP, k -> new ArrayList<>());

		// Load Group Order
		final String orderJson = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_ORDER);
		List<String> loadedOrder = null;
		if (!Strings.isNullOrEmpty(orderJson)) {
			try {
				loadedOrder = gson.fromJson(orderJson, new TypeToken<ArrayList<String>>() {
				}.getType());
			} catch (Exception e) {
				System.err.println("Error parsing group order JSON: " + e.getMessage());
				loadedOrder = null;
			}
		}

		if (loadedOrder != null) {
			groupOrderList.addAll(loadedOrder.stream()
					.filter(markerGroups::containsKey)
					.collect(Collectors.toList()));
		}

		List<String> groupsToAdd = new ArrayList<>();
		for (String groupName : markerGroups.keySet()) {
			if (!groupOrderList.contains(groupName)) {
				groupsToAdd.add(groupName);
			}
		}
		groupsToAdd.sort(String.CASE_INSENSITIVE_ORDER);

		int insertIndex = groupOrderList.size();
		if (groupOrderList.contains(IMPORTED_GROUP)) {
			insertIndex = groupOrderList.indexOf(IMPORTED_GROUP);
		}
		if (groupOrderList.contains(UNASSIGNED_GROUP)) {
			insertIndex = Math.min(insertIndex, groupOrderList.indexOf(UNASSIGNED_GROUP));
		}
		groupOrderList.addAll(insertIndex, groupsToAdd);

		if (markerGroups.containsKey(UNASSIGNED_GROUP)) {
			groupOrderList.remove(UNASSIGNED_GROUP);
			if (groupOrderList.contains(IMPORTED_GROUP)) {
				groupOrderList.add(groupOrderList.indexOf(IMPORTED_GROUP), UNASSIGNED_GROUP);
			} else {
				groupOrderList.add(UNASSIGNED_GROUP);
			}
		}
		if (markerGroups.containsKey(IMPORTED_GROUP)) {
			groupOrderList.remove(IMPORTED_GROUP);
			groupOrderList.add(IMPORTED_GROUP);
		}

		groupOrderList.retainAll(markerGroups.keySet());

		// Load Group Visibility States
		final String visibilityJson = configManager.getConfiguration(CONFIG_GROUP,
				CONFIG_KEY_VISIBILITY);
		if (!Strings.isNullOrEmpty(visibilityJson)) {
			try {
				final Map<String, Boolean> loadedVisibility = gson.fromJson(visibilityJson,
						new TypeToken<HashMap<String, Boolean>>() {
						}.getType());

				if (loadedVisibility != null) {
					// Only load states for groups that actually exist
					loadedVisibility.forEach((groupName, isVisible) -> {
						if (markerGroups.containsKey(groupName)) {
							groupVisibilityStates.put(groupName, isVisible);
						}
					});
				}
			} catch (Exception e) {
				System.err.println("Error parsing group visibility JSON: " + e.getMessage());
				groupVisibilityStates.clear(); // Reset on error
			}
		}
	}

	/**
	 * Checks if a group is currently set to be visible.
	 * Defaults to true if the group has no specific state saved.
	 *
	 * @param groupName The name of the group.
	 * @return True if the group is visible, false otherwise.
	 */
	public boolean isGroupVisible(String groupName) {
		return groupVisibilityStates.getOrDefault(groupName, true);
	}

	/**
	 * Sets the visibility state for a specific group and saves the configuration.
	 * Also updates the OverlayManager to add/remove overlays accordingly.
	 *
	 * @param groupName The name of the group.
	 * @param isVisible The desired visibility state.
	 */
	public void setGroupVisibility(String groupName, boolean isVisible) {
		if (!markerGroups.containsKey(groupName)) {
			return; // Ignore if group doesn't exist
		}

		boolean previousState = isGroupVisible(groupName);
		groupVisibilityStates.put(groupName, isVisible);
		updateVisibilityConfig(); // Save the change

		// Update overlays if state actually changed
		if (previousState != isVisible) {
			List<ScreenMarkerOverlay> groupOverlays = markerGroups.get(groupName);
			if (groupOverlays != null) {
				if (isVisible) {
					// Add overlays back if their individual marker is visible
					// OverlayManager.add is idempotent, so no need to check if it's already present
					groupOverlays.stream()
							.filter(overlay -> overlay.getMarker().isVisible())
							.forEach(overlayManager::add);
				} else {
					// Remove overlays from the manager
					groupOverlays.forEach(overlayManager::remove);
				}
			}
			// Trigger a repaint or rebuild if necessary, though render method check might
			// suffice
			// SwingUtilities.invokeLater(pluginPanel::rebuild); // Might be too heavy, let
			// render handle it first
		}
	}

	public boolean addGroup(String name) {
		if (Strings.isNullOrEmpty(name) || markerGroups.containsKey(name)) {
			return false;
		}
		markerGroups.put(name, new ArrayList<>());
		groupVisibilityStates.put(name, true); // Default new group to visible
		int insertIndex = groupOrderList.size();
		if (groupOrderList.contains(IMPORTED_GROUP)) {
			insertIndex = groupOrderList.indexOf(IMPORTED_GROUP);
		}
		if (groupOrderList.contains(UNASSIGNED_GROUP)) {
			insertIndex = Math.min(insertIndex, groupOrderList.indexOf(UNASSIGNED_GROUP));
		}
		groupOrderList.add(insertIndex, name);

		updateGroupsConfig(); // Saves markers, order, and visibility
		SwingUtilities.invokeLater(pluginPanel::rebuild);
		return true;
	}

	public void deleteGroup(String groupName) {
		// Allow deleting any group except "Unassigned"
		if (groupName.equals(UNASSIGNED_GROUP)) {
			JOptionPane.showMessageDialog(pluginPanel,
					"Cannot delete the special '" + UNASSIGNED_GROUP + "' group.",
					"Delete Group Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		List<ScreenMarkerOverlay> markersInGroup = markerGroups.get(groupName);
		if (markersInGroup == null)
			return;

		String message = "Delete group '" + groupName + "'?";
		if (!markersInGroup.isEmpty()) {
			message += "\nWhat should happen to the " + markersInGroup.size() + " marker(s) inside?";
		}
		String[] options = markersInGroup.isEmpty() ? new String[] { "Delete Group", "Cancel" }
				: new String[] { "Delete Markers", "Move to Unassigned", "Cancel" };
		int choice = JOptionPane.showOptionDialog(pluginPanel, message, "Confirm Group Deletion",
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options,
				options[options.length - 1]);

		if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION)
			return;

		if (choice == 0) { // Delete Markers
			markersInGroup.forEach(overlayManager::remove);
			markersInGroup.forEach(overlayManager::resetOverlay);
		} else if (choice == 1) { // Move to Unassigned
			List<ScreenMarkerOverlay> unassignedList = markerGroups.computeIfAbsent(UNASSIGNED_GROUP,
					k -> new ArrayList<>());
			unassignedList.addAll(markersInGroup);
			if (!groupOrderList.contains(UNASSIGNED_GROUP)) {
				groupOrderList.add(UNASSIGNED_GROUP);
			}
		}

		markerGroups.remove(groupName);
		groupOrderList.remove(groupName);
		groupVisibilityStates.remove(groupName); // Remove visibility state
		updateGroupsConfig(); // Saves markers, order, and visibility
		SwingUtilities.invokeLater(pluginPanel::rebuild);
	}

	public boolean renameGroup(String oldName, String newName) {
		if (Strings.isNullOrEmpty(newName) || newName.equals(UNASSIGNED_GROUP) || newName.equals(IMPORTED_GROUP)
				|| markerGroups.containsKey(newName)) {
			return false;
		}
		if (oldName.equals(UNASSIGNED_GROUP) || oldName.equals(IMPORTED_GROUP) || !markerGroups.containsKey(oldName)) {
			return false;
		}

		List<ScreenMarkerOverlay> markers = markerGroups.remove(oldName);
		Boolean visibility = groupVisibilityStates.remove(oldName); // Get and remove old visibility state

		if (markers != null) {
			markerGroups.put(newName, markers);
			groupVisibilityStates.put(newName, visibility != null ? visibility : true); // Add new state, preserving old
																						// if possible

			int index = groupOrderList.indexOf(oldName);
			if (index != -1) {
				groupOrderList.set(index, newName);
			} else {
				int insertIndex = groupOrderList.size();
				if (groupOrderList.contains(IMPORTED_GROUP)) {
					insertIndex = groupOrderList.indexOf(IMPORTED_GROUP);
				}
				if (groupOrderList.contains(UNASSIGNED_GROUP)) {
					insertIndex = Math.min(insertIndex, groupOrderList.indexOf(UNASSIGNED_GROUP));
				}
				groupOrderList.add(insertIndex, newName);
			}
			updateGroupsConfig(); // Saves markers, order, and visibility
			SwingUtilities.invokeLater(pluginPanel::rebuild);
			return true;
		}
		return false;
	}

	public void moveGroupUp(String groupName) {
		int currentIndex = groupOrderList.indexOf(groupName);
		if (currentIndex <= 0 || groupName.equals(UNASSIGNED_GROUP) || groupName.equals(IMPORTED_GROUP)) {
			return;
		}
		Collections.swap(groupOrderList, currentIndex, currentIndex - 1);
		updateGroupsConfig(); // Saves markers, order, and visibility
		SwingUtilities.invokeLater(pluginPanel::rebuild);
	}

	public void moveGroupDown(String groupName) {
		int currentIndex = groupOrderList.indexOf(groupName);
		int lastValidIndex = groupOrderList.size() - 1;
		if (groupOrderList.contains(IMPORTED_GROUP))
			lastValidIndex--;
		if (groupOrderList.contains(UNASSIGNED_GROUP))
			lastValidIndex--;
		if (currentIndex < 0 || currentIndex >= lastValidIndex || groupName.equals(UNASSIGNED_GROUP)
				|| groupName.equals(IMPORTED_GROUP)) {
			return;
		}
		Collections.swap(groupOrderList, currentIndex, currentIndex + 1);
		updateGroupsConfig(); // Saves markers, order, and visibility
		SwingUtilities.invokeLater(pluginPanel::rebuild);
	}

	public String findGroupForMarker(ScreenMarkerOverlay markerOverlay) {
		for (Map.Entry<String, List<ScreenMarkerOverlay>> entry : markerGroups.entrySet()) {
			if (entry.getValue().contains(markerOverlay)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public void moveMarkerUp(ScreenMarkerOverlay markerOverlay) {
		String groupName = findGroupForMarker(markerOverlay);
		if (groupName == null)
			return;
		List<ScreenMarkerOverlay> groupList = markerGroups.get(groupName);
		int currentIndex = groupList.indexOf(markerOverlay);
		if (currentIndex > 0) {
			Collections.swap(groupList, currentIndex, currentIndex - 1);
			updateGroupsConfig(); // Saves markers, order, and visibility
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	public void moveMarkerDown(ScreenMarkerOverlay markerOverlay) {
		String groupName = findGroupForMarker(markerOverlay);
		if (groupName == null)
			return;
		List<ScreenMarkerOverlay> groupList = markerGroups.get(groupName);
		int currentIndex = groupList.indexOf(markerOverlay);
		if (currentIndex >= 0 && currentIndex < groupList.size() - 1) {
			Collections.swap(groupList, currentIndex, currentIndex + 1);
			updateGroupsConfig(); // Saves markers, order, and visibility
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	public void moveMarkerToGroup(ScreenMarkerOverlay markerOverlay, String targetGroupName) {
		String sourceGroupName = findGroupForMarker(markerOverlay);
		if (sourceGroupName == null || sourceGroupName.equals(targetGroupName)
				|| !markerGroups.containsKey(targetGroupName)) {
			return;
		}
		List<ScreenMarkerOverlay> sourceList = markerGroups.get(sourceGroupName);
		List<ScreenMarkerOverlay> targetList = markerGroups.computeIfAbsent(targetGroupName, k -> new ArrayList<>());
		if (sourceList.remove(markerOverlay)) {
			targetList.add(markerOverlay);
			// Update overlay manager based on target group visibility
			if (!isGroupVisible(targetGroupName)) {
				overlayManager.remove(markerOverlay);
			} else if (markerOverlay.getMarker().isVisible()) { // OverlayManager.add is idempotent
				// Add only if target group is visible and marker is visible
				overlayManager.add(markerOverlay);
			}
			updateGroupsConfig(); // Saves markers, order, and visibility
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals(CONFIG_GROUP)) {
			return;
		}

		// Check if the "importTrigger" config item was changed (likely toggled to true)
		if (event.getKey().equals("importTrigger")) {
			// We only trigger if the new value is true (meaning it was clicked)
			if (Boolean.parseBoolean(event.getNewValue())) {
				// Use invokeLater to avoid issues with config changes during event handling
				SwingUtilities.invokeLater(() -> {
					// Reset the trigger back to false immediately
					configManager.setConfiguration(CONFIG_GROUP, "importTrigger", false);
					// Perform the import
					importScreenMarkers();
				});
			}
		}
	}

	/**
	 * Imports screen markers from the original RuneLite Screen Markers plugin.
	 * Reads the configuration from the "screenmarkers" group and adds them
	 * to the "Imported" group in this plugin. Shows dialogs for success,
	 * failure, or if no markers were found/imported.
	 */
	public void importScreenMarkers() {
		String originalMarkersJson = configManager.getConfiguration("screenmarkers", "markers");
		if (Strings.isNullOrEmpty(originalMarkersJson)) {
			JOptionPane.showMessageDialog(pluginPanel,
					"No markers found in the original Screen Markers plugin configuration.",
					"Import Failed", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		List<ScreenMarker> loadedMarkers;
		try {
			loadedMarkers = gson.fromJson(originalMarkersJson, new TypeToken<ArrayList<ScreenMarker>>() {
			}.getType());
		} catch (Exception e) {
			System.err.println("Error parsing original screen markers JSON: " + e.getMessage());
			JOptionPane.showMessageDialog(pluginPanel,
					"Failed to parse markers from the original plugin.",
					"Import Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (loadedMarkers == null || loadedMarkers.isEmpty()) {
			JOptionPane.showMessageDialog(pluginPanel,
					"No valid markers found to import.",
					"Import Failed", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		List<ScreenMarkerOverlay> importedGroupList = markerGroups.computeIfAbsent(IMPORTED_GROUP,
				k -> new ArrayList<>());
		if (!groupOrderList.contains(IMPORTED_GROUP)) {
			groupOrderList.add(IMPORTED_GROUP);
		}
		int importedCount = 0;
		for (ScreenMarker markerData : loadedMarkers) {
			if (markerData != null) {
				boolean exists = importedGroupList.stream()
						.anyMatch(overlay -> overlay.getMarker().getId() == markerData.getId());
				if (!exists) {
					// Pass plugin instance 'this' to the constructor
					ScreenMarkerOverlay newOverlay = new ScreenMarkerOverlay(markerData, this);
					importedGroupList.add(newOverlay);
					overlayManager.add(newOverlay);
					overlayManager.saveOverlay(newOverlay);
					importedCount++;
				}
			}
		}
		if (importedCount > 0) {
			updateGroupsConfig();
			SwingUtilities.invokeLater(pluginPanel::rebuild);
			JOptionPane.showMessageDialog(pluginPanel,
					"Successfully imported " + importedCount + " marker(s) into the '" + IMPORTED_GROUP + "' group.",
					"Import Successful", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(pluginPanel,
					"No new markers were imported (they might already exist in the 'Imported' group).",
					"Import Information", JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
