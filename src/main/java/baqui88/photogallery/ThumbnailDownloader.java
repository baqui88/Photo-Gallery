package baqui88.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// all handler are created in HandlerThread will be attached to a particular Looper
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    // will store a reference to the Handler responsible for queueing download requests as messages onto background thread
    private Handler mRequestHandler;
    //  thread-safe version of HashMap
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    // this handler is passed from main thread then it will be attached to main Looper
    private Handler mResponseHandler;
    // this listener will be accessed via mResponseHandler.post --> respond on main thread
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        // https://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T obj = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(obj));
                    handleRequest(obj);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T obj, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(obj);
        } else {
            mRequestMap.put(obj, url);
            // create new task and add to MessageQueue
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, obj).sendToTarget();
        }
    }
    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    private void handleRequest(final T obj) {
        try {
            final String url = mRequestMap.get(obj);
            if (url == null) {
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            // respond to main thread
            mResponseHandler.post(new Runnable() {
                public void run() {
                    if ( mHasQuit) {
                        return;
                    }
                    mRequestMap.remove(obj);
                    mThumbnailDownloadListener.onThumbnailDownloaded(obj, bitmap);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }
}
