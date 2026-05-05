package cn.breezeth.ordertocook.client.gecko;

public final class FirstPersonArmCalibration {
    public enum HandTarget {
        BOTH,
        LEFT,
        RIGHT
    }

    public enum Profile {
        DRIVE,
        ACTION
    }

    public static final float PIXEL = 1.0f / 16.0f;

    public static volatile float ROOT_Y = -27.0f * PIXEL;

    public static volatile float MAIN_X = -0.1875f;
    public static volatile float MAIN_Y = -0.5625f;
    public static volatile float MAIN_Z = -1.03125f;
    public static volatile float MAIN_X_ROT = 36.5f;
    public static volatile float MAIN_Y_ROT = 1.5f;
    public static volatile float MAIN_Z_ROT = 0.0f;

    public static volatile float OFF_X = -0.75f;
    public static volatile float OFF_Y = -0.5625f;
    public static volatile float OFF_Z = -1.03125f;
    public static volatile float OFF_X_ROT = 36.5f;
    public static volatile float OFF_Y_ROT = 1.5f;
    public static volatile float OFF_Z_ROT = 0.0f;

    public static volatile float ACTION_MAIN_X = -0.375f;
    public static volatile float ACTION_MAIN_Y = -0.4375f;
    public static volatile float ACTION_MAIN_Z = -0.84375f;
    public static volatile float ACTION_MAIN_X_ROT = 90.5f;
    public static volatile float ACTION_MAIN_Y_ROT = 25.5f;
    public static volatile float ACTION_MAIN_Z_ROT = -9.0f;

    public static volatile float ACTION_OFF_X = -0.5f;
    public static volatile float ACTION_OFF_Y = 0.0f;
    public static volatile float ACTION_OFF_Z = -0.96875f;
    public static volatile float ACTION_OFF_X_ROT = 69.5f;
    public static volatile float ACTION_OFF_Y_ROT = -18.5f;
    public static volatile float ACTION_OFF_Z_ROT = -11.0f;

    public static volatile float HORN_MAIN_X = 0.48f * PIXEL;
    public static volatile float HORN_MAIN_Y = 0.48f * PIXEL;
    public static volatile float HORN_MAIN_Z = 0.48f * PIXEL;
    public static volatile float HORN_MAIN_X_ROT = -8.0f;
    public static volatile float HORN_MAIN_Y_ROT = 8.0f;
    public static volatile float HORN_MAIN_Z_ROT = -8.0f;

    public static volatile float HORN_REPEAT_X = 0.32f * PIXEL;
    public static volatile float HORN_REPEAT_Y = 0.32f * PIXEL;
    public static volatile float HORN_REPEAT_Z = 0.32f * PIXEL;
    public static volatile float HORN_REPEAT_X_ROT = -6.0f;
    public static volatile float HORN_REPEAT_Y_ROT = 6.0f;
    public static volatile float HORN_REPEAT_Z_ROT = -6.0f;

    public static float mainX() {
        return MAIN_X;
    }

    public static float mainX(Profile profile) {
        return profile == Profile.ACTION ? ACTION_MAIN_X : MAIN_X;
    }

    public static float offX() {
        return OFF_X;
    }

    public static float offX(Profile profile) {
        return profile == Profile.ACTION ? ACTION_OFF_X : OFF_X;
    }

    public static float mainY() {
        return MAIN_Y;
    }

    public static float mainY(Profile profile) {
        return profile == Profile.ACTION ? ACTION_MAIN_Y : MAIN_Y;
    }

    public static float offY() {
        return OFF_Y;
    }

    public static float offY(Profile profile) {
        return profile == Profile.ACTION ? ACTION_OFF_Y : OFF_Y;
    }

    public static float mainZ() {
        return MAIN_Z;
    }

    public static float mainZ(Profile profile) {
        return profile == Profile.ACTION ? ACTION_MAIN_Z : MAIN_Z;
    }

    public static float offZ() {
        return OFF_Z;
    }

    public static float offZ(Profile profile) {
        return profile == Profile.ACTION ? ACTION_OFF_Z : OFF_Z;
    }

    public static float mainXRot() {
        return MAIN_X_ROT;
    }

    public static float mainXRot(Profile profile) {
        return profile == Profile.ACTION ? ACTION_MAIN_X_ROT : MAIN_X_ROT;
    }

    public static float offXRot() {
        return OFF_X_ROT;
    }

    public static float offXRot(Profile profile) {
        return profile == Profile.ACTION ? ACTION_OFF_X_ROT : OFF_X_ROT;
    }

    public static float mainYRot() {
        return MAIN_Y_ROT;
    }

    public static float mainYRot(Profile profile) {
        return profile == Profile.ACTION ? ACTION_MAIN_Y_ROT : MAIN_Y_ROT;
    }

    public static float offYRot() {
        return OFF_Y_ROT;
    }

