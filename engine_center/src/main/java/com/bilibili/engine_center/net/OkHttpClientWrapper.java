package com.bilibili.engine_center.net;
import javax.net.SocketFactory;

import okhttp3.CertificatePinner;
import okhttp3.ConnectionPool;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * Wraps a global {@code OkHttpClient} instance. <br />
 */
public final class OkHttpClientWrapper {
    private static volatile OkHttpClientWrapper sInstance;

    public static OkHttpClientWrapper instance() {
        if (sInstance == null) {
            synchronized (OkHttpClientWrapper.class) {
                if (sInstance == null) {
                    sInstance = new OkHttpClientWrapper();
                }
            }
        }
        return sInstance;
    }

    private volatile OkHttpClient.Builder mBuilder;

    private OkHttpClientWrapper() {
    }

    /**
     * Creates new {@code OkHttpClient.Builder} from the wrapped client
     *
     * @return the builder for building OkHttpClient
     */
    public static OkHttpClient.Builder newBuilder() {
        return get().newBuilder();
    }

    /**
     * Get the wrapped {@code OkHttpClient.Builder}
     */
    private OkHttpClient.Builder builder() {
        if (mBuilder == null) {
            synchronized (OkHttpClientWrapper.class) {
                if (mBuilder == null) {
                    mBuilder = new OkHttpClient.Builder();
                }
            }
        }
        return mBuilder;
    }

    /**
     * Gets the wrapped global instance
     *
     * @return the instance
     */
    public static OkHttpClient get() {
        return instance().build();
    }

    /**
     * Wrap an instance as global OkHttpClient
     *
     * @param target the target instance
     * @since 1.0.1
     * @deprecated
     */
    @Deprecated
    public static synchronized void wrap(OkHttpClient target) {
        if (target == null) {
            throw new NullPointerException("WTF?");
        }
        instance().mBuilder = target.newBuilder();
    }

    /**
     * Add global interceptor to wrapped okhttp-client
     *
     * @param interceptor
     * @see OkHttpClient.Builder#addInterceptor(Interceptor)
     * @since 1.0.3
     */
    public OkHttpClientWrapper addInterceptor(Interceptor interceptor) {
        if (!builder().interceptors().contains(interceptor)) {
            builder().addInterceptor(interceptor);
        }
        return this;
    }

    /**
     * Add global network interceptor to wrapped okhttp-client
     *
     * @param interceptor
     * @see OkHttpClient.Builder#addNetworkInterceptor(Interceptor)
     * @since 1.0.4
     */
    public OkHttpClientWrapper addNetworkInterceptor(Interceptor interceptor) {
        if (!builder().networkInterceptors().contains(interceptor)) {
            builder().addNetworkInterceptor(interceptor);
        }
        return this;
    }

    /**
     * Set global dns to wrapped okhttp-client
     *
     * @param dns
     * @see OkHttpClient.Builder#dns(Dns)
     * @since 1.0.3
     */
    public OkHttpClientWrapper dns(Dns dns) {
        builder().dns(dns);
        return this;
    }

    /**
     * Set global Dispatcher to wrapped okhttp-client
     *
     * @param dispatcher
     * @see OkHttpClient.Builder#dispatcher(Dispatcher)
     * @since 1.0.3
     */
    public OkHttpClientWrapper dispatcher(Dispatcher dispatcher) {
        builder().dispatcher(dispatcher);
        return this;
    }

    /**
     * Set global SocketFactory to wrapped okhttp-client
     *
     * @param socketFactory
     * @see OkHttpClient.Builder#socketFactory(SocketFactory)
     * @since 1.0.3
     */
    public OkHttpClientWrapper socketFactory(SocketFactory socketFactory) {
        builder().socketFactory(socketFactory);
        return this;
    }

    /**
     * @since 1.0.3
     */
    public OkHttpClientWrapper certificatePinner(CertificatePinner certificatePinner) {
        builder().certificatePinner(certificatePinner);
        return this;
    }

    /**
     * Set global ConnectionPool to wrapped okhttp-client
     *
     * @param pool
     * @see OkHttpClient.Builder#connectionPool(ConnectionPool)
     * @since 1.0.3
     */
    public OkHttpClientWrapper connectionPool(ConnectionPool pool) {
        builder().connectionPool(pool);
        return this;
    }

    /**
     * Set global CookieJar to wrapped okhttp-client
     *
     * @param cookieJar
     * @see OkHttpClient.Builder#cookieJar(CookieJar)
     * @since 1.1.2
     */
    public OkHttpClientWrapper cookieJar(CookieJar cookieJar) {
        builder().cookieJar(cookieJar);
        return this;
    }

    /**
     * Build a client from wrapped builder
     * @since 1.0.3
     */
    public OkHttpClient build() {
        return builder().build();
    }

}

