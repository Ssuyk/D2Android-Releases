package com.winlator.d2;




final class D2PadInventory {
    private D2PadInventory() {}

    static final int LEFT = 0, RIGHT = 1, DOWN = 2, UP = 3;

    private static final int PANEL_X = 400, PANEL_Y = 60, CELL = 29, COLS = 10, GRID_LEFT = 19;
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
        float ox = (gameWidth - 800) / 2f + PANEL_X, oy = (gameHeight - 600) / 2f + PANEL_Y;
        int rows = big ? D2InventoryEdge.BIG_H : D2InventoryEdge.SMALL_H, top = big ? GRID_TOP_BIG : GRID_TOP_SMALL;
        int[][] boxes = big ? BOXES_BIG : BOXES_SMALL;
        float[][] out = new float[COLS * rows + boxes.length][];
        int n = 0;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < COLS; c++)
                out[n++] = new float[]{ox + GRID_LEFT + c * CELL + CELL / 2f - 1, oy + top + r * CELL + CELL / 2f - 1};
        for (int[] b : boxes) out[n++] = new float[]{ox + b[0] + b[2] / 2f, oy + b[1] + b[3] / 2f};
        return out;
    }


    static int nearest(float[][] slots, float x, float y) {
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < slots.length; i++) {
            double d = Math.hypot(slots[i][0] - x, slots[i][1] - y);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return bestD <= CELL ? best : -1;
    }



    static float[] step(float[][] slots, float x, float y, int dir) {
        int from = nearest(slots, x, y);
        if (from < 0) return slots[0];
        float fx = slots[from][0], fy = slots[from][1];
        float ux = dir == LEFT ? -1 : dir == RIGHT ? 1 : 0, uy = dir == UP ? -1 : dir == DOWN ? 1 : 0;
        float[] best = null;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < slots.length; i++) {
            if (i == from) continue;
            float dx = slots[i][0] - fx, dy = slots[i][1] - fy;
            double along = dx * ux + dy * uy, across = Math.abs(dx * uy - dy * ux);
            if (along < 4) continue;
            double score = along + 2 * across;
            if (score < bestScore) {
                bestScore = score;
                best = slots[i];
            }
        }
        return best != null ? best : slots[from];
    }
}
