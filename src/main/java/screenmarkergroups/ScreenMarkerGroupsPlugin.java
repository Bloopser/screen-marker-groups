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
	private static final String OVERLAY_CONFIG_GROUP = "runelite"; // Config group for overlay properties

	@Provides
	ScreenMarkerGroupsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ScreenMarkerGroupsConfig.class);
	}

	private static final String PLUGIN_NAME = "Screen Marker Groups";
	private static final String CONFIG_GROUP = "screenmarkergroups";
	private static final String CONFIG_KEY_MARKERS = "markerGroups";
	private static final String CONFIG_KEY_ORDER = "groupOrder";
	private static final String CONFIG_KEY_VISIBILITY = "groupVisibility";
	private static final String CONFIG_KEY_EXPANSION = "groupExpansion"; // New config key for expansion
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
	// Map to store expansion state for each group
	private final Map<String, Boolean> groupExpansionStates = new ConcurrentHashMap<>();

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
		groupVisibilityStates.clear();
		groupExpansionStates.clear(); // Clear expansion state
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
		groupVisibilityStates.clear();
		groupExpansionStates.clear(); // Clear expansion state
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
	 * Enters the marker creation mode, preparing the plugin and UI.
	 * Called from the UI (+ button).
	 *
	 * @param groupName The target group for the new marker.
	 */
	public void enterCreationMode(String groupName) {
		this.targetGroupNameForCreation = markerGroups.containsKey(groupName) ? groupName : UNASSIGNED_GROUP;
		this.creatingScreenMarker = true;
		this.setMouseListenerEnabled(true);
		this.currentMarker = null; // Clear any previous marker being created
		this.startLocation = null; // Clear start location
		this.drawingScreenMarker = false; // Not drawing yet
		this.selectedWidgetBounds = null; // Clear selected widget

		// Reset the creation overlay
		overlay.setPreferredLocation(null);
		overlay.setPreferredSize(null);

		// Update the UI panel
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
		// Don't create if already created or location is invalid
		if (currentMarker != null || location == null) {
			return;
		}

		// Generate a unique ID (simple increment for now, consider better approach if
		// needed)
		long nextMarkerId = findMaxMarkerId() + 1;

		currentMarker = new ScreenMarker(
				nextMarkerId, // Use generated ID
				DEFAULT_MARKER_NAME + " " + nextMarkerId, // Default name
				ScreenMarkerGroupsPluginPanel.SELECTED_BORDER_THICKNESS,
				ScreenMarkerGroupsPluginPanel.SELECTED_COLOR,
				ScreenMarkerGroupsPluginPanel.SELECTED_FILL_COLOR,
				true,
				false,
				null); // Pass null for importedId
		startLocation = location;
		overlay.setPreferredLocation(location);
		overlay.setPreferredSize(size != null ? size : DEFAULT_SIZE);
		drawingScreenMarker = true;
	}

	public void finishCreation(boolean aborted) {
		ScreenMarker marker = currentMarker;
		String targetGroup = targetGroupNameForCreation != null ? targetGroupNameForCreation : UNASSIGNED_GROUP;
		// Get bounds from the creation overlay, which is set correctly by startCreation
		Rectangle overlayBounds = overlay.getBounds();

		if (!aborted && marker != null && overlayBounds != null && overlayBounds.width > 0
				&& overlayBounds.height > 0) {
			// Pass plugin instance 'this' to the constructor
			final ScreenMarkerOverlay screenMarkerOverlay = new ScreenMarkerOverlay(marker, this);
			// Use the overlay's bounds for location and size
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
			updateGroupsConfig(); // This saves markers, order, visibility, and expansion
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
			updateGroupsConfig(); // This saves markers, order, visibility, and expansion
			SwingUtilities.invokeLater(pluginPanel::rebuild);
		}
	}

	void resizeMarker(Point point) {
		if (startLocation == null) {
			// Call the renamed method
			startCreation(point, DEFAULT_SIZE);
			return;
		}
		drawingScreenMarker = true;
		Rectangle bounds = new Rectangle(startLocation);
		bounds.add(point);
		overlay.setPreferredLocation(bounds.getLocation());
		overlay.setPreferredSize(bounds.getSize());
	}

	public void updateGroupsConfig() {
		boolean shouldSaveMarkers = !markerGroups.isEmpty();
		boolean shouldSaveOrder = !groupOrderList.isEmpty();
		boolean shouldSaveVisibility = !groupVisibilityStates.isEmpty();
		boolean shouldSaveExpansion = !groupExpansionStates.isEmpty();

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

		// Save group order
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

		// Save visibility and expansion states separately
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
		groupExpansionStates.clear(); // Clear before loading

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
				System.err.println("Error parsing group visibility JSON: " + e.getMessage());
				groupVisibilityStates.clear(); // Reset on error
			}
		}

		// Load Group Expansion States
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
				System.err.println("Error parsing group expansion JSON: " + e.getMessage());
				groupExpansionStates.clear(); // Reset on error
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
		return groupVisibilityStates.getOrDefault(groupName, true); // Default to visible
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

	/**
	 * Checks if a group is currently set to be expanded.
	 * Defaults to true if the group has no specific state saved.
	 *
	 * @param groupName The name of the group.
	 * @return True if the group is expanded, false otherwise.
	 */
	public boolean isGroupExpanded(String groupName) {
		return groupExpansionStates.getOrDefault(groupName, true); // Default to expanded
	}

	/**
	 * Sets the expansion state for a specific group and saves the configuration.
	 * Note: The UI update (showing/hiding markers) is handled by the panel's
	 * rebuild triggered by the callback.
	 *
	 * @param groupName  The name of the group.
	 * @param isExpanded The desired expansion state.
	 */
	public void setGroupExpansion(String groupName, boolean isExpanded) {
		if (!markerGroups.containsKey(groupName)) {
			return; // Ignore if group doesn't exist
		}
		groupExpansionStates.put(groupName, isExpanded);
		updateExpansionConfig(); // Save the change
		// No need to directly manipulate overlays here, panel rebuild handles it
	}

	public boolean addGroup(String name) {
		if (Strings.isNullOrEmpty(name) || markerGroups.containsKey(name)) {
			return false;
		}
		markerGroups.put(name, new ArrayList<>());
		groupVisibilityStates.put(name, true); // Default new group to visible
		groupExpansionStates.put(name, true); // Default new group to expanded
		int insertIndex = groupOrderList.size();
		if (groupOrderList.contains(IMPORTED_GROUP)) {
			insertIndex = groupOrderList.indexOf(IMPORTED_GROUP);
		}
		if (groupOrderList.contains(UNASSIGNED_GROUP)) {
			insertIndex = Math.min(insertIndex, groupOrderList.indexOf(UNASSIGNED_GROUP));
		}
		groupOrderList.add(insertIndex, name);

		updateGroupsConfig(); // Saves markers, order, visibility, and expansion
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
		groupVisibilityStates.remove(groupName);
		groupExpansionStates.remove(groupName); // Remove expansion state
		updateGroupsConfig(); // Saves markers, order, visibility, and expansion
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
		Boolean visibility = groupVisibilityStates.remove(oldName);
		Boolean expansion = groupExpansionStates.remove(oldName); // Get and remove old expansion state

		if (markers != null) {
			markerGroups.put(newName, markers);
			groupVisibilityStates.put(newName, visibility != null ? visibility : true); // Preserve visibility
			groupExpansionStates.put(newName, expansion != null ? expansion : true); // Preserve expansion

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
			updateGroupsConfig(); // Saves markers, order, visibility, and expansion
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
		updateGroupsConfig(); // Saves markers, order, visibility, and expansion
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
		updateGroupsConfig(); // Saves markers, order, visibility, and expansion
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
			updateGroupsConfig(); // Saves markers, order, visibility, and expansion
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
			updateGroupsConfig(); // Saves markers, order, visibility, and expansion
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
			updateGroupsConfig(); // Saves markers, order, visibility, and expansion
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
		long maxId = findMaxMarkerId(); // Find current max ID to avoid potential collisions with newly created
										// markers

		for (ScreenMarker markerData : loadedMarkers) {
			if (markerData == null) {
				continue;
			}

			// Check if this marker (by original ID) has already been imported
			final long originalMarkerId = markerData.getId(); // Use final for lambda
			boolean alreadyImported = importedGroupList.stream()
					.map(ScreenMarkerOverlay::getMarker)
					.filter(m -> m.getImportedId() != null) // Check markers that have an importedId
					.anyMatch(existingMarker -> originalMarkerId == existingMarker.getImportedId()); // Use primitive
																										// comparison

			if (alreadyImported) {
				continue; // Skip this marker if it's already in the "Imported" group
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
					markerData.isVisible(), // Keep original visibility
					markerData.isLabelled(),
					null); // Pass null for importedId initially
			newMarker.setImportedId(originalMarkerId); // Store the original ID

			// Create the overlay for the new marker
			ScreenMarkerOverlay newOverlay = new ScreenMarkerOverlay(newMarker, this);

			// Try to read original position and size using original ID
			// final long originalId = markerData.getId(); // Already defined above
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
				// Ensure any old config for this new ID is removed if no location found
				configManager.unsetConfiguration(OVERLAY_CONFIG_GROUP, newLocationKey);
			}

			if (originalSize != null) {
				configManager.setConfiguration(OVERLAY_CONFIG_GROUP, newSizeKey,
						originalSize.width + "x" + originalSize.height);
			} else {
				// Ensure any old config for this new ID is removed if no size found
				configManager.unsetConfiguration(OVERLAY_CONFIG_GROUP, newSizeKey);
			}

			// Add the new overlay to the internal group list
			importedGroupList.add(newOverlay);

			// Add to overlay manager IF the group is visible AND the marker is visible
			// This controls runtime visibility
			if (isGroupVisible(IMPORTED_GROUP) && newMarker.isVisible()) {
				overlayManager.add(newOverlay);
			}

			// Save the overlay's marker data (JSON blob) using the OverlayManager
			// This likely doesn't handle position/size, which we now do explicitly above
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
}
