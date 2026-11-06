package dev.nxnative.android;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings › Gamepad buttons, the pure part (no Android classes, so PadMapJvmTest checks it on the JVM): how a gamepad's buttons reach the game's Switch buttons.
 * Android names face buttons by position as on an Xbox pad (BUTTON_A bottom, B right, X left, Y top); the host maps them by position by default (input_map.c
 * nxin_key_button). Three layouts:
 * <ul>
 * <li>"position" (the default): a pad's bottom / right / left / top buttons are the Switch's B / A / Y / X, wherever their labels are;</li>
 * <li>"label": BUTTON_A is the Switch's A, and so on, for a pad whose driver reports the names printed on it (DM_PAD_LABELLED=1);</li>
 * <li>"custom": the player's own map, key by key (DM_PAD_MAP, input_map.h nxin_map_parse: "&lt;keycode&gt;=&lt;button&gt;" pairs, ',' between them); keys left out keep the
 *     position layout. The AYN Odin 3 reports its face buttons swapped (A as B, X as Y): the label layout, or a custom map, puts them right.</li>
 * </ul>
 */
final class PadMap {
    private PadMap() {}

    static final String POSITION = "position", LABEL = "label", CUSTOM = "custom";
    static final String[] LAYOUTS = {POSITION, LABEL, CUSTOM};
    static final String[] LAYOUT_NAMES = {"By position (default)", "By label", "Custom"};

    /** The Switch buttons a key can be mapped to, in the order the custom map lists them; NONE takes a key away */
    static final String[] BUTTONS = {"A", "B", "X", "Y", "L", "R", "ZL", "ZR", "PLUS", "MINUS", "LS", "RS", "UP", "DOWN", "LEFT", "RIGHT"};
    static final String NONE = "NONE";

    /** Android's key codes a gamepad sends (android.view.KeyEvent), for the names the custom map shows */
    private static final Map<Integer, String> KEY_NAMES = new LinkedHashMap<>();
    static {
        KEY_NAMES.put(96, "A"); KEY_NAMES.put(97, "B"); KEY_NAMES.put(98, "C"); KEY_NAMES.put(99, "X"); KEY_NAMES.put(100, "Y"); KEY_NAMES.put(101, "Z");
        KEY_NAMES.put(102, "L1"); KEY_NAMES.put(103, "R1"); KEY_NAMES.put(104, "L2"); KEY_NAMES.put(105, "R2");
        KEY_NAMES.put(106, "Left stick"); KEY_NAMES.put(107, "Right stick"); KEY_NAMES.put(108, "Start"); KEY_NAMES.put(109, "Select");
        KEY_NAMES.put(19, "D-pad up"); KEY_NAMES.put(20, "D-pad down"); KEY_NAMES.put(21, "D-pad left"); KEY_NAMES.put(22, "D-pad right"); KEY_NAMES.put(23, "D-pad centre");
        for (int k = 188; k <= 203; k++) KEY_NAMES.put(k, "Button " + (k - 187));   // KEYCODE_BUTTON_1..16 (generic HID pads)
    }

    /** A key's name as the custom map shows it: the pad's own name for it, else its number */
    static String keyName(int keycode) {
        String n = KEY_NAMES.get(keycode);
        return n != null ? n : "Key " + keycode;
    }

    /** A Switch button's name as the settings show it */
    static String buttonName(String b) {
        switch (b) {
            case "PLUS": return "+";
            case "MINUS": return "−";
            case "LS": return "Left stick press";
            case "RS": return "Right stick press";
            case "UP": return "D-pad up";
            case "DOWN": return "D-pad down";
            case "LEFT": return "D-pad left";
            case "RIGHT": return "D-pad right";
            case NONE: return "Nothing";
            default: return b;
        }
    }

    static boolean isButton(String b) {
        if (NONE.equals(b)) return true;
        for (String x : BUTTONS) if (x.equals(b)) return true;
        return false;
    }

    /** A stored layout as one of LAYOUTS (an unknown value reads as the default) */
    static String layoutOf(String v) {
        for (String l : LAYOUTS) if (l.equals(v)) return l;
        return POSITION;
    }

    /**
     * A stored custom map, keycode → Switch button, in its order: the host's format (nxin_map_parse), pairs that are not a keycode 1..999 and a button skipped,
     * the first of a repeated key kept, at most 32 (NXIN_MAP_MAX)
     */
    static LinkedHashMap<Integer, String> parse(String s) {
        LinkedHashMap<Integer, String> m = new LinkedHashMap<>();
        if (s == null) return m;
        for (String pair : s.split(",")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String k = pair.substring(0, eq).trim(), b = pair.substring(eq + 1).trim().toUpperCase(java.util.Locale.ROOT);
            if (!k.matches("[0-9]{1,3}") || !isButton(b)) continue;
            int key = Integer.parseInt(k);
            if (key < 1 || key > 999 || m.containsKey(key) || m.size() >= 32) continue;
            m.put(key, b);
        }
        return m;
    }

    /** A custom map in the stored (and the host's) format */
    static String format(Map<Integer, String> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> e : m.entrySet()) sb.append(sb.length() == 0 ? "" : ",").append(e.getKey()).append('=').append(e.getValue());
        return sb.toString();
    }

    /** The key a custom map gives a Switch button (the first), -1 when none does */
    static int keyFor(Map<Integer, String> m, String button) {
        for (Map.Entry<Integer, String> e : m.entrySet()) if (e.getValue().equals(button)) return e.getKey();
        return -1;
    }

    /**
     * The map with keycode given to the button: the key's earlier button and the button's earlier key are dropped first, so one key reaches one button and a button
     * comes from one key. A face key the position layout would give the button left elsewhere is not taken away: the player maps it too, or sets it to Nothing
     */
    static LinkedHashMap<Integer, String> assign(Map<Integer, String> m, String button, int keycode) {
        LinkedHashMap<Integer, String> r = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> e : m.entrySet()) if (e.getKey() != keycode && !e.getValue().equals(button)) r.put(e.getKey(), e.getValue());
        r.put(keycode, button);
        return r;
    }

    /** The face buttons each to the Switch button of the same name: the label layout as a custom map, a starting point to change */
    static final String LABEL_MAP = "96=A,97=B,99=X,100=Y";

    /** The settings row's value */
    static String label(String layout, String map) {
        switch (layoutOf(layout)) {
            case LABEL: return "By label: the pad's A is the Switch's A";
            case CUSTOM: {
                int n = parse(map).size();
                return n == 0 ? "Custom: nothing mapped yet (by position)" : "Custom: " + n + " key" + (n == 1 ? "" : "s") + " mapped";
            }
            default: return "By position: the bottom button is the Switch's B";
        }
    }

    /** The host's lines for the layout (GameActivity.start, before env.txt and the game's own lines, which win) */
    static void env(String layout, String map, List<String> env) {
        switch (layoutOf(layout)) {
            case LABEL: env.add("DM_PAD_LABELLED=1"); break;
            case CUSTOM: {
                String m = format(parse(map));
                if (!m.isEmpty()) env.add("DM_PAD_MAP=" + m);
                break;
            }
            default: break;
        }
    }
}
