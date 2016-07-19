package main.litigationpreservation.com.preservationofgold;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;


/**
 * Created by Shulin on 2016/6/16.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PieView pieView = (PieView) findViewById(R.id.pie_view);
        ArrayList<PieHelper> pieHelperArrayList = new ArrayList<>();
        PieHelper pieHelper = new PieHelper(0, 0, 5, 8);
        pieHelperArrayList.add(pieHelper);
        pieView.setDate(pieHelperArrayList);
        zouni();
    }

    private void zouni() {
        // 月份显示
        LineView lineView = (LineView) findViewById(R.id.line_view);
        ArrayList<String> strList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            strList.add((i + 1) + "月");
        }
        lineView.setBottomTextList(strList);


        // 保全金额值随机数
        ArrayList<Integer> dataList = new ArrayList<>();
        int random = (int) (Math.random() * 1000 + 1);
        for (int i = 0; i < 8; i++) {
            dataList.add((int) (Math.random() * random));
        }
        lineView.setDataList(dataList);
    }
}
