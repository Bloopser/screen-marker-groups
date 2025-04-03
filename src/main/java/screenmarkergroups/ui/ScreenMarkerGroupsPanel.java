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
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

/**
 * Represents a panel within the main plugin sidebar, displaying controls
 * for a single ScreenMarkerOverlay. Allows editing name, colors, thickness,
 * visibility, label display, and deletion.
 */
class ScreenMarkerGroupsPanel extends JPanel {
	private static final int DEFAULT_FILL_OPACITY = 75;

	private static final Border NAME_BOTTOM_BORDER = new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

	private static final ImageIcon BORDER_COLOR_ICON;
	private static final ImageIcon BORDER_COLOR_HOVER_ICON;
	private static final ImageIcon NO_BORDER_COLOR_ICON;
	private static final ImageIcon NO_BORDER_COLOR_HOVER_ICON;

	private static final ImageIcon FILL_COLOR_ICON;
	private static final ImageIcon FILL_COLOR_HOVER_ICON;
	private static final ImageIcon NO_FILL_COLOR_ICON;
	private static final ImageIcon NO_FILL_COLOR_HOVER_ICON;

	private static final ImageIcon LABEL_ICON;
	private static final ImageIcon LABEL_HOVER_ICON;
	private static final ImageIcon NO_LABEL_ICON;
	private static final ImageIcon NO_LABEL_HOVER_ICON;

	private static final ImageIcon VISIBLE_ICON;
	private static final ImageIcon VISIBLE_HOVER_ICON;
	private static final ImageIcon INVISIBLE_ICON;
	private static final ImageIcon INVISIBLE_HOVER_ICON;

	private static final ImageIcon CONFIGURE_ICON;
	private static final ImageIcon CONFIGURE_HOVER_ICON;

	private static final ImageIcon DELETE_ICON;
	private static final ImageIcon DELETE_HOVER_ICON;

	private final ScreenMarkerGroupsPlugin plugin;
	private final ScreenMarkerOverlay marker;
	private final JPopupMenu contextMenu;

	private final JLabel borderColorIndicator = new JLabel();
	private final JLabel fillColorIndicator = new JLabel();
	private final JLabel labelIndicator = new JLabel();
	private final JLabel configureLabel = new JLabel();
	private final JLabel visibilityLabel = new JLabel();
	private final JLabel deleteLabel = new JLabel();

	private final FlatTextField nameInput = new FlatTextField();
	private final JLabel save = new JLabel("Save");
	private final JLabel cancel = new JLabel("Cancel");
	private final JLabel rename = new JLabel("Rename");

	private final SpinnerModel spinnerModel = new SpinnerNumberModel(5, 0, Integer.MAX_VALUE, 1);
	private final JSpinner thicknessSpinner = new JSpinner(spinnerModel);

	private boolean visible;
	private boolean showLabel;

	static {
		final BufferedImage borderImg = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class,
				"border_color_icon.png");
		final BufferedImage borderImgHover = ImageUtil.luminanceOffset(borderImg, -150);
		BORDER_COLOR_ICON = new ImageIcon(borderImg);
		BORDER_COLOR_HOVER_ICON = new ImageIcon(borderImgHover);

