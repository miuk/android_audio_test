//
// Created by Kenji Miura on 2020/08/08.
//

#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <poll.h>
#include <jni.h>
#include <android/log.h>
#include <assert.h>
#include <errno.h>

// Android log function wrappers
static const char* kTAG = "udpsocket";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, kTAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, kTAG, __VA_ARGS__))

static JavaVM* s_jvm = NULL;

struct context {
    int idx;
    int sock;
    pthread_t recvThread;
    struct sockaddr_storage remote;
    socklen_t slen;
    jclass callbackClass;
    jclass callbackObj;
    int bRunning;
};

static void*
recv_routine(void* arg)
{
    struct context* ctx = (struct context*)arg;
    LOGI("recv_routine start");
    JNIEnv *env;
    jint res = (*s_jvm)->GetEnv(s_jvm, (void**)&env, JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*s_jvm)->AttachCurrentThread(s_jvm, &env, NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return NULL;
        }
    }
    jmethodID callbackFunc = (*env)->GetMethodID(env, ctx->callbackClass, "received", "([S)V");
    if (callbackFunc == NULL) {
        LOGI("received method not found");
    }

    while (ctx->bRunning) {
        struct pollfd ev;
        ev.fd = ctx->sock;
        ev.events = POLLIN;
        int ret = poll(&ev, 1, 100);
        if (ret < 0) {
            LOGI("recv_routine poll failed, sock=%d, %s", ctx->sock, strerror(errno));
            break;
        }
        if (ret == 0) {
            continue;
        }
        struct sockaddr_storage ss;
        socklen_t slen = sizeof(ss);
        char buf[2048];
        ret = (int)recvfrom(ctx->sock, buf, sizeof(buf), 0
                            , (struct sockaddr*)&ss, &slen);
        if (ret < 0) {
            LOGI("recv_routine recvfrom failed, sock=%d, %s", ctx->sock, strerror(errno));
            break;
        }
        if (ret == 0) {
            continue;
        }
        /*LOGI("recv %d\n", ret);*/
        if (ctx->callbackObj != NULL && callbackFunc != NULL) {
            int len = ret / 2;
            jshortArray dsta = (*env)->NewShortArray(env, len);
            jshort* dst = (*env)->GetShortArrayElements(env, dsta, NULL);
            const short* src = (const short*)buf;
            for (int i = 0; i < len; i++)
                dst[i] = src[i];
            (*env)->ReleaseShortArrayElements(env, dsta, dst, 0);
            (*env)->CallVoidMethod(env, ctx->callbackObj, callbackFunc, dsta);
        }
    }
    (*s_jvm)->DetachCurrentThread(s_jvm);
    LOGI("recv_routine finish");
    return NULL;
}

static struct context** ctxs = NULL;
static int num_ctx = 0;

static int
registerContext(struct context* ctx) {
    if (ctxs == NULL) {
        ctxs = (struct context**)malloc(sizeof(struct context*));
        num_ctx = 1;
        ctx->idx= 0;
        ctxs[0] = ctx;
        return 0;
    }
    for (int i = 0; i < num_ctx; i++) {
        if (ctxs[i] == 0) {
            ctx->idx = i;
            ctxs[i] = ctx;
            return i;
        }
    }
    ctx->idx = num_ctx;
    num_ctx *= 2;
    ctxs = (struct context**)realloc(ctxs, sizeof(struct context*) * num_ctx);
    for (int i = ctx->idx; i < num_ctx; i++)
        ctxs[i] = NULL;
    ctxs[ctx->idx] = ctx;
    return ctx->idx;
}

static struct context* getContext(int idx) {
    if (idx < 0 || idx >= num_ctx)
        return NULL;
    struct context *ctx = ctxs[idx];
    return ctx;
}

static void freeContext(struct context* ctx)
{
    int idx = ctx->idx;
    free(ctx);
    if (idx >= 0 && idx < num_ctx) {
        ctxs[idx] = NULL;
    }
}

