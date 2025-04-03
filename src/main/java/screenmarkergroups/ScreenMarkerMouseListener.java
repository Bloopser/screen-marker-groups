/*
 * Copyright (c) 2025, Bloopser <https://github.com/Bloopser>
 * Copyright (c) 2018, Kamiel, <https://github.com/Kamielvf>
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

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import net.runelite.client.input.MouseAdapter;

/**
 * Mouse listener responsible for handling user interactions during the
 * screen marker creation process. It listens for clicks, presses, releases,
 * and drags to define the marker's bounds or cancel the creation.
 */
class ScreenMarkerMouseListener extends MouseAdapter {
	private final ScreenMarkerGroupsPlugin plugin;

	/**
	 * Constructs the mouse listener.
	 *
	 * @param plugin The main plugin instance.
	 */
	ScreenMarkerMouseListener(ScreenMarkerGroupsPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Consumes click events during marker creation to prevent unintended
	 * interactions.
	 *
	 * @param event The mouse event.
	 * @return The potentially consumed mouse event.
	 */
	@Override
	public MouseEvent mouseClicked(MouseEvent event) {
		if (SwingUtilities.isMiddleMouseButton(event)) {
			return event;
		}
		if (plugin.isCreatingScreenMarker()) {
			event.consume();
		}
		return event;
	}

	/**
	 * Handles the initial mouse press for marker creation.
	 * If left-clicking on a highlighted widget, creates a marker matching the
	 * widget bounds.
	 * If left-clicking elsewhere, starts the drag-to-create process.
	 * If right-clicking during creation, cancels the process.
	 *
	 * @param event The mouse event.
	 * @return The potentially consumed mouse event.
	 */
	@Override
	public MouseEvent mousePressed(MouseEvent event) {
		if (SwingUtilities.isMiddleMouseButton(event)) {
			return event;
		}

		if (SwingUtilities.isLeftMouseButton(event)) {
			if (plugin.isCreatingScreenMarker()) {
				final Rectangle bounds = plugin.getSelectedWidgetBounds();

				if (bounds != null) {
					plugin.startCreation(bounds.getLocation(), bounds.getSize());
					plugin.completeSelection();
				} else if (plugin.getStartLocation() == null) {
					plugin.startCreation(event.getPoint(), ScreenMarkerGroupsPlugin.DEFAULT_SIZE);
				}

				event.consume();
				return event;
			}
		} else if (plugin.isCreatingScreenMarker()) {
			plugin.finishCreation(true);
			event.consume();
		}
		return event;
	}

	/**
	 * Handles the mouse release event during marker creation.
	 * If the left button is released while drawing, it completes the marker
	 * selection.
	 *
	 * @param event The mouse event.
	 * @return The potentially consumed mouse event.
	 */
	@Override
	public MouseEvent mouseReleased(MouseEvent event) {
		if (SwingUtilities.isMiddleMouseButton(event)) {
			return event;
		}

		if (SwingUtilities.isLeftMouseButton(event) && plugin.isCreatingScreenMarker()
				&& plugin.isDrawingScreenMarker()) {
			plugin.setDrawingScreenMarker(false);
			plugin.completeSelection();
			event.consume();
		}
		return event;
	}

	/**
	 * Handles mouse drag events during marker creation.
	 * If dragging with the left button, it initializes creation if needed and
	 * resizes the marker based on the current mouse position.
	 *
	 * @param event The mouse event.
	 * @return The potentially consumed mouse event.
	 */
	@Override
	public MouseEvent mouseDragged(MouseEvent event) {
		if (!plugin.isCreatingScreenMarker() || SwingUtilities.isMiddleMouseButton(event)) {
			return event;
		}

		if (SwingUtilities.isLeftMouseButton(event)) {
			if (plugin.getStartLocation() == null) {
				plugin.startCreation(event.getPoint(), ScreenMarkerGroupsPlugin.DEFAULT_SIZE);
			}
			plugin.resizeMarker(event.getPoint());
			event.consume();
		}
		return event;
	}
}