		NO_BORDER_COLOR_ICON = new ImageIcon(borderImgHover);
		NO_BORDER_COLOR_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(borderImgHover, -100));

		final BufferedImage fillImg = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class,
				"fill_color_icon.png");
		final BufferedImage fillImgHover = ImageUtil.luminanceOffset(fillImg, -150);
		FILL_COLOR_ICON = new ImageIcon(fillImg);
		FILL_COLOR_HOVER_ICON = new ImageIcon(fillImgHover);

		NO_FILL_COLOR_ICON = new ImageIcon(fillImgHover);
		NO_FILL_COLOR_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(fillImgHover, -100));

		final BufferedImage labelImg = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class, "label_icon.png");
		final BufferedImage labelImgHover = ImageUtil.luminanceOffset(labelImg, -150);
		LABEL_ICON = new ImageIcon(labelImg);
		LABEL_HOVER_ICON = new ImageIcon(labelImgHover);

		NO_LABEL_ICON = new ImageIcon(labelImgHover);
		NO_LABEL_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(labelImgHover, -100));

		final BufferedImage visibleImg = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class,
				"visible_icon.png");
		VISIBLE_ICON = new ImageIcon(visibleImg);
		VISIBLE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(visibleImg, -100));

		final BufferedImage invisibleImg = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class,
				"invisible_icon.png");
		INVISIBLE_ICON = new ImageIcon(invisibleImg);
		INVISIBLE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(invisibleImg, -100));

		final BufferedImage configureImg = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class, "configure.png");
		CONFIGURE_ICON = new ImageIcon(configureImg);
		CONFIGURE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(configureImg, -100));

		final BufferedImage deleteImg = ImageUtil.loadImageResource(ScreenMarkerGroupsPlugin.class, "delete_icon.png");
		DELETE_ICON = new ImageIcon(deleteImg);
		DELETE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(deleteImg, -100));
	}

	/**
	 * Constructs the panel for a specific screen marker.
	 *
	 * @param plugin The main plugin instance.
	 * @param marker The screen marker overlay this panel represents.
	 */
	ScreenMarkerGroupsPanel(ScreenMarkerGroupsPlugin plugin, ScreenMarkerOverlay marker) {
		this.plugin = plugin;
		this.marker = marker;
		this.visible = marker.getMarker().isVisible();
		this.showLabel = marker.getMarker().isLabelled();

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel nameWrapper = new JPanel(new BorderLayout());
		nameWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameWrapper.setBorder(NAME_BOTTOM_BORDER);

		JPanel nameActions = new JPanel(new BorderLayout(3, 0));
		nameActions.setBorder(new EmptyBorder(0, 0, 0, 8));
		nameActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		save.setVisible(false);
		save.setFont(FontManager.getRunescapeSmallFont());
		save.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		save.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				save();
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				save.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR.darker());
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				save.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
			}
		});

		cancel.setVisible(false);
		cancel.setFont(FontManager.getRunescapeSmallFont());
		cancel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		cancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				cancel();
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				cancel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR.darker());
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				cancel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
			}
		});

		rename.setFont(FontManager.getRunescapeSmallFont());
		rename.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
		rename.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				nameInput.setEditable(true);
				updateNameActions(true);
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				rename.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker().darker());
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				rename.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
			}
		});

		nameActions.add(save, BorderLayout.EAST);
		nameActions.add(cancel, BorderLayout.WEST);
		nameActions.add(rename, BorderLayout.CENTER);

		nameInput.setText(marker.getMarker().getName());
		nameInput.setBorder(null);
		nameInput.setEditable(false);
		nameInput.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameInput.setPreferredSize(new Dimension(0, 24));
		nameInput.getTextField().setForeground(Color.WHITE);
		nameInput.getTextField().setBorder(new EmptyBorder(0, 8, 0, 0));
		nameInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					save();
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					cancel();
				}
			}
		});
		nameInput.getTextField().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				preview(true);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				preview(false);
			}
		});

		nameWrapper.add(nameInput, BorderLayout.CENTER);
		nameWrapper.add(nameActions, BorderLayout.EAST);

		JPanel bottomContainer = new JPanel(new BorderLayout());
		bottomContainer.setBorder(new EmptyBorder(2, 0, 0, 0));
		bottomContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
		leftActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		borderColorIndicator.setToolTipText("Edit border color");
		borderColorIndicator.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				openBorderColorPicker();
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				borderColorIndicator.setIcon(marker.getMarker().getBorderThickness() == 0 ? NO_BORDER_COLOR_HOVER_ICON
						: BORDER_COLOR_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				borderColorIndicator.setIcon(
						marker.getMarker().getBorderThickness() == 0 ? NO_BORDER_COLOR_ICON : BORDER_COLOR_ICON);
			}
		});

		fillColorIndicator.setToolTipText("Edit fill color");
		fillColorIndicator.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				openFillColorPicker();
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				fillColorIndicator.setIcon(marker.getMarker().getFill().getAlpha() == 0 ? NO_FILL_COLOR_HOVER_ICON
						: FILL_COLOR_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				fillColorIndicator
						.setIcon(marker.getMarker().getFill().getAlpha() == 0 ? NO_FILL_COLOR_ICON : FILL_COLOR_ICON);
			}
		});

		thicknessSpinner.setValue(marker.getMarker().getBorderThickness());
		thicknessSpinner.setPreferredSize(new Dimension(50, 20));
		thicknessSpinner.addChangeListener(ce -> updateThickness(true));
		thicknessSpinner.setToolTipText("Border thickness");

		labelIndicator.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				toggleLabelling(!showLabel);
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				labelIndicator.setIcon(showLabel ? LABEL_HOVER_ICON : NO_LABEL_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				labelIndicator.setIcon(showLabel ? LABEL_ICON : NO_LABEL_ICON);
			}
		});

		leftActions.add(borderColorIndicator);
		leftActions.add(fillColorIndicator);
		leftActions.add(labelIndicator);
		leftActions.add(thicknessSpinner);

		JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 8));
		rightActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		this.contextMenu = setupContextMenu();

		configureLabel.setIcon(CONFIGURE_ICON);
		configureLabel.setToolTipText("Configure marker");
		configureLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				if (configureLabel.isEnabled() && SwingUtilities.isLeftMouseButton(mouseEvent)) {
					contextMenu.show(configureLabel, mouseEvent.getX(), mouseEvent.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				configureLabel.setIcon(CONFIGURE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				configureLabel.setIcon(CONFIGURE_ICON);
			}
		});

		visibilityLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				if (visibilityLabel.isEnabled()) {
					toggle(!visible);
				}
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				visibilityLabel.setIcon(visible ? VISIBLE_HOVER_ICON : INVISIBLE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				updateVisibility();
			}
		});

		deleteLabel.setIcon(DELETE_ICON);
		deleteLabel.setToolTipText("Delete screen marker");
		deleteLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				if (deleteLabel.isEnabled()) {
					int confirm = JOptionPane.showConfirmDialog(ScreenMarkerGroupsPanel.this,
							"Are you sure you want to permanently delete this screen marker?",
							"Warning", JOptionPane.OK_CANCEL_OPTION);

					if (confirm == 0) {
						plugin.deleteMarker(marker);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				deleteLabel.setIcon(DELETE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				deleteLabel.setIcon(DELETE_ICON);
			}
		});

		rightActions.add(configureLabel);
		rightActions.add(visibilityLabel);
		rightActions.add(deleteLabel);

		bottomContainer.add(leftActions, BorderLayout.WEST);
		bottomContainer.add(rightActions, BorderLayout.EAST);

		add(nameWrapper, BorderLayout.NORTH);
		add(bottomContainer, BorderLayout.CENTER);

		updateVisibility();
		updateFill();
		updateBorder();
		updateLabelling();
	}

	/**
	 * Temporarily shows the marker overlay if it's currently hidden.
	 * Used for previewing during hover events.
	 *
	 * @param on True to show, false to revert to original visibility.
	 */
	private void preview(boolean on) {
		if (visible) {
			return;
		}
		marker.getMarker().setVisible(on);
	}

	/**
	 * Toggles the persistent visibility state of the marker.
	 * Updates the marker data, saves configuration, and updates the UI icon.
	 *
	 * @param on True to make visible, false to hide.
	 */
	private void toggle(boolean on) {
		visible = on;
		marker.getMarker().setVisible(visible);
		plugin.updateGroupsConfig();
		updateVisibility();
	}

	/**
	 * Toggles the label display state for the marker.
	 * Updates the marker data, saves configuration, and updates the UI icon.
	 *
	 * @param on True to show the label, false to hide.
	 */
	private void toggleLabelling(boolean on) {
		showLabel = on;
		marker.getMarker().setLabelled(showLabel);
		plugin.updateGroupsConfig();
		updateLabelling();
	}

	/**
	 * Saves the currently entered name in the name input field to the marker data,
	 * updates configuration, and reverts the name field to a non-editable state.
	 */
	private void save() {
		marker.getMarker().setName(nameInput.getText());
		plugin.updateGroupsConfig();
		nameInput.setEditable(false);
		updateNameActions(false);
		requestFocusInWindow();
	}

	/**
	 * Cancels the renaming process, reverting the name input field to the marker's
	 * current name and making it non-editable.
	 */
	private void cancel() {
		nameInput.setEditable(false);
		nameInput.setText(marker.getMarker().getName());
		updateNameActions(false);
		requestFocusInWindow();
	}

	/**
	 * Updates the visibility of the save/cancel/rename actions based on whether
	 * the name field is currently being edited.
	 *
	 * @param saveAndCancel True if the name is being edited (show save/cancel),
	 *                      false otherwise (show rename).
	 */
	private void updateNameActions(boolean saveAndCancel) {
		save.setVisible(saveAndCancel);
		cancel.setVisible(saveAndCancel);
		rename.setVisible(!saveAndCancel);
		if (saveAndCancel) {
			nameInput.getTextField().requestFocusInWindow();
			nameInput.getTextField().selectAll();
		}
	}

	/**
	 * Updates the marker's border thickness based on the spinner value.
	 *
	 * @param save True to save the configuration change immediately, false
	 *             otherwise.
	 */
	private void updateThickness(boolean save) {
		marker.getMarker().setBorderThickness((Integer) thicknessSpinner.getValue());
		updateBorder();
		if (save) {
			plugin.updateGroupsConfig();
		}
	}

	/**
	 * Updates the visibility indicator icon and tooltip based on the current
	 * visibility state.
	 */
	private void updateVisibility() {
		visibilityLabel.setIcon(visible ? VISIBLE_ICON : INVISIBLE_ICON);
		visibilityLabel.setToolTipText(visible ? "Hide screen marker" : "Show screen marker");
	}

	/**
	 * Updates the label indicator icon and tooltip based on the current label
	 * display state.
	 */
	private void updateLabelling() {
		labelIndicator.setIcon(showLabel ? LABEL_ICON : NO_LABEL_ICON);
		labelIndicator.setToolTipText(showLabel ? "Hide label" : "Show label");
	}

	/**
	 * Updates the fill color indicator icon and border based on the marker's fill
	 * color and alpha.
	 */
	private void updateFill() {
		final boolean isFullyTransparent = marker.getMarker().getFill().getAlpha() == 0;
		if (isFullyTransparent) {
			fillColorIndicator.setBorder(null);
		} else {
			Color color = marker.getMarker().getFill();
			Color fullColor = new Color(color.getRed(), color.getGreen(), color.getBlue());
			fillColorIndicator.setBorder(new MatteBorder(0, 0, 3, 0, fullColor));
		}
		fillColorIndicator.setIcon(isFullyTransparent ? NO_FILL_COLOR_ICON : FILL_COLOR_ICON);
	}

	/**
	 * Updates the border color indicator icon and border based on the marker's
	 * border color and thickness.
	 */
	private void updateBorder() {
		if (marker.getMarker().getBorderThickness() == 0) {
			borderColorIndicator.setBorder(null);
		} else {
			Color color = marker.getMarker().getColor();
			borderColorIndicator.setBorder(new MatteBorder(0, 0, 3, 0, color));
		}
		borderColorIndicator
				.setIcon(marker.getMarker().getBorderThickness() == 0 ? NO_BORDER_COLOR_ICON : BORDER_COLOR_ICON);
	}

	/**
	 * Opens the color picker for editing the marker's fill color.
	 * Updates the marker data and UI indicator upon color change.
	 * Saves configuration when the color picker is closed.
	 */
	private void openFillColorPicker() {
		final Color fillColor = marker.getMarker().getFill();
		RuneliteColorPicker colorPicker = plugin.getColorPickerManager().create(
				this,
				fillColor.getAlpha() == 0 ? ColorUtil.colorWithAlpha(fillColor, DEFAULT_FILL_OPACITY) : fillColor,
				marker.getMarker().getName() + " Fill",
				false);
		colorPicker.setLocationRelativeTo(this);
		colorPicker.setOnColorChange(c -> {
			marker.getMarker().setFill(c);
			updateFill();
		});
		colorPicker.setOnClose(c -> plugin.updateGroupsConfig());
		colorPicker.setVisible(true);
	}

	/**
	 * Opens the color picker for editing the marker's border color.
	 * Updates the marker data and UI indicator upon color change.
	 * Saves configuration when the color picker is closed.
	 */
	private void openBorderColorPicker() {
		RuneliteColorPicker colorPicker = plugin.getColorPickerManager().create(
				this,
				marker.getMarker().getColor(),
				marker.getMarker().getName() + " Border",
				false);
		colorPicker.setLocationRelativeTo(this);
		colorPicker.setOnColorChange(c -> {
			marker.getMarker().setColor(c);
			updateBorder();
		});
		colorPicker.setOnClose(c -> plugin.updateGroupsConfig());
		colorPicker.setVisible(true);
	}

	/**
	 * Sets up the context menu for the marker panel. Includes options for moving
	 * the marker within or between groups.
	 *
	 * @return The configured JPopupMenu.
	 */
	private JPopupMenu setupContextMenu() {
		final JPopupMenu popupMenu = new JPopupMenu();

		final JMenuItem moveUpItem = new JMenuItem("Move Up");
		moveUpItem.addActionListener(e -> plugin.moveMarkerUp(marker));
		popupMenu.add(moveUpItem);

		final JMenuItem moveDownItem = new JMenuItem("Move Down");
		moveDownItem.addActionListener(e -> plugin.moveMarkerDown(marker));
		popupMenu.add(moveDownItem);

		final JMenu moveToGroupMenu = new JMenu("Move to Group");
		popupMenu.add(moveToGroupMenu);

		// Populate "Move to Group" submenu dynamically when shown
		popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
				moveToGroupMenu.removeAll();
				String currentGroupName = plugin.findGroupForMarker(marker);

				List<String> otherGroupNames = plugin.getGroupOrderList().stream()
						.filter(name -> !name.equals(currentGroupName))
						.collect(Collectors.toList());

				if (otherGroupNames.isEmpty()) {
					JMenuItem noOtherGroups = new JMenuItem("No other groups");
					noOtherGroups.setEnabled(false);
					moveToGroupMenu.add(noOtherGroups);
				} else {
					for (String groupName : otherGroupNames) {
						JMenuItem groupItem = new JMenuItem(groupName);
						groupItem.addActionListener(ae -> plugin.moveMarkerToGroup(marker, groupName));
						moveToGroupMenu.add(groupItem);
					}
				}
				moveToGroupMenu.setEnabled(!otherGroupNames.isEmpty());
			}

			@Override
			public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
			}

			@Override
			public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
			}
		});

		return popupMenu;
	}

	/**
	 * Enables or disables the configuration controls for this marker panel.
	 * Used when entering/exiting marker creation mode for a group.
	 *
	 * @param enabled True to enable controls, false to disable.
	 */
	void setControlsEnabled(boolean enabled) {
		configureLabel.setEnabled(enabled);
		configureLabel.setToolTipText(enabled ? "Configure marker" : null);

		borderColorIndicator.setEnabled(enabled);
		fillColorIndicator.setEnabled(enabled);
		labelIndicator.setEnabled(enabled);
		thicknessSpinner.setEnabled(enabled);
		visibilityLabel.setEnabled(enabled);
		deleteLabel.setEnabled(enabled);
		rename.setEnabled(enabled);
	}
}
