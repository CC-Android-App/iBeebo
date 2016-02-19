
package org.zarroboogs.util.net;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lib.org.zarroboogs.weibo.login.javabean.UploadPicResult;

import org.zarroboogs.devutils.Constaces;
import org.zarroboogs.http.AsyncHttpHeaders;
import org.zarroboogs.http.AsyncHttpPostFile;
import org.zarroboogs.http.AsyncHttpRequest;
import org.zarroboogs.http.AsyncHttpResponse;
import org.zarroboogs.http.AsyncHttpResponseHandler;
import org.zarroboogs.utils.PatternUtils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;


public class UploadHelper {

    private String mCookie = "";
    private Context mContext;
    private AsyncHttpRequest mAsyncHttpRequest = new AsyncHttpRequest();

    public UploadHelper(Context context) {
        this.mContext = context;
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
        asyncUploadFile(mWaterMark, mNeedToUpload.get(mHasUploadFlag), mCookie);
    }


    private void asyncUploadFile(String waterMark, String filePath, String cookie) {

        String markUrl = "http://picupload.service.weibo.com/interface/pic_upload.php?" + "app=miniblog&data=1" + waterMark
                + "&mime=image/png&ct=0.2805887470021844";

        AsyncHttpHeaders simpleHeaders = new AsyncHttpHeaders();
        simpleHeaders.addAccept("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        simpleHeaders.addAcceptEncoding("gzip,deflate");
        simpleHeaders.addAcceptLanguage("en-US,en;q=0.8");
        simpleHeaders.addConnection("keep-alive");
        simpleHeaders.addContentType("application/octet-stream");
        simpleHeaders.addHost("picupload.service.weibo.com");
        simpleHeaders.addOrigin("http://weibo.com");
        simpleHeaders.addUserAgent(Constaces.User_Agent);
        simpleHeaders.addReferer("http://tjs.sjs.sinajs.cn/open/widget/static/swf/MultiFilesUpload.swf?version=1411256448572");
        simpleHeaders.addCookie(cookie);
        simpleHeaders.add("Cache-Control", "max-age=0");


        File uploadFile = new File(filePath);

        Map<String, String> headers = new HashMap<>();

        mAsyncHttpRequest.post(markUrl, simpleHeaders, new AsyncHttpPostFile("application/octet-stream", uploadFile), new AsyncHttpResponseHandler() {
            @Override
            public void onFailure(IOException e) {
                if (mOnUpFilesListener != null) {
                    mOnUpFilesListener.onUpLoadFailed();
                }
            }

            @Override
            public void onSuccess(AsyncHttpResponse response) {
                String result = response.getBody();
                Log.d("uploadFile ", result);
                Gson mGson = new Gson();
                UploadPicResult ur = mGson.fromJson(PatternUtils.preasePid(result), UploadPicResult.class);
                if (ur != null) {
                    if (TextUtils.isEmpty(ur.getPid())) {
                        if (mOnUpFilesListener != null) {
                            mOnUpFilesListener.onUpLoadFailed();
                        }
                        return;
                    }
                    Log.d("uploadFile   pid: ", ur.getPid());
                    mHasUploadFlag++;
                    mPids += ur.getPid() + ",";
                    if (mHasUploadFlag < mNeedToUpload.size()) {
                        asyncUploadFile(mWaterMark, mNeedToUpload.get(mHasUploadFlag), mCookie);
                    } else {
                        if (mOnUpFilesListener != null) {
                            mOnUpFilesListener.onUpSuccess(mPids);
                        }
                    }
                } else {
                    if (mOnUpFilesListener != null) {
                        mOnUpFilesListener.onUpSuccess(mPids);
                    }
                }
            }
        });
    }
}
