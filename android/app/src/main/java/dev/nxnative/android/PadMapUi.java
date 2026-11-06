package dev.nxnative.android;

import android.app.AlertDialog;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.LinkedHashMap;

/**
 * Settings › Controls › Gamepad buttons (PadMap): the layout, and for Custom the map, one Switch button a row; tapping a row waits for a key on the gamepad.
 * Stored for every game (Prefs.padLayout, Prefs.padMap), read by GameActivity at the game's start (DM_PAD_LABELLED, DM_PAD_MAP).
 */
final class PadMapUi {
    private PadMapUi() {}

    static void dialog(MainActivity a, Runnable done) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        AlertDialog[] d = new AlertDialog[1];
        Runnable again = () -> { d[0].dismiss(); dialog(a, done); };
        String layout = Prefs.padLayout(a);
        box.addView(Ui.small(a, "How a gamepad's buttons reach the game, for every game from its next start. By position: the bottom, right, left and top buttons are "
                + "the Switch's B, A, Y and X, as on a Switch controller. By label: the button the pad calls A is A (a pad whose labels are swapped, such as the AYN "
                + "Odin 3's). Custom: choose a key for each button. A DM_PAD_LABELLED or DM_PAD_MAP line in env.txt or a game's own lines wins."));
        RadioGroup rg = new RadioGroup(a);
        for (int k = 0; k < PadMap.LAYOUTS.length; k++) {
            RadioButton rb = new RadioButton(a);
            rb.setText(PadMap.LAYOUT_NAMES[k]);
            rb.setId(k + 1);
            rg.addView(rb);
        }
        rg.check(1 + java.util.Arrays.asList(PadMap.LAYOUTS).indexOf(layout));
        rg.setOnCheckedChangeListener((x, w) -> {
            String l = PadMap.LAYOUTS[w - 1];
            Prefs.padLayout(a, l);
            if (l.equals(PadMap.CUSTOM) && PadMap.parse(Prefs.padMap(a)).isEmpty()) Prefs.padMap(a, PadMap.LABEL_MAP);   // a start: each face button to its label
            again.run();
        });
        Ui.add(box, rg, 8);
        if (layout.equals(PadMap.CUSTOM)) {
            LinkedHashMap<Integer, String> map = PadMap.parse(Prefs.padMap(a));
            Ui.add(box, Ui.small(a, "Tap a button, then press the key for it on the gamepad. Keys not listed keep their place by position."), 12);
            LinearLayout list = Ui.add(box, Ui.listCard(a), 8);
            for (String b : PadMap.BUTTONS) {
                int key = PadMap.keyFor(map, b);
                list.addView(new Ui.Setting(a, "🎮", PadMap.buttonName(b), v -> capture(a, b, again))
                        .value(key < 0 ? "By position" : "Key: " + PadMap.keyName(key)).chevron(a).view);
            }
            int off = 0;
            for (String v : map.values()) if (v.equals(PadMap.NONE)) off++;
            if (off > 0) Ui.add(box, Ui.small(a, off + " key(s) set to do nothing."), 8);
        }
        AlertDialog.Builder bld = Ui.sheet(a, "Gamepad buttons", box).setPositiveButton("Done", null);
        if (layout.equals(PadMap.CUSTOM)) bld.setNeutralButton("Reset map", (x, w) -> { Prefs.padMap(a, PadMap.LABEL_MAP); again.run(); });
        d[0] = bld.setOnDismissListener(x -> done.run()).show();
    }

    /** Waits for one key on a gamepad and gives it the button (Back and the volume keys cancel or are left to the phone) */
    private static void capture(MainActivity a, String button, Runnable done) {
        AlertDialog[] d = new AlertDialog[1];
        d[0] = new AlertDialog.Builder(a).setTitle("Switch " + PadMap.buttonName(button))
                .setMessage("Press the gamepad key for the Switch's " + PadMap.buttonName(button) + " button.")
                .setNeutralButton("Nothing", (x, w) -> {
                    // this button from no key: its mapped key does nothing now
                    LinkedHashMap<Integer, String> m = PadMap.parse(Prefs.padMap(a));
                    int k = PadMap.keyFor(m, button);
                    if (k >= 0) { m.put(k, PadMap.NONE); Prefs.padMap(a, PadMap.format(m)); }
                    done.run();
                })
                .setNegativeButton("Cancel", null)
                .setOnKeyListener((x, keyCode, e) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                            || keyCode == KeyEvent.KEYCODE_BUTTON_MODE || keyCode == KeyEvent.KEYCODE_HOME || keyCode > 999) return false;
                    if (e.getAction() != KeyEvent.ACTION_UP) return true;   // taken on release, so the press does not also reach the dialog's buttons
                    Prefs.padMap(a, PadMap.format(PadMap.assign(PadMap.parse(Prefs.padMap(a)), button, keyCode)));
                    d[0].dismiss();
                    done.run();
                    return true;
                })
                .show();
    }
}
