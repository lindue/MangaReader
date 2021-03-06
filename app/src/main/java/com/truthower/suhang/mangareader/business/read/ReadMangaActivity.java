package com.truthower.suhang.mangareader.business.read;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.truthower.suhang.mangareader.R;
import com.truthower.suhang.mangareader.adapter.ReadMangaAdapter;
import com.truthower.suhang.mangareader.base.BaseActivity;
import com.truthower.suhang.mangareader.bean.LoginBean;
import com.truthower.suhang.mangareader.bean.YoudaoResponse;
import com.truthower.suhang.mangareader.business.tag.TagManagerActivity;
import com.truthower.suhang.mangareader.config.Configure;
import com.truthower.suhang.mangareader.config.ShareKeys;
import com.truthower.suhang.mangareader.listener.JsoupCallBack;
import com.truthower.suhang.mangareader.listener.OnEditResultListener;
import com.truthower.suhang.mangareader.spider.FileSpider;
import com.truthower.suhang.mangareader.spider.SpiderBase;
import com.truthower.suhang.mangareader.utils.SharedPreferencesUtils;
import com.truthower.suhang.mangareader.volley.VolleyCallBack;
import com.truthower.suhang.mangareader.volley.VolleyTool;
import com.truthower.suhang.mangareader.widget.bar.TopBar;
import com.truthower.suhang.mangareader.widget.dialog.MangaDialog;
import com.truthower.suhang.mangareader.widget.dialog.MangaImgEditDialog;
import com.truthower.suhang.mangareader.widget.dialog.OnlyEditDialog;
import com.truthower.suhang.mangareader.widget.shotview.ScreenShot;
import com.truthower.suhang.mangareader.widget.shotview.ShotView;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * /storage/sdcard0/reptile/one-piece
 * <p/>
 * Created by Administrator on 2016/4/4.
 */
public class ReadMangaActivity extends BaseActivity implements OnClickListener {
    private HackyViewPager mangaPager;
    private SpiderBase spider;
    private DiscreteSeekBar seekBar;
    private View showSeekBar;
    private TextView readProgressTv;
    // 截图的view
    private ShotView shotView = null;
    private ReadMangaAdapter adapter;
    private ArrayList<String> pathList = new ArrayList<String>();
    private int historyPosition = 1;
    private int finalPosition = 1;
    private ProgressDialog loadBar;
    private boolean isLocalManga = false;
    private TopBar topBar;
    private OnlyEditDialog searchDialog;
    private MangaImgEditDialog mangaImgEditDialog;
    private ClipboardManager clip;//复制文本用
    private MangaDialog translateResultDialog;
    private String chapterUrl;//线上漫画章节地址
    private String progressSaveKey = "";
    private int toPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        initSpider();
        initUI();
        initProgressBar();
        Intent intent = getIntent();
        pathList = (ArrayList<String>) intent.getSerializableExtra("pathList");
        if (null == pathList || pathList.size() == 0) {
            isLocalManga = false;
            chapterUrl = intent.getStringExtra("chapterUrl");
            doGetWebPics();
        } else {
            if (!pathList.get(0).contains("file://")) {
                //这种情况说明是one shot
                isLocalManga = false;
                toPage = intent.getIntExtra("img_position", 0);
            } else {
                isLocalManga = true;
            }
            refresh();
            if (toPage != 0) {
                mangaPager.setCurrentItem(toPage);
            }
        }
        String currentMangaName = intent.getStringExtra("currentMangaName");
        topBar.setTitle(currentMangaName);

