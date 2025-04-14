package screenmarkergroups;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum defining the possible positions for a screen marker's label.
 */
@Getter
@RequiredArgsConstructor
public enum LabelPosition {
    ABOVE("Above"), // Label is drawn above the marker border
    INSIDE("Inside"); // Label is drawn inside the marker border

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
