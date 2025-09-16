package me.yohom.foundation_fluttify

import android.app.Activity
import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.StandardMethodCodec
import io.flutter.plugin.platform.PlatformViewRegistry
import me.yohom.foundation_fluttify.android.app.ActivityHandler
import me.yohom.foundation_fluttify.android.app.ApplicationHandler
import me.yohom.foundation_fluttify.android.app.NotificationHandler
import me.yohom.foundation_fluttify.android.app.PendingIntentHandler
import me.yohom.foundation_fluttify.android.content.BroadcastReceiverHandler
import me.yohom.foundation_fluttify.android.content.ContextHandler
import me.yohom.foundation_fluttify.android.content.IntentFilterHandler
import me.yohom.foundation_fluttify.android.content.IntentHandler
import me.yohom.foundation_fluttify.android.graphics.BitmapHandler
import me.yohom.foundation_fluttify.android.graphics.PointHandler
import me.yohom.foundation_fluttify.android.location.LocationHandler
import me.yohom.foundation_fluttify.android.os.BundleHandler
import me.yohom.foundation_fluttify.android.util.PairHandler
import me.yohom.foundation_fluttify.android.view.SurfaceHolderHandler
import me.yohom.foundation_fluttify.android.view.SurfaceViewHandler
import me.yohom.foundation_fluttify.android.view.ViewGroupHandler
import me.yohom.foundation_fluttify.android.view.ViewHandler
import me.yohom.foundation_fluttify.android.widget.ImageViewHandler
import me.yohom.foundation_fluttify.core.FluttifyMessageCodec
import me.yohom.foundation_fluttify.core.PlatformService
import me.yohom.foundation_fluttify.java.io.FileHandler
import me.yohom.foundation_fluttify.platform_view.android_opengl_GLSurfaceViewFactory
import me.yohom.foundation_fluttify.platform_view.android_view_SurfaceViewFactory
import me.yohom.foundation_fluttify.platform_view.android_widget_FrameLayoutFactory


// The stack that exists on the Dart side for a method call is enabled only when the MethodChannel passing parameters are limited
val STACK = mutableMapOf<String, Any>()

// Container for Dart side random access objects
val HEAP = mutableMapOf<String, Any>()

// whether enable log or not
var enableLog: Boolean = true

// method channel for foundation
lateinit var gMethodChannel: MethodChannel

class FoundationFluttifyPlugin : FlutterPlugin, ActivityAware, MethodCallHandler {
    private var applicationContext: Context? = null
    private var activity: Activity? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var platformViewRegistry: PlatformViewRegistry? = null
    private var binaryMessenger: BinaryMessenger? = null

    companion object {}

    override fun onMethodCall(methodCall: MethodCall, methodResult: Result) {
        val rawArgs = methodCall.arguments ?: mapOf<String, Any>()
        methodCall.method.run {
            when {
                startsWith("android.app.Application::") -> ApplicationHandler(methodCall.method, rawArgs, methodResult, applicationContext)
                startsWith("android.app.Activity::") -> ActivityHandler(methodCall.method, rawArgs, methodResult, activity)
                startsWith("android.app.PendingIntent::") -> PendingIntentHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.app.Notification::") -> NotificationHandler(methodCall.method, rawArgs, methodResult, activity)
                startsWith("android.os.Bundle::") -> BundleHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.content.Intent::") -> IntentHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.content.Context::") -> ContextHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.content.BroadcastReceiver::") -> BroadcastReceiverHandler(methodCall.method, rawArgs, binaryMessenger, methodResult)
                startsWith("android.content.IntentFilter::") -> IntentFilterHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.graphics.Bitmap::") -> BitmapHandler(methodCall.method, rawArgs, methodResult, activity)
                startsWith("android.graphics.Point::") -> PointHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.location.Location::") -> LocationHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.util.Pair::") -> PairHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.view.View::") -> ViewHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.view.SurfaceView::") -> SurfaceViewHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.view.SurfaceHolder::") -> SurfaceHolderHandler(binaryMessenger, methodCall.method, rawArgs, methodResult)
                startsWith("android.view.ViewGroup::") -> ViewGroupHandler(methodCall.method, rawArgs, methodResult)
                startsWith("android.widget.ImageView::") -> ImageViewHandler(methodCall.method, rawArgs, methodResult, activity)
                startsWith("java.io.File::") -> FileHandler(methodCall.method, rawArgs, methodResult)
                startsWith("PlatformService::") -> PlatformService(methodCall.method, rawArgs as Map<String, Any>, methodResult, activityBinding, pluginBinding)
                else -> methodResult.notImplemented()
            }
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        pluginBinding = binding
        platformViewRegistry = binding.platformViewRegistry
        binaryMessenger = binding.binaryMessenger

        gMethodChannel = MethodChannel(
                binding.binaryMessenger,
                "com.fluttify/foundation_method",
                StandardMethodCodec(FluttifyMessageCodec())
        )
        gMethodChannel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        pluginBinding = null
        activity = null
        activityBinding = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        activityBinding = binding
        platformViewRegistry?.registerViewFactory("me.yohom/foundation_fluttify/android.view.SurfaceView", android_view_SurfaceViewFactory(binaryMessenger))
        platformViewRegistry?.registerViewFactory("me.yohom/foundation_fluttify/android.widget.FrameLayout", android_widget_FrameLayoutFactory())
        platformViewRegistry?.registerViewFactory("me.yohom/foundation_fluttify/android.opengl.GLSurfaceView", android_opengl_GLSurfaceViewFactory())
    }

    override fun onDetachedFromActivity() {
        activity = null
        activityBinding = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        activityBinding = binding
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
        activityBinding = null
    }
}