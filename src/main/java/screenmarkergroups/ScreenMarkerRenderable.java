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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.RenderableEntity;

@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
/**
 * Represents the renderable entity for a screen marker.
 * This class handles the actual drawing of the marker's border, fill, and label
 * based on the provided properties.
 */
class ScreenMarkerRenderable implements RenderableEntity {
	private Dimension size;
	private int borderThickness;
	private Color color;
	private Color fill;
	private Stroke stroke;
	private String label;

	/**
	 * Renders the screen marker onto the provided graphics context.
	 * Draws the fill, border, and label according to the set properties.
	 *
	 * @param graphics The graphics context to draw on.
	 * @return The dimensions of the rendered marker.
	 */
	@Override
	public Dimension render(Graphics2D graphics) {
		int thickness = borderThickness;
		int width = size.width;
		int height = size.height;

		graphics.setColor(fill);
		graphics.fillRect(thickness, thickness, width - thickness * 2, height - thickness * 2);

		int offset = thickness / 2;
		graphics.setColor(color);
		graphics.setStroke(stroke);
		graphics.drawRect(offset, offset, width - thickness, height - thickness);

		if (!label.isEmpty()) {
			graphics.drawString(label, offset + thickness, offset + thickness + graphics.getFontMetrics().getAscent());
		}

		return size;
	}
}
