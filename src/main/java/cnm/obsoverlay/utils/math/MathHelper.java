package cnm.obsoverlay.utils.math;

public class MathHelper {
    private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
    private static final double[] ASIN_TABLE = new double[257];
    private static final double[] COS_TABLE = new double[257];

    public static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) {
            value -= 360.0F;
        }

        if (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }

    public static double wrapDegrees(double value) {
        value %= 360.0;
        if (value >= 180.0) {
            value -= 360.0;
        }

        if (value < -180.0) {
            value += 360.0;
        }

        return value;
    }

    public static int wrapDegrees(int angle) {
        angle %= 360;
        if (angle >= 180) {
            angle -= 360;
        }

        if (angle < -180) {
            angle += 360;
        }

        return angle;
    }

    public static int clamp(int num, int min, int max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }

    public static float clamp(float num, float min, float max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }

    public static double clamp(double num, double min, double max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }

    public static double atan2(double y, double x) {
        final double distanceSquared = x * x + y * y;

        if (Double.isNaN(distanceSquared)) {
            return Double.NaN;
        } else {
            final boolean yNegative = y < 0.0D;

            if (yNegative) {
                y = -y;
            }

            final boolean xNegative = x < 0.0D;

            if (xNegative) {
                x = -x;
            }

            final boolean yGreaterThanX = y > x;

            if (yGreaterThanX) {
                final double temp = x;
                x = y;
                y = temp;
            }

            final double invSqrt = fastInverseSqrt(distanceSquared);
            x = x * invSqrt;
            y = y * invSqrt;
            final double biasedY = FRAC_BIAS + y;
            final int tableIndex = (int) Double.doubleToRawLongBits(biasedY);
            final double asinValue = ASIN_TABLE[tableIndex];
            final double cosValue = COS_TABLE[tableIndex];
            final double yOffset = biasedY - FRAC_BIAS;
            final double delta = y * cosValue - x * yOffset;
            final double correction = (6.0D + delta * delta) * delta * 0.16666666666666666D;
            double result = asinValue + correction;

            if (yGreaterThanX) {
                result = (Math.PI / 2D) - result;
            }

            if (xNegative) {
                result = Math.PI - result;
            }

            if (yNegative) {
                result = -result;
            }

            return result;
        }
    }

    /**
     * Fast inverse square root algorithm (Quake III)
     * Calculates 1 / sqrt(value)
     */
    public static double fastInverseSqrt(double value) {
        final double halfValue = 0.5D * value;
        long bits = Double.doubleToRawLongBits(value);
        bits = 6910469410427058090L - (bits >> 1);  // Magic constant for double precision
        value = Double.longBitsToDouble(bits);
        value = value * (1.5D - halfValue * value * value);  // Newton-Raphson iteration
        return value;
    }
}
