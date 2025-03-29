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
package screenmarkergroups.ui; // Correct package

import screenmarkergroups.ScreenMarkerGroupsPlugin; // Correct import
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter; // Added import
import java.awt.event.MouseEvent; // Added import
import javax.swing.JLabel;
import javax.swing.JMenuItem; // Added import
import javax.swing.JOptionPane; // Added import for dialogs
import javax.swing.JPanel;
import javax.swing.JPopupMenu; // Added import
import javax.swing.SwingUtilities; // Added import
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil; // Added for icon loading
import javax.swing.ImageIcon; // Added for icon loading
import java.awt.image.BufferedImage; // Added for icon loading

/**
 * A panel that represents the header for a screen marker group.
 * Displays the group name and will later include controls for
 * collapsing/expanding and context menus.
 */
class GroupHeaderPanel extends JPanel {
    private static final ImageIcon ADD_MARKER_ICON;
    private static final ImageIcon ADD_MARKER_HOVER_ICON;

    private final JLabel nameLabel;
    private final String groupName; // Store group name
    private final ScreenMarkerGroupsPlugin plugin; // Store plugin reference
    private final JLabel addMarkerButton = new JLabel(ADD_MARKER_ICON); // Add marker button

    static {
        // Load the add marker icon
        final BufferedImage addIcon = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class, "add_icon.png");
        ADD_MARKER_ICON = new ImageIcon(addIcon);
        ADD_MARKER_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));
    }

    GroupHeaderPanel(ScreenMarkerGroupsPlugin plugin, String groupName) {
        this.plugin = plugin;
        this.groupName = groupName;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR); // Or a slightly different color for distinction
        setBorder(new EmptyBorder(5, 5, 5, 5)); // Add some padding

        nameLabel = new JLabel(groupName);
        nameLabel.setFont(FontManager.getRunescapeBoldFont());
        nameLabel.setForeground(Color.WHITE); // Standard white for now

        // Setup Add Marker button
        addMarkerButton.setToolTipText("Add new marker to this group");
        addMarkerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Initiate creation process in the plugin, targeting this specific group
                    plugin.startCreation(null, null, groupName);
                    // Immediately update the panel UI to show the creation panel
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

        // Create context menu
        final JPopupMenu contextMenu = new JPopupMenu();

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
                // Panel will be rebuilt automatically by renameGroup if successful
            }
        });

        final JMenuItem deleteItem = new JMenuItem("Delete Group");
        deleteItem.addActionListener(e -> plugin.deleteGroup(groupName)); // Call the plugin method

        final JMenuItem moveUpItem = new JMenuItem("Move Up");
        moveUpItem.addActionListener(e -> plugin.moveGroupUp(groupName)); // Call plugin method

        final JMenuItem moveDownItem = new JMenuItem("Move Down");
        moveDownItem.addActionListener(e -> plugin.moveGroupDown(groupName)); // Call plugin method

        contextMenu.add(renameItem);
        contextMenu.add(deleteItem);
        contextMenu.addSeparator();
        contextMenu.add(moveUpItem);
        contextMenu.add(moveDownItem);

        // Add mouse listener for context menu
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Disable move options for special groups or if only one group exists
                    boolean isSpecialGroup = groupName.equals(ScreenMarkerGroupsPlugin.UNASSIGNED_GROUP)
                            || groupName.equals(ScreenMarkerGroupsPlugin.IMPORTED_GROUP);
                    boolean canMove = plugin.getMarkerGroups().size() > 1 && !isSpecialGroup; // Basic check

                    renameItem.setEnabled(!isSpecialGroup); // Cannot rename special groups
                    deleteItem.setEnabled(!isSpecialGroup);
                    moveUpItem.setEnabled(canMove);
                    moveDownItem.setEnabled(canMove);

                    contextMenu.show(GroupHeaderPanel.this, e.getX(), e.getY());
                }
            }
        });

        JPanel rightActions = new JPanel(new BorderLayout());
        rightActions.setBackground(getBackground());
        rightActions.add(addMarkerButton, BorderLayout.EAST);

        add(nameLabel, BorderLayout.CENTER);
        add(rightActions, BorderLayout.EAST); // Add actions panel to the east
    }

    // Method to update name if renaming is implemented
    // public void updateGroupName(String newName) {
    // nameLabel.setText(newName);
    // }
}
