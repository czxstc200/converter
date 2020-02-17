package cn.edu.bupt.client;

public interface PromiseCallback<I> {

    public void onSuccess(I response);

    public void onFailed(I response);
}
