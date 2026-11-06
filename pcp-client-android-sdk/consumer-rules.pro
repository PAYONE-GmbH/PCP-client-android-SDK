# Gson uses the field names of these tokenizer wire models as JSON keys.
-keepclassmembers,allowoptimization class com.payone.pcp_client_android_sdk.cctokenizer.** implements java.io.Serializable {
    <fields>;
    java.lang.Object readResolve();
}
# Gson constructs card details reflectively, including in R8 full mode.
-keep,allowoptimization class com.payone.pcp_client_android_sdk.cctokenizer.CardDetails { *; }
-keepclassmembers class com.payone.pcp_client_android_sdk.cctokenizer.** {
    @android.webkit.JavascriptInterface <methods>;
}
