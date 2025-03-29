/*
 * Copyright (c) 2025, Bloopser <https://github.com/Bloopser>
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

import screenmarkergroups.ScreenMarkerGroupsPlugin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

/**
 * A panel that represents the header for a screen marker group.
 * Displays the group name and includes controls for configuration and adding
 * markers.
 */
class GroupHeaderPanel extends JPanel {
	private static final ImageIcon ADD_MARKER_ICON;
	private static final ImageIcon ADD_MARKER_HOVER_ICON;
	private static final ImageIcon CONFIGURE_ICON;
	private static final ImageIcon CONFIGURE_HOVER_ICON;

	private final JLabel nameLabel;
	private final String groupName;
	private final ScreenMarkerGroupsPlugin plugin;
	private final JLabel configureLabel = new JLabel();
	private final JLabel addMarkerButton = new JLabel();
	private final JPopupMenu contextMenu;

	static {
		// Load icons
		final BufferedImage addIcon = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class, "add_icon.png");
		ADD_MARKER_ICON = new ImageIcon(addIcon);
		ADD_MARKER_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, -100));

		final BufferedImage configureIcon = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class,
				"configure.png");
		CONFIGURE_ICON = new ImageIcon(configureIcon);
		CONFIGURE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(configureIcon, -100));
	}

	/**
	 * Updates the enabled state of context menu items based on the group type and
	 * count.
	 */
	private void updateContextMenuItems() {
		// Find menu items
		JMenuItem renameItem = (JMenuItem) contextMenu.getComponent(0);
		JMenuItem deleteItem = (JMenuItem) contextMenu.getComponent(1);
		JMenuItem moveUpItem = (JMenuItem) contextMenu.getComponent(3); // Skip separator
		JMenuItem moveDownItem = (JMenuItem) contextMenu.getComponent(4);

		boolean isSpecialGroup = groupName.equals(ScreenMarkerGroupsPlugin.UNASSIGNED_GROUP)
				|| groupName.equals(ScreenMarkerGroupsPlugin.IMPORTED_GROUP);
		// TODO: Add more sophisticated move logic based on actual position
		boolean canMove = plugin.getMarkerGroups().size() > 1 && !isSpecialGroup;

		renameItem.setEnabled(!isSpecialGroup);
		deleteItem.setEnabled(!isSpecialGroup);
		moveUpItem.setEnabled(canMove);
		moveDownItem.setEnabled(canMove);
	}

	/**
	 * Sets up the context menu for the group header.
	 * 
	 * @return The configured JPopupMenu.
	 */
	private JPopupMenu setupContextMenu() {
		final JPopupMenu popupMenu = new JPopupMenu();

		final JMenuItem renameItem = new JMenuItem("Rename Group");
		renameItem.addActionListener(e -> {
			String newName = JOptionPane.showInputDialog(
					GroupHeaderPanel.this,
					"Enter new name for group '" + groupName + "':",
					"Rename Group",
					JOptionPane.PLAIN_MESSAGE);

			if (!com.google.common.base.Strings.isNullOrEmpty(newName)) {
				if (!plugin.renameGroup(groupName, newName)) {
					JOptionPane.showMessageDialog(
							GroupHeaderPanel.this,
							"Failed to rename group. New name may be invalid or already exist.",
							"Rename Group Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		final JMenuItem deleteItem = new JMenuItem("Delete Group");
		deleteItem.addActionListener(e -> plugin.deleteGroup(groupName));

		final JMenuItem moveUpItem = new JMenuItem("Move Up");
		moveUpItem.addActionListener(e -> plugin.moveGroupUp(groupName));

		final JMenuItem moveDownItem = new JMenuItem("Move Down");
		moveDownItem.addActionListener(e -> plugin.moveGroupDown(groupName));

		popupMenu.add(renameItem);
		popupMenu.add(deleteItem);
		popupMenu.addSeparator();
		popupMenu.add(moveUpItem);
		popupMenu.add(moveDownItem);

		return popupMenu;
	}

	GroupHeaderPanel(ScreenMarkerGroupsPlugin plugin, String groupName) {
		this.plugin = plugin;
		this.groupName = groupName;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 5, 5, 5));

		nameLabel = new JLabel(groupName);
		nameLabel.setFont(FontManager.getRunescapeBoldFont());
		nameLabel.setForeground(Color.WHITE);

		// Setup context menu first
		this.contextMenu = setupContextMenu();

		// Setup Configure button
		configureLabel.setIcon(CONFIGURE_ICON);
		configureLabel.setToolTipText("Configure group");
		configureLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				// Only show context menu if the label is enabled
				if (configureLabel.isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
					updateContextMenuItems(); // Update states before showing
					contextMenu.show(configureLabel, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				configureLabel.setIcon(CONFIGURE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				configureLabel.setIcon(CONFIGURE_ICON);
			}
		});

		// Setup Add Marker button
		addMarkerButton.setIcon(ADD_MARKER_ICON);
		addMarkerButton.setToolTipText("Add new marker to this group");
		addMarkerButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					plugin.startCreation(null, null, groupName);
					if (plugin.getPluginPanel() != null) {
						plugin.getPluginPanel().setCreation(true);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				addMarkerButton.setIcon(ADD_MARKER_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				addMarkerButton.setIcon(ADD_MARKER_ICON);
			}
		});

		// Panel for right-side controls
		JPanel rightActions = new JPanel(new BorderLayout(3, 0)); // Gap between buttons
		rightActions.setBackground(getBackground());
		rightActions.add(configureLabel, BorderLayout.CENTER); // Configure on the left
		rightActions.add(addMarkerButton, BorderLayout.EAST); // Add on the right

		// Add components to the main panel
		add(nameLabel, BorderLayout.CENTER);
		add(rightActions, BorderLayout.EAST);
	}

	/**
	 * Enables or disables the configuration controls for this group header.
	 * 
	 * @param enabled True to enable, false to disable.
	 */
	void setControlsEnabled(boolean enabled) {
		configureLabel.setEnabled(enabled);
		// Optionally, change icon to a disabled version if available/needed
		// configureLabel.setIcon(enabled ? CONFIGURE_ICON : CONFIGURE_DISABLED_ICON);
		configureLabel.setToolTipText(enabled ? "Configure group" : null);
	}
}
