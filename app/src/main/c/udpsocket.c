//
// Created by Kenji Miura on 2020/08/08.
//

#include "udpsocket.h"

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

static pthread_t s_recvThread = 0;
static int s_sock = -1;

static JavaVM* s_jvm = NULL;
static jclass   callbackClass = NULL;
static jobject  callbackObj = NULL;
static JNIEnv* s_env = NULL;

static void*
recv_routine(void* arg)
{
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
    jmethodID callbackFunc = (*env)->GetMethodID(env, callbackClass, "received", "([S)V");
    if (callbackFunc == NULL) {
        LOGI("received method not found");
    }

    for (;;) {
        struct pollfd ev;
        ev.fd = s_sock;
        ev.events = POLLIN;
        int ret = poll(&ev, 1, 100);
        if (ret < 0) {
            LOGI("recv_routine poll failed, sock=%d, %s", s_sock, strerror(errno));
            break;
        }
        if (ret == 0) {
            continue;
        }
        struct sockaddr_storage ss;
        socklen_t slen = sizeof(ss);
        char buf[2048];
        ret = (int)recvfrom(s_sock, buf, sizeof(buf), 0
                            , (struct sockaddr*)&ss, &slen);
        if (ret < 0) {
            LOGI("recv_routine recvfrom failed, sock=%d, %s", s_sock, strerror(errno));
            break;
        }
        if (ret == 0) {
            continue;
        }
        /*LOGI("recv %d\n", ret);*/
        if (callbackObj != NULL && callbackFunc != NULL) {
            int len = ret / 2;
            jshortArray dsta = (*env)->NewShortArray(env, len);
            jshort* dst = (*env)->GetShortArrayElements(env, dsta, NULL);
            const short* src = (const short*)buf;
            for (int i = 0; i < len; i++)
                dst[i] = src[i];
            (*env)->ReleaseShortArrayElements(env, dsta, dst, 0);
            (*env)->CallVoidMethod(env, callbackObj, callbackFunc, dsta);
        }
    }
    (*s_jvm)->DetachCurrentThread(s_jvm);
    LOGI("recv_routine finish");
    return NULL;
}

int
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
        return -1;
    }
    int sock = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (sock < 0) {
        perror("socket");
        freeaddrinfo(res);
        return -1;
    }
    int opt = 1;
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    if (bind(sock, res->ai_addr, res->ai_addrlen) < 0) {
        freeaddrinfo(res);
        close(sock);
        return -1;
    }
    freeaddrinfo(res);
    sprintf(portstr, "%d", remotePort);
    err = getaddrinfo(remoteHost, portstr, &hints, &res);
    if (err != 0) {
        printf("getaddrinfo failed: %s\n", gai_strerror(err));
        close(sock);
        return -1;
    }
    if (connect(sock, res->ai_addr, res->ai_addrlen) < 0) {
        perror("connect");
        freeaddrinfo(res);
        close(sock);
        return -1;
    }

    s_sock = sock;
    pthread_create(&s_recvThread, NULL, recv_routine, NULL);

    return sock;
}

int
sendUDPDatagram(int sock, const short* data, int len)
{
    ssize_t ret = send(sock, data, sizeof(short) * len, 0);
    /*printf("send %d %zd\n", len, ret);*/
    return (int)ret;
}

void
closeUDPSocket(int sock)
{
    close(sock);
    if (s_sock == sock) {
        s_sock = -1;
    }
    if (s_recvThread != 0) {
        void* arg = NULL;
        pthread_join(s_recvThread, &arg);
        s_recvThread = 0;
    }
}

JNIEXPORT jint JNICALL
Java_com_example_audiotest_SendRecv_openUDPSocket(JNIEnv* env, jobject thiz, jstring remoteHost, jint remotePort, jint myPort)
{
    const char* cRemoteHost = (*env)->GetStringUTFChars(env, remoteHost, NULL);
    int ret = openUDPSocket(cRemoteHost, remotePort, myPort);
    /*LOGI("openUDPSocket %s, %d, %d, sock=%d", cRemoteHost, remotePort, myPort, ret);*/
    (*env)->ReleaseStringUTFChars(env, remoteHost, cRemoteHost);
    jclass clz = (*env)->GetObjectClass(env, thiz);
    callbackClass = (*env)->NewGlobalRef(env, clz);
    callbackObj = (*env)->NewGlobalRef(env, thiz);
    s_env = env;
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_example_audiotest_SendRecv_sendUDPDatagram(JNIEnv* env, jobject thiz, jint sock, jshortArray srca, jint len)
{
    jshort* src = (*env)->GetShortArrayElements(env, srca, NULL);
    int ret = sendUDPDatagram(sock, src, len);
    (*env)->ReleaseShortArrayElements(env, srca, src, JNI_ABORT);
    /*LOGI("sendUDPDatagram %d %d %d", len, sock, ret);*/
    return ret;
}

JNIEXPORT void JNICALL
Java_com_example_audiotest_SendRecv_closeUDPSocket(JNIEnv* env, jobject thiz, jint sock)
{
    closeUDPSocket(sock);
    if (callbackClass != NULL) {
        (*env)->DeleteGlobalRef(env, callbackClass);
        callbackClass = NULL;
    }
    if (callbackObj != NULL) {
        (*env)->DeleteGlobalRef(env, callbackObj);
        callbackObj = NULL;
    }
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
