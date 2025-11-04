//两个都是vs让加的
#define _CRT_SECURE_NO_WARNINGS
#include "pch.h"

#include <stdio.h>
#include <iostream>

//上一个文件说过了
#include "jni.h"
#include "jvmti.h"
#include "jni_md.h"


jvmtiEnv* jvmti_env_global;//env只能取一次，不要反复取新的

//现在在c里写取classloader的原因是因为取到了jvmtienv，hook取jnienv的方法不能用于jvmtienv（我不会）
jobject GetMCClassLoader(jvmtiEnv* jvmti_env) {
    jint count;
    jthread* threads;
    jvmti_env->GetAllThreads(&count, &threads);
    for (int i = 0; i < count; i++) {
        jthread thread = threads[i];
        jvmtiThreadInfo threadInfo;
        jvmti_env->GetThreadInfo(thread, &threadInfo);
        if (threadInfo.context_class_loader != NULL) {
            if (strcmp(threadInfo.name, "Render thread") == 0) {
                return threadInfo.context_class_loader;
            }
        }
    }
    return NULL;
}

//说过了
jclass LoadClass(JNIEnv* jni_env, const char* ClassName, jobject TargetCL) {
    jmethodID loadClass = jni_env->GetMethodID(jni_env->GetObjectClass(TargetCL), "loadClass",
        "(Ljava/lang/String;)Ljava/lang/Class;");
    return (jclass)jni_env->CallObjectMethod(TargetCL, loadClass, jni_env->NewStringUTF(ClassName));
}

//这个方法的声明是jdk内定的，详情：https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#ClassFileLoadHook
void ProcessHookedClassFile(jvmtiEnv* jvmti_env, JNIEnv* jni_env,
    jclass captured_class, jobject its_own_classloader, const char* its_name,
    jobject protection_domain, jint its_data_len, const unsigned char* its_data,
    jint* replace_with_len, unsigned char** replace_with) {

    if (captured_class == NULL)return;
    if (its_own_classloader == NULL)return;
    if (its_name == NULL)return;

    //这段发布时应该删除
    /*std::cout << "[ProcessHookedClassFile]" << std::endl;
    std::cout << its_name << std::endl;*/

    //下面都抄的法国开挂哥Lefraudeur还有他的Mujina的
    jbyteArray old = jni_env->NewByteArray(its_data_len);
    jni_env->SetByteArrayRegion(old, 0, its_data_len, (jbyte*)its_data);

    //这个重定义已定义的类的流程基本是：在Java层用System.load()加载这个dll，然后JNI_OnLoad是第一个被自动调用运行的，先取到jvmtienv，然后用jvmti打开jvm的一些选项，比如说启用jvm内置的类文件加载勾子，我们需要的就是这个东西，把钩子设置上我们的处理函数然后启用勾子，然后就结束，返回Java层，然后这个时候钩子就已经启用了，任何正在加载的类，或者被要求重加载的类都会开始被我们捕获了，然后我们在处理函数里还又用JNI Call回Java层，然后由Java层决定这个类要不要改，然后返回native层，只有这样我们才能拿到一个类的byte[]，然后刚说到返回Java层，就是返回到System.load()的结束，我们在下一行很快就调用了javaCallNative()，并传一个Class对象（刚好jvmti->RetransformClasses()就要求Class对象），看名字就知道这是从Java层进到native层的函数，进到哪里？进到最下面的那个Java_goose_Goose_javaCallNative，为什么名字要这样起是因为jvm限制不然找不到（这个名字可以用javac -h . 源码文件.java生成），Goose.java最下面不是有个public native static void的方法就叫javaCallNative吗，这两个是匹配的，然后这个函数会调用jvmti->RetransformClasses(1, &arg1)，这个函数的意思是要求一个类重新加载，因为我们要找的类它早在启动游戏时就已经加载过了，然后参数1和&arg1是什么意思呢？其实这个函数要求传一个数组的长度和一个数组，在C里，数组和指针没区别，直接取地址传进去，而1就是长度，本来它是要求传数组的，但是我们分多次一个一个来也行
    jclass ModifierClass = LoadClass(jni_env, "LzgwVJZW02ifWYTO.Y",
        GetMCClassLoader(jvmti_env));//我们这个类已经进MC的ClassLoader里了，所以又要拿一遍MC的ClassLoader
    jmethodID ModifierMethod = jni_env->GetStaticMethodID(ModifierClass, "nativeCallJava",
        "(Ljava/lang/Class;Ljava/lang/ClassLoader;Ljava/lang/String;[B)[B");//靠后面这一大串是叫Java类型简写还是叫JVM方法签名来着的，很简单，几分钟就可以上手，记得L不要漏;分号
    jbyteArray result = (jbyteArray)jni_env->CallStaticObjectMethod(ModifierClass, ModifierMethod, captured_class,
        its_own_classloader,
        jni_env->NewStringUTF(its_name), old);

    jsize new_size = jni_env->GetArrayLength(result);
    unsigned char* new_ = nullptr;
    jvmti_env->Allocate(new_size, &new_);
    jni_env->GetByteArrayRegion(result, 0, new_size, (jbyte*)new_);
    *replace_with_len = new_size;
    *replace_with = new_;
}

void SetJVMTIClassFileLoadHook(jvmtiEnv* jvmti_env) {
    jvmtiCapabilities capas{};
    capas.can_generate_all_class_hook_events = JVMTI_ENABLE;
    capas.can_retransform_any_class = JVMTI_ENABLE;
    capas.can_retransform_classes = JVMTI_ENABLE;
    capas.can_redefine_any_class = JVMTI_ENABLE;
    capas.can_redefine_classes = JVMTI_ENABLE;
    jvmti_env->AddCapabilities(&capas);
    jvmtiEventCallbacks callbacks{};
    callbacks.ClassFileLoadHook = ProcessHookedClassFile;
    jvmti_env->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));
    jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, 0);
}

JNIEXPORT jint JNI_OnLoad(JavaVM* java_vm, void* reserved) {
    java_vm->GetEnv((void**)&jvmti_env_global, JVMTI_VERSION);
    SetJVMTIClassFileLoadHook(jvmti_env_global);
    return JNI_VERSION_10;
}

extern "C" {
    JNIEXPORT void JNICALL Java_LzgwVJZW02ifWYTO_Y_javaCallNative(JNIEnv* jni_env, jclass caller, jclass arg1) {
        jvmti_env_global->RetransformClasses(1, &arg1);
    }
    JNIEXPORT void
        JNICALL Java_LzgwVJZW02ifWYTO_Y_redefineClasses(JNIEnv* jni_env, jclass caller, jclass target, jbyteArray data) {
        jvmtiClassDefinition classDef;
        classDef.klass = target;
        classDef.class_byte_count = jni_env->GetArrayLength(data);
        classDef.class_bytes = (unsigned char*)jni_env->GetByteArrayElements(data, NULL);
        jvmti_env_global->RedefineClasses(1, &classDef);
    }
}