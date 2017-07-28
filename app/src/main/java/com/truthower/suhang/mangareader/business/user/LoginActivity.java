package com.truthower.suhang.mangareader.business.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.truthower.suhang.mangareader.R;
import com.truthower.suhang.mangareader.base.BaseActivity;
import com.truthower.suhang.mangareader.bean.LoginBean;

import java.util.HashMap;


public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private ImageView crossIv;
    private EditText userIdEt;
    private EditText userPsdEt;
    private Button loginBtn;
    private TextView forgotPsdTv;
    private TextView registerNowTv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
        Intent intent = getIntent();
        String toast = intent.getStringExtra("toast");
        if (!TextUtils.isEmpty(toast)) {
            baseToast.showToast(toast);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(LoginBean.getInstance().getUsername())) {
            userIdEt.setText(LoginBean.getInstance().getUsername());
        }
    }

    private void initUI() {
        crossIv = (ImageView) findViewById(R.id.cross_iv);
        userIdEt = (EditText) findViewById(R.id.user_id_et);
        userPsdEt = (EditText) findViewById(R.id.user_psd_et);
        userPsdEt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //因为DOWN和UP都算回车 所以这样写 避免调用两次
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            if (checkData()) {
                                doLogin();
                            }
                            break;
                    }
                }
                return false;
            }
        });
        loginBtn = (Button) findViewById(R.id.login_btn);
        forgotPsdTv = (TextView) findViewById(R.id.forgot_psd_tv);
        registerNowTv = (TextView) findViewById(R.id.register_now_tv);

        crossIv.setOnClickListener(this);
        loginBtn.setOnClickListener(this);
        forgotPsdTv.setOnClickListener(this);
        registerNowTv.setOnClickListener(this);

        hideBaseTopBar();
        setColorHolderColor(R.color.divide);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }

    private boolean checkData() {
        if (TextUtils.isEmpty(userIdEt.getText().toString())) {
            baseToast.showToast("请输入用户名!");
            return false;
        }
        if (TextUtils.isEmpty(userPsdEt.getText().toString())) {
            baseToast.showToast("请输入密码!");
            return false;
        }
        return true;
    }


    private void doLogin() {
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.login_btn:
                if (checkData()) {
                    doLogin();
                }
                break;
            case R.id.cross_iv:
                LoginActivity.this.finish();
                break;
            case R.id.register_now_tv:
                intent = new Intent(LoginActivity.this, RegisterActivity.class);
                break;
            case R.id.forgot_psd_tv:
//                intent = new Intent(LoginActivity.this, ModifyPsdActivity.class);
                break;
        }
        if (null != intent) {
            startActivity(intent);
        }
    }
}