package xpdemo.cy.com.xpdemo;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @author congyang.guo
 */
public class Auto {
    public static Object curH5Fragment;
    public static Activity h5Activity;
    private static String TAG = "alilogforest";
    private static ArrayList<String> canCollectEnergyidList = new ArrayList();
    private static Object curH5PageImpl;
    private static ArrayList<String> friendsRankUseridList = new ArrayList();
    private static boolean isWebViewRefresh;
    private static Integer totalEnergy = 0;
    private static Integer pageCount = 0;


    /**
     * 自动获取有能量的好友信息
     *
     * @param loader   加载器
     * @param response json
     */
    public static void autoGetCanCollectUserIdList(final ClassLoader loader, String response) {
        if (isWebViewRefresh) {
            // 如果已经刷新了，这里又回来response了，就表示这里是我们刷新webview结束来到的逻辑
            finishWork();
            return;
        }
        // 开始解析好友信息，循环把所有有能量的好友信息都解析完
        boolean isSucc = parseFrienRankPageDataResponse(response);
        if (isSucc) {
            showToast("开始获取可以收取能量的好友信息...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 发送获取下一页好友信息接口
                    rpcCall_FriendRankList(loader);
                }
            }) {
            }.start();
        } else {
            Log.i(TAG, "friendsRankUseridList " + friendsRankUseridList);
            //如果发现已经解析完成了，如果有好友能量收取，就开始收取
            if (friendsRankUseridList.size() > 0) {
                showToast("开始获取每个好友能够偷取的能量信息...");
                for (String userId : friendsRankUseridList) {
                    // 开始收取每个用户的能量
                    rpcCall_CanCollectEnergy(loader, userId);
                    canCollectEnergyidList.clear();
                }
                Log.i(TAG, "collect energy finish refresh webview...");
            }
            // 执行完了调用刷新页面，看看总能量效果
            refreshWebView();
            friendsRankUseridList.clear();
            pageCount = 0;
        }
    }

    /**
     * 自动获取能收取的能量ID
     *
     * @param loader   加载器
     * @param response json
     */

    public static void autoGetCanCollectBubbleIdList(ClassLoader loader, String response) {
        if (!TextUtils.isEmpty(response) && response.contains("collectStatus")) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.optJSONArray("bubbles");
                if (jsonArray != null && jsonArray.length() > 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                        if ("AVAILABLE".equals(jsonObject1.optString("collectStatus"))) {
                            Log.i(TAG, "可收 " + jsonObject1.optString("userId") + "---" + jsonObject1.optLong("id"));
                            rpcCall_CollectEnergy(loader, jsonObject1.optString("userId"),
                                    jsonObject1.optLong("id"));
                        }
                        if (jsonObject1.optBoolean("canHelpCollect")) {
                            forFriendCollectEnergy(loader, jsonObject1.optString("userId"), jsonObject1.optLong("id"));
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public static boolean isRankList(String response) {
        return !TextUtils.isEmpty(response) && response.contains("friendRanking");
    }

    public static boolean isUserDetail(String response) {
        return !TextUtils.isEmpty(response) && response.contains("userEnergy");
    }

    /**
     * 刷新页面
     */
    private static void refreshWebView() {
        showToast("一共收取了" + totalEnergy + "g能量");
        isWebViewRefresh = true;
    }

    /**
     * 结束工作
     */
    private static void finishWork() {
        isWebViewRefresh = false;
        // 打印收取了多少能量
        Log.i(TAG, "一共收取了" + totalEnergy + "g能量");
    }

    /**
     * 解析好友信息
     *
     * @param response json
     * @return
     */
    private static boolean parseFrienRankPageDataResponse(String response) {
        try {
            JSONArray optJSONArray = new JSONObject(response).optJSONArray("friendRanking");
            if (optJSONArray == null || optJSONArray.length() == 0) {
                return false;
            } else {
                for (int i = 0; i < optJSONArray.length(); i++) {
                    JSONObject jsonObject = optJSONArray.getJSONObject(i);
                    boolean optBoolean = jsonObject.optBoolean("canCollectEnergy");
                    boolean optBooleans = jsonObject.optBoolean("canHelpCollect");
                    String userId = jsonObject.optString("userId");
                    if (optBoolean || optBooleans) {
                        if (!friendsRankUseridList.contains(userId)) {
                            friendsRankUseridList.add(userId);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return true;
    }

    /**
     * 获取分页好友信息命令
     *
     * @param loader
     */
    private static void rpcCall_FriendRankList(final ClassLoader loader) {
        try {
            Method rpcCallMethod = getRpcCallMethod(loader);
            JSONArray jsonArray = new JSONArray();
            JSONObject json = new JSONObject();
            json.put("av", "5");
            json.put("ct", "android");
            json.put("pageSize", 20);
            json.put("startPoint", "" + (pageCount * 20 + 1));
            pageCount++;
            jsonArray.put(json);
            Log.i(TAG, "call friendranklist params:" + jsonArray);

            rpcCallMethod.invoke(null, "alipay.antmember.forest.h5.queryEnergyRanking", jsonArray.toString(), "",
                    true, null, null, false, curH5PageImpl, 0, "", false, -1);

        } catch (Exception e) {
            Log.i(TAG, "rpcCall_FriendRankList err: " + Log.getStackTraceString(e));
        }
    }

    /**
     * 获取指定用户可以收取的能量信息
     *
     * @param loader
     * @param userId
     */
    private static void rpcCall_CanCollectEnergy(final ClassLoader loader, String userId) {
        try {
            Method rpcCallMethod = getRpcCallMethod(loader);
            JSONArray jsonArray = new JSONArray();
            JSONObject json = new JSONObject();
            json.put("userId", userId);
            json.put("version", "20181220");
            jsonArray.put(json);
            Log.i(TAG, "call cancollect energy params:" + jsonArray);

            rpcCallMethod.invoke(null, "alipay.antmember.forest.h5.queryNextAction", jsonArray.toString(), "", true,
                    null, null, false, curH5PageImpl, 0, "", false, -1);

            rpcCallMethod.invoke(null, "alipay.antmember.forest.h5.pageQueryDynamics", jsonArray.toString(), "", true
                    , null, null, false, curH5PageImpl, 0, "", false, -1);

        } catch (Exception e) {
            Log.i(TAG, "rpcCall_CanCollectEnergy err: " + Log.getStackTraceString(e));
        }
    }

    /**
     * 收取能量命令
     *
     * @param loader
     * @param userId
     * @param bubbleId
     */
    public static void rpcCall_CollectEnergy(final ClassLoader loader, String userId, Long bubbleId) {
        try {
            Method rpcCallMethod = getRpcCallMethod(loader);
            JSONArray jsonArray = new JSONArray();
            JSONArray bubbleAry = new JSONArray();
            bubbleAry.put(bubbleId);
            JSONObject json = new JSONObject();
            json.put("bubbleIds", bubbleAry);
            json.put("userId", userId);
            jsonArray.put(json);
            Log.i(TAG, "call cancollect energy params:" + jsonArray);

            Object resp = rpcCallMethod.invoke(null, "alipay.antmember.forest.h5.collectEnergy", jsonArray.toString()
                    , "", true, null, null, false, curH5PageImpl, 0, "", false, -1);
            Method method = resp.getClass().getMethod("getResponse");
            String response = (String) method.invoke(resp, new Object[]{});
            boolean isSucc = Auto.parseCollectEnergyResponse(response);
            if (isSucc) {
                Log.i(TAG, "userid：" + userId + ",bubbleId：" + bubbleId);
            } else {
                Log.i(TAG, "userid：" + userId + ",bubbleId：" + bubbleId);
            }
        } catch (Exception e) {
            Log.i(TAG, "rpcCall_CanCollectEnergy err: " + Log.getStackTraceString(e));
        }
    }

    public static Method getRpcCallMethod(ClassLoader loader) {
        try {
            Field aF = curH5Fragment.getClass().getDeclaredField("a");
            aF.setAccessible(true);
            Object viewHolder = aF.get(curH5Fragment);
            Field hF = viewHolder.getClass().getDeclaredField("h");
            hF.setAccessible(true);
            curH5PageImpl = hF.get(viewHolder);
            Class<?> h5PageClazz = loader.loadClass("com.alipay.mobile.h5container.api.H5Page");
            Class<?> jsonClazz = loader.loadClass("com.alibaba.fastjson.JSONObject");
            Class<?> rpcClazz = loader.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5RpcUtil");
            if (curH5PageImpl != null) {
                Method callM = rpcClazz.getMethod("rpcCall", String.class, String.class, String.class, boolean.class,
                        jsonClazz, String.class, boolean.class, h5PageClazz, int.class, String.class, boolean.class,
                        int.class);
                return callM;

            }
        } catch (Exception e) {
            Log.i(TAG, "getRpcCallMethod err: " + Log.getStackTraceString(e));
        }
        return null;
    }

    public static boolean parseCollectEnergyResponse(String response) {
        if (!TextUtils.isEmpty(response) && response.contains("failedBubbleIds")) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.optJSONArray("bubbles");
                for (int i = 0; i < jsonArray.length(); i++) {
                    totalEnergy += jsonArray.getJSONObject(i).optInt("collectedEnergy");
                }
                if ("SUCCESS".equals(jsonObject.optString("resultCode"))) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    private static void showToast(final String str) {
        if (h5Activity != null) {
            try {
                h5Activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(h5Activity, str, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.i(TAG, "showToast err: " + Log.getStackTraceString(e));
            }
        }
    }

    /**
     * alipay.antmember.forest.h5.forFriendCollectEnergy,[{"bubbleIds":[],"targetUserId":""}],
     *
     * @param loader
     * @param userId
     * @param bubbleId
     */

    public static void forFriendCollectEnergy(ClassLoader loader, String userId, Long bubbleId) {
        try {
            Method rpcCallMethod = getRpcCallMethod(loader);
            JSONArray jsonArray = new JSONArray();
            JSONArray bubbleAry = new JSONArray();
            bubbleAry.put(bubbleId);
            JSONObject json = new JSONObject();
            json.put("bubbleIds", bubbleAry);
            json.put("targetUserId", userId);
            jsonArray.put(json);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("call cancollect energy params:");
            stringBuilder.append(jsonArray);
            Log.i(str, stringBuilder.toString());
            Object resp = rpcCallMethod.invoke(null, "alipay.antmember.forest.h5.forFriendCollectEnergy",
                    jsonArray.toString(), "", Boolean.TRUE, null, null, Boolean.FALSE, curH5PageImpl, 0, "",
                    Boolean.FALSE, -1);
        } catch (Exception e) {
            Log.i(TAG, "forFriendCollectEnergy err: " + e.getMessage());
        }
    }

}

