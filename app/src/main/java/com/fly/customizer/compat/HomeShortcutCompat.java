package com.fly.customizer.compat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

/**
 * Pins a home screen shortcut for an installed app using a drawable supplied by an icon pack.
 *
 * <p>Uses the ShortcutManager API on Android 8+ and the legacy INSTALL_SHORTCUT broadcast on
 * older releases.</p>
 */
public final class HomeShortcutCompat {

    private static final String TAG = "HomeShortcutCompat";

    private HomeShortcutCompat() {
    }

    /** Result callback invoked on the calling thread. */
    public interface Callback {
        void onResult(boolean success, String message);
    }

    public static boolean isPinSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager manager = context.getSystemService(ShortcutManager.class);
            return manager != null && manager.isRequestPinShortcutSupported();
        }
        return true;
    }

    public static void pinShortcut(
            Context context,
            String packageName,
            String className,
            String label,
            Drawable icon,
            Callback callback
    ) {
        if (callback == null) {
            return;
        }
        if (packageName == null || className == null) {
            callback.onResult(false, "Missing component");
            return;
        }
        Bitmap bitmap = toBitmap(icon, 192);
        if (bitmap == null) {
            callback.onResult(false, "Icon could not be rendered");
            return;
        }

        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setComponent(new ComponentName(packageName, className));
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager manager = context.getSystemService(ShortcutManager.class);
            if (manager == null) {
                callback.onResult(false, "Shortcut service unavailable");
                return;
            }
            if (!manager.isRequestPinShortcutSupported()) {
                callback.onResult(false, "Your launcher does not support pinned shortcuts");
                return;
            }
            String shortcutId = "fly_" + packageName + "_" + Math.abs(className.hashCode());
            ShortcutInfo info = new ShortcutInfo.Builder(context, shortcutId)
                    .setShortLabel(label == null ? packageName : label)
                    .setLongLabel(label == null ? packageName : label)
                    .setIcon(Icon.createWithBitmap(bitmap))
                    .setIntent(launchIntent)
                    .build();
            boolean requested;
            try {
                requested = manager.requestPinShortcut(info, null);
            } catch (Exception error) {
                Log.e(TAG, "requestPinShortcut failed", error);
                callback.onResult(false, error.getMessage() == null ? "Request failed" : error.getMessage());
                return;
            }
            if (requested) {
                callback.onResult(true, "Confirm the shortcut on your home screen");
            } else {
                callback.onResult(false, "The launcher refused the shortcut request");
            }
            return;
        }

        // Legacy path for Android 7.x and below.
        Intent installIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        installIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
        installIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label == null ? packageName : label);
        installIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
        installIntent.putExtra("duplicate", false);
        context.sendBroadcast(installIntent);
        callback.onResult(true, "Shortcut broadcast sent");
    }

    private static Bitmap toBitmap(Drawable drawable, int size) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                return Bitmap.createScaledBitmap(bitmap, size, size, true);
            }
        }
        try {
            Bitmap target = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(target);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
            return target;
        } catch (OutOfMemoryError error) {
            Log.e(TAG, "Unable to rasterise icon", error);
            return null;
        }
    }
}
