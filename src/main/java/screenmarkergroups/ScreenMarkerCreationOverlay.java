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

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Overlay responsible for rendering the visual representation of
 * a screen marker during its creation process (when the user is dragging).
 * It displays a striped rectangle matching the marker's intended bounds and
 * style.
 */
class ScreenMarkerCreationOverlay extends Overlay {
	private final ScreenMarkerGroupsPlugin plugin;

	/**
	 * Injects dependencies and sets up the overlay properties.
	 *
	 * @param plugin The main plugin instance, used to access the currently drawn
	 *               marker.
	 */
	@Inject
	private ScreenMarkerCreationOverlay(final ScreenMarkerGroupsPlugin plugin) {
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
		setMovable(true);
	}

	/**
	 * Renders the creation overlay.
	 * Draws a striped rectangle based on the current marker being created.
	 *
	 * @param graphics Graphics2D context for drawing.
	 * @return The dimensions of the rendered overlay, or null if no marker is being
	 *         created.
	 */
	@Override
	public Dimension render(Graphics2D graphics) {
		ScreenMarker marker = plugin.getCurrentMarker();

		if (marker == null) {
			return null;
		}

		int thickness = marker.getBorderThickness();
		int offset = thickness / 2;
		int width = getBounds().width - thickness;
		int height = getBounds().height - thickness;

		graphics.setStroke(createStripedStroke(thickness));
		graphics.setColor(marker.getColor());
		graphics.drawRect(offset, offset, width, height);

		return getBounds().getSize();
	}

	/**
	 * Creates a dashed stroke style used for rendering the marker creation outline.
	 *
	 * @param thickness The desired thickness of the stroke.
	 * @return A Stroke object configured with a dashed pattern.
	 */
	private Stroke createStripedStroke(int thickness) {
		return new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0);
	}
}
