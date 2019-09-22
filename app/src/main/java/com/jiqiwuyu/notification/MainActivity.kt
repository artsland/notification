package com.jiqiwuyu.notification

import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.content.ActivityNotFoundException
import android.content.Context
import androidx.core.content.ContextCompat
import android.app.ActivityManager;
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Log
import android.service.notification.NotificationListenerService



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    /**
     * 检查是否有通知访问权限
     */
    private fun checkPermistion():Boolean {

        val pkgName:String = getPackageName()
        val setting = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        println("已有通知权限的应用 ${pkgName} ${setting}")
        if ( !TextUtils.isEmpty(setting) ) {
            val names = setting.split(":")
            for( name in names ) {
                val com = ComponentName.unflattenFromString(name)
                if ( com != null  ) {
                    if ( TextUtils.equals(pkgName, com.packageName )) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 打开通知权限配置界面
     */
    private fun openSetting():Boolean {

        try {
            val intent: Intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true
        } catch ( e : ActivityNotFoundException ) {
            Toast.makeText(applicationContext, "系统不支持此方法", Toast.LENGTH_SHORT).show()
            return false;
        }
    }


    /**
     * 检查服务是否正在运行
     */
    private fun serviceIsRunning():Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.jiqiwuyu.notification.HookNotificationService" == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * 重新绑定服务
     */
    private fun requestRebind() {
        val serviceClass = HookNotificationService::class.java
        val com = ComponentName(this, serviceClass)
        packageManager.setComponentEnabledSetting( com, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP )
        packageManager.setComponentEnabledSetting( com, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP )
    }

    /**
     * 解绑服务
     */
    private fun requestUnbind() {
        val serviceClass = HookNotificationService::class.java
        val com = ComponentName(this, serviceClass)
        packageManager.setComponentEnabledSetting( com, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP )
    }


    /**
     * 启动服务
     */
    fun startServiceClick( view: View ) {

        Log.d("HookManager","startServiceClick() called");

        // 通知读取权限检查
        if (!this.checkPermistion() ) {
            this.openSetting()
            return
        }

        // 读取接收数据地址
        val urlText = findViewById<EditText>(R.id.input_url)
        var url:String = urlText.text.toString()
        if ( url == "" ) {
            url = getString(R.string.url_default)
        }

        // 保存URL地址
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        with (sharedPref.edit()) {
            putString(getString(R.string.url_key), url)
            commit()
        }

        val setUrl = sharedPref.getString(getString(R.string.url_key), "Not Set")
        Log.d("HookManager ", "setUrl=${setUrl}");

        if ( !this.serviceIsRunning() ) {

            //  启动服务操作
            val serviceClass = HookNotificationService::class.java
            val serviceIntent = Intent(applicationContext, serviceClass)
            startService(serviceIntent)

            // 重新绑定服务
            this.requestRebind()

            if ( !this.serviceIsRunning() ) {
                Toast.makeText(applicationContext, "服务启动失败", Toast.LENGTH_SHORT).show();
                return;
            }

        } else {
            val serviceClass = HookNotificationService::class.java
            NotificationListenerService.requestRebind(ComponentName(this, serviceClass))
            Toast.makeText(applicationContext, "服务已经启动", Toast.LENGTH_SHORT).show()
        }


        // 读取状态控制器
        val statusText = findViewById<TextView>(R.id.text_status)
        statusText.setText("服务已启动")
        statusText.setTextColor( ContextCompat.getColor(applicationContext,R.color.colorSuccess))

        // 读取消息通知
        val messageText = findViewById<TextView>(R.id.text_message)
        messageText.setText(TextUtils.concat("系统通知将同步推送至:","\n${url}"))
    }

    /**
     * 关闭服务
     * todo
     *  - 服务无法关闭 BUG
     */
    fun stopServiceClick( view: View ) {

        // 关闭服务操作
        if ( this.serviceIsRunning() ) {

            this.requestUnbind();

            val serviceClass = HookNotificationService::class.java
            val serviceIntent = Intent(applicationContext, serviceClass)
            stopService(serviceIntent)

            if ( this.serviceIsRunning() ) {
                Toast.makeText(applicationContext, "服务关闭失败", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(applicationContext, "服务尚未启动", Toast.LENGTH_SHORT).show()
        }

        // 读取状态控制器
        val statusText = findViewById<TextView>(R.id.text_status)
        statusText.setText("服务未开启")
        statusText.setTextColor( ContextCompat.getColor(applicationContext,R.color.colorAccent))

        // 读取消息通知
        val messageText = findViewById<TextView>(R.id.text_message)
        messageText.setText("")

    }
}
