package com.winlator.d2;

import android.content.Context;
import android.view.InputDevice;

import java.util.Locale;




// Which buttons the hints draw. The face buttons are named by place: "A" is always the bottom one,
// so on a Nintendo pad it reads "B", the label printed there.
final class D2PadStyle {
    private D2PadStyle() {}

    static final int AUTO = 0, XBOX = 1, PLAYSTATION = 2, NINTENDO = 3;
    static final String KEY = "padStyle";

    private static final int VENDOR_SONY = 0x054C, VENDOR_NINTENDO = 0x057E;


    static int chosen(Context context) {
        int v = context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE).getInt(KEY, AUTO);
        return v >= AUTO && v <= NINTENDO ? v : AUTO;
    }

    static int current(Context context) {
        int v = chosen(context);
        return v == AUTO ? detect() : v;
    }


    static int detect() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d == null || d.isVirtual()) continue;
            int sources = d.getSources();
            if ((sources & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD
                && (sources & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) continue;
            int style = of(d.getVendorId(), d.getName());
            if (style != AUTO) return style;
        }
        return XBOX;
    }

    static int of(int vendor, String name) {
        if (vendor == VENDOR_SONY) return PLAYSTATION;
        if (vendor == VENDOR_NINTENDO) return NINTENDO;
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (n.contains("dualsense") || n.contains("dualshock") || n.contains("playstation") || n.contains("ps4") || n.contains("ps5")
            || n.equals("wireless controller")) return PLAYSTATION;
        if (n.contains("nintendo") || n.contains("switch") || n.contains("pro controller") || n.contains("joy-con")) return NINTENDO;
        if (n.contains("xbox") || n.contains("x-box")) return XBOX;
        return AUTO;
    }


    static String label(int style, int glyph) {
        switch (glyph) {
            case D2PadNav.GLYPH_A: return style == NINTENDO ? "B" : "A";
            case D2PadNav.GLYPH_B: return style == NINTENDO ? "A" : "B";
            case D2PadNav.GLYPH_X: return style == NINTENDO ? "Y" : "X";
            case D2PadNav.GLYPH_Y: return style == NINTENDO ? "X" : "Y";
            case D2PadNav.GLYPH_LB: return style == PLAYSTATION ? "L1" : style == NINTENDO ? "L" : "LB";
            case D2PadNav.GLYPH_RB: return style == PLAYSTATION ? "R1" : style == NINTENDO ? "R" : "RB";
            default: return "";
        }
    }


    // The name of the top face button for text such as "Press Y to identify".
    static String topName(Context context, int style) {
        if (style == PLAYSTATION) return D2Text.get(context, com.winlator.R.string.pad_name_triangle);
        return label(style, D2PadNav.GLYPH_Y);
    }
}
