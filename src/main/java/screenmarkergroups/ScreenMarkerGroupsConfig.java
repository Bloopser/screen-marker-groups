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
package screenmarkergroups;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * Configuration interface for the Screen Marker Groups plugin.
 */
@ConfigGroup("screenmarkergroups") // Matches the CONFIG_GROUP constant in the plugin
public interface ScreenMarkerGroupsConfig extends Config {
    /**
     * Stores the screen marker groups and their associated markers as a JSON
     * string.
     * The structure is expected to be a Map<String, List<ScreenMarker>>,
     * where the key is the group name and the value is the list of markers in that
     * group.
     *
     * @return JSON string representing the marker groups.
     */
    @ConfigItem(keyName = "markerGroups", // Matches the CONFIG_KEY in the plugin
            name = "Marker Groups Data", description = "Stores the configuration for screen marker groups and markers (internal).", hidden = true // Hide
                                                                                                                                                  // from
                                                                                                                                                  // the
                                                                                                                                                  // regular
                                                                                                                                                  // settings
                                                                                                                                                  // UI
    )
    default String markerGroups() {
        return "{}"; // Default to an empty JSON object
    }

    /**
     * Stores the display order of the groups as a JSON list of strings.
     *
     * @return JSON string representing the ordered list of group names.
     */
    @ConfigItem(keyName = "groupOrder", name = "Group Order Data", description = "Stores the display order for screen marker groups (internal).", hidden = true)
    default String groupOrder() {
        return "[]"; // Default to an empty JSON array
    }

    /**
     * A config item that acts like a button to trigger the import process.
     * It's a boolean checkbox, but added tooltip to suggest clicking.
     * The plugin listens for changes and resets it immediately.
     *
     * @return false, always.
     */
    @ConfigItem(position = 1, keyName = "importTrigger", name = "Import screenmarkers", description = "Click this to import markers from the original Screen Markers plugin into the 'Imported' group.")
    default boolean importTrigger() {
        return false; // Default value, plugin resets it after triggering
    }
}
