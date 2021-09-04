package com.jesen.simple_hook;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private Button hookIt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);
        hookIt = findViewById(R.id.hookIt);

        button.setOnClickListener(view -> {
            Toast.makeText(this,((Button)view).getText(),Toast.LENGTH_SHORT).show();
        });

        hookIt.setOnClickListener(view -> {
            // 在不修改以上代码的情况下，通过Hook把 ((Button) v).getText() 内容修改
            try {
                hook(button); // button就是View对象
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Hook失败" + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 目标： 执行ListenerInfo.getListenerInfo() 拿到 mListenerInfo 对象
     *       再拿到mListenerInfo的 mOnClickListener
     *       然后替换mListenerInfo的 mOnClickListener 为 mOnClickListenerProxy
     * */
    private void hook(View view) throws Exception{
        Class viewClass = Class.forName("android.view.View");
        Method getListenerInfoMethod = viewClass.getDeclaredMethod("getListenerInfo");
        // 设置方法权限
        getListenerInfoMethod.setAccessible(true);
        // 执行方法得到ListenerInfo的对象mListenerInfo
        Object mListenerInfo = getListenerInfoMethod.invoke(view);

        Class mListenerInfoClass = Class.forName("android.view.View$ListenerInfo");
        Field mOnClickListenerField = mListenerInfoClass.getField("mOnClickListener");
        // 获取到mListenerInfo 的 mOnClickListener
        final Object mOnClickListenerObj = mOnClickListenerField.get(mListenerInfo);

        // 动态代理监听用户点击事件，拦截事件
        InvocationHandler invocationHandler = new InvocationHandler() {

            /**
             * void onClick(View v);
             *
             * @param method --> onClick
             * @param objects --> View v
             */
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                // 加入了自己逻辑
                Log.d("hook", "拦截到了 OnClickListener的方法了");
                //Button button = new Button(MainActivity.this);
                button.setText("内容被Hook偷换了....");

                // 让系统程序片段 --- 正常继续的执行下去
                return method.invoke(mOnClickListenerObj, button);
            }
        };

        Object mOnClickListenerProxy = Proxy.newProxyInstance(MainActivity.class.getClassLoader(),
                new Class[]{View.OnClickListener.class},
                invocationHandler);

        // 把系统的 mOnClickListener 换成自己的mOnClickListenerProxy
        mOnClickListenerField.set(mListenerInfo, mOnClickListenerProxy); // 替换的 我们自己的动态代理
    }
}