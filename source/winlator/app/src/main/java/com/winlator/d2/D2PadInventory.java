package com.winlator.d2;




final class D2PadInventory {
    private D2PadInventory() {}

    static final int LEFT = 0, RIGHT = 1, DOWN = 2, UP = 3;

    private static final int PANEL_X = 400, PANEL_Y = 60, CELL = 29, COLS = 10, GRID_LEFT = 19;
    private static final int FREE_STEP = 15;
    private static final int GRID_TOP_SMALL = 255, GRID_TOP_BIG = 145;


    private static final int[][] BOXES_SMALL = {
        {20, 47, 55, 112}, {133, 77, 56, 82}, {251, 47, 55, 112}, {135, 8, 54, 51}, {209, 35, 23, 24},
        {95, 180, 23, 24}, {209, 180, 23, 24}, {136, 179, 52, 25}, {252, 182, 54, 52}, {21, 181, 54, 53},
    };
    private static final int[][] BOXES_BIG = {
        {8, 25, 55, 112}, {132, 8, 54, 82}, {255, 25, 55, 112}, {72, 8, 52, 51}, {208, 8, 23, 24},
        {195, 43, 23, 24}, {223, 43, 23, 24}, {133, 101, 52, 25}, {194, 78, 52, 52}, {72, 72, 52, 53},
    };


    static float[][] slots(boolean big, int gameWidth, int gameHeight) {
        return slots(big, gameWidth, gameHeight, null, null);
    }


    static final class Area {
        final D2PanelGrids.Grid grid;
        final java.util.List<int[]> items;

        Area(D2PanelGrids.Grid grid, java.util.List<int[]> items) {
            this.grid = grid;
            this.items = items;
        }
    }



    static float[][] slots(boolean big, int gameWidth, int gameHeight, java.util.List<int[]> items, java.util.List<Area> others) {
        float ox = (gameWidth - 800) / 2f + PANEL_X, oy = (gameHeight - 600) / 2f + PANEL_Y;
        int rows = big ? D2InventoryEdge.BIG_H : D2InventoryEdge.SMALL_H, top = big ? GRID_TOP_BIG : GRID_TOP_SMALL;
        int[][] boxes = big ? BOXES_BIG : BOXES_SMALL;
        float gx = ox + GRID_LEFT - 1, gy = oy + top - 1;
        java.util.List<float[]> out = new java.util.ArrayList<>();
        addGrid(out, gx, gy, CELL, CELL, COLS, rows, items);
        if (others != null) {
            float fx = (gameWidth - 800) / 2f, fy = (gameHeight - 600) / 2f;
            for (Area a : others)
                if (a.grid != null) addGrid(out, fx + a.grid.left - 1, fy + a.grid.top - 1, a.grid.cellW, a.grid.cellH, a.grid.cols, a.grid.rows, a.items);
        }
        for (int[] b : boxes) out.add(new float[]{ox + b[0] + b[2] / 2f, oy + b[1] + b[3] / 2f, b[2] / 2f, b[3] / 2f});
        float[][] slots = out.toArray(new float[0][]);

        int first = 0;
        for (int i = 1; i < slots.length; i++)
            if (Math.hypot(slots[i][0] - gx, slots[i][1] - gy) < Math.hypot(slots[first][0] - gx, slots[first][1] - gy)) first = i;
        float[] t = slots[0];
        slots[0] = slots[first];
        slots[first] = t;
        return slots;
    }


    private static void addGrid(java.util.List<float[]> out, float gx, float gy, int cw, int ch, int cols, int rows,
                                java.util.List<int[]> items) {
        boolean[] covered = new boolean[cols * rows];
        if (items != null) {
            for (int[] it : items) {
                if (it[0] + it[2] > cols || it[1] + it[3] > rows || it[2] < 1 || it[3] < 1) continue;
                for (int y = it[1]; y < it[1] + it[3]; y++) for (int x = it[0]; x < it[0] + it[2]; x++) covered[y * cols + x] = true;
                out.add(new float[]{gx + (it[0] + it[2] / 2f) * cw, gy + (it[1] + it[3] / 2f) * ch, it[2] * cw / 2f, it[3] * ch / 2f});
            }
        }
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (!covered[r * cols + c]) out.add(new float[]{gx + c * cw + cw / 2f, gy + r * ch + ch / 2f, cw / 2f, ch / 2f});
    }


    static int nearest(float[][] slots, float x, float y) {
        for (int i = 0; i < slots.length; i++)
            if (Math.abs(slots[i][0] - x) < slots[i][2] && Math.abs(slots[i][1] - y) < slots[i][3]) return i;
        return -1;
    }







    static float[] step(float[][] slots, float x, float y, int dir) {
        int from = nearest(slots, x, y);
        float fx = from >= 0 ? slots[from][0] : x, fy = from >= 0 ? slots[from][1] : y;
        float ux = dir == LEFT ? -1 : dir == RIGHT ? 1 : 0, uy = dir == UP ? -1 : dir == DOWN ? 1 : 0;
        float half = from < 0 ? 0 : ux != 0 ? slots[from][3] : slots[from][2];
        float[] best = null;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < slots.length; i++) {
            if (i == from) continue;
            float dx = slots[i][0] - fx, dy = slots[i][1] - fy;
            double along = dx * ux + dy * uy, across = Math.abs(dx * uy - dy * ux);
            if (along < 4) continue;
            double gap = Math.max(0, across - half - (ux != 0 ? slots[i][3] : slots[i][2]));
            if (gap > along) continue;
            double score = along + 3 * gap;
            if (score < bestScore) {
                bestScore = score;
                best = slots[i];
            }
        }
        if (best != null) return best;

        float out = (from < 0 ? 0 : ux != 0 ? slots[from][2] : slots[from][3]) + FREE_STEP;
        return new float[]{fx + ux * out, fy + uy * out};
    }
}