    public static float offYRot(Profile profile) {
        return profile == Profile.ACTION ? ACTION_OFF_Y_ROT : OFF_Y_ROT;
    }

    public static float mainZRot() {
        return MAIN_Z_ROT;
    }

    public static float mainZRot(Profile profile) {
        return profile == Profile.ACTION ? ACTION_MAIN_Z_ROT : MAIN_Z_ROT;
    }

    public static float offZRot() {
        return OFF_Z_ROT;
    }

    public static float offZRot(Profile profile) {
        return profile == Profile.ACTION ? ACTION_OFF_Z_ROT : OFF_Z_ROT;
    }

    public static void adjustBaseX(float delta, HandTarget target) {
        adjustBaseX(Profile.DRIVE, delta, target);
    }

    public static void adjustBaseX(Profile profile, float delta, HandTarget target) {
        if (target == HandTarget.BOTH || target == HandTarget.RIGHT) {
            if (profile == Profile.ACTION) {
                ACTION_MAIN_X += delta;
            } else {
                MAIN_X += delta;
            }
        }
        if (target == HandTarget.BOTH || target == HandTarget.LEFT) {
            if (profile == Profile.ACTION) {
                ACTION_OFF_X += delta;
            } else {
                OFF_X += delta;
            }
        }
    }

    public static void adjustBaseY(float delta, HandTarget target) {
        adjustBaseY(Profile.DRIVE, delta, target);
    }

    public static void adjustBaseY(Profile profile, float delta, HandTarget target) {
        if (target == HandTarget.BOTH || target == HandTarget.RIGHT) {
            if (profile == Profile.ACTION) {
                ACTION_MAIN_Y += delta;
            } else {
                MAIN_Y += delta;
            }
        }
        if (target == HandTarget.BOTH || target == HandTarget.LEFT) {
            if (profile == Profile.ACTION) {
                ACTION_OFF_Y += delta;
            } else {
                OFF_Y += delta;
            }
        }
    }

    public static void adjustBaseZ(float delta, HandTarget target) {
        adjustBaseZ(Profile.DRIVE, delta, target);
    }

    public static void adjustBaseZ(Profile profile, float delta, HandTarget target) {
        if (target == HandTarget.BOTH || target == HandTarget.RIGHT) {
            if (profile == Profile.ACTION) {
                ACTION_MAIN_Z += delta;
            } else {
                MAIN_Z += delta;
            }
        }
        if (target == HandTarget.BOTH || target == HandTarget.LEFT) {
            if (profile == Profile.ACTION) {
                ACTION_OFF_Z += delta;
            } else {
                OFF_Z += delta;
            }
        }
    }

    public static void adjustBaseXRot(float delta, HandTarget target) {
        adjustBaseXRot(Profile.DRIVE, delta, target);
    }

    public static void adjustBaseXRot(Profile profile, float delta, HandTarget target) {
        if (target == HandTarget.BOTH || target == HandTarget.RIGHT) {
            if (profile == Profile.ACTION) {
                ACTION_MAIN_X_ROT += delta;
            } else {
                MAIN_X_ROT += delta;
            }
        }
        if (target == HandTarget.BOTH || target == HandTarget.LEFT) {
            if (profile == Profile.ACTION) {
                ACTION_OFF_X_ROT += delta;
            } else {
                OFF_X_ROT += delta;
            }
        }
    }

    public static void adjustBaseYRot(float delta, HandTarget target) {
        adjustBaseYRot(Profile.DRIVE, delta, target);
    }

    public static void adjustBaseYRot(Profile profile, float delta, HandTarget target) {
        if (target == HandTarget.BOTH || target == HandTarget.RIGHT) {
            if (profile == Profile.ACTION) {
                ACTION_MAIN_Y_ROT += delta;
            } else {
                MAIN_Y_ROT += delta;
            }
        }
        if (target == HandTarget.BOTH || target == HandTarget.LEFT) {
            if (profile == Profile.ACTION) {
                ACTION_OFF_Y_ROT += delta;
            } else {
                OFF_Y_ROT += delta;
            }
        }
    }

    public static void adjustBaseZRot(float delta, HandTarget target) {
        adjustBaseZRot(Profile.DRIVE, delta, target);
    }

    public static void adjustBaseZRot(Profile profile, float delta, HandTarget target) {
        if (target == HandTarget.BOTH || target == HandTarget.RIGHT) {
            if (profile == Profile.ACTION) {
                ACTION_MAIN_Z_ROT += delta;
            } else {
                MAIN_Z_ROT += delta;
            }
        }
        if (target == HandTarget.BOTH || target == HandTarget.LEFT) {
            if (profile == Profile.ACTION) {
                ACTION_OFF_Z_ROT += delta;
            } else {
                OFF_Z_ROT += delta;
            }
        }
    }

    private FirstPersonArmCalibration() {
    }
}

