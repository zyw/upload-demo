package cn.v5cn.upload.demo.client;

import cn.v5cn.upload.demo.client.util.Crc32;

import java.io.File;
import java.io.IOException;

public final class FormUploader {

    private final String url;
    private final File file;
    private final byte[] data;
    private final String mime;
    private StringMap params;
    private Client client;
    private String fileName;
    private String name;

    /**
     * 构建一个表单上传字节数组的对象
     */
    public FormUploader(Client client,String url, byte[] data, StringMap params,String mime) {
        this(client,url,data, null,null,null, params, mime);
    }
    /**
     * 构建一个表单上传字节数组的对象
     */
    public FormUploader(Client client,String url, byte[] data,String name,String fileName, StringMap params,String mime) {
        this(client,url,data, null,name,fileName, params, mime);
    }

    /**
     * 构建一个表单上传字节数组的对象
     */
    public FormUploader(Client client,String url, byte[] data,String name,String fileName) {
        this(client,url,data, null,name,fileName, new StringMap(), Client.DefaultMime);
    }

    /**
     * 构建一个表单上传文件的对象
     */
    public FormUploader(Client client,String url,File file, StringMap params,String mime) {
        this(client, url, null, file,null,null, params, mime);
    }

    private FormUploader(Client client,String url, byte[] data, File file,String name,String fileName, StringMap params,String mime) {
        this.client = client;
        this.url = url;
        this.file = file;
        this.data = data;
        this.fileName = fileName;
        this.name = name;
        this.params = params;
        this.mime = mime;
    }

    /**
     * 同步上传文件
     */
    public Response upload() throws UploadIOException {
        buildParams();
        if (data != null) {
            return client.multipartPost(url, params, name == null?"file":name, fileName, data,
                    mime, new StringMap());
        }
        return client.multipartPost(url, params, name == null?"file":name, fileName, file,
                mime, new StringMap());
    }

    /**
     * 异步上传文件
     */
    public void asyncUpload(final UpCompletionHandler handler) throws IOException {
        buildParams();
        if (data != null) {
            client.asyncMultipartPost(url, params, "file", fileName,
                    data, mime, new StringMap(), new AsyncCallback() {
                        @Override
                        public void complete(Response r) {
                            handler.complete(r);
                        }
                    });
            return;
        }
        client.asyncMultipartPost(url, params, "file", fileName,
                file, mime, new StringMap(), new AsyncCallback() {
                    @Override
                    public void complete(Response r) {
                        handler.complete(r);
                    }
                });
    }

    private void buildParams() throws UploadIOException {
        if (file != null) {
            fileName = file.getName();
        }
        if (fileName == null || fileName.trim().length() == 0) {
            fileName = "fileName";
        }

        long crc32 = 0;
        if (file != null) {
            try {
                crc32 = Crc32.file(file);
            } catch (IOException e) {
                throw new UploadIOException(e);
            }
        } else {
            crc32 = Crc32.bytes(data);
        }
        if(params == null) {
            params = new StringMap();
        }
        params.put("crc32", "" + crc32);

    }
}
