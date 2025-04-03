/*
 * Copyright (c) 2025, Bloopser <https://github.com/Bloopser>
 * Copyright (c) 2018, Jasper <Jasper0781@gmail.com>
 * Copyright (c) 2020, melky <https://github.com/melkypie>
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
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Overlay responsible for highlighting widgets when the user is in
 * screen marker creation mode and hovers over a widget menu entry.
 * This helps the user create markers aligned with specific UI elements.
 */
class ScreenMarkerWidgetHighlightOverlay extends Overlay {
	private final ScreenMarkerGroupsPlugin plugin;
	private final Client client;

	/**
	 * Injects dependencies and sets up the overlay properties.
	 *
	 * @param plugin The main plugin instance.
	 * @param client The RuneLite client instance.
	 */
	@Inject
	private ScreenMarkerWidgetHighlightOverlay(final ScreenMarkerGroupsPlugin plugin, final Client client) {
		this.plugin = plugin;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
		setMovable(true);
	}

	/**
	 * Renders the widget highlight overlay.
	 * Only draws if the user is in creation mode but not actively drawing a marker.
	 * Highlights the widget bounds corresponding to the last menu entry hovered.
	 *
	 * @param graphics Graphics2D context for drawing.
	 * @return null, as this overlay doesn't have specific dimensions.
	 */
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!plugin.isCreatingScreenMarker() || plugin.isDrawingScreenMarker()) {
			return null;
		}

		final MenuEntry[] menuEntries = client.getMenuEntries();
		if (client.isMenuOpen() || menuEntries.length == 0) {
			plugin.setSelectedWidgetBounds(null);
			return null;
		}

		final MenuEntry menuEntry = menuEntries[menuEntries.length - 1];
		final int childIdx = menuEntry.getParam0();
		final int widgetId = menuEntry.getParam1();

		final Widget widget = client.getWidget(widgetId);
		if (widget == null) {
			plugin.setSelectedWidgetBounds(null);
			return null;
		}

		Rectangle bounds = null;
		if (childIdx > -1) {
			final Widget child = widget.getChild(childIdx);
			if (child != null) {
				bounds = child.getBounds();
			}
		} else {
			bounds = widget.getBounds();
		}

		if (bounds == null) {
			plugin.setSelectedWidgetBounds(null);
			return null;
		}

		drawHighlight(graphics, bounds);
		plugin.setSelectedWidgetBounds(bounds);

		return null;
	}

	/**
	 * Draws a green highlight rectangle around the given bounds.
	 *
	 * @param graphics The graphics context.
	 * @param bounds   The rectangle bounds to highlight.
	 */
	private static void drawHighlight(Graphics2D graphics, Rectangle bounds) {
		graphics.setColor(Color.GREEN);
		graphics.draw(bounds);
	}
}
