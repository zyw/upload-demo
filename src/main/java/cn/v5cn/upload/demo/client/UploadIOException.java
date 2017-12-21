package cn.v5cn.upload.demo.client;

import java.io.IOException;

public class UploadIOException extends IOException {
    public final Response response;
    private String error;


    public UploadIOException(Response response) {
        super(response != null ? response.getInfo() : null);
        this.response = response;
    }

    public UploadIOException(Exception e) {
        this(e, null);
    }

    public UploadIOException(Exception e, String msg) {
        super(msg, e);
        this.response = null;
        this.error = msg;
    }

    public String url() {
        return response.url();
    }

    public int code() {
        return response == null ? -1 : response.statusCode;
    }

    public String error() {
        if (error != null) {
            return error;
        }
        if (response == null || response.statusCode / 100 == 2 || !response.isJson()) {
            return null;
        }
        Error e = null;
        try {
            e = response.jsonToObject(Error.class);
        } catch (UploadIOException e1) {
            e1.printStackTrace();
        }
        error = e == null ? "" : "";
        return error;
    }
}
