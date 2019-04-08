package com.bilibili.engine_center.net;

import com.bilibili.engine_center.util.Const;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.fastjson.FastJsonConverterFactory;


public class ServiceGenerator {
    public static OkClientConfig sOkClientConfig = new OkClientConfig(); // default okClient config
    private static volatile OkHttpClient sBaseClient;
    private static volatile Retrofit sRetrofit;
    /**
     * create api service
     */
    public static <T> T createService(Class<T> service) {
        return getRetrofit(service).create(service);
    }

    private static <T> Retrofit getRetrofit(Class<T> service) {
        if (sRetrofit == null) {
            synchronized (ServiceGenerator.class) {
                if (sRetrofit == null) {
                    OkHttpClient okClient = getOkHttpClient();
                    sRetrofit = new Retrofit.Builder()
                            .baseUrl(extractBaseUrl(service))
                            .addConverterFactory(FastJsonConverterFactory.create())
                            .client(okClient)
                            .build();
                }
            }
        }
        return sRetrofit;
    }

    private static OkHttpClient getOkHttpClient() {
        if (sBaseClient == null) {
            synchronized (ServiceGenerator.class) {
                if (sBaseClient == null) {
                    OkHttpClient.Builder builder = OkHttpClientWrapper.newBuilder();
                    builder.connectTimeout(sOkClientConfig.connectionTimeout(), TimeUnit.MILLISECONDS);
                    builder.readTimeout(sOkClientConfig.readTimeout(), TimeUnit.MILLISECONDS);
                    builder.writeTimeout(sOkClientConfig.writeTimeout(), TimeUnit.MILLISECONDS);
                    builder.interceptors().addAll(sOkClientConfig.interceptors());
                    builder.networkInterceptors().addAll(sOkClientConfig.networkInterceptors());
                    builder.eventListenerFactory(OkHttpEventListener.FACTORY);
                    sBaseClient = builder.build();
                }
            }
        }
        return sBaseClient;
    }

    private static HttpUrl extractBaseUrl(Class<?> service) {
        for (Annotation anno : service.getDeclaredAnnotations()) {
            if (anno instanceof BaseUrl) {
                String baseUrl = ((BaseUrl) anno).value();
                HttpUrl httpUrl = HttpUrl.parse(baseUrl);
                if (httpUrl == null) {
                    throw new IllegalArgumentException("Illegal URL: " + baseUrl);
                }
                List<String> pathSegments = httpUrl.pathSegments();
                if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
                    throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl);
                }
                return httpUrl;
            }
        }
        throw new IllegalArgumentException("Annotation @BaseUrl is needed!");
    }

    public static class OkClientConfig {
        private long connectionTimeout = Const.TIMEOUT_DEFAULT * 2;
        private long readTimeout = Const.TIMEOUT_DEFAULT * 2;
        private long writeTimeout = Const.TIMEOUT_DEFAULT * 2;
        private List<Interceptor> interceptors = new ArrayList<>(5);
        private List<Interceptor> networkInterceptors = new ArrayList<>(5);

        public OkClientConfig setConnectTimeout(int timeMs) {
            connectionTimeout = timeMs;
            return this;
        }

        public OkClientConfig setReadTimeout(int timeMs) {
            readTimeout = timeMs;
            return this;
        }

        public OkClientConfig setWriteTimeout(int timeMs) {
            writeTimeout = timeMs;
            return this;
        }

        public OkClientConfig addInterceptor(Interceptor interceptor) {
            interceptors.add(interceptor);
            return this;
        }

        public OkClientConfig addNetworkInterceptor(Interceptor interceptor) {
            networkInterceptors.add(interceptor);
            return this;
        }

        /**
         * get
         **/
        public long connectionTimeout() {
            return connectionTimeout;
        }

        public long readTimeout() {
            return readTimeout;
        }

        public long writeTimeout() {
            return writeTimeout;
        }

        public List<Interceptor> interceptors() {
            return interceptors;
        }

        public List<Interceptor> networkInterceptors() {
            return networkInterceptors;
        }
    }
}
