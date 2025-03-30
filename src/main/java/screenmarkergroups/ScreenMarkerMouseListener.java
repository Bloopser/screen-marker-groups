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

class ScreenMarkerMouseListener extends MouseAdapter {
	private final ScreenMarkerGroupsPlugin plugin;

	ScreenMarkerMouseListener(ScreenMarkerGroupsPlugin plugin) {
		this.plugin = plugin;
	}

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

	@Override
	public MouseEvent mousePressed(MouseEvent event) {
		if (SwingUtilities.isMiddleMouseButton(event)) {
			return event;
		}

		if (SwingUtilities.isLeftMouseButton(event)) {
			if (plugin.isCreatingScreenMarker()) {
				final Rectangle bounds = plugin.getSelectedWidgetBounds();

				// Check if clicking a highlighted widget while in creation mode
				if (bounds != null) {
					// Prepare the marker using widget bounds
					plugin.startCreation(bounds.getLocation(), bounds.getSize());
					// Immediately complete selection since no dragging is needed
					plugin.completeSelection();
				}
				// If not clicking a widget, but creation mode is active,
				// this is the start of a drag operation.
				else if (plugin.getStartLocation() == null) {
					// Prepare the marker using the click point
					plugin.startCreation(event.getPoint(), ScreenMarkerGroupsPlugin.DEFAULT_SIZE);
					// Don't call completeSelection yet, wait for mouseRelease
				}

				// Consume the event to prevent other interactions
				event.consume();
				return event;
			}
			// If not in creation mode, let the event pass through (no widget click handling
			// here)
		} else if (plugin.isCreatingScreenMarker()) {
			// Right-click cancels creation if already in progress
			plugin.finishCreation(true); // Abort creation
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event) {
		if (SwingUtilities.isMiddleMouseButton(event)) {
			return event;
		}

		if (SwingUtilities.isLeftMouseButton(event) && plugin.isCreatingScreenMarker()
				&& plugin.isDrawingScreenMarker()) {
			// Complete the drawing process
			plugin.setDrawingScreenMarker(false); // No longer drawing
			plugin.completeSelection(); // Unlock the confirm button in the UI
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event) {
		if (!plugin.isCreatingScreenMarker() || SwingUtilities.isMiddleMouseButton(event)) {
			return event;
		}

		if (SwingUtilities.isLeftMouseButton(event)) {
			// Initialize marker creation on first drag if not already done by mousePressed
			if (plugin.getStartLocation() == null) {
				// Call the renamed method
				plugin.startCreation(event.getPoint(), ScreenMarkerGroupsPlugin.DEFAULT_SIZE);
			}
			plugin.resizeMarker(event.getPoint());
			event.consume();
		}
		return event;
	}
}
