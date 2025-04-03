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
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import screenmarkergroups.ui.ScreenMarkerGroupsPluginPanel;
import screenmarkergroups.ui.ScreenMarkerGroupsCreationPanel;

@PluginDescriptor(name = "Screen Marker Groups", description = "Enable drawing of screen markers on top of the client, organized into groups", tags = {
		"boxes", "overlay", "panel", "group", "organize" })
public class ScreenMarkerGroupsPlugin extends Plugin {
	private static final String OVERLAY_CONFIG_GROUP = "runelite";

	/**
	 * Provides the configuration object for the plugin.
	 *
	 * @param configManager The RuneLite configuration manager.
	 * @return The configuration object.
	 */
	@Provides
	ScreenMarkerGroupsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ScreenMarkerGroupsConfig.class);
	}

	private static final String PLUGIN_NAME = "Screen Marker Groups";
	private static final String CONFIG_GROUP = "screenmarkergroups";
	private static final String CONFIG_KEY_MARKERS = "markerGroups";
	private static final String CONFIG_KEY_ORDER = "groupOrder";
	private static final String CONFIG_KEY_VISIBILITY = "groupVisibility";
	private static final String CONFIG_KEY_EXPANSION = "groupExpansion";
	private static final String ICON_FILE = "panel_icon.png";
	private static final String DEFAULT_MARKER_NAME = "Marker";
	public static final Dimension DEFAULT_SIZE = new Dimension(2, 2);
	public static final String UNASSIGNED_GROUP = "Unassigned";
	public static final String IMPORTED_GROUP = "Imported";

	@Getter
	private final Map<String, List<ScreenMarkerOverlay>> markerGroups = new ConcurrentHashMap<>();

	@Getter
	private final List<String> groupOrderList = new ArrayList<>();

	private final Map<String, Boolean> groupVisibilityStates = new ConcurrentHashMap<>();
	private final Map<String, Boolean> groupExpansionStates = new ConcurrentHashMap<>();

	@Inject
	private ConfigManager configManager;

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

	/**
	 * Called when the plugin is started. Loads configuration, sets up UI,
	 * adds overlays, and registers listeners.
	 *
	 * @throws Exception If an error occurs during startup.
	 */
	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);
		overlayManager.add(widgetHighlight);
		loadGroupsConfig();
		// Add overlays respecting group visibility from loaded config
		markerGroups.forEach((groupName, overlays) -> {
			if (isGroupVisible(groupName)) {
				overlays.forEach(overlayManager::add);
			}
		});
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

	/**
	 * Called when the plugin is shut down. Removes overlays, clears state,
	 * removes UI components, and unregisters listeners.
	 *
	 * @throws Exception If an error occurs during shutdown.
	 */
	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		overlayManager.remove(widgetHighlight);
		overlayManager.removeIf(ScreenMarkerOverlay.class::isInstance);
		markerGroups.clear();
		groupOrderList.clear();
		groupVisibilityStates.clear();
		groupExpansionStates.clear();
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

	/**
	 * Handles the RuneLite ProfileChanged event. Clears current state and reloads
	 * configuration for the new profile. Ensures overlays and UI reflect the
	 * profile's settings.
	 *
	 * @param profileChanged The event object associated with the profile change.
	 */
	@Subscribe
	public void onProfileChanged(ProfileChanged profileChanged) {
		overlayManager.removeIf(ScreenMarkerOverlay.class::isInstance);
		markerGroups.clear();
		groupOrderList.clear();
		groupVisibilityStates.clear();
		groupExpansionStates.clear();
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

	/**
	 * Registers or unregisters the mouse listener responsible for handling
	 * screen marker creation clicks and drags.
	 *
	 * @param enabled True to register the listener, false to unregister.
	 */
	public void setMouseListenerEnabled(boolean enabled) {
		if (enabled) {
			mouseManager.registerMouseListener(mouseListener);
		} else {
			mouseManager.unregisterMouseListener(mouseListener);
		}
	}

	/**
	 * Enters the marker creation mode, preparing the plugin and UI.
	 * Called from the UI (+ button).
	 *
	 * @param groupName The target group for the new marker.
	 */
	public void enterCreationMode(String groupName) {
		this.targetGroupNameForCreation = markerGroups.containsKey(groupName) ? groupName : UNASSIGNED_GROUP;
		this.creatingScreenMarker = true;
		this.setMouseListenerEnabled(true);
		this.currentMarker = null;
		this.startLocation = null;
		this.drawingScreenMarker = false;
		this.selectedWidgetBounds = null;

		overlay.setPreferredLocation(null);
		overlay.setPreferredSize(null);

		if (pluginPanel != null) {
			pluginPanel.setCreation(true);
		}
	}

	/**
	 * Prepares a new marker object and sets the initial bounds for the creation
	 * overlay.
	 * Called by the mouse listener on the first click/drag or when clicking a
	 * widget.
	 *
	 * @param location The initial location for the marker.
	 * @param size     The initial size for the marker.
	 */
	public void startCreation(Point location, Dimension size) {
		if (currentMarker != null || location == null) {
			return;
		}

		long nextMarkerId = findMaxMarkerId() + 1;

		// Determine the target group name, defaulting to UNASSIGNED_GROUP if null
		String targetGroup = targetGroupNameForCreation != null ? targetGroupNameForCreation : UNASSIGNED_GROUP;
		// Calculate the next marker number within the target group
		int nextMarkerNumberInGroup = markerGroups.getOrDefault(targetGroup, Collections.emptyList()).size() + 1;

		currentMarker = new ScreenMarker(
				nextMarkerId,
				DEFAULT_MARKER_NAME + " " + nextMarkerNumberInGroup, // Use group-specific count for default name
				ScreenMarkerGroupsPluginPanel.SELECTED_BORDER_THICKNESS,
				ScreenMarkerGroupsPluginPanel.SELECTED_COLOR,
				ScreenMarkerGroupsPluginPanel.SELECTED_FILL_COLOR,
				true,
				false,
				null);
		startLocation = location;
		overlay.setPreferredLocation(location);
		overlay.setPreferredSize(size != null ? size : DEFAULT_SIZE);
		drawingScreenMarker = true;
	}

	/**
	 * Finalizes the screen marker creation process. If not aborted, it creates
	 * the marker overlay, adds it to the target group, saves the configuration,
	 * and updates the UI. Resets the creation state regardless of outcome.
	 *
	 * @param aborted True if the creation process was cancelled, false otherwise.
	 */
	public void finishCreation(boolean aborted) {
		ScreenMarker marker = currentMarker;
		String targetGroup = targetGroupNameForCreation != null ? targetGroupNameForCreation : UNASSIGNED_GROUP;
		Rectangle overlayBounds = overlay.getBounds();

		if (!aborted && marker != null && overlayBounds != null && overlayBounds.width > 0
				&& overlayBounds.height > 0) {
			final ScreenMarkerOverlay screenMarkerOverlay = new ScreenMarkerOverlay(marker, this);
			screenMarkerOverlay.setPreferredLocation(overlayBounds.getLocation());
			screenMarkerOverlay.setPreferredSize(overlayBounds.getSize());

			List<ScreenMarkerOverlay> groupList = markerGroups.computeIfAbsent(targetGroup, k -> new ArrayList<>());
			groupList.add(screenMarkerOverlay);

			if (!groupOrderList.contains(targetGroup)) {
				int insertIndex = calculateGroupInsertIndex();
				if (!targetGroup.equals(UNASSIGNED_GROUP) && !targetGroup.equals(IMPORTED_GROUP)) {
					groupOrderList.add(insertIndex, targetGroup);
				} else if (!groupOrderList.contains(targetGroup)) { // Add Unassigned/Imported if not present at all
					groupOrderList.add(targetGroup); // Add to end if somehow missing
				}
				// Re-ensure Unassigned/Imported are at the end if they were just added
				ensureSpecialGroupsOrder();
			}
			overlayManager.saveOverlay(screenMarkerOverlay);
			if (isGroupVisible(targetGroup)) {
				overlayManager.add(screenMarkerOverlay);
			}
			updateGroupsConfig();
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

	/**
	 * Called when the user confirms the marker creation settings in the UI panel.
	 * Currently unlocks the confirmation button in the specific group's creation
	 * panel.
	 */
	public void completeSelection() {
		if (pluginPanel != null && targetGroupNameForCreation != null) {
			ScreenMarkerGroupsCreationPanel creationPanel = pluginPanel.getCreationPanelsMap()
					.get(targetGroupNameForCreation);
			if (creationPanel != null) {
				creationPanel.unlockConfirm();
			}
		}
	}

	/**
	 * Deletes a specific screen marker overlay from its group, removes it from
	 * the overlay manager, resets its configuration, saves the changes, and
	 * updates the UI panel.
	 *
	 * @param markerToDelete The overlay instance to delete.
	 */
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
			updateGroupsConfig();
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	/**
	 * Handles resizing of the marker currently being created based on mouse drag.
	 * If creation hasn't started, it initiates it. Otherwise, it updates the
	 * bounds of the creation overlay.
	 *
	 * @param point The current mouse cursor position.
	 */
	void resizeMarker(Point point) {
		if (startLocation == null) {
			startCreation(point, DEFAULT_SIZE);
			return;
		}
		drawingScreenMarker = true;
		Rectangle bounds = new Rectangle(startLocation);
		bounds.add(point);
		overlay.setPreferredLocation(bounds.getLocation());
		overlay.setPreferredSize(bounds.getSize());
	}

	/**
	 * Saves the current state of all marker groups, their order, visibility,
	 * and expansion states to the RuneLite configuration. Unsets configuration
	 * keys if the corresponding data structures are empty.
	 */
	public void updateGroupsConfig() {
		boolean shouldSaveMarkers = !markerGroups.isEmpty();
		boolean shouldSaveOrder = !groupOrderList.isEmpty();

		if (!shouldSaveMarkers) {
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY_MARKERS);
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

		if (!shouldSaveOrder) {
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY_ORDER);
		} else {
			// Filter order list to only contain existing groups before saving
			List<String> orderToSave = groupOrderList.stream()
					.filter(markerGroups::containsKey)
					.collect(Collectors.toList());
			if (orderToSave.isEmpty()) {
				configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY_ORDER);
			} else {
				final String orderJson = gson.toJson(orderToSave);
				configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_ORDER, orderJson);
			}
		}

		updateVisibilityConfig();
		updateExpansionConfig();
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

	/**
	 * Saves the current group expansion states to the config manager.
	 */
	private void updateExpansionConfig() {
		// Clean up expansion states for groups that no longer exist
		groupExpansionStates.keySet().retainAll(markerGroups.keySet());

		if (groupExpansionStates.isEmpty()) {
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY_EXPANSION);
		} else {
			final String expansionJson = gson.toJson(groupExpansionStates);
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_EXPANSION, expansionJson);
		}
	}

	private void loadGroupsConfig() {
		markerGroups.clear();
		groupOrderList.clear();
		groupVisibilityStates.clear();
		groupExpansionStates.clear();

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
								.map(marker -> new ScreenMarkerOverlay(marker, this))
								.collect(Collectors.toList());
						markerGroups.put(groupName, new ArrayList<>(overlayList));
					});
				}
			} catch (Exception e) {
				markerGroups.clear();
			}
		}
		markerGroups.computeIfAbsent(UNASSIGNED_GROUP, k -> new ArrayList<>());

		final String orderJson = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_ORDER);
		List<String> loadedOrder = null;
		if (!Strings.isNullOrEmpty(orderJson)) {
			try {
				loadedOrder = gson.fromJson(orderJson, new TypeToken<ArrayList<String>>() {
				}.getType());
			} catch (Exception e) {
				loadedOrder = null;
			}
		}

		// Add loaded, valid groups to the order list
		if (loadedOrder != null) {
			groupOrderList.addAll(loadedOrder.stream()
					.filter(markerGroups::containsKey) // Only add groups that actually exist
					.collect(Collectors.toList()));
		}

		// Find groups present in data but missing from the loaded order
		List<String> groupsToAdd = markerGroups.keySet().stream()
				.filter(groupName -> !groupOrderList.contains(groupName))
				.filter(groupName -> !groupName.equals(UNASSIGNED_GROUP) && !groupName.equals(IMPORTED_GROUP))
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.collect(Collectors.toList());

		// Add the missing normal groups at the calculated insertion point
		if (!groupsToAdd.isEmpty()) {
			int insertIndex = calculateGroupInsertIndex();
			groupOrderList.addAll(insertIndex, groupsToAdd);
		}

		// Ensure Unassigned and Imported groups are present and at the end, in the
		// correct order
		ensureSpecialGroupsOrder();

		// Clean up any potential stale entries in groupOrderList
		groupOrderList.retainAll(markerGroups.keySet());

		final String visibilityJson = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_VISIBILITY);
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
				groupVisibilityStates.clear();
			}
		}

		final String expansionJson = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_EXPANSION);
		if (!Strings.isNullOrEmpty(expansionJson)) {
			try {
				final Map<String, Boolean> loadedExpansion = gson.fromJson(expansionJson,
						new TypeToken<HashMap<String, Boolean>>() {
						}.getType());

				if (loadedExpansion != null) {
					// Only load states for groups that actually exist
					loadedExpansion.forEach((groupName, isExpanded) -> {
						if (markerGroups.containsKey(groupName)) {
							groupExpansionStates.put(groupName, isExpanded);
						}
					});
				}
			} catch (Exception e) {
				groupExpansionStates.clear();
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
			return;
		}

		boolean previousState = isGroupVisible(groupName);
		groupVisibilityStates.put(groupName, isVisible);
		updateVisibilityConfig();

		if (previousState != isVisible) {
			List<ScreenMarkerOverlay> groupOverlays = markerGroups.get(groupName);
			if (groupOverlays != null) {
				if (isVisible) {
					groupOverlays.stream()
							.filter(overlay -> overlay.getMarker().isVisible())
							.forEach(overlayManager::add);
				} else {
					groupOverlays.forEach(overlayManager::remove);
				}
			}
		}
	}

	/**
	 * Checks if a group is currently set to be expanded.
	 * Defaults to true if the group has no specific state saved.
	 *
	 * @param groupName The name of the group.
	 * @return True if the group is expanded, false otherwise.
	 */
	public boolean isGroupExpanded(String groupName) {
		return groupExpansionStates.getOrDefault(groupName, true);
	}

	/**
	 * Sets the expansion state for a specific group and saves the configuration.
	 *
	 * @param groupName  The name of the group.
	 * @param isExpanded The desired expansion state.
	 */
	public void setGroupExpansion(String groupName, boolean isExpanded) {
		if (!markerGroups.containsKey(groupName)) {
			return;
		}
		groupExpansionStates.put(groupName, isExpanded);
		updateExpansionConfig();
	}

	/**
	 * Adds a new, empty group with the given name. The group defaults to visible
	 * and expanded. Updates configuration and rebuilds the UI panel.
	 *
	 * @param name The name for the new group. Must not be null, empty, or already
	 *             exist.
	 * @return True if the group was added successfully, false otherwise.
	 */
	public boolean addGroup(String name) {
		if (Strings.isNullOrEmpty(name) || markerGroups.containsKey(name)) {
			return false;
		}
		markerGroups.put(name, new ArrayList<>());
		groupVisibilityStates.put(name, true);
		groupExpansionStates.put(name, true);

		// Add the new group name before Unassigned/Imported
		int insertIndex = calculateGroupInsertIndex();
		groupOrderList.add(insertIndex, name);

		updateGroupsConfig();
		SwingUtilities.invokeLater(pluginPanel::rebuild);
		return true;
	}

	/**
	 * Deletes a group and handles the markers within it based on user choice
	 * (delete markers, move to Unassigned). Cannot delete the "Unassigned" group.
	 * Updates configuration and rebuilds the UI panel.
	 *
	 * @param groupName The name of the group to delete.
	 */
	public void deleteGroup(String groupName) {
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
				groupOrderList.add(UNASSIGNED_GROUP); // Add if not present
				ensureSpecialGroupsOrder(); // Ensure it's placed correctly
			}
		}

		markerGroups.remove(groupName);
		groupOrderList.remove(groupName);
		groupVisibilityStates.remove(groupName);
		groupExpansionStates.remove(groupName);
		updateGroupsConfig();
		SwingUtilities.invokeLater(pluginPanel::rebuild);
	}

	/**
	 * Renames an existing group. Cannot rename to "Unassigned", "Imported", or an
	 * already existing group name. Cannot rename the "Unassigned" or "Imported"
	 * groups themselves. Preserves the group's markers, visibility, and expansion
	 * state. Updates configuration and rebuilds the UI panel.
	 *
	 * @param oldName The current name of the group.
	 * @param newName The desired new name for the group.
	 * @return True if the group was renamed successfully, false otherwise.
	 */
	public boolean renameGroup(String oldName, String newName) {
		if (Strings.isNullOrEmpty(newName) || newName.equals(UNASSIGNED_GROUP) || newName.equals(IMPORTED_GROUP)
				|| markerGroups.containsKey(newName)) {
			return false;
		}
		if (oldName.equals(UNASSIGNED_GROUP) || oldName.equals(IMPORTED_GROUP) || !markerGroups.containsKey(oldName)) {
			return false;
		}

		List<ScreenMarkerOverlay> markers = markerGroups.remove(oldName);
		Boolean visibility = groupVisibilityStates.remove(oldName);
		Boolean expansion = groupExpansionStates.remove(oldName);

		if (markers != null) {
			markerGroups.put(newName, markers);
			groupVisibilityStates.put(newName, visibility != null ? visibility : true);
			groupExpansionStates.put(newName, expansion != null ? expansion : true);

			int index = groupOrderList.indexOf(oldName);
			if (index != -1) {
				groupOrderList.set(index, newName);
			} else {
				// If somehow not in order list, add it before special groups
				int insertIndex = calculateGroupInsertIndex();
				groupOrderList.add(insertIndex, newName);
			}
			updateGroupsConfig();
			SwingUtilities.invokeLater(pluginPanel::rebuild);
			return true;
		}
		return false;
	}

	/**
	 * Moves a group one position up in the display order, unless it's already
	 * at the top or is a special group ("Unassigned", "Imported"). Updates
	 * configuration and rebuilds the UI panel.
	 *
	 * @param groupName The name of the group to move up.
	 */
	public void moveGroupUp(String groupName) {
		int currentIndex = groupOrderList.indexOf(groupName);
		// Cannot move up if first, or if it's a special group
		if (currentIndex <= 0 || groupName.equals(UNASSIGNED_GROUP) || groupName.equals(IMPORTED_GROUP)) {
			return;
		}
		Collections.swap(groupOrderList, currentIndex, currentIndex - 1);
		updateGroupsConfig();
		SwingUtilities.invokeLater(pluginPanel::rebuild);
	}

	/**
	 * Moves a group one position down in the display order, unless it's already
	 * at the bottom of the regular groups or is a special group ("Unassigned",
	 * "Imported"). Updates configuration and rebuilds the UI panel.
	 *
	 * @param groupName The name of the group to move down.
	 */
	public void moveGroupDown(String groupName) {
		int currentIndex = groupOrderList.indexOf(groupName);

		// Calculate the last index allowed for a non-special group to move down
		int lastMovableIndex = calculateGroupInsertIndex() - 1; // Index before the first special group

		// Cannot move down if already at the last movable position, or if it's a
		// special group
		if (currentIndex < 0 || currentIndex >= lastMovableIndex || groupName.equals(UNASSIGNED_GROUP)
				|| groupName.equals(IMPORTED_GROUP)) {
			return;
		}

		Collections.swap(groupOrderList, currentIndex, currentIndex + 1);
		updateGroupsConfig();
		SwingUtilities.invokeLater(pluginPanel::rebuild);
	}

	/**
	 * Finds the name of the group that contains the given screen marker overlay.
	 *
	 * @param markerOverlay The overlay to find the group for.
	 * @return The name of the group containing the overlay, or null if not found.
	 */
	public String findGroupForMarker(ScreenMarkerOverlay markerOverlay) {
		for (Map.Entry<String, List<ScreenMarkerOverlay>> entry : markerGroups.entrySet()) {
			if (entry.getValue().contains(markerOverlay)) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Moves a marker one position up within its current group's list. Updates
	 * configuration and rebuilds the UI panel.
	 *
	 * @param markerOverlay The overlay of the marker to move up.
	 */
	public void moveMarkerUp(ScreenMarkerOverlay markerOverlay) {
		String groupName = findGroupForMarker(markerOverlay);
		if (groupName == null)
			return;
		List<ScreenMarkerOverlay> groupList = markerGroups.get(groupName);
		int currentIndex = groupList.indexOf(markerOverlay);
		if (currentIndex > 0) {
			Collections.swap(groupList, currentIndex, currentIndex - 1);
			updateGroupsConfig();
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	/**
	 * Moves a marker one position down within its current group's list. Updates
	 * configuration and rebuilds the UI panel.
	 *
	 * @param markerOverlay The overlay of the marker to move down.
	 */
	public void moveMarkerDown(ScreenMarkerOverlay markerOverlay) {
		String groupName = findGroupForMarker(markerOverlay);
		if (groupName == null)
			return;
		List<ScreenMarkerOverlay> groupList = markerGroups.get(groupName);
		int currentIndex = groupList.indexOf(markerOverlay);
		if (currentIndex >= 0 && currentIndex < groupList.size() - 1) {
			Collections.swap(groupList, currentIndex, currentIndex + 1);
			updateGroupsConfig();
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	/**
	 * Moves a marker from its current group to a different target group. Updates
	 * configuration, overlay visibility based on the target group, and rebuilds
	 * the UI panel.
	 *
	 * @param markerOverlay   The overlay of the marker to move.
	 * @param targetGroupName The name of the destination group.
	 */
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
				overlayManager.add(markerOverlay);
			}
			updateGroupsConfig();
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	/**
	 * Handles RuneLite ConfigChanged events specific to this plugin's group.
	 * Currently used to detect clicks on the "Import Screen Markers" trigger.
	 *
	 * @param event The configuration change event.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals(CONFIG_GROUP)) {
			return;
		}

		if (event.getKey().equals("importTrigger")) {
			if (Boolean.parseBoolean(event.getNewValue())) {
				SwingUtilities.invokeLater(() -> {
					configManager.setConfiguration(CONFIG_GROUP, "importTrigger", false);
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
			// Error logged during development, removed for production cleanup
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
			groupOrderList.add(IMPORTED_GROUP); // Add if not present
			ensureSpecialGroupsOrder(); // Ensure it's placed correctly
		}
		int importedCount = 0;
		long maxId = findMaxMarkerId();

		for (ScreenMarker markerData : loadedMarkers) {
			if (markerData == null) {
				continue;
			}

			final long originalMarkerId = markerData.getId();
			boolean alreadyImported = importedGroupList.stream()
					.map(ScreenMarkerOverlay::getMarker)
					.filter(m -> m.getImportedId() != null)
					.anyMatch(existingMarker -> originalMarkerId == existingMarker.getImportedId());

			if (alreadyImported) {
				continue;
			}

			// Apply defaults for potentially missing fields from older plugin versions
			if (markerData.getColor() == null) {
				markerData.setColor(ScreenMarkerGroupsPluginPanel.DEFAULT_BORDER_COLOR);
			}
			if (markerData.getFill() == null) {
				markerData.setFill(ScreenMarkerGroupsPluginPanel.DEFAULT_FILL_COLOR);
			}
			if (markerData.getBorderThickness() <= 0) {
				markerData.setBorderThickness(ScreenMarkerGroupsPluginPanel.DEFAULT_BORDER_THICKNESS);
			}

			// Generate a new unique ID
			long newId = Math.max(Instant.now().toEpochMilli(), maxId + 1);
			maxId = newId; // Update maxId for the next iteration

			// Create a new marker object with the new ID and copied properties
			ScreenMarker newMarker = new ScreenMarker(
					newId,
					markerData.getName(),
					markerData.getBorderThickness(),
					markerData.getColor(),
					markerData.getFill(),
					markerData.isVisible(),
					markerData.isLabelled(),
					null);
			newMarker.setImportedId(originalMarkerId); // Store the original ID

			// Create the overlay for the new marker
			ScreenMarkerOverlay newOverlay = new ScreenMarkerOverlay(newMarker, this);

			// Try to read original position and size using original ID
			Point originalLocation = parsePoint(
					configManager.getConfiguration(OVERLAY_CONFIG_GROUP,
							"marker" + originalMarkerId + "_preferredLocation"));
			Dimension originalSize = parseDimension(
					configManager.getConfiguration(OVERLAY_CONFIG_GROUP,
							"marker" + originalMarkerId + "_preferredSize"));

			// Set location/size on the overlay object
			if (originalLocation != null) {
				newOverlay.setPreferredLocation(originalLocation);
			}
			if (originalSize != null) {
				newOverlay.setPreferredSize(originalSize);
			}

			// Explicitly save the location and size config using the NEW ID
			String newLocationKey = "marker" + newId + "_preferredLocation";
			String newSizeKey = "marker" + newId + "_preferredSize";

			if (originalLocation != null) {
				configManager.setConfiguration(OVERLAY_CONFIG_GROUP, newLocationKey,
						originalLocation.x + ":" + originalLocation.y);
			} else {
				configManager.unsetConfiguration(OVERLAY_CONFIG_GROUP, newLocationKey);
			}

			if (originalSize != null) {
				configManager.setConfiguration(OVERLAY_CONFIG_GROUP, newSizeKey,
						originalSize.width + "x" + originalSize.height);
			} else {
				configManager.unsetConfiguration(OVERLAY_CONFIG_GROUP, newSizeKey);
			}

			// Add the new overlay to the internal group list
			importedGroupList.add(newOverlay);

			// Add to overlay manager IF the group is visible AND the marker is visible
			if (isGroupVisible(IMPORTED_GROUP) && newMarker.isVisible()) {
				overlayManager.add(newOverlay);
			}

			// Save the overlay's marker data (JSON blob) using the OverlayManager
			overlayManager.saveOverlay(newOverlay);

			importedCount++;
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

	/**
	 * Finds the maximum marker ID currently used within this plugin.
	 * Used to help generate unique IDs during import.
	 *
	 * @return The maximum ID found, or 0 if no markers exist.
	 */
	private long findMaxMarkerId() {
		return markerGroups.values().stream()
				.flatMap(List::stream)
				.map(overlay -> overlay.getMarker().getId())
				.max(Long::compare)
				.orElse(0L);
	}

	/**
	 * Parses a Point object from a string representation "x:y".
	 *
	 * @param pointString The string to parse.
	 * @return The parsed Point, or null if parsing fails or input is null/empty.
	 */
	private Point parsePoint(String pointString) {
		if (Strings.isNullOrEmpty(pointString)) {
			return null;
		}
		String[] parts = pointString.split(":");
		if (parts.length != 2) {
			return null;
		}
		try {
			int x = Integer.parseInt(parts[0]);
			int y = Integer.parseInt(parts[1]);
			return new Point(x, y);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Parses a Dimension object from a string representation "widthxheight".
	 *
	 * @param dimensionString The string to parse.
	 * @return The parsed Dimension, or null if parsing fails or input is
	 *         null/empty.
	 */
	private Dimension parseDimension(String dimensionString) {
		if (Strings.isNullOrEmpty(dimensionString)) {
			return null;
		}
		String[] parts = dimensionString.split("x");
		if (parts.length != 2) {
			return null;
		}
		try {
			int width = Integer.parseInt(parts[0]);
			int height = Integer.parseInt(parts[1]);
			// Ensure minimum dimensions
			width = Math.max(width, 1);
			height = Math.max(height, 1);
			return new Dimension(width, height);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Calculates the correct insertion index for new groups in the groupOrderList.
	 * New groups should be inserted before "Unassigned" and "Imported".
	 *
	 * @return The index where a new non-special group should be inserted.
	 */
	private int calculateGroupInsertIndex() {
		int insertIndex = groupOrderList.size();
		if (groupOrderList.contains(IMPORTED_GROUP)) {
			insertIndex = groupOrderList.indexOf(IMPORTED_GROUP);
		}
		if (groupOrderList.contains(UNASSIGNED_GROUP)) {
			// Insert before Unassigned if it comes before Imported (or if Imported doesn't
			// exist)
			insertIndex = Math.min(insertIndex, groupOrderList.indexOf(UNASSIGNED_GROUP));
		}
		return insertIndex;
	}

	/**
	 * Ensures the "Unassigned" and "Imported" groups are present in the
	 * groupOrderList (if they exist in markerGroups) and are positioned at the end,
	 * with "Unassigned" before "Imported".
	 */
	private void ensureSpecialGroupsOrder() {
		boolean unassignedExists = markerGroups.containsKey(UNASSIGNED_GROUP);
		boolean importedExists = markerGroups.containsKey(IMPORTED_GROUP);

		// Remove existing instances to reposition them correctly
		groupOrderList.remove(UNASSIGNED_GROUP);
		groupOrderList.remove(IMPORTED_GROUP);

		// Add them back at the end in the correct order if they exist
		if (unassignedExists) {
			groupOrderList.add(UNASSIGNED_GROUP);
		}
		if (importedExists) {
			groupOrderList.add(IMPORTED_GROUP);
		}
	}
}
