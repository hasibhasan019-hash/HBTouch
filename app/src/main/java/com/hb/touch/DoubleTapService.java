package com.hb.touch;

import android.accessibilityservice.AccessibilityService;
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

public class DoubleTapService extends AccessibilityService {

    private WindowManager wm;
    private View overlay;
    private long lastTap = 0;
    private String currentPkg = null;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        overlay = new View(this);
        overlay.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                if (!isHomeForeground()) {
                    lastTap = 0;
                    return false;
                }
                long now = SystemClock.uptimeMillis();
                long gap = now - lastTap;
                if (gap > 40 && gap <= ViewConfiguration.getDoubleTapTimeout()) {
                    lastTap = 0;
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
                } else {
                    lastTap = now;
                }
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

    private boolean isHomeForeground() {
        if (currentPkg == null) return false;
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, 0);
        return ri != null && ri.activityInfo != null
                && currentPkg.equals(ri.activityInfo.packageName);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        CharSequence cls = event.getClassName();
        if (pkg == null) return;
        if (cls != null && cls.toString().startsWith("android.widget")) return;
        currentPkg = pkg.toString();
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        try { if (wm != null && overlay != null) wm.removeView(overlay); } catch (Exception ignored) { }
        super.onDestroy();
    }
}
