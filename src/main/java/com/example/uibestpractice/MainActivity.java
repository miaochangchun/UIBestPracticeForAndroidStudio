package com.example.uibestpractice;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uitl.Aes;
import uitl.Md5;
import uitl.PostServer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TEXT_UPDATE = 0;
    private static final int NULL_UPDATE = 1;
    List<Msg> msgList = new ArrayList<Msg>();
    private MsgAdapter adapter;
    private EditText inputText;
    private Button send;
    private ListView msgListView;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
            case TEXT_UPDATE:
                Bundle bundle = msg.getData();
                String answer = bundle.getString("answer");
                Msg msg2 = new Msg(answer, Msg.TYPE_RECEIVED);
                msgList.add(msg2);
                adapter.notifyDataSetChanged();
                msgListView.setSelection(msgList.size());
                break;
            case NULL_UPDATE:
                Msg msg1 = new Msg("机器人无法理解你的意思", Msg.TYPE_RECEIVED);
                msgList.add(msg1);
                adapter.notifyDataSetChanged();
                msgListView.setSelection(msgList.size());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initMsgs();//初始化消息数据
        adapter = new MsgAdapter(MainActivity.this, R.layout.msg_item, msgList);
        inputText = (EditText) findViewById(R.id.input_text);
        send = (Button) findViewById(R.id.send);
        msgListView = (ListView) findViewById(R.id.msg_list_view);
        msgListView.setAdapter(adapter);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String content = inputText.getText().toString();
                if(!"".equals(content)){
                    Msg msg = new Msg(content, Msg.TYPE_SEND);
                    msgList.add(msg);
                    //当有新消息时，刷新ListView中的显示
                    adapter.notifyDataSetChanged();
                    //将ListView定位到最后一行
                    msgListView.setSelection(msgList.size());
                    //清空输入框中的内容
                    inputText.setText("");

                    //回答上面的提出的问题
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //使用图灵机器人回答问题
                            String str = tuLingTest(content);
                            if(!"".equals(str)){
                                Message message = new Message();
                                message.what = TEXT_UPDATE;
                                Bundle bundle = message.getData();
                                bundle.putString("answer", str);
                                message.setData(bundle);
                                handler.sendMessage(message);
                            }else{
                                Message message = Message.obtain();
                                message.what = NULL_UPDATE;
                                handler.sendMessage(message);
                            }
                        }
                    }).start();

                }
            }
        });
    }

    /**
     * 图灵机器人API接口，传入文本返回一个答案
     * @param content
     * @return 传入的文本串
     */
    private String tuLingTest(String content) {
        //图灵网站上的secret
        String secret = "0f22c2a62088603f";
        //图灵网站上的apiKey
        String apiKey = "72d3e0a03e11673db74b6f705f42bef2";
        Log.e(TAG, content);
        //待加密的json数据
        String data = "{\"key\":\"" + apiKey + "\",\"info\":\"" + content + "\"}";
        //获取时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());

        //生成密钥
        String keyParam = secret+timestamp+apiKey;
        String key = Md5.MD5(keyParam);

        //加密
        Aes mc = new Aes(key);
        data = mc.encrypt(data);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key", apiKey);
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e(TAG, jsonObject.toString());
        //请求图灵api
        String result = PostServer.SendPost(jsonObject.toString(), "http://www.tuling123.com/openapi/api");
//          System.out.println(result);
        Log.e(TAG, result);
        JSONObject json = null;
        int code = 0;
        String answer = null;
        try {
            json = new JSONObject(result);
            code = json.getInt("code");
            if(code == 100000){
                answer = json.getString("text");
                return answer;
            }else if(code == 200000){
                answer = json.getString("text");
                String url = json.getString("url");
                return answer + url;
            }else if(code == 302000){
                answer = json.getString("text");
                JSONArray jsonArray = json.getJSONArray("list");
                for(int i=0; i<jsonArray.length(); i++){
                    JSONObject jsonObject1 = (JSONObject) jsonArray.get(i);
                    String article = jsonObject1.getString("article");
                    String source = jsonObject1.getString("source");
                    String icon = jsonObject1.getString("icon");
                    String detailurl = jsonObject1.getString("detailurl");
                    Log.e(TAG, "article = " + article + "\nsource = " + source + "\nicon = " + icon + "\ndetailurl = " + detailurl);
                }
            }else if(code == 308000){
                answer = json.getString("text");
                JSONArray jsonArray = json.getJSONArray("list");
                for(int i=0; i<jsonArray.length(); i++){
                    JSONObject jsonObject1 = (JSONObject) jsonArray.get(i);
                    String name = jsonObject1.getString("name");
                    String icon = jsonObject1.getString("icon");
                    String info = jsonObject1.getString("info");
                    String detailurl = jsonObject1.getString("detailurl");
                    Log.e(TAG, "name = " + name + "\nicon = " + icon + "\ninfo = " + info + "\ndetailurl = " + detailurl);
                }
            }else if(code == 305000){
                answer = json.getString("text");
                JSONArray jsonArray = json.getJSONArray("list");
                for(int i=0; i<jsonArray.length(); i++){
                    JSONObject jsonObject1 = (JSONObject) jsonArray.get(i);
                    String trainnum = jsonObject1.getString("trainnum");
                    String start = jsonObject1.getString("start");
                    String terminal = jsonObject1.getString("terminal");
                    String starttime = jsonObject1.getString("starttime");
                    String endtime = jsonObject1.getString("endtime");
                    String detailurl = jsonObject1.getString("detailurl");
                    Log.e(TAG, "列车信息 = " + trainnum + "\n始发站 = " + start + "\n终点站 = " + terminal + "\n始发时间 = " + starttime
                                + "\n终到时间" + endtime + "\ndetailurl" + detailurl);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e(TAG, code + "");
        Log.e(TAG, answer);

        return answer;
    }

    private void initMsgs() {
        Msg msg1 = new Msg("Hello Guy. ", Msg.TYPE_RECEIVED);
        msgList.add(msg1);
        Msg msg2 = new Msg("Hello,Who is that?", Msg.TYPE_SEND);
        msgList.add(msg2);
        Msg msg3 = new Msg("This is Tom. Nice Talking to you.", Msg.TYPE_RECEIVED);
        msgList.add(msg3);
    }
}
