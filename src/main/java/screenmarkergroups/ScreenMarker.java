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
package screenmarkergroups;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Represents a screen marker object with its properties.
 * This class is used to store and manage individual screen markers,
 * including their visual attributes and state.
 */
public class ScreenMarker {
	/**
	 * Unique identifier for the screen marker.
	 */
	private long id;

	/**
	 * User-defined name for the screen marker.
	 */
	private String name;

	/**
	 * Thickness of the border around the marker, in pixels.
	 */
	private int borderThickness;

	/**
	 * Color of the border around the marker.
	 */
	private Color color;

	/**
	 * Fill color of the marker. Can be null or transparent for no fill.
	 */
	private Color fill;

	/**
	 * Current visibility state of the marker. If false, the marker is not rendered.
	 */
	private boolean visible;

	/**
	 * Whether the marker's name label should be displayed.
	 */
	private boolean labelled;

	/**
	 * The ID of the marker in the original RuneLite Screen Marker plugin,
	 * if this marker was imported. Used for potential future migration or
	 * reference.
	 * Can be null if the marker was created directly in this plugin.
	 */
	private Long importedId;
}
