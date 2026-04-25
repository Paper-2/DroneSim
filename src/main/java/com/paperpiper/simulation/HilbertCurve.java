package com.paperpiper.simulation;

import java.util.ArrayList;
import java.util.List;

import com.jme3.math.Vector3f;


public final class HilbertCurve {

    private HilbertCurve() {
        /* utility class */ }

    public static List<Vector3f> generate(int order, float worldW, float worldD, float altitude) {
        int n = 1 << order;          // grid side length: 2^order
        int total = n * n;           // total waypoints: 4^order

        List<Vector3f> points = new ArrayList<>(total);

        float scaleX = worldW / Math.max(1, n - 1);
        float scaleZ = worldD / Math.max(1, n - 1);

        for (int d = 0; d < total; d++) {
            int[] xy = hilbertDToXY(n, d);
            float wx = xy[0] * scaleX;
            float wz = xy[1] * scaleZ;
            points.add(new Vector3f(wx, altitude, wz));
        }

        return points;
    }



    static int[] hilbertDToXY(int n, int d) {
        int x = 0;
        int y = 0;
        int rx, ry, s, t = d;
        for (s = 1; s < n; s <<= 1) {
            rx = 1 & (t >> 1);
            ry = 1 & (t ^ rx);
            int[] rotated = rotateXY(s, x, y, rx, ry);
            x = rotated[0];
            y = rotated[1];
            x += s * rx;
            y += s * ry;
            t >>= 2;
        }
        return new int[]{x, y};
    }

    private static int[] rotateXY(int n, int x, int y, int rx, int ry) {
        if (ry == 0) {
            if (rx == 1) {
                x = n - 1 - x;
                y = n - 1 - y;
            }
            int tmp = x;
            x = y;
            y = tmp;
        }
        return new int[]{x, y};
    }
}