static struct context*
openUDPSocket(const char* remoteHost, int remotePort, int localPort)
{
    struct addrinfo* res = NULL;
    struct addrinfo hints;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = PF_INET;
    hints.ai_socktype = SOCK_DGRAM;
    hints.ai_flags = AI_PASSIVE;
    char portstr[32];
    sprintf(portstr, "%d", localPort);
    int err = getaddrinfo(NULL, portstr, &hints, &res);
    if (err != 0) {
        printf("getaddrinfo failed: %s\n", gai_strerror(err));
        return NULL;
    }
    int sock = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (sock < 0) {
        perror("socket");
        freeaddrinfo(res);
        return NULL;
    }
    int opt = 1;
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    if (bind(sock, res->ai_addr, res->ai_addrlen) < 0) {
        freeaddrinfo(res);
        close(sock);
        return NULL;
    }
    freeaddrinfo(res);
    sprintf(portstr, "%d", remotePort);
    err = getaddrinfo(remoteHost, portstr, &hints, &res);
    if (err != 0) {
        printf("getaddrinfo failed: %s\n", gai_strerror(err));
        close(sock);
        return NULL;
    }

    struct context* ctx = (struct context*)malloc(sizeof(struct context));
    memset(ctx, 0, sizeof(struct context));
    ctx->sock = sock;
    memcpy(&ctx->remote, res->ai_addr, res->ai_addrlen);
    ctx->slen = res->ai_addrlen;
    freeaddrinfo(res);
    registerContext(ctx);
    ctx->bRunning = 1;
    pthread_create(&ctx->recvThread, NULL, recv_routine, ctx);
    return ctx;
}

static int
sendUDPDatagram(struct context* ctx, const short* data, int len)
{
    ssize_t ret = sendto(ctx->sock, data, sizeof(short) * len, 0, (struct sockaddr*)&ctx->remote, ctx->slen);
    /*printf("send %d %zd\n", len, ret);*/
    return (int)ret;
}

static void
closeUDPSocket(struct context* ctx)
{
    ctx->bRunning = 0;
    close(ctx->sock);
    void* arg = NULL;
    pthread_join(ctx->recvThread, &arg);
}

JNIEXPORT jint JNICALL
Java_com_example_audiotest_SendRecv_openUDPSocket(JNIEnv* env, jobject thiz, jstring remoteHost, jint remotePort, jint myPort)
{
    const char* cRemoteHost = (*env)->GetStringUTFChars(env, remoteHost, NULL);
    struct context* ctx = openUDPSocket(cRemoteHost, remotePort, myPort);
    (*env)->ReleaseStringUTFChars(env, remoteHost, cRemoteHost);
    if (ctx == NULL)
        return -1;
    /*LOGI("openUDPSocket %s, %d, %d, sock=%d", cRemoteHost, remotePort, myPort, ret);*/
    jclass clz = (*env)->GetObjectClass(env, thiz);
    ctx->callbackClass = (*env)->NewGlobalRef(env, clz);
    ctx->callbackObj = (*env)->NewGlobalRef(env, thiz);
    return ctx->idx;
}

JNIEXPORT jint JNICALL
Java_com_example_audiotest_SendRecv_sendUDPDatagram(JNIEnv* env, jobject thiz, jint idx, jshortArray srca, jint len)
{
    struct context* ctx = getContext(idx);
    if (ctx == NULL)
        return -1;
    jshort* src = (*env)->GetShortArrayElements(env, srca, NULL);
    int ret = sendUDPDatagram(ctx, src, len);
    (*env)->ReleaseShortArrayElements(env, srca, src, JNI_ABORT);
    /*LOGI("sendUDPDatagram %d %d %d", len, sock, ret);*/
    return ret;
}

JNIEXPORT void JNICALL
Java_com_example_audiotest_SendRecv_closeUDPSocket(JNIEnv* env, jobject thiz, jint idx)
{
    struct context* ctx = getContext(idx);
    if (ctx == NULL)
        return;
    closeUDPSocket(ctx);
    if (ctx->callbackClass != NULL) {
        (*env)->DeleteGlobalRef(env, ctx->callbackClass);
    }
    if (ctx->callbackObj != NULL) {
        (*env)->DeleteGlobalRef(env, ctx->callbackObj);
    }
    freeContext(ctx);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved)
{
    LOGI("JNI_OnLoad");
    JNIEnv* env;
    s_jvm = vm;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR; // JNI version not supported.
    }
    return  JNI_VERSION_1_6;
}
