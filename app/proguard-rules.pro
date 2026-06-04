# smbj
-keep class com.hierynomus.** { *; }
-keep class com.hierynomus.smbj.** { *; }
-dontwarn com.hierynomus.**

# sshj
-keep class net.schmizz.** { *; }
-keep class com.hierynomus.sshj.** { *; }
-dontwarn net.schmizz.**

# Commons Net
-keep class org.apache.commons.net.** { *; }

# BouncyCastle (transitive dep of smbj and sshj)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# javax.el (dependency of smbj/sshj)
-dontwarn javax.el.BeanELResolver
-dontwarn javax.el.ELContext
-dontwarn javax.el.ELResolver
-dontwarn javax.el.ExpressionFactory
-dontwarn javax.el.FunctionMapper
-dontwarn javax.el.ValueExpression
-dontwarn javax.el.VariableMapper
