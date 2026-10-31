package com.hb.touch;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class DoubleTapService extends AccessibilityService {

    private WindowManager wm;
    private View overlay;
    private long lastTap = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        overlay = new View(this);
        overlay.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                handleTap();
            }
            return false;
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        wm.addView(overlay, lp);
    }

    private void handleTap() {
        if (!isPlainHome()) {
            lastTap = 0;
            return;
        }
        long now = SystemClock.uptimeMillis();
        long gap = now - lastTap;
        if (lastTap != 0 && gap > 40 && gap <= ViewConfiguration.getDoubleTapTimeout()) {
            lastTap = 0;
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        } else {
            lastTap = now;
        }
    }

    /** true = lock screen na, home launcher shamne, drawer/recents/folder khola na */
    private boolean isPlainHome() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardLocked()) return false;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        try {
            CharSequence pkg = root.getPackageName();
            if (pkg == null || !pkg.toString().equals(homePackage())) return false;
            return !overlayOpen(root, 0);
        } finally {
            root.recycle();
        }
    }

    private boolean overlayOpen(AccessibilityNodeInfo n, int depth) {
        if (depth > 25) return false;
        if (n.isVisibleToUser()) {
            String cls = n.getClassName() == null ? "" : n.getClassName().toString();
            String id = n.getViewIdResourceName() == null
                    ? "" : n.getViewIdResourceName().toLowerCase();
            if (cls.contains("RecyclerView") || cls.contains("ListView")
                    || id.contains("overview") || id.contains("recents")
                    || id.contains("folder") || id.contains("widgets")
                    || id.contains("apps_list") || id.contains("apps_view")
                    || id.contains("all_apps")) {
                return true;
            }
        }
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c == null) continue;
            boolean b = overlayOpen(c, depth + 1);
            c.recycle();
            if (b) return true;
        }
        return false;
    }

    private String homePackage() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, 0);
        return (ri != null && ri.activityInfo != null) ? ri.activityInfo.packageName : "";
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        try { if (wm != null && overlay != null) wm.removeView(overlay); } catch (Exception ignored) { }
        super.onDestroy();
    }
}