        if (!SharedPreferencesUtils.getBooleanSharedPreferencesData(this, ShareKeys.CLOSE_TUTORIAL, false)) {
            MangaDialog dialog = new MangaDialog(this);
            dialog.show();
            dialog.setTitle("教程");
            dialog.setMessage("1,点击漫画标题或右上角图标可调出查单词弹窗" +
                    "\n2,点击左上角方块图标可调出截屏查单词弹窗(可用手指划出指定区域翻译,可以避免输入法遮挡)" +
                    "\n3,双击漫画图片可放大" +
                    "\n4,点击屏幕中间稍微靠下位置可调出进度条,可以跳转到指定位置" +
                    "\n5,长按屏幕中间稍微靠下位置可保存或删除当前图片");
        }
    }


    @Override
    protected int getLayoutId() {
        return R.layout.activity_read_manga;
    }

    private void doGetWebPics() {
        loadBar.show();
        spider.getMangaChapterPics(this, chapterUrl, new JsoupCallBack<ArrayList<String>>() {
            @Override
            public void loadSucceed(final ArrayList<String> result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadBar.dismiss();
                        pathList = result;
                        refresh();
                    }
                });
            }

            @Override
            public void loadFailed(String error) {
                loadBar.dismiss();
            }
        });
    }

    private void initSpider() {
        try {
            spider = (SpiderBase) Class.forName
                    ("com.truthower.suhang.mangareader.spider." + Configure.currentWebSite + "Spider").newInstance();
        } catch (ClassNotFoundException e) {
            baseToast.showToast(e + "");
            e.printStackTrace();
        } catch (java.lang.InstantiationException e) {
            baseToast.showToast(e + "");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            baseToast.showToast(e + "");
            e.printStackTrace();
        }
    }

    private void initProgressBar() {
        loadBar = new ProgressDialog(ReadMangaActivity.this);
        loadBar.setCancelable(false);
        loadBar.setMessage("稍等...");

    }

    /**
     * 进度条
     */
    private void initSeekBar() {
        seekBar.setMin(1);
        seekBar.setMax(pathList.size());
        seekBar.setProgress(historyPosition);
        seekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                finalPosition = value;
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if (finalPosition >= 0) {
                    mangaPager.setCurrentItem(finalPosition - 1);
//                    cutSeekBar();
//                seekBar.setVisibility(View.GONE);
                }
            }
        });
    }


    private void refresh() {
        initViewPager();
        initSeekBar();
        readProgressTv.setText(historyPosition + 1 + "/" + pathList.size());
    }


    private void initUI() {
        mangaPager = (HackyViewPager) findViewById(R.id.manga_viewpager);
        seekBar = (DiscreteSeekBar) findViewById(R.id.seekbar);
        showSeekBar = findViewById(R.id.show_seek_bar);
        readProgressTv = (TextView) findViewById(R.id.read_progress_tv);
        readProgressTv.setOnClickListener(this);

        showSeekBar.setOnClickListener(this);
        showSeekBar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                String file = pathList.get(historyPosition);
                if (file.contains("file://")) {
                    file = file.substring(7, file.length());
                    showDeleteDialog(file);
                } else {
                    showSaveDialog(file);
                }
                return true;
            }
        });

        hideBaseTopBar();
        topBar = (TopBar) findViewById(R.id.read_manga_top_bar);
        topBar.setOnTopBarClickListener(new TopBar.OnTopBarClickListener() {
            @Override
            public void onRightClick() {
                showSearchDialog();
            }

            @Override
            public void onTitleClick() {
                showSearchDialog();
            }

            @Override
            public void onLeftClick() {
                try {
                    baseToast.showToast("开始划词翻译");

                    if (shotView == null) {
                        shotView = (ShotView) findViewById(R.id.shot_view);
                        shotView.setL(new ShotView.FinishShotListener() {
                            @Override
                            public void finishShot(Bitmap bp) {
                                showImgEditDialog(bp);
                            }

                        });
                    } else {
                        shotView.setIsRunning(true);
                    }

                    Bitmap bgBitmap = shotView.getBitmap();
                    if (bgBitmap != null) {
                        bgBitmap.recycle();
                    }
                    bgBitmap = ScreenShot.takeScreenShot(ReadMangaActivity.this);
                    shotView.setBitmap(bgBitmap);
                    shotView.setVisibility(View.VISIBLE);
                } catch (Exception e) {

                }
            }
        });
        topBar.setTopBarLongClickLister(new TopBar.OnTopBarLongClickListener() {
            @Override
            public void onLeftLongClick() {

            }

            @Override
            public void onRightLongClick() {

            }

            @Override
            public void onTitleLongClick() {
                if (LoginBean.getInstance().isMaster()) {
                    Intent intent = new Intent(ReadMangaActivity.this, TagManagerActivity.class);
                    intent.putExtra("imgUrl", pathList.get(historyPosition));
                    startActivity(intent);
                }
            }
        });
    }

    private void showSearchDialog() {
        if (null == searchDialog) {
            searchDialog = new OnlyEditDialog(this);
            searchDialog.setOnEditResultListener(new OnEditResultListener() {
                @Override
                public void onResult(String text) {
                    translateWord(text);
                }

                @Override
                public void onCancelClick() {

                }
            });
            searchDialog.setCancelable(true);
        }
        searchDialog.show();
        searchDialog.clearEdit();
    }

    private void showImgEditDialog(Bitmap bp) {
        if (null == mangaImgEditDialog) {
            mangaImgEditDialog = new MangaImgEditDialog(this);
            mangaImgEditDialog.setOnEditResultListener(new OnEditResultListener() {
                @Override
                public void onResult(String text) {
                    translateWord(text);
                }

                @Override
                public void onCancelClick() {

                }
            });
        }
        mangaImgEditDialog.show();
        mangaImgEditDialog.setImgRes(bp);
        mangaImgEditDialog.clearEdit();
    }

    private void translateWord(final String word) {
        clip.setText(word);
        if (SharedPreferencesUtils.getBooleanSharedPreferencesData(this, ShareKeys.CLOSE_TRANSLATE, false)) {
            //关闭自动翻译
            return;
        }
        String url = Configure.YOUDAO + word;
        HashMap<String, String> params = new HashMap<String, String>();
        VolleyCallBack<YoudaoResponse> callback = new VolleyCallBack<YoudaoResponse>() {

            @Override
            public void loadSucceed(YoudaoResponse result) {
                if (null != result && result.getErrorCode() == 0) {
                    YoudaoResponse.BasicBean item = result.getBasic();
                    String t = "";
                    if (null != item) {
                        for (int i = 0; i < item.getExplains().size(); i++) {
                            t = t + item.getExplains().get(i) + ";";
                        }

                        showTranslateResultDialog(word, result.getQuery() + "  [" + item.getPhonetic() + "]: " + "\n" + t);
                    } else {
                        baseToast.showToast("没查到该词");
                    }
                } else {
                    baseToast.showToast("没查到该词");
                }
            }

            @Override
            public void loadFailed(VolleyError error) {
                baseToast.showToast("error\n" + error);
            }

            @Override
            public void loadSucceedButNotNormal(YoudaoResponse result) {

            }
        };
        VolleyTool.getInstance(this).requestData(Request.Method.GET,
                ReadMangaActivity.this, url, params,
                YoudaoResponse.class, callback);
    }

    private void showTranslateResultDialog(final String title, String msg) {
        if (null == translateResultDialog) {
            translateResultDialog = new MangaDialog(this);
        }
        translateResultDialog.show();

        translateResultDialog.setTitle(title);
        translateResultDialog.setMessage(msg);
        translateResultDialog.setOkText("确定");
        translateResultDialog.setCancelable(true);
    }

    private void showDeleteDialog(final String fileName) {
        MangaDialog deleteDialog = new MangaDialog(this);
        deleteDialog.setOnPeanutDialogClickListener(new MangaDialog.OnPeanutDialogClickListener() {
            @Override
            public void onOkClick() {
                FileSpider.getInstance().deleteFile(fileName);
                pathList.remove(historyPosition);
                baseToast.showToast("已删除");
                initViewPager();
            }

            @Override
            public void onCancelClick() {

            }
        });
        deleteDialog.show();

        deleteDialog.setTitle("确定删除?");
        deleteDialog.setOkText("删除");
        deleteDialog.setCancelText("算了");
        deleteDialog.setCancelable(true);
    }


    private void showSaveDialog(final String imgPath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("是否保存图片?");
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downLoadPic(imgPath);
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.show();
    }

    /**
     *
     */
    private void downLoadPic(final String path) {
        // 将图片下载并保存
        new Thread() {
            public void run() {
                Bitmap bp = null;
                if (!TextUtils.isEmpty(path)) {
                    //从网络上获取到图片
                    Long time = new Date().getTime();
                    String timeString = time + "";
                    timeString = timeString.substring(5);
                    FileSpider.getInstance().loadImageFromNetwork(path, "scattered", "/scattered(" + timeString + ").png");
                }
            }
        }.start();
    }


    private void initViewPager() {
        if (null == adapter) {
            adapter = new ReadMangaAdapter(ReadMangaActivity.this, pathList);
            if (isLocalManga) {
                mangaPager.setOffscreenPageLimit(1);
            } else {
                mangaPager.setOffscreenPageLimit(3);
            }
            mangaPager.setAdapter(adapter);

            recoverState();
            mangaPager.setPageTransformer(true, new DepthPageTransformer());
            mangaPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    historyPosition = position;
                    readProgressTv.setText(position + 1 + "/" + pathList.size());
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
//            mangaPager.setCurrentItem(2);
        } else {
            adapter.setPathList(pathList);
            adapter.notifyDataSetChanged();
        }
    }

    private void cutSeekBar() {
        if (seekBar.isShown()) {
            seekBar.setVisibility(View.GONE);
        } else {
            seekBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recoverState();
    }

    private void saveState() {
        initProgressKey();
        SharedPreferencesUtils.setSharedPreferencesData(this, progressSaveKey + "progress",
                historyPosition);
    }

    private void initProgressKey() {
        if (TextUtils.isEmpty(progressSaveKey)) {
            if (isLocalManga) {
                int pos = pathList.get(0).lastIndexOf("/");
                progressSaveKey = pathList.get(0).substring(0, pos);
            } else {
                progressSaveKey = chapterUrl;
            }
        }
    }

    private void recoverState() {
        initProgressKey();
        int p = SharedPreferencesUtils.getIntSharedPreferencesData(this,
                progressSaveKey + "progress");
        if (p >= 0) {
            historyPosition = p;
            mangaPager.setCurrentItem(p);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.show_seek_bar:
                cutSeekBar();
                break;
            case R.id.read_progress_tv:

                break;
        }
    }
}
