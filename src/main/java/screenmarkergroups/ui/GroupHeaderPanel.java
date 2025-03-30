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
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.SwingUtil;
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
	private static final ImageIcon VISIBLE_ICON;
	private static final ImageIcon VISIBLE_HOVER_ICON;
	private static final ImageIcon INVISIBLE_ICON;
	private static final ImageIcon INVISIBLE_HOVER_ICON;
	private static final ImageIcon EXPANDED_ICON;
	private static final ImageIcon EXPANDED_HOVER_ICON;
	private static final ImageIcon COLLAPSED_ICON;
	private static final ImageIcon COLLAPSED_HOVER_ICON;

	private final JLabel nameLabel;
	private final String groupName;
	private boolean isVisible; // State for group visibility
	private boolean isExpanded; // State for group expansion
	private final Consumer<Boolean> onVisibilityChange; // Callback for visibility
	private final Consumer<Boolean> onExpansionChange; // Callback for expansion
	private final ScreenMarkerGroupsPlugin plugin;
	private final JLabel expansionLabel = new JLabel(); // Label for expand/collapse icon
	private final JLabel configureLabel = new JLabel();
	private final JLabel visibilityLabel = new JLabel(); // New label for visibility
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

		final BufferedImage visibleIcon = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class,
				"visible_icon.png");
		VISIBLE_ICON = new ImageIcon(visibleIcon);
		VISIBLE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(visibleIcon, -100));

		final BufferedImage invisibleIcon = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class,
				"invisible_icon.png");
		INVISIBLE_ICON = new ImageIcon(invisibleIcon);
		INVISIBLE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(invisibleIcon, -100));

		final BufferedImage expandedIcon = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class, "expanded.png");
		EXPANDED_ICON = new ImageIcon(expandedIcon);
		EXPANDED_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(expandedIcon, -100));

		final BufferedImage collapsedIcon = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class,
				"collapsed.png");
		COLLAPSED_ICON = new ImageIcon(collapsedIcon);
		COLLAPSED_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(collapsedIcon, -100));
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

		boolean isUnassignedGroup = groupName.equals(ScreenMarkerGroupsPlugin.UNASSIGNED_GROUP);
		boolean isImportedGroup = groupName.equals(ScreenMarkerGroupsPlugin.IMPORTED_GROUP);
		boolean isSpecialGroupForMove = isUnassignedGroup || isImportedGroup; // Both are special for moving/renaming

		boolean canMove = plugin.getMarkerGroups().size() > 1 && !isSpecialGroupForMove;

		renameItem.setEnabled(!isSpecialGroupForMove); // Cannot rename Unassigned or Imported
		deleteItem.setEnabled(!isUnassignedGroup); // Can delete Imported, but not Unassigned
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

	/**
	 * Constructs a GroupHeaderPanel.
	 *
	 * @param plugin             The main plugin instance.
	 * @param groupName          The name of the group this header represents.
	 * @param initialVisibility  The initial visibility state of the group.
	 * @param initialExpansion   The initial expansion state of the group.
	 * @param onVisibilityChange Callback function invoked when visibility is
	 *                           toggled.
	 * @param onExpansionChange  Callback function invoked when expansion is
	 *                           toggled.
	 */
	GroupHeaderPanel(ScreenMarkerGroupsPlugin plugin, String groupName, boolean initialVisibility,
			boolean initialExpansion, Consumer<Boolean> onVisibilityChange, Consumer<Boolean> onExpansionChange) {
		this.plugin = plugin;
		this.groupName = groupName;
		this.isVisible = initialVisibility;
		this.isExpanded = initialExpansion;
		this.onVisibilityChange = onVisibilityChange;
		this.onExpansionChange = onExpansionChange;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Create a compound border: padding + bottom line
		Border padding = new EmptyBorder(4, 5, 4, 5); // Increased top/bottom padding from 2 to 4
		Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR);
		setBorder(new CompoundBorder(padding, line));

		nameLabel = new JLabel(groupName);
		nameLabel.setFont(FontManager.getRunescapeBoldFont());
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setBorder(new EmptyBorder(0, 3, 0, 0)); // Add padding between icon and text

		// Setup context menu first
		this.contextMenu = setupContextMenu();

		// Setup Expansion button
		updateExpansionIcon(); // Set initial icon
		expansionLabel.setToolTipText(isExpanded ? "Collapse group" : "Expand group");
		expansionLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					isExpanded = !isExpanded; // Toggle state
					updateExpansionIcon();
					expansionLabel.setToolTipText(isExpanded ? "Collapse group" : "Expand group");
					if (onExpansionChange != null) {
						onExpansionChange.accept(isExpanded); // Notify listener
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				expansionLabel.setIcon(isExpanded ? EXPANDED_HOVER_ICON : COLLAPSED_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				updateExpansionIcon(); // Revert to non-hover icon
			}
		});

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

		// Setup Visibility button
		updateVisibilityIcon(); // Set initial icon based on state
		visibilityLabel.setToolTipText(isVisible ? "Hide group markers" : "Show group markers");
		visibilityLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					isVisible = !isVisible; // Toggle state
					updateVisibilityIcon();
					visibilityLabel.setToolTipText(isVisible ? "Hide group markers" : "Show group markers");
					if (onVisibilityChange != null) {
						onVisibilityChange.accept(isVisible); // Notify listener
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				visibilityLabel.setIcon(isVisible ? VISIBLE_HOVER_ICON : INVISIBLE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				updateVisibilityIcon(); // Revert to non-hover icon
			}
		});

		// Setup Add Marker button
		addMarkerButton.setIcon(ADD_MARKER_ICON);
		addMarkerButton.setToolTipText("Add new marker to this group");
		addMarkerButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					// Call the new method to enter creation mode targeting this group
					plugin.enterCreationMode(groupName);
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

		// Panel for right-side controls using FlowLayout for easier horizontal
		// arrangement
		JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 2)); // Right-aligned, 3px horizontal gap
		rightActions.setBackground(getBackground());
		rightActions.setBorder(new EmptyBorder(0, 0, 4, 2)); // Add 2px right padding
		rightActions.add(configureLabel);
		rightActions.add(visibilityLabel); // Add visibility button in the middle
		rightActions.add(addMarkerButton);

		// Panel for left-side elements (expansion icon + name)
		JPanel leftActions = new JPanel(new BorderLayout());
		leftActions.setBackground(getBackground());
		leftActions.setBorder(new EmptyBorder(2, 2, 4, 0)); // Add 2px left padding
		leftActions.add(expansionLabel, BorderLayout.WEST);
		leftActions.add(nameLabel, BorderLayout.CENTER);

		// Add components to the main panel
		add(leftActions, BorderLayout.CENTER);
		add(rightActions, BorderLayout.EAST);
	}

	/**
	 * Enables or disables the configuration controls for this group header.
	 * 
	 * @param enabled True to enable, false to disable.
	 */
	void setControlsEnabled(boolean enabled) {
		expansionLabel.setEnabled(enabled); // Enable/disable expansion toggle
		configureLabel.setEnabled(enabled);
		visibilityLabel.setEnabled(enabled); // Also enable/disable visibility toggle
		addMarkerButton.setEnabled(enabled); // Assuming add should also be disabled

		// Update icons and tooltips based on enabled state
		if (enabled) {
			updateExpansionIcon(); // Set correct expanded/collapsed icon
			expansionLabel.setToolTipText(isExpanded ? "Collapse group" : "Expand group");
		} else {
			// Use a dimmed version of the current icon when disabled
			expansionLabel.setIcon(isExpanded ? new ImageIcon(ImageUtil.alphaOffset(EXPANDED_ICON.getImage(), 0.5f))
					: new ImageIcon(ImageUtil.alphaOffset(COLLAPSED_ICON.getImage(), 0.5f)));
			expansionLabel.setToolTipText(null);
		}

		configureLabel.setIcon(
				enabled ? CONFIGURE_ICON : new ImageIcon(ImageUtil.alphaOffset(CONFIGURE_ICON.getImage(), 0.5f)));
		configureLabel.setToolTipText(enabled ? "Configure group" : null);

		if (enabled) {
			updateVisibilityIcon(); // Set correct visible/invisible icon
			visibilityLabel.setToolTipText(isVisible ? "Hide group markers" : "Show group markers");
		} else {
			// Use a dimmed version of the current icon when disabled
			visibilityLabel.setIcon(isVisible ? new ImageIcon(ImageUtil.alphaOffset(VISIBLE_ICON.getImage(), 0.5f))
					: new ImageIcon(ImageUtil.alphaOffset(INVISIBLE_ICON.getImage(), 0.5f)));
			visibilityLabel.setToolTipText(null);
		}

		addMarkerButton.setIcon(
				enabled ? ADD_MARKER_ICON : new ImageIcon(ImageUtil.alphaOffset(ADD_MARKER_ICON.getImage(), 0.5f)));
		addMarkerButton.setToolTipText(enabled ? "Add new marker to this group" : null);
	}

	/**
	 * Updates the visibility icon based on the current isVisible state.
	 */
	private void updateVisibilityIcon() {
		visibilityLabel.setIcon(isVisible ? VISIBLE_ICON : INVISIBLE_ICON);
	}

	/**
	 * Updates the expansion icon based on the current isExpanded state.
	 */
	private void updateExpansionIcon() {
		expansionLabel.setIcon(isExpanded ? EXPANDED_ICON : COLLAPSED_ICON);
	}
}
