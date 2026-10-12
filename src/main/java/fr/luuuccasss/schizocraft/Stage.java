package fr.luuuccasss.schizocraft;

import net.minecraft.ChatFormatting;

/** Les stades de paranoïa et la fréquence des hallucinations (en secondes, entre deux). */
public enum Stage {
    LUCIDE("lucide", ChatFormatting.GREEN, 0, 0, 0),
    NERVEUX("nerveux", ChatFormatting.YELLOW, 25, 45, 90),
    PARANOIAQUE("paranoïaque", ChatFormatting.GOLD, 50, 18, 40),
    DELIRE("délire", ChatFormatting.DARK_RED, 75, 7, 18);

    public final String label;
    public final ChatFormatting color;
    public final float threshold;
    public final int minDelay;
    public final int maxDelay;

    Stage(String label, ChatFormatting color, float threshold, int minDelay, int maxDelay) {
        this.label = label;
        this.color = color;
        this.threshold = threshold;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
    }

    public static Stage of(float paranoia) {
        Stage stage = LUCIDE;
        for (Stage s : values()) if (paranoia >= s.threshold) stage = s;
        return stage;
    }

    public boolean atLeast(Stage other) {
        return ordinal() >= other.ordinal();
    }
}
