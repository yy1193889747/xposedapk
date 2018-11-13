package xpdemo.cy.com.xpdemo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Method;

public class Module implements IXposedHookLoadPackage {
    private static String TAG = "alilog";
    private static boolean first = false;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.i(TAG, lpparam.packageName);
        if ("com.eg.android.AlipayGphone".equals(lpparam.packageName)) {
            hookRpcCall(lpparam);
        }
    }


    private void hookRpcCall(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class,
                    new ApplicationAttachMethodHook());
        } catch (Exception e2) {
            Log.i(TAG, "hookRpcCall err:" + Log.getStackTraceString(e2));
        }
    }

    private class ApplicationAttachMethodHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            super.afterHookedMethod(param);
            if (first) {
                return;
            }
            final ClassLoader loader = ((Context) param.args[0]).getClassLoader();
            Class clazz = loader.loadClass("com.alipay.mobile.nebulacore.ui.H5FragmentManager");
            if (clazz != null) {
                Class<?> h5FragmentClazz = loader.loadClass("com.alipay.mobile.nebulacore.ui.H5Fragment");
                if (h5FragmentClazz != null) {
                    XposedHelpers.findAndHookMethod(clazz, "pushFragment", h5FragmentClazz, boolean.class,
                            Bundle.class, boolean.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            Log.i("fragment", "cur fragment: " + param.args[0]);
                            Auto.curH5Fragment = param.args[0];
                        }
                    });
                }
            }

            clazz = loader.loadClass("com.alipay.mobile.nebulacore.ui.H5Activity");
            if (clazz != null) {
                XposedHelpers.findAndHookMethod(clazz, "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Auto.h5Activity = (Activity) param.thisObject;
                    }
                });
            }

            clazz = loader.loadClass("com.alipay.mobile.nebulabiz.rpc.H5RpcUtil");
            if (clazz != null) {
                first = true;
                Log.i(TAG, "first");

                Class<?> h5PageClazz = loader.loadClass("com.alipay.mobile.h5container.api.H5Page");
                Class<?> jsonClazz = loader.loadClass("com.alibaba.fastjson.JSONObject");
                if (h5PageClazz != null && jsonClazz != null) {
                    XposedHelpers.findAndHookMethod(clazz, "rpcCall", String.class, String.class, String.class,
                            boolean.class, jsonClazz, String.class, boolean.class, h5PageClazz, int.class,
                            String.class, boolean.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            Log.i(TAG,
                                    "param" + param.args[0] + "," + param.args[1] + "," + param.args[2] + ","
                                            + param.args[3] + "," + param.args[4] + "," + param.args[5] + ","
                                            + param.args[6] + "," + param.args[7] + "," + param.args[8] + ","
                                            + param.args[9] + "," + param.args[10] + "," + param.args[11]);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            Object resp = param.getResult();
                            if (resp != null) {
                                Method method = resp.getClass().getMethod("getResponse", new Class<?>[]{});
                                String response = (String) method.invoke(resp, new Object[]{});
                                Log.i(TAG, "response: " + response);

                                if (Auto.isRankList(response)) {
                                    Log.i(TAG, "autoGetCanCollectUserIdList");
                                    Auto.autoGetCanCollectUserIdList(loader, response);
                                }

                                if (Auto.isUserDetail(response)) {
                                    Log.i(TAG, "autoGetCanCollectBubbleIdList");
                                    Auto.autoGetCanCollectBubbleIdList(loader, response);
                                }
                            }
                        }
                    });
                }
            }
        }
    }
}


