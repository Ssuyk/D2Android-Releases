package com.winlator.d2;

import java.util.ArrayList;
import java.util.List;




final class D2PanelGrids {
    static final int PLACE_INVENTORY = 0, PLACE_CUBE = 3, PLACE_STASH = 4;
    static final int ROW_MONSTER = 5, ROW_TRADE_1 = 6, ROW_TRADE_2 = 7, ROW_BANK = 8, ROW_CUBE = 9, ROW_BIG_BANK = 12;


    static final class Grid {
        final int cols, rows, cellW, cellH, left, top;

        Grid(int cols, int rows, int cellW, int cellH, int left, int top) {
            this.cols = cols;
            this.rows = rows;
            this.cellW = cellW;
            this.cellH = cellH;
            this.left = left;
            this.top = top;
        }
    }


    private final Grid[] grids = new Grid[16];

    final List<int[]> items = new ArrayList<>();

    // Shop wares as {location << 16 | node page << 8 | body location, x, y, w, h}. The location is the tab (0 armor, 1 and 2
    // weapons, 3 misc, seen at Charsi); which tab is visible is not known, so the ware under the pointer tells it.
    final List<int[]> wares = new ArrayList<>();
    int hoveredWareKey = -1;

    private D2PanelGrids() {}


    static D2PanelGrids parse(byte[] d, int at, int length) {
        if (at >= length) return null;
        D2PanelGrids g = new D2PanelGrids();
        int n = d[at++] & 0xFF;
        for (int i = 0; i < n; i++, at += 9) {
            if (at + 9 > length) return null;
            int row = d[at] & 0xFF, cols = d[at + 1] & 0xFF, rows = d[at + 2] & 0xFF, cw = d[at + 3] & 0xFF, ch = d[at + 4] & 0xFF;
            int left = (d[at + 5] & 0xFF) | (d[at + 6] & 0xFF) << 8, top = (d[at + 7] & 0xFF) | (d[at + 8] & 0xFF) << 8;
            if (row < g.grids.length && cols > 0 && rows > 0 && cols <= 16 && rows <= 16 && cw > 0 && ch > 0)
                g.grids[row] = new Grid(cols, rows, cw, ch, left, top);
        }
        if (at >= length) return null;
        int count = d[at++] & 0xFF;
        for (int i = 0; i < count; i++, at += 3) {
            if (at + 3 > length) return null;
            int xy = d[at + 1] & 0xFF, wh = d[at + 2] & 0xFF;
            g.items.add(new int[]{d[at] & 0xFF, xy >> 4, xy & 15, wh >> 4, wh & 15});
        }
        if (at >= length) return g;
        int wares = d[at++] & 0xFF;
        if (at + wares * 5 + 1 > length) return g;
        for (int i = 0; i < wares; i++, at += 5) {
            int key = (d[at] & 0xFF) << 16 | (d[at + 1] & 0xFF) << 8 | (d[at + 2] & 0xFF), xy = d[at + 3] & 0xFF, wh = d[at + 4] & 0xFF;
            g.wares.add(new int[]{key, xy >> 4, xy & 15, wh >> 4, wh & 15});
        }
        int hovered = d[at] & 0xFF;
        g.hoveredWareKey = hovered < wares ? g.wares.get(hovered)[0] : -1;
        return g;
    }


    Grid grid(int row) {
        return row >= 0 && row < grids.length ? grids[row] : null;
    }


    Grid stash() {
        Grid big = grid(ROW_BIG_BANK);
        return big != null ? big : grid(ROW_BANK);
    }



    Grid shop() {
        for (int row : new int[]{ROW_MONSTER, ROW_TRADE_1, ROW_TRADE_2}) {
            Grid g = grid(row);
            if (g != null && g.cols == 10 && g.rows == 10) return g;
        }
        return grid(ROW_TRADE_1);
    }


    List<int[]> waresWithKey(int key) {
        List<int[]> out = new ArrayList<>();
        for (int[] it : wares) if (it[0] == key) out.add(new int[]{it[1], it[2], it[3], it[4]});
        // Wares that overlap cannot be on one tab: the key does not tell the tabs apart.
        for (int i = 0; i < out.size(); i++) {
            for (int j = i + 1; j < out.size(); j++) {
                int[] a = out.get(i), b = out.get(j);
                if (a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3]) return null;
            }
        }
        return out;
    }


    List<int[]> itemsAt(int place) {
        List<int[]> out = new ArrayList<>();
        for (int[] it : items) if (it[0] == place) out.add(new int[]{it[1], it[2], it[3], it[4]});
        return out;
    }
}
