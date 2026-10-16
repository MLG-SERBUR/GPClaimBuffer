package com.github.gpaddons.gpclaimbuffer;

/**
 * Pure edge-to-edge distance math for inclusive block ranges.
 *
 * <p>Claims store inclusive block coordinates, so a claim ending at X=100 and
 * another starting at X=101 are adjacent with a gap of 0 empty blocks.
 */
final class DistanceChecker
{

    private DistanceChecker()
    {
    }

    /**
     * Empty blocks between two inclusive ranges.
     *
     * @return 0 when ranges overlap or are adjacent, otherwise block count between them
     */
    static int gapBetweenRanges(int min1, int max1, int min2, int max2)
    {
        return Math.max(0, Math.max(min1 - max2 - 1, min2 - max1 - 1));
    }

    /**
     * Whether two 2D boxes are closer than the required buffer.
     *
     * @param distance required empty blocks between borders (0 disables)
     * @return true when {@code max(gapX, gapZ) < distance}
     */
    static boolean conflicts(
            int min1x, int max1x, int min1z, int max1z,
            int min2x, int max2x, int min2z, int max2z,
            int distance)
    {
        if (distance <= 0) return false;
        return min2x - distance <= max1x && min1x <= max2x + distance
                && min2z - distance <= max1z && min1z <= max2z + distance;
    }

}
