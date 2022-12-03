package com.example.photogallery;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbNailDownloader<T> extends HandlerThread {
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final String TAG = "ThumbNailDownloader";
    public LruCache<String, Bitmap> mCache;
    private Context mContext;

    public boolean mHasQuit = false;
    private Handler mRequestHandler;

    public ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap();
    private Handler mResponseHandler;
    public ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T t, Bitmap bitmap);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        this.mThumbnailDownloadListener = listener;
    }

    public ThumbNailDownloader(Handler handler, Context context) {
        super(TAG);
        this.mResponseHandler = handler;
        this.mContext = context;
        this.mCache = new LruCache<String, Bitmap>(((((ActivityManager) context.getSystemService("activity")).getMemoryClass() * 1024) * 1024) / 8) {
            /* access modifiers changed from: protected */
            public int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);
        if (url == null) {
            this.mRequestMap.remove(target);
            return;
        }
        Bitmap bitmap = this.mCache.get(url);
        this.mRequestMap.put(target, url);
        if (bitmap == null || this.mRequestMap.get(target) != url) {
            this.mRequestHandler.obtainMessage(0, target).sendToTarget();
            return;
        }
        this.mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
        this.mRequestMap.remove(target);
    }

    /* access modifiers changed from: protected */
    public void onLooperPrepared() {
        this.mRequestHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    T target = (T) msg.obj;
                    Log.i(ThumbNailDownloader.TAG, "Got a request for URL: " + ((String) ThumbNailDownloader.this.mRequestMap.get(target)));
                    ThumbNailDownloader.this.handleRequest(target);
                }
            }
        };
    }

    /* access modifiers changed from: private */
    public void handleRequest(final T target) {
        try {
            final String url = (String) this.mRequestMap.get(target);
            if (url != null) {
                byte[] byteArray = new FlickrFetchr().getUrlBytes(url);
                final Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                Log.i(TAG, "Bitmap created");
                this.mResponseHandler.post(new Runnable() {
                    public void run() {
                        if (ThumbNailDownloader.this.mRequestMap.get(target) == url && !ThumbNailDownloader.this.mHasQuit) {
                            ThumbNailDownloader.this.mRequestMap.remove(target);
                            ThumbNailDownloader.this.mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                            ThumbNailDownloader.this.mCache.put(url, bitmap);
                        }
                    }
                });
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void clearQueue() {
        this.mRequestHandler.removeMessages(0);
        this.mRequestMap.clear();
    }

    public boolean quit() {
        this.mHasQuit = true;
        return super.quit();
    }
}
