package com.example.navbarbutton;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * LSPosed/libxposed API 101 module.
 *
 * Target: com.android.systemui
 *
 * We hook NavigationBarInflaterView.getDefaultLayout() and add a custom
 * "toast" button spec. Then we hook createView() and turn that spec into
 * an ordinary TextView with an OnClickListener.
 *
 * AOSP's NavigationBarInflaterView is responsible for splitting the navbar
 * layout into button specs and creating each button. OEM SystemUI builds
 * can move the class between:
 *   com.android.systemui.navigationbar.NavigationBarInflaterView
 *   com.android.systemui.statusbar.phone.NavigationBarInflaterView
 *
 * We try both names.
 */
public class NavbarHook extends XposedModule {

    private static final String TAG = "NavbarButton";
    private static final String SYSTEMUI = "com.android.settings";
    private static final String TOAST_SPEC = "lsposed101";

    private boolean hooked = false;

    private String prevActivity = "";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(android.util.Log.INFO, TAG, "Module loaded zzz");
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        ClassLoader cl = param.getClassLoader();
        String packageName = param.getPackageName();

        if (!SYSTEMUI.equals(packageName)) {
            return;
        }

        if (hooked) {
            return;
        }

        hooked = true;

        log(android.util.Log.INFO, TAG, "SystemUI ready: " + cl);

        try {
            final Context[] ctx = new Context[1];

            hook(AccessibilityService.class.getDeclaredMethod("getAccessibilityButtonController")).intercept(chain -> {
                log(Log.INFO, TAG, "Hooked getAccessibilityButtonController " + chain.getThisObject());
                ctx[0] = (Context) chain.getThisObject();
                return chain.proceed();
            });

            hook(AccessibilityButtonController.class.getDeclaredMethod("registerAccessibilityButtonCallback",
                    AccessibilityButtonController.AccessibilityButtonCallback.class)).intercept(chain -> {
                log(Log.INFO, TAG, "Hooked registerAccessibilityButtonCallback");
                return chain.proceed(new Object[] {
                        new AccessibilityButtonController.AccessibilityButtonCallback() {
                            @Override
                            public void onClicked(AccessibilityButtonController controller) {
                                log(Log.INFO, TAG, "ZZZ " + chain.getThisObject());

                                ActivityManager am = (ActivityManager) ctx[0].getSystemService(Context.ACTIVITY_SERVICE);
                                ActivityManager.RunningTaskInfo task = am.getRunningTasks(1).get(0);
                                String curActivity = task.topActivity.getPackageName();

                                // try {
                                //     String activity = task.topActivity.toString().replaceAll("(ComponentInfo\\{|\\})", "");
                                //     log(Log.INFO, TAG, "Top activity " + task.taskId + " " + activity);
                                //     Runtime.getRuntime().exec(
                                //             "am start --windowingMode 3 -n " + activity
                                //     );
                                //     Toast.makeText(ctx[0], task.topActivity.getPackageName(), Toast.LENGTH_SHORT).show();
                                // } catch (Exception e) {
                                //     log(Log.ERROR, TAG, "Freeform ", e);
                                // }
                                // if(true) return;

                                String packageName = getLaunchApp(ctx[0]);
                                if (packageName == null || packageName.isEmpty()) {
                                    Toast.makeText(ctx[0], "No app has been set to launch", Toast.LENGTH_SHORT).show();
                                } else if (curActivity.equals(packageName) && !prevActivity.isEmpty()) {
                                    Intent intent = ctx[0].getPackageManager().getLaunchIntentForPackage(prevActivity);
                                    if (intent != null) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        ctx[0].startActivity(intent);
                                    }
                                    prevActivity = "";
                                } else {
                                    Intent intent = ctx[0].getPackageManager().getLaunchIntentForPackage(packageName);
                                    if (intent != null) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        ctx[0].startActivity(intent);
                                    }
                                    prevActivity = curActivity;
                                }
                            }
                        }
                });
            });

            // hook(AccessibilityManager.class.getDeclaredMethod("notifyAccessibilityButtonLongClicked", int.class)).intercept(chain -> {
            //     log(Log.INFO, TAG, "Hooked notifyAccessibilityButtonLongClicked " + chain.getThisObject());
            //     return chain.proceed();
            // });
        } catch (Throwable t) {
            log(android.util.Log.WARN, TAG, "Failed to hook settings", t);
        }
    }

    private String getLaunchApp(Context context) {
        Uri uri = Uri.parse("content://com.example.navbarbutton.data/launcher");
        Cursor c = context.getContentResolver().query(uri, null, null, null, null);
        String value = "";
        if (c != null) {
            if (c.moveToFirst()) {
                value = c.getString(c.getColumnIndexOrThrow("value"));
            }
            c.close();
        }
        return value;
    }

    private static Method findMethod(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static final String ACCESSIBILITY_BUTTON = "accessibility_button";

    private boolean isAccessibilityButton(View view) {
        if (view == null) {
            return false;
        }

        try {
            int id = view.getId();
            log(Log.INFO, TAG, "view id: " + id);
            if (id == View.NO_ID) {
                return false;
            }

            Resources res = view.getResources();
            String entryName = res.getResourceEntryName(id);
            log(Log.INFO, TAG, "button: " + entryName);

            return ACCESSIBILITY_BUTTON.equals(entryName);

        } catch (Resources.NotFoundException ignored) {
            return false;
        } catch (Throwable t) {
            log(Log.INFO, TAG, "isAccessibilityButton failed: " + t);
            return false;
        }
    }

    private void hookButtonDispatcher(Object dispatcher) {

        try {
            Class<?> clazz = dispatcher.getClass();
            Field mClickListener = clazz.getSuperclass().getDeclaredField("mClickListener");
            log(Log.INFO, TAG,"mClickListener: " + mClickListener);
            mClickListener.set(dispatcher, (View.OnClickListener) v -> {
                log(Log.INFO, TAG,"MY BUTTON CLICKED");
            });

            // Method setOnClickListener = clazz.getSuperclass().getMethod(
            //         "setOnClickListener",
            //         View.OnClickListener.class
            // );

            // log(Log.INFO, TAG, "Hooking: " + setOnClickListener);

            // setOnClickListener.invoke(dispatcher, (View.OnClickListener) v -> {
            //     log(Log.INFO, TAG,"MY BUTTON CLICKED");
            // });

            // hook(setOnClickListener).intercept(chain -> {
            //     log(Log.INFO, TAG,"MY BUTTON SET ON CLICKED");
            //     return chain.proceed();
            // });
        } catch (Throwable t) {
            log(Log.INFO, TAG, "hook ButtonDispatcher failed: " + t);
        }
    }
}
