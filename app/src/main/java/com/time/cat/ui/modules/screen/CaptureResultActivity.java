package com.time.cat.ui.modules.screen;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.microsoft.projectoxford.vision.contract.OCR;
import com.time.cat.R;
import com.time.cat.data.Constants;
import com.time.cat.data.model.APImodel.ImageUpload;
import com.time.cat.ui.base.BaseActivity;
import com.time.cat.ui.modules.activity.DiyOcrKeyActivity;
import com.time.cat.ui.modules.activity.TimeCatActivity;
import com.time.cat.ui.modules.activity.WebActivity;
import com.time.cat.ui.modules.editor.RichTextEditorActivity;
import com.time.cat.ui.widgets.dialog.DialogFragment;
import com.time.cat.ui.widgets.dialog.SimpleDialog;
import com.time.cat.util.OcrAnalyser;
import com.time.cat.util.UrlCountUtil;
import com.time.cat.util.override.LogUtil;
import com.time.cat.util.override.ToastUtil;
import com.time.cat.util.view.ColorUtil;
import com.time.cat.util.view.ViewUtil;
import com.timecat.commonjar.contentProvider.SPHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CaptureResultActivity extends BaseActivity {
    public static final String HTTP_IMAGE_BAIDU_COM = "http://image.baidu.com/wiseshitu?rn=30&appid=0&tag=1&isMobile=1&";
    int alpha = SPHelper.getInt(Constants.TIMECAT_ALPHA, 100);
    int lastPickedColor = SPHelper.getInt(Constants.TIMECAT_DIY_BG_COLOR, Color.parseColor("#03A9F4"));
    private ImageView capturedImage;
    private Bitmap bitmap;
    private TextView share, save, add, ocr, timecat, search;
    private TextView ocrResult;
    private RelativeLayout ocrResultRL;

    private void initWindow() {
        WindowManager.LayoutParams localLayoutParams = getWindow().getAttributes();
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(localDisplayMetrics);
        localLayoutParams.width = ((int) (localDisplayMetrics.widthPixels * 0.99D));
        localLayoutParams.gravity = 17;
        localLayoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(localLayoutParams);
        getWindow().setGravity(17);
        getWindow().getAttributes().windowAnimations = R.anim.anim_scale_in;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        init();
    }

    private void init() {
        alpha = SPHelper.getInt(Constants.TIMECAT_ALPHA, 100);
        lastPickedColor = SPHelper.getInt(Constants.TIMECAT_DIY_BG_COLOR, Color.parseColor("#03A9F4"));

        CardView cardView = new CardView(this);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_capture_result, null, false);
        cardView.setRadius(ViewUtil.dp2px(10));

        int value = (int) ((alpha / 100.0f) * 255);
        cardView.setCardBackgroundColor(Color.argb(value, Color.red(lastPickedColor), Color.green(lastPickedColor), Color.blue(lastPickedColor)));
        cardView.addView(view);

        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.trans));
        setContentView(cardView);
        initWindow();

        Intent intent = getIntent();
        String fileName = intent.getStringExtra(ScreenCapture.FILE_NAME);
        if (fileName == null) {
            ToastUtil.e(R.string.screen_capture_fail);
            finish();
            return;
        }
        LogUtil.d("CaptureResultActivity", fileName);
        File capturedFile = new File(fileName);
        if (capturedFile.exists()) {
            bitmap = BitmapFactory.decodeFile(fileName);
        } else {
            ToastUtil.e(R.string.screen_capture_fail);
            finish();
            return;
        }
        ocrResult = findViewById(R.id.ocr_result);
        ocrResultRL = findViewById(R.id.ocr_result_rl);
        capturedImage = findViewById(R.id.captured_pic);
        share = findViewById(R.id.share);
        save = findViewById(R.id.save);
        add = findViewById(R.id.add_task);
        ocr = findViewById(R.id.recognize);
        timecat = findViewById(R.id.timecat);
        search = findViewById(R.id.search);

        ocrResultRL.setVisibility(View.GONE);

        WindowManager.LayoutParams localLayoutParams = getWindow().getAttributes();
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(localDisplayMetrics);
        if (bitmap.getHeight() > localDisplayMetrics.heightPixels * 2 / 3 || 1.0 * bitmap.getHeight() / bitmap.getWidth() >= 1.2) {
            LinearLayout container = findViewById(R.id.container);
            container.setOrientation(LinearLayout.HORIZONTAL);

            capturedImage.setMaxWidth(localDisplayMetrics.widthPixels / 2);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) capturedImage.getLayoutParams();
            if (bitmap.getWidth() > localDisplayMetrics.widthPixels / 2) {
                layoutParams.width = bitmap.getWidth() * 2 / 5;
                layoutParams.height = bitmap.getHeight() * 2 / 5;
            } else {
                layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
            }
            capturedImage.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) ocrResultRL.getLayoutParams();
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            ocrResultRL.setLayoutParams(layoutParams);

        }


        capturedImage.setImageBitmap(bitmap);

        save.setOnClickListener(v -> {
            try {
                UrlCountUtil.onEvent(UrlCountUtil.CLICK_CAPTURERESULT_SAVE);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/", format.format(new Date()) + ".jpg");
                file.getParentFile().mkdirs();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
                Intent intent1 = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri uri = Uri.fromFile(file);
                intent1.setData(uri);
                sendBroadcast(intent1);
                ToastUtil.ok(getResources().getString(R.string.save_sd_card));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                ToastUtil.e(R.string.save_sd_card_fail);
            }

        });

        add.setOnClickListener(v -> {
            UrlCountUtil.onEvent(UrlCountUtil.CLICK_CAPTURERESULT_SAVE);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
            String s = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/"
                    + format.format(new Date()) + ".jpg";
            //保存
            try {
                File file = new File(s);
                file.getParentFile().mkdirs();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
                Intent intent12 = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri uri = Uri.fromFile(file);
                intent12.setData(uri);
                sendBroadcast(intent12);
                ToastUtil.ok(getResources().getString(R.string.save_sd_card));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                ToastUtil.e(R.string.save_sd_card_fail);
            }
            //把文件名发过去
            Intent intent2RichTextEditorActivity = new Intent(CaptureResultActivity.this, RichTextEditorActivity.class);
            intent2RichTextEditorActivity.putExtra(RichTextEditorActivity.TO_SAVE_IMG, s);
            startActivity(intent2RichTextEditorActivity);
        });

        share.setOnClickListener(v -> {
            try {
                UrlCountUtil.onEvent(UrlCountUtil.CLICK_CAPTURERESULT_SHARE);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/", format.format(new Date()) + ".jpg");
                file.getParentFile().mkdirs();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
                shareMsg("分享给", "截图", "来自timecat的截图", file.getAbsolutePath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        search.setOnClickListener(v -> {
            ToastUtil.i(R.string.upload_img);
            OcrAnalyser.getInstance().uploadImage(CaptureResultActivity.this, fileName, new OcrAnalyser.ImageUploadCallBack() {
                @Override
                public void onSuccess(ImageUpload imageUpload) {
                    if (imageUpload != null && imageUpload.getData() != null && !TextUtils.isEmpty(imageUpload.getData().getUrl())) {

                        String url = HTTP_IMAGE_BAIDU_COM + "queryImageUrl=" + imageUpload.getData().getUrl() + "&querySign=4074500770,3618317556&fromProduct= ";
                        Intent intent13 = new Intent();
                        intent13.putExtra("url", url);
                        intent13.setClass(CaptureResultActivity.this, WebActivity.class);
                        startActivity(intent13);
                    } else {
                        ToastUtil.e(R.string.upload_img_fail);
                    }

                }

                @Override
                public void onFail(Throwable throwable) {
                    ToastUtil.e(throwable.getMessage());
                }
            });
        });
        ocr.setOnClickListener(v -> {
            if (SPHelper.getInt(Constants.OCR_TIME, 0) == Constants.OCR_TIME_TO_ALERT) {
                showBeyondQuoteDialog();
                int time = SPHelper.getInt(Constants.OCR_TIME, 0) + 1;
                SPHelper.save(Constants.OCR_TIME, time);
                return;
            }
            ToastUtil.i(R.string.ocr_recognize);
            UrlCountUtil.onEvent(UrlCountUtil.CLICK_CAPTURERESULT_OCR);
            OcrAnalyser.getInstance().analyse(CaptureResultActivity.this, fileName, true, new OcrAnalyser.CallBack() {
                @Override
                public void onSuccess(OCR ocr) {
                    ocrResultRL.setVisibility(View.VISIBLE);
                    ocrResult.setText(OcrAnalyser.getInstance().getPassedMiscSoftText(ocr));
                    ocrResult.setTextColor(ColorUtil.getPropertyTextColor(lastPickedColor, alpha));
                }

                @Override
                public void onFail(Throwable throwable) {
                    if (SPHelper.getString(Constants.DIY_OCR_KEY, "").equals("")) {
                        ToastUtil.e(getResources().getString(R.string.ocr_useup_toast));
                    } else {
                        ToastUtil.e(throwable.getMessage());
                    }
                }
            });
        });

        timecat.setOnClickListener(v -> {
            UrlCountUtil.onEvent(UrlCountUtil.CLICK_CAPTURERESULT_TIMECAT);
            if (TextUtils.isEmpty(ocrResult.getText())) {
                if (SPHelper.getInt(Constants.OCR_TIME, 0) == Constants.OCR_TIME_TO_ALERT) {
                    showBeyondQuoteDialog();
                    int time = SPHelper.getInt(Constants.OCR_TIME, 0) + 1;
                    SPHelper.save(Constants.OCR_TIME, time);
                    return;
                }
                ToastUtil.i(R.string.ocr_recognize);
                OcrAnalyser.getInstance().analyse(CaptureResultActivity.this, fileName, true, new OcrAnalyser.CallBack() {
                    @Override
                    public void onSuccess(OCR ocr) {
                        if (!TextUtils.isEmpty(ocrResult.getText())) {
                            Intent intent14 = new Intent(CaptureResultActivity.this, TimeCatActivity.class);
                            intent14.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent14.putExtra(TimeCatActivity.TO_SPLIT_STR, ocrResult.getText());
                            startActivity(intent14);
                            finish();
                        } else {
                            Intent intent14 = new Intent(CaptureResultActivity.this, TimeCatActivity.class);
                            intent14.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent14.putExtra(TimeCatActivity.TO_SPLIT_STR, OcrAnalyser.getInstance().getPassedMiscSoftText(ocr));
                            startActivity(intent14);
                            finish();
                        }
                    }

                    @Override
                    public void onFail(Throwable throwable) {

                        if (SPHelper.getString(Constants.DIY_OCR_KEY, "").equals("")) {
                            ToastUtil.e(getResources().getString(R.string.ocr_useup_toast));
                        } else {
                            ToastUtil.e(throwable.getMessage());
                        }
                    }
                });
            } else {
                if (!TextUtils.isEmpty(ocrResult.getText())) {
                    Intent intent14 = new Intent(CaptureResultActivity.this, TimeCatActivity.class);
                    intent14.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent14.putExtra(TimeCatActivity.TO_SPLIT_STR, ocrResult.getText().toString());
                    startActivity(intent14);
                    finish();
                }
            }

        });

        ocrResult.setOnLongClickListener(v -> {
            UrlCountUtil.onEvent(UrlCountUtil.CLICK_CAPTURERESULT_OCRRESULT);
            if (!TextUtils.isEmpty(ocrResult.getText())) {
                Intent intent15 = new Intent(CaptureResultActivity.this, TimeCatActivity.class);
                intent15.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent15.putExtra(TimeCatActivity.TO_SPLIT_STR, ocrResult.getText().toString());
                startActivity(intent15);
                finish();
            }
            return true;
        });

        timecat.setTextColor(ColorUtil.getPropertyTextColor(lastPickedColor, alpha));
        search.setTextColor(ColorUtil.getPropertyTextColor(lastPickedColor, alpha));
        share.setTextColor(ColorUtil.getPropertyTextColor(lastPickedColor, alpha));
        save.setTextColor(ColorUtil.getPropertyTextColor(lastPickedColor, alpha));
        ocr.setTextColor(ColorUtil.getPropertyTextColor(lastPickedColor, alpha));
        add.setTextColor(ColorUtil.getPropertyTextColor(lastPickedColor, alpha));
    }

    private void showBeyondQuoteDialog() {
        SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.SimpleDialogLight) {

            @Override
            public void onPositiveActionClicked(DialogFragment fragment) {
                // 这里是保持开启
                UrlCountUtil.onEvent(UrlCountUtil.CLICK_SHOW_BEYOND_QUOTE);
                super.onPositiveActionClicked(fragment);
                Intent intent = new Intent();
                intent.setClass(CaptureResultActivity.this, DiyOcrKeyActivity.class);
                startActivity(intent);

            }

            @Override
            public void onDismiss(DialogInterface dialog) {
                super.onCancel(dialog);
            }
        };
        builder.message(this.getString(R.string.ocr_quote_beyond_time)).positiveAction(this.getString(R.string.free_use));
        DialogFragment fragment = DialogFragment.newInstance(builder);
        fragment.show(getSupportFragmentManager(), null);
    }


    /**
     * 分享功能
     *
     * @param activityTitle Activity的名字
     * @param msgTitle      消息标题
     * @param msgText       消息内容
     * @param imgPath       图片路径，不分享图片则传null
     */
    public void shareMsg(String activityTitle, String msgTitle, String msgText, String imgPath) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        if (imgPath == null || imgPath.equals("")) {
            intent.setType("text/plain");
            // 纯文本
        } else {
            File f = new File(imgPath);
            if (f != null && f.exists() && f.isFile()) {
                intent.setType("image/jpg");
                Uri u = Uri.fromFile(f);
                intent.putExtra(Intent.EXTRA_STREAM, u);
            }
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, msgTitle);
        intent.putExtra(Intent.EXTRA_TEXT, msgText);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, activityTitle));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
