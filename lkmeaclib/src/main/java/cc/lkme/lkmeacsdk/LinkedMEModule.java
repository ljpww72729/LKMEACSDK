package cc.lkme.lkmeacsdk;

import android.net.Uri;
import android.text.TextUtils;

import com.microquation.linkedme.android.LinkedME;
import com.microquation.linkedme.android.callback.LMLinkCreateListener;
import com.microquation.linkedme.android.callback.LMSimpleInitListener;
import com.microquation.linkedme.android.indexing.LMUniversalObject;
import com.microquation.linkedme.android.referral.LMError;
import com.microquation.linkedme.android.referral.PrefHelper;
import com.microquation.linkedme.android.util.LinkProperties;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by LinkedME06 on 16/11/2.
 */

public class LinkedMEModule extends UZModule {
    LinkedME linkedME = null;
    String linkedme_key = null;
    LMSimpleInitListener simpleInitListener = null;

    public LinkedMEModule(UZWebView webView) {
        super(webView);
    }

    private void init(final UZModuleContext moduleContext) {
        linkedme_key = moduleContext.optString("linkedme_key");
        linkedME = LinkedME.getInstance(getContext(), linkedme_key, false);
        Uri intentData = null;
        if (getContext().getIntent() != null) {
            intentData = getContext().getIntent().getData();
        }
        PrefHelper.Debug("LinkedME", "jsmethod_lkmeInit: " + intentData);
        LinkedME.getInstance().setHandleStatus(false);
        LinkedME.getInstance().initSessionWithData(intentData, getContext()); // indicate  starting of session.表示session开始
    }

    public void jsmethod_lkmeInit(final UZModuleContext moduleContext) {
        init(moduleContext);
    }

    public void jsmethod_lkmeInitCallback(final UZModuleContext moduleContext) {
        init(moduleContext);
        //apicloud第一次加载页面的时候不会调用它提供的resume()方法
        jsmethod_lkmeObtainData(moduleContext);
    }

    public void jsmethod_lkmeDebug(final UZModuleContext moduleContext) {
        if (linkedME == null) {
            linkedME = LinkedME.getInstance(getContext(), linkedme_key, false);
        }
        linkedME.setDebug();
    }


    public void jsmethod_lkmeObtainData(final UZModuleContext moduleContext) {
        PrefHelper.Debug("LinkedME", "jsmethod_lkmeOnResume: ");
        /**
         * <p>解析深度链获取跳转参数，开发者自己实现参数相对应的页面内容</p>
         * <p>通过LinkProperties对象调用getControlParams方法获取自定义参数的HashMap对象,
         * 通过创建的自定义key获取相应的值,用于数据处理。</p>
         */
        simpleInitListener = new LMSimpleInitListener() {
            @Override
            public void onSimpleInitFinished(LMUniversalObject lmUniversalObject, LinkProperties linkProperties, LMError error) {
                try {
                    //true 代表参数返回，false 代表错误或者无参数返回
                    boolean status = false;
                    String error_info = "";
                    JSONObject result = new JSONObject();
                    PrefHelper.Debug("LinkedME", "开始处理deep linking数据... " + this.getClass().getSimpleName());
                    if (error != null) {
                        error_info = "LinkedME初始化失败！";
                        PrefHelper.Debug("LinkedME-Demo", "LinkedME初始化失败. " + error.getMessage());
                    } else {

                        //LinkedME SDK初始化成功，获取跳转参数，具体跳转参数在LinkProperties中，和创建深度链接时设置的参数相同；
                        PrefHelper.Debug("LinkedME-Demo", "LinkedME初始化完成");
                        if (linkProperties != null) {
                            status = true;
                            result.put("lkme_channel", linkProperties.getChannel());
                            result.put("lkme_feature", linkProperties.getFeature());
                            result.put("lkme_tag", linkProperties.getTags());
                            result.put("lkme_stage", linkProperties.getStage());
                            result.put("lkme_link", linkProperties.getLMLink());
                            result.put("lkme_new_user", linkProperties.isLMNewUser());
                            result.put("lkme_controlParam", new JSONObject(linkProperties.getControlParams()));
                        } else {
                            error_info = "无相关参数！";
                        }

                        if (lmUniversalObject != null) {
                            result.put("lmUO_title", lmUniversalObject.getTitle());
                            result.put("lmUO_metadata", new JSONObject(lmUniversalObject.getMetadata()));
                        }

                    }
                    result.put("status", status);
                    result.put("error_info", error_info);
                    moduleContext.success(result, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            //如果消息未处理则会初始化initSession，因此不会每次都去处理数据，不会影响应用原有性能问题
            if (!LinkedME.getInstance().isHandleStatus()) {
                PrefHelper.Debug("LinkedME", "LinkedME +++++++ initSession... " + this.getClass().getSimpleName());
                PrefHelper.Debug("LinkedME", "jsmethod_lkmeOnResume: " + getContext().getIntent().getData());
                //初始化LinkedME实例
                linkedME = LinkedME.getInstance();
                //初始化Session，获取Intent内容及跳转参数
                linkedME.initSession(simpleInitListener, getContext().getIntent().getData(), getContext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void jsmethod_lkmeCloseSession(final UZModuleContext moduleContext) {
        PrefHelper.Debug("LinkedME", "jsmethod_lkmeOnPause: ");
        LinkedME.getInstance().closeSession(null);
    }

    /**
     * <strong>函数</strong><br><br>
     * 该函数映射至Javascript中moduleDemo对象的showAlert函数<br><br>
     * <strong>JS Example：</strong><br>
     * moduleDemo.showAlert(argument);
     *
     * @param moduleContext (Required)
     */
    public void jsmethod_lkmeCreateUrl(final UZModuleContext moduleContext) {
        String channel = moduleContext.optString("lkme_channel", "");
        String feature = moduleContext.optString("lkme_feature", "");
        String tag = moduleContext.optString("lkme_tag", "");
        String stage = moduleContext.optString("lkme_stage", "");
        JSONObject controlParam = moduleContext.optJSONObject("lkme_controlParam");

        /**创建深度链接*/
        //深度链接属性设置
        final LinkProperties properties = new LinkProperties();
        //渠道
        if (!"".equals(channel)) {
            properties.setChannel(channel);  //微信、微博、QQ
        }
        //功能
        if (!"".equals(feature)) {
            properties.setFeature(feature);
        }
        //标签
        if (!"".equals(tag)) {
            properties.addTag(tag);
        }
        //阶段
        if (!"".equals(stage)) {
            properties.setStage(stage);
        }
        //自定义参数,用于在深度链接跳转后获取该数据
        if (controlParam != null) {
            Iterator<String> keys = controlParam.keys();
            while (keys.hasNext()) {
                try {
                    String key = keys.next();
                    properties.addControlParameter(key, controlParam.getString(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        LMUniversalObject universalObject = new LMUniversalObject();

        String lmUO_title = moduleContext.optString("lmUO_title", "");
        if (!"".equals(lmUO_title)) {
            universalObject.setTitle(lmUO_title);
        }

        // Async Link creation example
        universalObject.generateShortUrl(getContext(), properties, new LMLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, LMError error) {
                boolean status = false;
                String lkme_url = "";
                String error_info = "";
                //url为生成的深度链接
                if (error != null) {
                    error_info = error.toString();
                } else if (!TextUtils.isEmpty(url)) {
                    status = true;
                    lkme_url = url;
                }
                JSONObject result = new JSONObject();
                try {
                    result.put("status", status);
                    result.put("lkme_url", lkme_url);
                    result.put("error_info", error_info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(result, true);
            }
        });
    }

}