package ru.rd3ay.AA_340UP;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

//==================================================================================================
//
//==================================================================================================


public class MainActivity extends AppCompatActivity implements
        CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemClickListener,
        View.OnClickListener {

    MediaPlayer mPlayer;

    private final static String FILE_NAME = "AA-340UP.txt";

    private long backPressdTime;
    private Toast backToast;

    private int Inter_step;
    int Value_step;
    int Value_start_Freq;
    private int Counter_String;

    private int impedans = 50;
    String command_scanning = "8";

    String[] step = {"100 KHz", "1 KHz", "10 KHz", "250 KHz"};

    String[] select_bend = {" 1-4 MHz ", " 4-8 MHz ", " 8-12 MHz ", " 12-16 MHz ",
            " 16-20 MHz ", " 20-24 MHz ", " 24-30 MHz ", " 1-30 MHz ", " SET ", "Ячейка №1", "Ячейка №2"};

    private static final String TAG = "MyLOG";

    private static final int REQ_ENABLE_BT = 10;
    public static final int BT_BOUNDED = 21;
    public static final int BT_SEARCH = 22;

    private FrameLayout frameMessage;
    private LinearLayout frameControls;
    private RelativeLayout frameLedControls;
    private LinearLayout frControls;

    private EditText etConsole;

    private Button btnDisconnect;
    private Button btn_exit;
    private Button btn_exit2;
    private Button btn_imp;
    private Button btnScanning;
    private Button btnEnableSearch;
    private Switch switchEnableBt;

    private ProgressBar pbProgress;
    private ProgressBar loadProgress;

    private ListView listBtDevices;

    private BluetoothAdapter bluetoothAdapter;
    private BtListAdapter listAdapter;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    private ArrayList<String> data_SRX;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private ProgressDialog progressDialog;

    private GraphView gvGraph;

    private final LineGraphSeries<DataPoint> series_R = new LineGraphSeries<>();
    private final LineGraphSeries<DataPoint> series_X = new LineGraphSeries<>();
    private final LineGraphSeries<DataPoint> series_SWR = new LineGraphSeries<>();
    private final LineGraphSeries<DataPoint> series_Marcker = new LineGraphSeries<>();

    private Spinner spinner_bend;
    private Spinner spinner;

    private final Handler handler = new Handler();

    private double Corent_Value_SWR = 10d;
    private int Corent_Freq;

    final String DIR_SD = "MyFiles";
    final String FILENAME_SD = "fileSD";

    //==============================================================================================
    //        CREATE
    //==============================================================================================

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameMessage = findViewById(R.id.frame_message);
        frameControls = findViewById(R.id.frame_control);
        frControls = findViewById(R.id.frame1_control);

        switchEnableBt = findViewById(R.id.switch_enable_bt);
        btnEnableSearch = findViewById(R.id.btn_enable_search);
        pbProgress = findViewById(R.id.pb_progress);
        loadProgress = findViewById(R.id.progressBar3);
        listBtDevices = findViewById(R.id.lv_bt_device);
        etConsole = findViewById(R.id.et_console);

        frameLedControls = findViewById(R.id.frameLedControls);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btn_exit = findViewById(R.id.btn_exit);
        btn_exit2 = findViewById(R.id.btn_exit2);
        btn_imp = findViewById(R.id.btn_imp);
        btnScanning = findViewById(R.id.btn_scann);

        spinner_bend = findViewById(R.id.bends);
        spinner = findViewById(R.id.steps);


        spinner.setPrompt(" Выбор шага сканирования");
        spinner_bend.setPrompt("Выбор диапазона сканирования");


        // Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, step);
        ArrayAdapter<String> adapter_bend;
        adapter_bend = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, select_bend);
        // Определяем разметку для использования при выборе элемента
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter_bend.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Применяем адаптер к элементу spinner
        spinner.setAdapter(adapter);
        spinner_bend.setAdapter(adapter_bend);


        gvGraph = findViewById(R.id.gv_graph);


        //  Толщина линии
        int THICKNESS = 5;
        series_SWR.setThickness(THICKNESS);  //  Толщина линии
        series_SWR.setColor(Color.RED);
        series_R.setThickness(THICKNESS);  //  Толщина линии
        series_R.setColor(Color.rgb(0, 180, 0));
        series_X.setThickness(THICKNESS);  //  Толщина линии
        series_X.setColor(Color.BLUE);

        //       series_Marcker.setAnimated(true);  // прибегает слева

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //для альбомного режима

        gvGraph.addSeries(series_SWR);
        gvGraph.addSeries(series_Marcker);

        gvGraph.getViewport().setMaxXAxisSize(0);
        gvGraph.getViewport().setMinX(400);  // Минимальное значение по x
        gvGraph.getViewport().setMaxX(31000);
        gvGraph.getViewport().setMinY(0);  // Минимальное значение по Y
        gvGraph.getViewport().setMaxY(10);
        gvGraph.getViewport().setYAxisBoundsManual(true); // Жесткая привязка шкалы
        //     gvGraph.setManualYAxisBounds(5, -5); // min=-5, max=5
        //     gvGraph.getGraphViewStyle().setNumVerticalLabels(11); // will have 11 y axis lables
        //   gvGraph.getViewport().setScalable(true);   //  Разрешает ручную раздвишку экрана
        gvGraph.getViewport().setScalableY(true);

