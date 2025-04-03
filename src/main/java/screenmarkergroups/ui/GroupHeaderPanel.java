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
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
// SwingUtil import removed as unused
import net.runelite.client.util.ImageUtil;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

/**
 * A panel that represents the header for a screen marker group.
 * Displays the group name and includes controls for visibility, expansion,
 * configuration, and adding markers.
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
	private boolean isVisible;
	private boolean isExpanded;
	private final ScreenMarkerGroupsPlugin plugin;
	private final JLabel expansionLabel = new JLabel();
	private final JLabel configureLabel = new JLabel();
	private final JLabel visibilityLabel = new JLabel();
	private final JLabel addMarkerButton = new JLabel();
	private final JPopupMenu contextMenu;

	static {
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
	 * Updates the enabled state of context menu items (Rename, Delete, Move Up,
	 * Move Down) based on whether the group is a special group ("Unassigned",
	 * "Imported") and the total number of groups.
	 */
	private void updateContextMenuItems() {
		JMenuItem renameItem = (JMenuItem) contextMenu.getComponent(0);
		JMenuItem deleteItem = (JMenuItem) contextMenu.getComponent(1);
		JMenuItem moveUpItem = (JMenuItem) contextMenu.getComponent(3);
		JMenuItem moveDownItem = (JMenuItem) contextMenu.getComponent(4);

		boolean isUnassignedGroup = groupName.equals(ScreenMarkerGroupsPlugin.UNASSIGNED_GROUP);
		boolean isImportedGroup = groupName.equals(ScreenMarkerGroupsPlugin.IMPORTED_GROUP);
		boolean isSpecialGroup = isUnassignedGroup || isImportedGroup;

		// Can only move non-special groups, and only if there's more than one group
		// total (implicitly > 2 if special groups exist)
		boolean canMove = !isSpecialGroup
				&& plugin.getMarkerGroups().size() > (isUnassignedGroup ? 1 : 0) + (isImportedGroup ? 1 : 0) + 1;

		renameItem.setEnabled(!isSpecialGroup);
		deleteItem.setEnabled(!isUnassignedGroup); // Allow deleting "Imported"
		moveUpItem.setEnabled(canMove);
		moveDownItem.setEnabled(canMove);
	}

	/**
	 * Creates and configures the right-click context menu for the group header.
	 * Includes options for renaming, deleting, and reordering the group.
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
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		Border padding = new EmptyBorder(4, 5, 4, 5);
		Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR);
		setBorder(new CompoundBorder(padding, line));

		nameLabel = new JLabel(groupName);
		nameLabel.setFont(FontManager.getRunescapeBoldFont());
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setBorder(new EmptyBorder(0, 3, 0, 0));

		this.contextMenu = setupContextMenu();

		updateExpansionIcon();
		expansionLabel.setToolTipText(isExpanded ? "Collapse group" : "Expand group");
		expansionLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					isExpanded = !isExpanded;
					updateExpansionIcon();
					expansionLabel.setToolTipText(isExpanded ? "Collapse group" : "Expand group");
					if (onExpansionChange != null) {
						onExpansionChange.accept(isExpanded);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				expansionLabel.setIcon(isExpanded ? EXPANDED_HOVER_ICON : COLLAPSED_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				updateExpansionIcon();
			}
		});

		configureLabel.setIcon(CONFIGURE_ICON);
		configureLabel.setToolTipText("Configure group");
		configureLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (configureLabel.isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
					updateContextMenuItems();
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

		updateVisibilityIcon();
		visibilityLabel.setToolTipText(isVisible ? "Hide group markers" : "Show group markers");
		visibilityLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					isVisible = !isVisible;
					updateVisibilityIcon();
					visibilityLabel.setToolTipText(isVisible ? "Hide group markers" : "Show group markers");
					if (onVisibilityChange != null) {
						onVisibilityChange.accept(isVisible);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				visibilityLabel.setIcon(isVisible ? VISIBLE_HOVER_ICON : INVISIBLE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				updateVisibilityIcon();
			}
		});

		addMarkerButton.setIcon(ADD_MARKER_ICON);
		addMarkerButton.setToolTipText("Add new marker to this group");
		addMarkerButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
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

		JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 2));
		rightActions.setBackground(getBackground());
		rightActions.setBorder(new EmptyBorder(0, 0, 4, 2));
		rightActions.add(configureLabel);
		rightActions.add(visibilityLabel);
		rightActions.add(addMarkerButton);

		JPanel leftActions = new JPanel(new BorderLayout());
		leftActions.setBackground(getBackground());
		leftActions.setBorder(new EmptyBorder(2, 2, 4, 0));
		leftActions.add(expansionLabel, BorderLayout.WEST);
		leftActions.add(nameLabel, BorderLayout.CENTER);

		add(leftActions, BorderLayout.CENTER);
		add(rightActions, BorderLayout.EAST);
	}

	/**
	 * Enables or disables the interactive controls (expansion, configure,
	 * visibility, add) on the header panel. Updates icons to appear dimmed when
	 * disabled.
	 *
	 * @param enabled True to enable controls, false to disable.
	 */
	void setControlsEnabled(boolean enabled) {
		expansionLabel.setEnabled(enabled);
		configureLabel.setEnabled(enabled);
		visibilityLabel.setEnabled(enabled);
		addMarkerButton.setEnabled(enabled);

		if (enabled) {
			updateExpansionIcon();
			expansionLabel.setToolTipText(isExpanded ? "Collapse group" : "Expand group");
			configureLabel.setIcon(CONFIGURE_ICON);
			configureLabel.setToolTipText("Configure group");
			updateVisibilityIcon();
			visibilityLabel.setToolTipText(isVisible ? "Hide group markers" : "Show group markers");
			addMarkerButton.setIcon(ADD_MARKER_ICON);
			addMarkerButton.setToolTipText("Add new marker to this group");
		} else {
			expansionLabel.setIcon(isExpanded ? new ImageIcon(ImageUtil.alphaOffset(EXPANDED_ICON.getImage(), 0.5f))
					: new ImageIcon(ImageUtil.alphaOffset(COLLAPSED_ICON.getImage(), 0.5f)));
			expansionLabel.setToolTipText(null);
			configureLabel.setIcon(new ImageIcon(ImageUtil.alphaOffset(CONFIGURE_ICON.getImage(), 0.5f)));
			configureLabel.setToolTipText(null);
			visibilityLabel.setIcon(isVisible ? new ImageIcon(ImageUtil.alphaOffset(VISIBLE_ICON.getImage(), 0.5f))
					: new ImageIcon(ImageUtil.alphaOffset(INVISIBLE_ICON.getImage(), 0.5f)));
			visibilityLabel.setToolTipText(null);
			addMarkerButton.setIcon(new ImageIcon(ImageUtil.alphaOffset(ADD_MARKER_ICON.getImage(), 0.5f)));
			addMarkerButton.setToolTipText(null);
		}
	}

	/**
	 * Updates the visibility icon (eye open/closed) based on the current
	 * `isVisible` state.
	 */
	private void updateVisibilityIcon() {
		visibilityLabel.setIcon(isVisible ? VISIBLE_ICON : INVISIBLE_ICON);
	}

	/**
	 * Updates the expansion icon (arrow down/right) based on the current
	 * `isExpanded` state.
	 */
	private void updateExpansionIcon() {
		expansionLabel.setIcon(isExpanded ? EXPANDED_ICON : COLLAPSED_ICON);
	}
}
