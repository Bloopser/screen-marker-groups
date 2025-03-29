/*
 * Copyright (c) 2025, Bloopser <https://github.com/Bloopser>
 * Copyright (c) 2018, Kamiel, <https://github.com/Kamielvf>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
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
package screenmarkergroups; // Added package declaration

// Removed ScreenMarkerRenderable import, now in same package
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject; // Import Inject
import lombok.Getter;
import lombok.NonNull;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class ScreenMarkerOverlay extends Overlay {
	@Getter
	private final ScreenMarker marker;
	private final ScreenMarkerRenderable screenMarkerRenderable; // This class is now in the root
	private final ScreenMarkerGroupsPlugin plugin; // Inject plugin

	@Inject
	ScreenMarkerOverlay(@NonNull ScreenMarker marker, ScreenMarkerGroupsPlugin plugin) { // Add plugin to constructor
		this.marker = marker;
		this.plugin = plugin; // Store plugin instance
		this.screenMarkerRenderable = new ScreenMarkerRenderable(); // This class is now in the root
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(PRIORITY_HIGH);
		setMovable(true);
		setResizable(true);
		setMinimumSize(16);
		setResettable(false);
	}

	@Override
	public String getName() {
		return "marker" + marker.getId();
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		// Find the group this marker belongs to
		String groupName = plugin.findGroupForMarker(this);

		// Render only if marker is visible AND its group is visible (or group not
		// found, which shouldn't happen)
		if (!marker.isVisible() || (groupName != null && !plugin.isGroupVisible(groupName))) {
			return null;
		}

		Dimension preferredSize = getPreferredSize();
		if (preferredSize == null) {
			// overlay has no preferred size in the renderer configuration!
			return null;
		}

		screenMarkerRenderable.setBorderThickness(marker.getBorderThickness());
		screenMarkerRenderable.setColor(marker.getColor());
		screenMarkerRenderable.setFill(marker.getFill());
		screenMarkerRenderable.setStroke(new BasicStroke(marker.getBorderThickness()));
		screenMarkerRenderable.setSize(preferredSize);
		screenMarkerRenderable.setLabel(marker.isLabelled() ? marker.getName() : "");
		return screenMarkerRenderable.render(graphics);
	}
}
