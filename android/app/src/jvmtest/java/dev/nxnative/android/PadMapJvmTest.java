package dev.nxnative.android;

import java.util.ArrayList;
import java.util.List;

/**
 * PadMap on the JVM, driven by dm/tools/tests/test_android_padmap.py. Commands, one per run:
 * <pre>
 * parse &lt;map&gt;                         → "parse &lt;the map as stored&gt;"
 * assign &lt;map&gt; &lt;button&gt; &lt;keycode&gt;    → "assign &lt;the map after it&gt;"
 * env &lt;layout&gt; &lt;map&gt;                  → "env &lt;the host's lines, ';' between them&gt;"
 * label &lt;layout&gt; &lt;map&gt;                → "label &lt;the settings row's value&gt;"
 * </pre>
 */
public final class PadMapJvmTest {
    public static void main(String[] a) {
        switch (a[0]) {
            case "parse": System.out.println("parse " + PadMap.format(PadMap.parse(a[1]))); break;
            case "assign": System.out.println("assign " + PadMap.format(PadMap.assign(PadMap.parse(a[1]), a[2], Integer.parseInt(a[3])))); break;
            case "env": { List<String> e = new ArrayList<>(); PadMap.env(a[1], a[2], e); System.out.println("env " + String.join(";", e)); break; }
            case "label": System.out.println("label " + PadMap.label(a[1], a[2])); break;
            default: throw new IllegalArgumentException(a[0]);
        }
    }
}
