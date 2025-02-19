package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.bumptech.glide.util.Synthetic;

/** A class that can safely recycle recursive resources. */
class ResourceRecycler {
  private boolean isRecycling;
  private final Handler handler =
      new Handler(Looper.getMainLooper(), new ResourceRecyclerCallback());

  // TODO Glide生命周期变化时调用-onStop()
  synchronized void recycle(Resource<?> resource, boolean forceNextFrame) {
    //正在释放或有子资源，发送handler进行释放
    if (isRecycling || forceNextFrame) {
      // If a resource has sub-resources, releasing a sub resource can cause it's parent to be synchronously evicted which leads to a recycle loop when the parent releases it's children.
      // synchronously evicted which leads to a recycle loop when the parent releases it's children.
      // Posting breaks this loop.
      handler.obtainMessage(ResourceRecyclerCallback.RECYCLE_RESOURCE, resource).sendToTarget();
    } else {
      isRecycling = true;
      //释放-bitmap的话使用BitmapResource放在BitmapPool
      resource.recycle();
      isRecycling = false;
    }
  }

  private static final class ResourceRecyclerCallback implements Handler.Callback {
    static final int RECYCLE_RESOURCE = 1;

    @Synthetic
    ResourceRecyclerCallback() {}

    @Override
    public boolean handleMessage(Message message) {
      if (message.what == RECYCLE_RESOURCE) {
        Resource<?> resource = (Resource<?>) message.obj;
        resource.recycle();
        return true;
      }
      return false;
    }
  }
}
