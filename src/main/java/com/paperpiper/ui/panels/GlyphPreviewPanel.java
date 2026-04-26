package com.paperpiper.ui.panels;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

/**
 * Preview panel displaying all monocraft font glyphs.
 */
public class GlyphPreviewPanel {

    private boolean visible = false;

    public void render() {
        if (!visible) {
            return;
        }

        ImGui.setNextWindowSize(1200, 800, ImGuiCond.FirstUseEver);
        if (ImGui.begin("Monocraft Font Glyphs")) {

            ImGui.text("UPPERCASE & LOWERCASE:");
            ImGui.text("A B C D E F G H I J K L M N O P Q R S T U V W X Y Z");
            ImGui.text("a b c d e f g h i j k l m n o p q r s t u v w x y z");

            ImGui.separator();
            ImGui.text("DIGITS:");
            ImGui.text("0 1 2 3 4 5 6 7 8 9");

            ImGui.separator();
            ImGui.text("PUNCTUATION & BASIC SYMBOLS:");
            ImGui.text("! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~");

            ImGui.separator();
            ImGui.text("EXTENDED ASCII:");
            ImGui.text("¡ ¢ £ ¤ ¥ ¦ § ¨ © ª « ¬ ® ¯ ° ± ² ³ ´ µ ¶ · ¸ ¹ º » ¼ ½ ¾ ¿");

            ImGui.separator();
            ImGui.text("ACCENTED LETTERS:");
            ImGui.text("À Á Â Ã Ä Å Æ Ç È É Ê Ë Ì Í Î Ï Ð Ñ");
            ImGui.text("à á â ã ä å æ ç è é ê ë ì í î ï ð ñ");

            ImGui.separator();
            ImGui.text("GREEK & RUSSIAN:");
            ImGui.text("Α Β Γ Δ Ε Ζ Η Θ Ι Κ Λ Μ Ν Ξ Ο Π Ρ Σ Τ Υ Φ Χ Ψ Ω");
            ImGui.text("α β γ δ ε ζ η θ ι κ λ μ ν ξ ο π ρ ς σ τ υ φ χ ψ ω");
            ImGui.text("А Б В Г Д Е Ж З И Й К Л М Н О П Р С Т У Ф Х Ц Ч Ш Щ");

            ImGui.separator();
            ImGui.text("HEBREW:");
            ImGui.text("א ב ג ד ה ו ז ח ט י ך כ ל ם מ ן נ ס ע ף פ ץ צ ק ר ש ת");

            ImGui.separator();
            ImGui.text("BOX DRAWING:");
            ImGui.text("─ │ ┌ ┐ └ ┘ ├ ┤ ┬ ┴ ┼");
            ImGui.text("═ ║ ╔ ╗ ╚ ╝ ╠ ╣ ╦ ╩ ╬");

            ImGui.separator();
            ImGui.text("BLOCKS & SHAPES:");
            ImGui.text("█ ▓ ▒ ░");
            ImGui.text("■ □ ▪ ▫ ● ○ ◆ ◇ ★ ☆");
            ImGui.text("▲ △ ▶ ▷ ▼ ▽ ◀ ◁");

            ImGui.separator();
            ImGui.text("ARROWS:");
            ImGui.text("← ↑ → ↓ ↔ ↕ ↖ ↗ ↘ ↙");
            ImGui.text("⇐ ⇑ ⇒ ⇓ ⇔ ⇕");

            ImGui.separator();
            ImGui.text("MATH SYMBOLS:");
            ImGui.text("+ - = * / < > ± ÷ × ≤ ≥ ≠ ≈");
            ImGui.text("∀ ∁ ∂ ∃ ∄ ∉ ∋ ∌ ∑ ∞ ∥ ∧ ∨ ∩ ∫ ∮");
            ImGui.text("⊂ ⊃ ⊄ ⊅ ⊆ ⊇ ⊢ ⊤ ⊥ ⊨ ⋃ ⋆");

            ImGui.separator();
            ImGui.text("CURRENCY:");
            ImGui.text("₠ ₡ ₢ ₣ ₤ ₥ ₦ ₩ ₪ ₫ € ₭ ₮ ₰ ₱ ₲ ₳ ₴ ₵ ₶ ₷ ₸ ₹ ₺ ₻ ₼ ₽ ₾ ₿");

            ImGui.separator();
            ImGui.text("MISCELLANEOUS:");
            ImGui.text("° ℗ ™ ⅐ ⅑ ⅓ ⅔ ⅕ ⅖ ⅗ ⅙ ⅚ ⅛ ⅜ ⅝ ⅞");
            ImGui.text("⌀ ⌂ ⌘ ⌚ ⌛ ⏏ ⏩ ⏪ ⏭ ⏮ ⏯ ⏳ ⏴ ⏵ ⏶ ⏷ ⏸ ⏹ ⏺");

            ImGui.separator();
            ImGui.text("WEATHER & SYMBOLS:");
            ImGui.text("☀ ☁ ☂ ☃ ☄ ★ ☆ ☈ ☔ ☠");
            ImGui.text("☰ ☱ ☲ ☳ ☴ ☵ ☶ ☷");
            ImGui.text("☹ ☺ ☻ ☽");

            ImGui.separator();
            ImGui.text("SUITS & MUSIC:");
            ImGui.text("♠ ♡ ♢ ♣ ♤ ♥ ♦ ♧");
            ImGui.text("♩ ♪ ♫ ♬ ♭ ♮ ♯");
            ImGui.text("♀ ♂");

            ImGui.separator();
            ImGui.text("DICE & TOOLS:");
            ImGui.text("⚀ ⚁ ⚂ ⚃ ⚄ ⚅");
            ImGui.text("⚐ ⚑ ⚓ ⚔ ⚗ ⚡ ⚥");
            ImGui.text("✂ ✉ ✎ ✔ ✘");

            ImGui.separator();
            ImGui.text("SPECIAL:");
            ImGui.text("❄ ❌ ❣ ❤ ⭐ ⛄ ⛈ ⛏");
        }
        ImGui.end();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
