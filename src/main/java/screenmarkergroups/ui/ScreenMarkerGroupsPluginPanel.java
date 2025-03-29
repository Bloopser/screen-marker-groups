/*
 * Copyright (c) 2025, Bloopser <https://github.com/Bloopser>
 * Copyright (c) 2018, Kamiel, <https://github.com/Kamielvf>
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
package screenmarkergroups.ui;

import screenmarkergroups.ScreenMarkerOverlay;
import screenmarkergroups.ScreenMarkerGroupsPlugin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;
import java.awt.Component; // Import Component

public class ScreenMarkerGroupsPluginPanel extends PluginPanel {
	@Getter
	private final Map<String, ScreenMarkerGroupsCreationPanel> creationPanelsMap = new HashMap<>();

	private static final ImageIcon ADD_GROUP_ICON;
	private static final ImageIcon ADD_GROUP_HOVER_ICON;
	private static final ImageIcon ADD_MARKER_ICON;
	private static final ImageIcon ADD_MARKER_HOVER_ICON;

	private static final Color DEFAULT_BORDER_COLOR = Color.GREEN;
	private static final Color DEFAULT_FILL_COLOR = new Color(0, 255, 0, 0);

	private static final int DEFAULT_BORDER_THICKNESS = 3;

	public static final Color SELECTED_COLOR = DEFAULT_BORDER_COLOR;
	public static final Color SELECTED_FILL_COLOR = DEFAULT_FILL_COLOR;
	public static final int SELECTED_BORDER_THICKNESS = DEFAULT_BORDER_THICKNESS;

	private final JLabel addGroupButton = new JLabel(ADD_GROUP_ICON);
	private final JLabel addMarker = new JLabel(ADD_MARKER_ICON);
	private final JLabel title = new JLabel();
	private final PluginErrorPanel noMarkersPanel = new PluginErrorPanel();
	private final JPanel markerView = new JPanel(new GridBagLayout());

	private final ScreenMarkerGroupsPlugin plugin;

	static {
		final BufferedImage addIcon = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class, "add_icon.png");
		ADD_MARKER_ICON = new ImageIcon(addIcon);
		ADD_MARKER_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));

		ADD_GROUP_ICON = new ImageIcon(addIcon);
		ADD_GROUP_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));
	}

	public ScreenMarkerGroupsPluginPanel(ScreenMarkerGroupsPlugin screenMarkerPlugin) {
		this.plugin = screenMarkerPlugin;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBorder(new EmptyBorder(1, 0, 10, 0));

		title.setText("Screen Marker Groups");
		title.setForeground(Color.WHITE);

		addGroupButton.setToolTipText("Add new group");
		addGroupButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				String groupName = JOptionPane.showInputDialog(
						ScreenMarkerGroupsPluginPanel.this,
						"Enter name for the new group:",
						"Add New Group",
						JOptionPane.PLAIN_MESSAGE);

				if (!com.google.common.base.Strings.isNullOrEmpty(groupName)) {
					if (!plugin.addGroup(groupName)) {
						JOptionPane.showMessageDialog(
								ScreenMarkerGroupsPluginPanel.this,
								"Group '" + groupName + "' already exists or is invalid.",
								"Error Adding Group",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				addGroupButton.setIcon(ADD_GROUP_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				addGroupButton.setIcon(ADD_GROUP_ICON);
			}
		});

		northPanel.add(title, BorderLayout.WEST);
		northPanel.add(addGroupButton, BorderLayout.EAST);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		markerView.setBackground(ColorScheme.DARK_GRAY_COLOR);

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;

		noMarkersPanel.setContent("Screen Markers", "Highlight a region on your screen.");
		noMarkersPanel.setVisible(false);

		markerView.add(noMarkersPanel, constraints);
		constraints.gridy++;

		addMarker.setToolTipText("Add new screen marker (to Unassigned)");
		addMarker.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				setCreation(true);
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				addMarker.setIcon(ADD_MARKER_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				addMarker.setIcon(ADD_MARKER_ICON);
			}
		});

		centerPanel.add(markerView, BorderLayout.CENTER);

		add(northPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}

	public void rebuild() {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;

		markerView.removeAll();
		creationPanelsMap.clear();
		int markerCount = 0;

		for (String groupName : plugin.getGroupOrderList()) {
			List<ScreenMarkerOverlay> markersInGroup = plugin.getMarkerGroups().get(groupName);

			if (markersInGroup == null) {
				continue;
			}

			if (markersInGroup.isEmpty() && groupName.equals(ScreenMarkerGroupsPlugin.UNASSIGNED_GROUP)) {
				continue;
			}

			// Assume plugin has methods: isGroupVisible(groupName) and
			// setGroupVisibility(groupName, isVisible)
			boolean initialVisibility = plugin.isGroupVisible(groupName); // Get initial state
			GroupHeaderPanel headerPanel = new GroupHeaderPanel(plugin, groupName, initialVisibility,
					(isVisible) -> plugin.setGroupVisibility(groupName, isVisible) // Provide callback
			);
			markerView.add(headerPanel, constraints);
			constraints.gridy++;

			ScreenMarkerGroupsCreationPanel currentCreationPanel = new ScreenMarkerGroupsCreationPanel(plugin);
			currentCreationPanel.setVisible(false);
			creationPanelsMap.put(groupName, currentCreationPanel);
			markerView.add(currentCreationPanel, constraints);
			constraints.gridy++;

			for (final ScreenMarkerOverlay marker : markersInGroup) {
				markerView.add(new ScreenMarkerGroupsPanel(plugin, marker), constraints);
				constraints.gridy++;
				markerCount++;

				markerView.add(Box.createRigidArea(new Dimension(0, 5)), constraints);
				constraints.gridy++;
			}

			markerView.add(Box.createRigidArea(new Dimension(0, 15)), constraints);
			constraints.gridy++;
		}

		boolean empty = markerCount == 0;
		noMarkersPanel.setVisible(empty);

		markerView.add(noMarkersPanel, constraints);
		constraints.gridy++;

		repaint();
		revalidate();
	}

	public void setCreation(boolean on) {
		creationPanelsMap.values().forEach(panel -> panel.setVisible(false));

		if (on) {
			String targetGroup = plugin.getTargetGroupNameForCreation();
			ScreenMarkerGroupsCreationPanel targetCreationPanel = creationPanelsMap
					.get(targetGroup != null ? targetGroup : ScreenMarkerGroupsPlugin.UNASSIGNED_GROUP);

			if (targetCreationPanel != null) {
				targetCreationPanel.setVisible(true);
				targetCreationPanel.lockConfirm();
				noMarkersPanel.setVisible(false);
				title.setVisible(true);
			}
		} else {
			boolean empty = plugin.getMarkerGroups().values().stream().allMatch(List::isEmpty);
			noMarkersPanel.setVisible(empty);
		}

		addGroupButton.setVisible(!on);

		// Enable/disable controls on existing marker and group panels
		for (Component comp : markerView.getComponents()) {
			if (comp instanceof ScreenMarkerGroupsPanel) {
				((ScreenMarkerGroupsPanel) comp).setControlsEnabled(!on);
			} else if (comp instanceof GroupHeaderPanel) {
				((GroupHeaderPanel) comp).setControlsEnabled(!on);
			}
		}
	}

	public Map<String, ScreenMarkerGroupsCreationPanel> getCreationPanelsMap() {
		return creationPanelsMap;
	}
}
