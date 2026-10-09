package com.mdvlcraft.binder.ability;

public final class WeaponFreeCasts {
    private static boolean casting;

    private WeaponFreeCasts() {
    }

    static void run(Runnable cast) {
        casting = true;

        try {
            cast.run();
        } finally {
            casting = false;
        }
    }

    public static boolean active() {
        return casting;
    }
}
