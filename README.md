# Screen Marker Groups

Screen Marker Groups allows you to draw boxes and shapes on top of your RuneLite client and organize them into different groups. This plugin enhances the original RuneLite 
[Screen Markers plugin](https://github.com/runelite/runelite/wiki/Screen-Markers) by adding organizational capabilities.

This is useful for setting up markers for specific tasks, bosses, or activities, and easily switching between different sets of markers without cluttering your screen.

![Screen marker groups](<readme/Screen Marker Groups.png>)

## Features

### Group Management

*   **Create Groups:** Click the **(+)** button in the top-right corner of the plugin panel to add a new group. You'll be prompted to enter a name for your group.

![New group creation prompt](<readme/new group.png>)
*   **Configure Group (Rename, Delete, Reorder):** Click the **gear icon** (![configure](configure.png)) on the group's header to open a menu with options to "Rename Group", "Delete Group", "Move Up", or "Move Down".
    *   *Rename:* Changes the group's name (cannot rename "Unassigned" or "Imported").
    *   *Delete:* Removes the group. You'll be asked if you want to delete its markers or move them to "Unassigned". ("Unassigned" cannot be deleted).

![Delete group confirmation prompt](<readme/delete group.png>)

    *   *Move Up/Down:* Changes the group's position in the list (special groups stay at the bottom).

![Group configuration context menu](<readme/Group config.png>)
*   **Collapse/Expand Group:** Click the **arrow icon** (![expanded](expanded.png)/![collapsed](collapsed.png)) on the group's header to collapse or expand the list of markers within that group, saving panel space.
  
  ![Collapsed groups view in panel](<readme/collapsed groups.png>)

### Marker Management

*   **Create Markers:** Click the **(+)** button on a specific group's header to start creating a marker within that group. The "Drag in-game to draw" panel will appear under that group's header. Click and drag on the game screen or a UI element to draw your marker.

![Creating a new marker within a group](<readme/new marker in group.png>)

*   **Configure Marker (Reorder, Move Group):** Click the **gear icon** (![configure](configure.png)) on a marker's panel entry to open a menu with options to "Move Up", "Move Down", or "Move to Group".
    *   *Move Up/Down:* Changes the marker's position *within its current group*.
    *   *Move to Group:* Select a different group to move the marker to.
  
![Marker configuration context menu](<readme/marker context menu.png>)

### Importing

*   **Import from Original Plugin:** Go to the plugin's settings menu (wrench icon next to the plugin name in the main RuneLite settings). Click the checkbox next to **"Import Screen markers"** to copy all markers from the original "Screen Markers" plugin into a new group called "Imported". This is useful for migrating your existing markers.
    *   *Note:* If imported markers don't appear immediately, try toggling the "Imported" group's visibility off and on in the panel.


#### Inherited Marker Settings:

 [Screen Markers plugin](https://github.com/runelite/runelite/wiki/Screen-Markers)

*   **Marker Settings:** Each marker has individual settings accessible via icons on its panel entry:
    *   **Border Color:** Change the marker's border color.
    *   **Fill Color:** Change the marker's fill color (including transparency).
    *   **Border Thickness:** Adjust the border thickness using the spinner.
    *   **Label:** Toggle a text label on/off for the marker.
    *   **Visibility:** Show or hide the individual marker.
    *   **Rename:** Click the "Rename" text to edit the marker's name.
    *   **Delete:** Permanently delete the marker using the **trash icon**.
*   **Moving Markers On-Screen:** Hold `Alt` and click and drag a marker on the game screen to reposition it.
