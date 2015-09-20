
package org.zarroboogs.util.net;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lib.org.zarroboogs.weibo.login.javabean.UploadPicResult;

import org.zarroboogs.asyncokhttpclient.AsyncOKHttpClient;
import org.zarroboogs.devutils.Constaces;
import org.zarroboogs.utils.PatternUtils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class UploadHelper {
	
	private String mCookie = "";
    private Context mContext;
    private AsyncHttpClient mAsyncHttpClient;
    private AsyncOKHttpClient mAsyncOKHttpClient = new AsyncOKHttpClient();

    public UploadHelper(Context context, AsyncHttpClient asyncHttpClient) {
        this.mContext = context;
        this.mAsyncHttpClient = asyncHttpClient;
    }

    public static final int MSG_UPLOAD = 0x1000;
    public static final int MSG_UPLOAD_DONE = 0x1001;
    public static final int MSG_UPLOAD_FAILED = 0x1002;

    public int mHasUploadFlag = -1;

    private String mPids = "";
    private List<String> mNeedToUpload = new ArrayList<String>();

    private OnUpFilesListener mOnUpFilesListener;
    private String mWaterMark;

    public interface OnUpFilesListener {
        void onUpSuccess(String pids);

        void onUpLoadFailed();
    }

    public void uploadFiles(String waterMark, List<String> files, OnUpFilesListener listener, String cookie) {
        this.mNeedToUpload.addAll(files);
        this.mOnUpFilesListener = listener;
        mHasUploadFlag = 0;
        this.mWaterMark = waterMark;
        this.mCookie = cookie;
        mHandler.sendEmptyMessage(MSG_UPLOAD);
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPLOAD: {
                    asyncUploadFile(mWaterMark, mNeedToUpload.get(mHasUploadFlag), mCookie);
                    break;
                }
                case MSG_UPLOAD_DONE: {
                    if (mOnUpFilesListener != null) {
                        mOnUpFilesListener.onUpSuccess(mPids);
                    }
                    break;
                }
                case MSG_UPLOAD_FAILED: {
                    if (mOnUpFilesListener != null) {
                        mOnUpFilesListener.onUpLoadFailed();
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    private void asyncUploadFile(String waterMark, String filePath, String cookie){

        String markUrl = "http://picupload.service.weibo.com/interface/pic_upload.php?" + "app=miniblog&data=1" + waterMark
                + "&mime=image/png&ct=0.2805887470021844";
        Headers.Builder headers = new Headers.Builder();
        headers.add("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.add("Accept-Encoding", "gzip,deflate");
        headers.add("Accept-Language", "en-US,en;q=0.8");
        headers.add("Cache-Control", "max-age=0");
        headers.add("Connection", "keep-alive");
        headers.add("Content-Type","application/octet-stream");
        headers.add("Host", "picupload.service.weibo.com");
        headers.add("Origin", "http://weibo.com");
        headers.add("User-Agent", Constaces.User_Agent);
        headers.add("Referer","http://tjs.sjs.sinajs.cn/open/widget/static/swf/MultiFilesUpload.swf?version=1411256448572");
        headers.add("Cookie",cookie);

        File uploadFile = new File(filePath);
        mAsyncOKHttpClient.asyncPostFile(markUrl, headers.build(), "application/octet-stream", uploadFile, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                mHandler.sendEmptyMessage(MSG_UPLOAD_FAILED);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String result = response.body().string();
                Log.d("uploadFile ", result);
                Gson mGson = new Gson();
                UploadPicResult ur = mGson.fromJson(PatternUtils.preasePid(result), UploadPicResult.class);
                if (ur != null) {
                    if (TextUtils.isEmpty(ur.getPid())) {
                        mHandler.sendEmptyMessage(MSG_UPLOAD_FAILED);
                        return;
                    }
                    Log.d("uploadFile   pid: ", ur.getPid());
                    mHasUploadFlag++;
                    mPids += ur.getPid() + ",";
                    if (mHasUploadFlag < mNeedToUpload.size()) {
                        mHandler.sendEmptyMessage(MSG_UPLOAD);
                    } else {
                        mHandler.sendEmptyMessage(MSG_UPLOAD_DONE);
                    }
                } else {
                    mHandler.sendEmptyMessage(MSG_UPLOAD_DONE);
                }
            }
        });
    }
}
