package cn.edu.bupt.client;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;

@Data
public class Promise {

    private long id;

    private PromiseCallback<FullHttpResponse> callback;

    public Promise(long id, PromiseCallback<FullHttpResponse> callback) {
        this.id = id;
        this.callback = callback;
    }

    public void onComplete(FullHttpResponse httpResponse) {
        if (httpResponse.status().equals(HttpResponseStatus.OK)) {
            callback.onSuccess(httpResponse);
        } else {
            callback.onFailed(httpResponse);
        }
    }
}
