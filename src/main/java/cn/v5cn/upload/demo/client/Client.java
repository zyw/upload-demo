package cn.v5cn.upload.demo.client;


import cn.v5cn.upload.demo.client.util.Constants;
import okhttp3.*;
import okio.BufferedSink;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Client {
    public static final String ContentTypeHeader = "Content-Type";
    public static final String DefaultMime = "application/octet-stream";
    public static final String JsonMime = "application/json";
    public static final String FormMime = "application/x-www-form-urlencoded";
    private final OkHttpClient httpClient;

    /**
     * 构建一个默认配置的 HTTP Client 类
     */
    public Client() {
        this(Constants.CONNECT_TIMEOUT, Constants.READ_TIMEOUT, Constants.WRITE_TIMEOUT,
                Constants.DISPATCHER_MAX_REQUESTS, Constants.DISPATCHER_MAX_REQUESTS_PER_HOST,
                Constants.CONNECTION_POOL_MAX_IDLE_COUNT, Constants.CONNECTION_POOL_MAX_IDLE_MINUTES);
    }

    /**
     * 构建一个自定义配置的 HTTP Client 类
     */
    public Client(int connTimeout, int readTimeout, int writeTimeout, int dispatcherMaxRequests,
                  int dispatcherMaxRequestsPerHost, int connectionPoolMaxIdleCount,
                  int connectionPoolMaxIdleMinutes) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(dispatcherMaxRequests);
        dispatcher.setMaxRequestsPerHost(dispatcherMaxRequestsPerHost);
        ConnectionPool connectionPool = new ConnectionPool(connectionPoolMaxIdleCount,
                connectionPoolMaxIdleMinutes, TimeUnit.MINUTES);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.dispatcher(dispatcher);
        builder.connectionPool(connectionPool);
        builder.addNetworkInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request();

                okhttp3.Response response = chain.proceed(request);
                IpTag tag = (IpTag) request.tag();
                try {
                    tag.ip = chain.connection().socket().getRemoteSocketAddress().toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    tag.ip = "";
                }
                return response;
            }
        });

        builder.connectTimeout(connTimeout, TimeUnit.SECONDS);
        builder.readTimeout(readTimeout, TimeUnit.SECONDS);
        builder.writeTimeout(writeTimeout, TimeUnit.SECONDS);
        httpClient = builder.build();
    }

    private static String userAgent() {
        String javaVersion = "Java/" + System.getProperty("java.version");
        String os = System.getProperty("os.name") + " "
                + System.getProperty("os.arch") + " " + System.getProperty("os.version");
        String v = "BuzzlyJava/" + Constants.VERSION;
        return v + " (" + os + ") " + javaVersion;
    }

    private static RequestBody create(final MediaType contentType,
                                      final byte[] content, final int offset, final int size) {
        if (content == null) throw new NullPointerException("content == null");

        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return size;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(content, offset, size);
            }
        };
    }

    public Response post(String url, byte[] body, StringMap headers) throws UploadIOException {
        return post(url, body, headers, DefaultMime);
    }

    public Response post(String url, byte[] body, StringMap headers, String contentType) throws UploadIOException {
        RequestBody rbody;
        if (body != null && body.length > 0) {
            MediaType t = MediaType.parse(contentType);
            rbody = RequestBody.create(t, body);
        } else {
            rbody = RequestBody.create(null, new byte[0]);
        }
        return post(url, rbody, headers);
    }

    public Response post(String url, byte[] body, int offset, int size,
                         StringMap headers, String contentType) throws UploadIOException {
        RequestBody rbody;
        if (body != null && body.length > 0) {
            MediaType t = MediaType.parse(contentType);
            rbody = create(t, body, offset, size);
        } else {
            rbody = RequestBody.create(null, new byte[0]);
        }
        return post(url, rbody, headers);
    }

    public Response get(String url) throws UploadIOException {
        return get(url, new StringMap());
    }

    public Response get(String url, StringMap headers) throws UploadIOException {
        Request.Builder requestBuilder = new Request.Builder().get().url(url);
        return send(requestBuilder, headers);
    }



    public Response post(String url, String body, StringMap headers) throws UploadIOException {
        return post(url, StringUtils.utf8Bytes(body), headers, DefaultMime);
    }

    public Response post(String url, StringMap params, StringMap headers) throws UploadIOException {
        final FormBody.Builder f = new FormBody.Builder();
        params.forEach(new StringMap.Consumer() {
            @Override
            public void accept(String key, Object value) {
                f.add(key, value.toString());
            }
        });
        return post(url, f.build(), headers);
    }

    private Response post(String url, RequestBody body, StringMap headers) throws UploadIOException {
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        return send(requestBuilder, headers);
    }

    public cn.v5cn.upload.demo.client.Response multipartPost(String url,
                                                             String name,
                                                             String fileName,
                                                             byte[] fileBody) throws UploadIOException {
        return multipartPost(url, null,name,fileName,fileBody,DefaultMime,null);
    }

    public cn.v5cn.upload.demo.client.Response multipartPost(String url,
                                                             String name,
                                                             String fileName,
                                                             byte[] fileBody,
                                                             String mimeType) throws UploadIOException {
        return multipartPost(url, null,name,fileName,fileBody,mimeType,null);
    }

    public cn.v5cn.upload.demo.client.Response multipartPost(String url,
                                                             StringMap fields,
                                                             String name,
                                                             String fileName,
                                                             byte[] fileBody,
                                                             String mimeType,
                                                             StringMap headers) throws UploadIOException {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        return multipartPost(url, fields, name, fileName, file, headers);
    }

    public cn.v5cn.upload.demo.client.Response multipartPost(String url,
                                                             StringMap fields,
                                                             String name,
                                                             String fileName,
                                                             File fileBody,
                                                             String mimeType,
                                                             StringMap headers) throws UploadIOException {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        return multipartPost(url, fields, name, fileName, file, headers);
    }

    private cn.v5cn.upload.demo.client.Response multipartPost(String url,
                                                              StringMap fields,
                                                              String name,
                                                              String fileName,
                                                              RequestBody file,
                                                              StringMap headers) throws UploadIOException {
        final MultipartBody.Builder mb = new MultipartBody.Builder();
        mb.addFormDataPart(name, fileName, file);

        if(fields != null) {
            fields.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    mb.addFormDataPart(key, value.toString());
                }
            });
        }
        mb.setType(MediaType.parse("multipart/form-data"));
        RequestBody body = mb.build();
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        return send(requestBuilder, headers);
    }

    public cn.v5cn.upload.demo.client.Response send(final Request.Builder requestBuilder, StringMap headers) throws UploadIOException {
        if (headers != null) {
            headers.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    requestBuilder.header(key, value.toString());
                }
            });
        }

        requestBuilder.header("User-Agent", userAgent());
        long start = System.currentTimeMillis();
        okhttp3.Response res = null;
        cn.v5cn.upload.demo.client.Response r;
        double duration = (System.currentTimeMillis() - start) / 1000.0;
        IpTag tag = new IpTag();
        try {
            res = httpClient.newCall(requestBuilder.tag(tag).build()).execute();
        } catch (IOException e) {
            e.printStackTrace();
            throw new UploadIOException(e);
        }
        r = cn.v5cn.upload.demo.client.Response.create(res, tag.ip, duration);
        if (r.statusCode >= 300) {
            throw new UploadIOException(r);
        }

        return r;
    }

    public void asyncSend(final Request.Builder requestBuilder, StringMap headers, final AsyncCallback cb) {
        if (headers != null) {
            headers.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    requestBuilder.header(key, value.toString());
                }
            });
        }

        requestBuilder.header("User-Agent", userAgent());
        final long start = System.currentTimeMillis();
        IpTag tag = new IpTag();
        httpClient.newCall(requestBuilder.tag(tag).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                long duration = (System.currentTimeMillis() - start) / 1000;
                cb.complete(cn.v5cn.upload.demo.client.Response.createError(null, "", duration, e.getMessage()));
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                long duration = (System.currentTimeMillis() - start) / 1000;
                cb.complete(cn.v5cn.upload.demo.client.Response.create(response, "", duration));
            }
        });
    }

    public void asyncPost(String url, byte[] body, int offset, int size,
                          StringMap headers, String contentType, AsyncCallback cb) {
        RequestBody rbody;
        if (body != null && body.length > 0) {
            MediaType t = MediaType.parse(contentType);
            rbody = create(t, body, offset, size);
        } else {
            rbody = RequestBody.create(null, new byte[0]);
        }

        Request.Builder requestBuilder = new Request.Builder().url(url).post(rbody);
        asyncSend(requestBuilder, headers, cb);
    }

    public void asyncMultipartPost(String url,
                                   StringMap fields,
                                   String name,
                                   String fileName,
                                   byte[] fileBody,
                                   String mimeType,
                                   StringMap headers,
                                   AsyncCallback cb) {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        asyncMultipartPost(url, fields, name, fileName, file, headers, cb);
    }

    public void asyncMultipartPost(String url,
                                   StringMap fields,
                                   String name,
                                   String fileName,
                                   File fileBody,
                                   String mimeType,
                                   StringMap headers,
                                   AsyncCallback cb) throws UploadIOException {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        asyncMultipartPost(url, fields, name, fileName, file, headers, cb);
    }

    private void asyncMultipartPost(String url,
                                    StringMap fields,
                                    String name,
                                    String fileName,
                                    RequestBody file,
                                    StringMap headers,
                                    AsyncCallback cb) {
        final MultipartBody.Builder mb = new MultipartBody.Builder();
        mb.addFormDataPart(name, fileName, file);

        fields.forEach(new StringMap.Consumer() {
            @Override
            public void accept(String key, Object value) {
                mb.addFormDataPart(key, value.toString());
            }
        });
        mb.setType(MediaType.parse("multipart/form-data"));
        RequestBody body = mb.build();
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        asyncSend(requestBuilder, headers, cb);
    }

    private static class IpTag {
        public String ip = null;
    }
}
