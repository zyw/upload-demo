package cn.v5cn.upload.demo.client;

import cn.v5cn.upload.demo.client.util.Constants;
import cn.v5cn.upload.demo.client.util.Crc32;
import cn.v5cn.upload.demo.client.util.ResumeBlockInfo;
import cn.v5cn.upload.demo.client.util.UrlSafeBase64;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public final class StreamUploader {
    private final StringMap params;
    private final String mime;
    private final ArrayList<String> contexts;
    private final Client client;
    private final byte[] blockBuffer;
    private final InputStream stream;
    private long size;
    private String url;
    private int retryMax;

    public StreamUploader(Client client,String url,InputStream stream,
                   StringMap params, String mime, Integer retryMax) {
        this.client = client;
        this.url = url;
        this.params = params;
        this.mime = mime == null ? Client.DefaultMime : mime;
        this.contexts = new ArrayList<>();
        this.blockBuffer = new byte[Constants.BLOCK_SIZE];
        this.stream = stream;
        this.retryMax = retryMax == null ? 3 : retryMax;
    }

    public Response upload() throws UploadIOException {
        try {
            return upload0();
        } finally {
            close();
        }
    }

    private Response upload0() throws UploadIOException {
        long uploaded = 0;
        int ret = 0;
        boolean retry = false;
        boolean eof = false;

        while (size == 0 && !eof) {
            int bufferIndex = 0;
            int blockSize = 0;

            //try to read the full BLOCK or until the EOF
            while (ret != -1 && bufferIndex != blockBuffer.length) {
                try {
                    blockSize = blockBuffer.length - bufferIndex;
                    ret = stream.read(blockBuffer, bufferIndex, blockSize);
                } catch (IOException e) {
                    close();
                    throw new UploadIOException(e);
                }
                if (ret != -1) {
                    //continue to read more
                    //advance bufferIndex
                    bufferIndex += ret;
                    if (ret == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    eof = true;
                    //file EOF here, trigger outer while-loop finish
                    size = uploaded + bufferIndex;
                }
            }

            //mkblk request
            long crc = Crc32.bytes(blockBuffer, 0, bufferIndex);
            Response response = null;
            UploadIOException temp = null;
            try {
                response = makeBlock(blockBuffer, bufferIndex);
            } catch (UploadIOException e) {
                if (e.response == null || e.response.needRetry()) {
                    retry = true;
                    temp = e;
                } else {
                    close();
                    throw e;
                }
            }
            if (!retry) {
                ResumeBlockInfo blockInfo0 = response.jsonToObject(ResumeBlockInfo.class);
                if (blockInfo0.crc32 != crc) {
                    retry = true;
                    temp = new UploadIOException(new Exception("block's crc32 is not match"));
                }
            }
            if (retry) {
                if (retryMax > 0) {
                    retryMax--;
                    try {
                        response = makeBlock(blockBuffer, bufferIndex);
                        retry = false;
                    } catch (UploadIOException e) {
                        close();
                        throw e;
                    }
                } else {
                    throw temp;
                }
            }
            ResumeBlockInfo blockInfo = response.jsonToObject(ResumeBlockInfo.class);
            contexts.add(blockInfo.ctx);
            uploaded += bufferIndex;
        }
        close();

        try {
            return makeFile();
        } catch (UploadIOException e) {
            try {
                return makeFile();
            } catch (UploadIOException e1) {
                throw e1;
            }
        }
    }

    private Response makeBlock(byte[] block, int blockSize) throws UploadIOException {
        return post(this.url, block, 0, blockSize);
    }

    private void close() {
        try {
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String fileUrl() {
        final StringBuilder b = new StringBuilder(this.url);
        if (params != null) {
            params.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    b.append("/");
                    b.append(key);
                    b.append("/");
                    b.append(UrlSafeBase64.encodeToString("" + value));
                }
            });
        }
        return b.toString();
    }

    private Response makeFile() throws UploadIOException {
        String url = fileUrl();
        String s = StringUtils.join(contexts, ",");
        return post(url, StringUtils.utf8Bytes(s));
    }

    private Response post(String url, byte[] data) throws UploadIOException {
        return client.post(url, data, new StringMap());
    }

    private Response post(String url, byte[] data, int offset, int size) throws UploadIOException {
        return client.post(url, data, offset, size, new StringMap(),
                Client.DefaultMime);
    }
}