// the y bounds are always manual for second scale
        //    gvGraph.getSecondScale();
        gvGraph.getGridLabelRenderer().setNumHorizontalLabels(5); // Количество вертикальных линий по горизонтали
        gvGraph.getGridLabelRenderer().setNumVerticalLabels(5);   // Количество горизонтальных линий по вертикали
        gvGraph.getSecondScale().addSeries(series_R);
        gvGraph.getSecondScale().addSeries(series_X);
        gvGraph.getSecondScale().setMinY(0);
        gvGraph.getSecondScale().setMaxY(500);
        gvGraph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.BLACK);
        gvGraph.getGridLabelRenderer().setGridColor(Color.GRAY);  // Color Grid
        //  gvGraph.getGridLabelRenderer().setLabelVerticalWidth(50); // Отступ верткальной шкалы от левого края пиксели
        //   gvGraph.getGridLabelRenderer().setHorizontalLabelsAngle(20);  // Угол наклона горизонтальных надписей  20 градусов
        // gvGraph.getViewport().setBorderColor(Color.RED);

        //      gvGraph.getGraphViewStyle().setNumVerticalLabels(5);
        //     gvGraph.getGraphViewStyle().setTextSize(10);
        //    gvGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);  // Отчлючает решотку или частично

        gvGraph.getViewport().setDrawBorder(true); //  Устанавливает бордюр
        //     gvGraph.getLegendRenderer().setTextSize(50);
        //     gvGraph.getLegendRenderer().resetStyles();
        //     gvGraph.getLegendRenderer().setTextColor(Color.RED);

        //    gvGraph.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.RED); ???????
        //    gvGraph.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));  установить цвет основы

        gvGraph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.rgb(0, 150, 0));//  Цвет цифр справа
        gvGraph.getGridLabelRenderer().setVerticalLabelsColor(Color.rgb(255, 0, 0));//  Цвет цифр
        gvGraph.getGridLabelRenderer().setHighlightZeroLines(false);
        //    gvGraph.getGridLabelRenderer().setLabelsSpace(20);


        // directly share it
        //    gvGraph.takeSnapshotAndShare(mActivity, "exampleGraph", "GraphViewSnapshot");
        //    Bitmap bitmap = gvGraph.takeSnapshot();


        // legend
        series_SWR.setTitle("КСВ");
        series_R.setTitle("R");
        series_X.setTitle("X");
        series_Marcker.setTitle("Mar");
        gvGraph.getLegendRenderer().setVisible(true);
        gvGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        gvGraph.getLegendRenderer().setBackgroundColor(Color.rgb(255, 245, 169));//  Цвет цифр#FFF5A9
        gvGraph.getLegendRenderer().setTextSize(40);

        switchEnableBt.setOnCheckedChangeListener(this);
        listBtDevices.setOnItemClickListener(this);

        btnEnableSearch.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        btn_imp.setOnClickListener(this);
        btn_exit.setOnClickListener(this);
        btn_exit2.setOnClickListener(this);
        btnScanning.setOnClickListener(this);

        bluetoothDevices = new ArrayList<>();
        data_SRX = new ArrayList<>();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(getString(R.string.connecting));
        progressDialog.setMessage(getString(R.string.please_wait));

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: " + getString(R.string.bluetooth_not_supported));
            finish();
        }

        if (bluetoothAdapter.isEnabled()) {
            showFrameControls();
            switchEnableBt.setChecked(true);
            setListAdapter(BT_BOUNDED);

        }
