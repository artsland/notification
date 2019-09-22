package com.jiqiwuyu.notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Bundle;
import android.util.Log;
import android.preference.PreferenceManager
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result;
import org.json.JSONObject;
import org.json.JSONException


class HookNotificationService : NotificationListenerService() {


    override fun onListenerConnected() {
    }


    /**
     * 发送数据到绑定地址
     */
    private fun postData( url:String, body: JSONObject){
        val httpAsync = url
            .httpPost()
            .jsonBody(body.toString())
            .responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        println(ex.message)
                    }
                    is Result.Success -> {
                        val data = result.get()
                        println(data)
                    }
                }
            }

        httpAsync.join()
    }

    /**
     * 监听状态栏通知消息
     *
     * Todo:
     *   - 1. 微信屏蔽了通知服务监听？ 启动微信时，监听不到消息
     *   - 2. 进程被杀死后，此方法失效？Fixed
     */
    override  fun onNotificationPosted( sbn: StatusBarNotification  ){

        Log.d("HookNotificationService","onNotificationPosted() called");
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val url = sharedPref.getString(getString(R.string.url_key), "Not Set").toString()
        val extras:Bundle = sbn.getNotification().extras;
        val json = JSONObject()
        val keys = extras.keySet()
        json.put("package", sbn.getPackageName());
        for (key in keys) {
            try {
                // json.put(key, bundle.get(key)); see edit below
                json.put(key, JSONObject.wrap(extras.get(key)))
            } catch (e: JSONException) {
                //Handle exception here
            }
        }

        // 提交数据请求
        postData( url, json );
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
    }
}