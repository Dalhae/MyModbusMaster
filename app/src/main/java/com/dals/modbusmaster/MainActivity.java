package com.dals.modbusmaster;
//커밋테스트
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    boolean isPageOpen = false;

    Animation translateLeftAnim;
    Animation translateRightAnim;

    LinearLayout page;

    Button button;

    Handler handler = new Handler();
    TextView textView,setState;
    ScrollView scrollView;
    EditText ip,port,stationNo,startAddr,dataLen,funcCode;

    final byte[] requst = new byte[12];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        setState = findViewById(R.id.settingState);
        scrollView = findViewById(R.id.scrollView);
        page = findViewById(R.id.page);
        ip = findViewById(R.id.editTextIP);
        port = findViewById(R.id.editTextPort);
        stationNo = findViewById(R.id.editTextStateNo);
        startAddr = findViewById(R.id.editTextStartAddress);
        funcCode = findViewById(R.id.editTextFunc);
        dataLen = findViewById(R.id.editTextDataLen);



        translateLeftAnim = AnimationUtils.loadAnimation(this,R.anim.translate_left);
        translateRightAnim = AnimationUtils.loadAnimation(this,R.anim.translate_right);

        SlidingPageAnimationListener animationListener = new SlidingPageAnimationListener();
        translateLeftAnim.setAnimationListener(animationListener);
        translateRightAnim.setAnimationListener(animationListener);

        button =findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPageOpen){
                    page.startAnimation(translateRightAnim);

                }else{
                    page.setVisibility(View.VISIBLE);
                    page.startAnimation(translateLeftAnim);

                }
            }
        });

       Button button2 = findViewById(R.id.button2);
       button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final byte[] data = {0x0,0x0,0x0,0x0,0x0,0x6,0x1,0x4,0x0,0x0,0x0,0xA};
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        send(data);
                    }
                }).start();
            }
       });

    }

    public void makeRequst(int count){
        byte[]  buf = new byte[2];
        buf = intTobyte(count,ByteOrder.BIG_ENDIAN);
        System.arraycopy(buf,2,requst,0,2);
        requst[2] = 0;
        requst[3] = 0;
        requst[4] = 0x0;
        requst[5] = 0x6;
        buf = intTobyte(Integer.parseInt(stationNo.getText().toString(),16),ByteOrder.BIG_ENDIAN);
        System.arraycopy(buf,3,requst,6,1);
        buf = intTobyte(Integer.parseInt(funcCode.getText().toString(),16),ByteOrder.BIG_ENDIAN);
        System.arraycopy(buf,3,requst,7,1);
        buf = intTobyte(Integer.parseInt(startAddr.getText().toString(),16),ByteOrder.BIG_ENDIAN);
        System.arraycopy(buf,2,requst,8,2);
        buf = intTobyte(Integer.parseInt(dataLen.getText().toString(),16),ByteOrder.BIG_ENDIAN);
        System.arraycopy(buf,2,requst,10,2);

    }

    public void send(byte[] data){
        try{
            int _port= Integer.parseInt(port.getText().toString());
            byte[] r_data = new byte[255];
            int[] i_data = new int[128];
            int length,count=0;
            makeRequst(count);
            printSetData(ip.getText().toString() + " " + port.getText().toString());
            Socket sock = new Socket(ip.getText().toString(),_port);
            showToast("소켓연결함");
            //Toast.makeText(this,"소켓연결함",Toast.LENGTH_LONG).show();
            DataOutputStream outputStream = new DataOutputStream(sock.getOutputStream());
            DataInputStream inputStream = new DataInputStream(sock.getInputStream());
            while(true) {
                makeRequst(count);
                outputStream.write(requst);
                outputStream.flush();
                length = inputStream.read(r_data);
                byteArrTOIntArr(r_data, i_data, length);
                String str = " ";
                for (int i = 0; i < (length - 9) / 2; i++) {
                    str += i_data[i] + " ";
                }
                printLog("DATA :" + r_data[1] + " "+ str);
                sleep(500);
                count++;
                if (count>50) {break;}

            }
            outputStream.close();
            inputStream.close();
            sock.close();
            emptyTextView();
        }catch (Exception e){
            showToast("연결실패"+ e.getMessage());
            //printLog("연결실패"+ e.getMessage());
            e.printStackTrace();
        }

    }

    public void emptyTextView(){
        handler.post(new Runnable() {
                         @Override
                         public void run() {
                            textView.setText("");
                         }
                     });
    }

    public void printSetData(final String string){
        handler.post(new Runnable() {
            @Override
            public void run() {
                setState.setText(string);
            }
        });
    }


    public void printLog(final String string){
        handler.post(new Runnable() {
            @Override
            public void run() {
                textView.append(string+"\n");
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                //textView.setText("");
            }
        });

    }

    public void byteArrTOIntArr(byte[] bdata, int[] idata, int len){
        int j = 0;
        byte[] temp = {0,0,0,0};
        for(int i=9;i<len;i+=2){
            System.arraycopy(bdata, i, temp,2 ,2);
            idata[j++] = byteArrayToInt(temp);
        }

    }
    public  int byteArrayToInt(byte[] bytes) {
        return ((((int)bytes[0] & 0xff) << 24) |
                (((int)bytes[1] & 0xff) << 16) |
                (((int)bytes[2] & 0xff) << 8) |
                (((int)bytes[3] & 0xff)));
    }

    public void showToast(final String data){
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
            }
        });
    }

    private class SlidingPageAnimationListener implements Animation.AnimationListener{

        public  void onAnimationEnd(Animation animation){
            if(isPageOpen){
                page.setVisibility(View.INVISIBLE);
                button.setText("Open");
                isPageOpen = false;
            }else{
                button.setText("Close");
                isPageOpen = true;
            }
        }

        @Override
        public void onAnimationStart(Animation animation){}

        @Override
        public void onAnimationRepeat(Animation animation){}

    }

    public static byte[] intTobyte(int integer, ByteOrder order) {

        ByteBuffer buff = ByteBuffer.allocate(Integer.SIZE/8);
        buff.order(order);

        // 인수로 넘어온 integer을 putInt로설정
        buff.putInt(integer);

        System.out.println("intTobyte : " + buff);
        return buff.array();
    }

}