//--------------------------------------------------------------------------------------------------

        mPlayer = MediaPlayer.create(this, R.raw.soho);
        mPlayer.setOnCompletionListener(mp -> stopPlay());

        //-----------------------------------------------------------------------------------------------
        // TODO ------------------   Рисуем вертикальный маркер  --------------------------------

        series_SWR.setOnDataPointTapListener((series, dataPointt) -> {

            mPlayer.start();

            DataPoint[] Date = new DataPoint[]{
                    new DataPoint(dataPointt.getX(), 0),
                    new DataPoint(dataPointt.getX(), 10),
            };

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.rgb(100, 100, 100));
            paint.setPathEffect(new DashPathEffect(new float[]{25, 5}, 0));

            series_Marcker.setDrawAsPath(true);
            series_Marcker.setCustomPaint(paint);
            series_Marcker.resetData(Date);

            double val_SWR = dataPointt.getY();
            double val_Freq = dataPointt.getX();

            etConsole.setText("Freq = " + val_Freq + "    КСВ = " + val_SWR);
        });

 //TODO --------------------------    выбор из спадающего меню шаг перестройки    --------------------

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                //     ((TextView) adapterView.getChildAt(0)).setTextColor(Color.BLUE);
                //     ((TextView) adapterView.getChildAt(0)).setTextSize(16);

                String step_command = "b";

                switch (position) {

                    case 0:
                        step_command = "c";
                        break;
                    case 1:
                        step_command = "a";
                        break;
                    case 2:
                        step_command = "b";
                        break;
                    case 3:
                        step_command = "d";
                        break;

                }


                if (connectedThread != null && connectThread.isConnect()) {
                    connectedThread.write(step_command);
                }
                Log.d(TAG, "Выбор шага сканирования =" + position);
                //    Toast.makeText(getBaseContext(),"Выбор шага сканирования = " + position, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {


            }
        });

//TODO --------------------------    выбор из спадающего меню диапазона    --------------------

        spinner_bend.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {


                String bend_command = "8";

                switch (position) {

                    case 0:
                        bend_command = "0";  // 1-4
                        break;
                    case 1:
                        bend_command = "1";  // 4-8
                        break;
                    case 2:
                        bend_command = "2";   // 8-12
                        break;
                    case 3:
                        bend_command = "4";  //12-16
                        break;
                    case 4:
                        bend_command = "5";  //16-20
                        break;
                    case 5:
                        bend_command = "6";   // 20-24
                        break;
                    case 6:
                        bend_command = "7";   //  24-30
                        break;
                    case 7:
                        bend_command = "8";  // 1-30
                        break;
                    case 8:
                        bend_command = "3";  //  SET
                        break;
                    case 9:
                        bend_command = "9";   // chell #1
                        break;
                    case 10:
                        bend_command = ":";   // chell #2
                        break;
                }

                command_scanning = bend_command;

                //         Toast.makeText(getBaseContext(),"Bend = " + bend_command, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Выбор диапазона = " + bend_command);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    //TODO========================  END OnCreate ====================================================

    //  Системная кнопка назад  начало

    @Override
    public void onBackPressed() {

        if (backPressdTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(getBaseContext(), getString(R.string.Thu_exit), Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressdTime = System.currentTimeMillis();
    }

//TODO ----------------------    Запись фала SD карту   --------------------------------------------

 void writeFileSD() {
// проверяем доступность SD
      if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
                Log.d(TAG, "SD-карта не доступна: " + Environment.getExternalStorageState());
        return;
    }
// получаем путь к SD
       File sdPath = Environment.getExternalStorageDirectory();
// добавляем свой каталог к пути
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
// создаем каталог
        sdPath.mkdirs();
// формируем объект File, который содержит путь к файлу
       File sdFile = new File(sdPath, FILENAME_SD);
try {
// открываем поток для записи
      BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile));
// пишем данные
       bw.write("Содержимое файла на SD");
// закрываем поток
      bw.close();
    Log.d(TAG, "Файл записан на SD: " + sdFile.getAbsolutePath());
           } catch (IOException e) {
                                    e.printStackTrace();
              }
}

