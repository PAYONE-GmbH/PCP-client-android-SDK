# AndroidJUnitRunner uses app-shared dependencies after app shrinking. These
# rules apply only with -PandroidTestBuildType=release, never to a normal build.
# SDK classes remain subject to their production consumer rules.
-keep class androidx.tracing.** { *; }
-keep class androidx.fragment.** { *; }
-keep class com.payone.pcp_client_android_sdk.R$* { *; }
-keep class kotlin.** { *; }
# Keep the externally invoked test entry points from being inlined into the
# demo. Obfuscation and shrinking of SDK implementation/model fields stay on.
-keepclassmembers,allowobfuscation class com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment {
    public static ** Companion;
}
-keep,allowobfuscation class com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment$Companion { *; }
-keepclassmembers,allowobfuscation class com.payone.pcp_client_android_sdk.cctokenizer.** {
    public <init>(...);
}