//TODO ----------------------    Запись фала    ----------------------------------------------------

    void writeFile() {
try {
// отрываем поток для записи

BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    openFileOutput(MainActivity.FILE_NAME, MODE_PRIVATE)));
// пишем данные
        for (int i = 0; i < (data_SRX.size()); i++) {
                                                      bw.write(data_SRX.get(i));
                                                    }
// закрываем поток
bw.close();
    Log.d(TAG, "Файл записан");
} catch (FileNotFoundException e) {
                                   e.printStackTrace();
                                 } catch (IOException e) {
                e.printStackTrace();
         }
}


//TODO -------------------- Чтение файла----------------------------------------------------------
void readFile() {
try {
// открываем поток для чтения
BufferedReader br = new BufferedReader(new InputStreamReader(
                openFileInput(FILE_NAME)));
String str;
// читаем содержимое
while ((str = br.readLine()) != null) {
    Log.d(TAG, str);
}
} catch (FileNotFoundException e) {
                                  e.printStackTrace();
                                 } catch (IOException e) {
                  e.printStackTrace();
          }
}

//==================================================================================================
//
//==================================================================================================

    private void stopPlay() {
        mPlayer.stop();

        try {
            mPlayer.prepare();
            mPlayer.seekTo(0);

        } catch (Throwable t) {
            Toast.makeText(this, t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
//==================================================================================================
//TODO        Выводим вертикальный курсор-линю и значение  КСВ в этой точке
//==================================================================================================

    @SuppressLint("SetTextI18n")
    public void set_Min_SWR() {

        DataPoint[] Date = new DataPoint[]{
                new DataPoint(Corent_Freq, 0),
                new DataPoint(Corent_Freq, 10),
        };

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.rgb(100, 100, 100));
        paint.setPathEffect(new DashPathEffect(new float[]{25, 5}, 0));

        series_Marcker.setDrawAsPath(true);
        series_Marcker.setCustomPaint(paint);
        series_Marcker.resetData(Date);

        etConsole.setText("Freq = " + Corent_Freq + "    КСВ = " + Corent_Value_SWR);

        Corent_Value_SWR = 10d;
    }



    //==================================================================================================
//TODO    Обработка нажатия кнопок
//==================================================================================================
    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {

             mPlayer.start();

        if (v.equals(btnEnableSearch)) {
            enableSearch();
        } else if (v.equals(btnDisconnect)) {
            //cancelTimer();

            if (connectedThread != null) {
                connectedThread.cancel();
            }

            if (connectThread != null) {
                connectThread.cancel();
            }

            showFrameControls();
        }

//TODO ------------------     Обработка события нажатия кнопки  Выход   ----------------------------

        if ((v.equals(btn_exit)) | (v.equals(btn_exit2))) {

            if (backPressdTime + 2000 > System.currentTimeMillis()) {
                backToast.cancel();
                super.onBackPressed();
                return;
            } else {
                backToast = Toast.makeText(getBaseContext(), getString(R.string.Thu_exit), Toast.LENGTH_SHORT);
                backToast.show();
            }
            backPressdTime = System.currentTimeMillis();

        }

//TODO ------------------ Обработка события нажатия кнопки сканировать-----------------------------

        if (v.equals(btnScanning)) {  //    Обработка события нажатия кнопки сканировать

            Log.d(TAG, "Нажата кнопка сканировать");

            if (connectedThread != null && connectThread.isConnect()) {

                etConsole.setText("");
                connectedThread.write(command_scanning);
                loadProgress.setVisibility(View.VISIBLE);
                btnScanning.setEnabled(false);               //  desable button Scanning
                spinner_bend.setEnabled(false);
                spinner.setEnabled(false);
                btn_imp.setEnabled(false);
            }

        }

        //TODO------------------- Обработка нажатия кнопки "импеданс" -------------------------------


        if (v.equals(btn_imp)) {  //    Обработка события нажатия кнопки 75-50

            if (connectedThread != null && connectThread.isConnect()) {

                if (impedans == 50) {
                    String command = "h";  //
                    btn_imp.setText(" 75 Ом");
                    impedans = 75;
                    connectedThread.write(command);
                } else {
                    String command = "g";  //
                    btn_imp.setText(" 50 Ом");
                    impedans = 50;
                    connectedThread.write(command);
                }
                Log.d(TAG, "Выбран мост на = " + impedans + " Om");
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(listBtDevices)) {
            BluetoothDevice device = bluetoothDevices.get(position);
            if (device != null) {
                connectThread = new ConnectThread(device);
                connectThread.start();

            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.equals(switchEnableBt)) {
            enableBt(isChecked);

            if (!isChecked) {
                showFrameMessage();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK && bluetoothAdapter.isEnabled()) {
                showFrameControls();
                setListAdapter(BT_BOUNDED);
            } else if (resultCode == RESULT_CANCELED) {
                enableBt(true);
            }
        }
    }

    //--------------- Управление показа окнами --------------------------------------

    private void showFrameMessage() {
        frameMessage.setVisibility(View.VISIBLE);
        frameLedControls.setVisibility(View.GONE);
        frameControls.setVisibility(View.GONE);
    }

    private void showFrameControls() {
        frameMessage.setVisibility(View.GONE);
        frameLedControls.setVisibility(View.GONE);
        frameControls.setVisibility(View.VISIBLE);
    }

    private void showFrameLedControls() {
        frameLedControls.setVisibility(View.VISIBLE);
        frameMessage.setVisibility(View.GONE);
        frameControls.setVisibility(View.GONE);
        frControls.setVisibility(View.GONE);

    }

    @SuppressLint("MissingPermission")
    private void enableBt(boolean flag) {
        if (flag) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BT);
        } else {
            bluetoothAdapter.disable();
        }
    }

    private void setListAdapter(int type) {

        bluetoothDevices.clear();
        int iconType = R.drawable.ic_bluetooth_bounded_device;

        switch (type) {
            case BT_BOUNDED:
                bluetoothDevices = getBoundedBtDevices();
                break;
            case BT_SEARCH:
                iconType = R.drawable.ic_bluetooth_search_device;
                break;
        }
        listAdapter = new BtListAdapter(this, bluetoothDevices, iconType);
        listBtDevices.setAdapter(listAdapter);
    }

    private ArrayList<BluetoothDevice> getBoundedBtDevices() {
        @SuppressLint("MissingPermission") Set<BluetoothDevice> deviceSet = bluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> tmpArrayList = new ArrayList<>();
        if (deviceSet.size() > 0) {
            tmpArrayList.addAll(deviceSet);
        }

        return tmpArrayList;
    }


    @SuppressLint("MissingPermission")
    private void enableSearch() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        } else {

            bluetoothAdapter.startDiscovery();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    btnEnableSearch.setText(R.string.stop_search);
                    pbProgress.setVisibility(View.VISIBLE);
                    setListAdapter(BT_SEARCH);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    btnEnableSearch.setText(R.string.start_search);
                    pbProgress.setVisibility(View.GONE);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        bluetoothDevices.add(device);
                        listAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };


    //==============================================================================================
    //
    //==============================================================================================

    private class ConnectThread extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private boolean success = false;

        public ConnectThread(BluetoothDevice device) {
            try {
                Method method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                bluetoothSocket = (BluetoothSocket) method.invoke(device, 1);

                progressDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //==============================================================================================
        //
        //==============================================================================================

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            try {
                bluetoothSocket.connect();
                success = true;

                progressDialog.dismiss();
            } catch (IOException e) {
                e.printStackTrace();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Не могу соединиться!", Toast.LENGTH_SHORT).show();
                });

                cancel();
            }

            if (success) {
                connectedThread = new ConnectedThread(bluetoothSocket);
                connectedThread.start();

                runOnUiThread(() -> {
                    showFrameLedControls();
                    connectedThread.write("g"); //  Принудительно в 50 Ом
                });
            }

        }

        //==============================================================================================
        //
        //==============================================================================================

        public boolean isConnect() {
            return bluetoothSocket.isConnected();
        }

        //==============================================================================================
        //
        //==============================================================================================

        public void cancel() {
            try {
                Log.d(TAG, "Закончили bluetoothSocket");
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //==============================================================================================
    //TODO       Создание потоков приема и передачи
    //==============================================================================================

    private class ConnectedThread extends Thread {

        private final InputStream inputStream;
        private final OutputStream outputStream;

        private boolean isConnected;

        public ConnectedThread(BluetoothSocket bluetoothSocket) {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.inputStream = inputStream;
            this.outputStream = outputStream;
            isConnected = true;

        }

 // TODO ---------------------  Чтение данных из потока ------------------------------------------


        @Override
        public void run() {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            StringBuilder buffer;
            buffer = new StringBuilder();

            while (isConnected) {                             // Если есть соединение с ВТ
                try {
                    int bytes = bis.read();                   // Читает байт из потока приема
                    buffer.append((char) bytes);              // Загружает прочитанный байт в буфер
                    int endOfLineIndex = buffer.indexOf("\r\n"); // Метод возвращает 1 если в буфере есть значение "\r\n" иначе -1

                    if (endOfLineIndex > 0) {                  // Если есть признак пeреноса строки то кидаем ее в буфер data_SRX и очищаем buffer
                        Counter_String++;                      // Считаем реальное количество полученных строк
                        data_SRX.add(buffer.toString());       // Кидаем принятую строку  в массив data_SRX

                        if ("E\r\n".equals(buffer.toString())) {
                        //          if(buffer.indexOf("E\r\n") > 0){
                            Log.d(TAG, "Получен флаг конца передачи - символ Е ");
                            Log.d(TAG, "Общее количество строк =  " + Counter_String);
                         //   writeFile(FILE_NAME);
                         //   readFile();
                            DataPoint[] Date_marker = new DataPoint[]{
                                                                new DataPoint(0, 0),
                                                              };


                            generateData();  // Расчет данных для графиков, данные разбираем из массива data_SRX
                            Log.d(TAG, "Расчет стартовой и конечной частоты ");
                            loadProgress.setVisibility(View.INVISIBLE);

                            series_SWR.resetData(Volue_SRX(1));  // очищает данные этого ряда и устанавливает новые. будет перекраивать график
                            series_R.resetData(Volue_SRX(2));  // очищает данные этого ряда и устанавливает новые. будет перекраивать график
                            series_X.resetData(Volue_SRX(3));  // очищает данные этого ряда и устанавливает новые. будет перекраивать график
                            series_Marcker.resetData(Date_marker);  // очищает данные этого ряда и устанавливает новые. будет перекраивать график


                            handler.postDelayed(() -> {

                                btnScanning.setEnabled(true);//чтоб она была снова доступна
                                set_Min_SWR();
                                spinner_bend.setEnabled(true);
                                spinner.setEnabled(true);
                                btn_imp.setEnabled(true);
                                writeFile();
                                data_SRX.clear();

                            }, 50);
                            Log.d(TAG, "Закончили вывод графиков, очистили data_SRX ");
                            Counter_String = 0;
                        }
                        Log.d(TAG, "Строка из буфера " + buffer);
                        buffer.delete(0, buffer.length()); // Очистка буфера строки от нуля до конца буфера


                    }  // конец строки


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //-------------------------------------------------------------------------------------------------

            try {
                bis.close();
                cancel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    //TODO---------------------- Запись данных в поток на передачу данных --------------------------


        public void write(String command) {
            byte[] bytes = command.getBytes();
            if (outputStream != null) {
                try {
                    outputStream.write(bytes);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            try {
                isConnected = false;
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //==============================================================================================
    //TODO   Расчет стартовой и конечной частоты, расчет интернаций и шгага частоты
    //==============================================================================================

    private void generateData() {

        int Value_step_cod;
        int Value_End_Freq;

//TODO  Из массива данных data_SRX вынимаем стартовую частоту, конечную частоту и шаг.
        Value_start_Freq = Integer.parseInt(data_SRX.get(1).replaceAll("[\\D]", "")); // Возвращает из строки только цифры, остальное отбрасывает
        Value_End_Freq = Integer.parseInt(data_SRX.get(2).replaceAll("[\\D]", "")); // Возвращает из строки только цифры, остальное отбрасывает
        Value_step_cod = Integer.parseInt(data_SRX.get(3).replaceAll("[\\D]", "")); // Возвращает из строки только цифры, остальное отбрасывает

        switch (Value_step_cod) {
            case 4:
                Value_step = 1;
                break;
            case 3:
                Value_step = 10;
                break;
            case 2:
                Value_step = 100;
                break;
            case 1:
                Value_step = 250;
                break;
            default:
                Value_step = 100;
                break;
        }

        Inter_step = (Value_End_Freq - Value_start_Freq) / Value_step;  // Вычисляем сколько будет интернаций - шагов точек по X

        Log.d(TAG, "Количество интернаций = " + Inter_step);

        Inter_step = Counter_String - 5; // Корректируем значение реально принятых строк к шагам разбора строк

        gvGraph.getViewport().setMinX(Value_start_Freq);  // Минимальное значение по X
        gvGraph.getViewport().setMaxX(Value_End_Freq);

    }

    //==============================================================================================
    //TODO                     Разбор
    //==============================================================================================

    private DataPoint[] Volue_SRX(int kl_data) {
        DataPoint[] Date = new DataPoint[Inter_step]; // Определяем массив Date с количеством интернаций - шагов точек по X

        Log.d(TAG, "Что разбираем = " + kl_data);

      //   writeFile(FILE_TEST);
       // writeFileSD();
        try {

            for (int i = 4; i < (Inter_step + 4); i++) {

                double value_SWR;
                int value_R;
                int value_X;
                try {

                String[] splitStr = data_SRX.get(i).split(";");
                //  String[] splitStr = "450;345;145".split(";");
                    value_SWR = (Integer.parseInt(splitStr[0])) / 100d;
                    value_R = Integer.parseInt(splitStr[1]);
                    value_X = Integer.parseInt(splitStr[2].replaceAll("[\\D]", "")); // Возвращает из строки только цифры, остальное отбрасывает


                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.d(TAG, "Индекс массива вышел за диапазон");
                    value_SWR = 100d;
                    value_R = 0;
                    value_X =0;
                }


                int x = (((i - 4) * Value_step) + Value_start_Freq); // Координаты по Х на графике

                switch (kl_data) {

                    case 1:
                        DataPoint swr = new DataPoint(x, value_SWR);
                        if (value_SWR < Corent_Value_SWR) {
                            Corent_Value_SWR = value_SWR;
                            Corent_Freq = x;
                        }

                        Date[i - 4] = swr;
                        Log.d(TAG, "Закончили case SWR_" + i);
                        break;
                    case 2:
                        DataPoint r = new DataPoint(x, value_R);
                        Date[i - 4] = r;
                        Log.d(TAG, "Закончили case R");
                        break;
                    case 3:
                        DataPoint xx = new DataPoint(x, value_X);
                        Date[i - 4] = xx;
                        Log.d(TAG, "Закончили case X");
                        break;
                }

            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Вернули данные разобранной строки");
        return Date;
    }
}

//TODO ------------------------------------ END  ---------------------------------------------------
