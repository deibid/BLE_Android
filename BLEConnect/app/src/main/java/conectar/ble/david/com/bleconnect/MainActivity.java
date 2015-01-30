package conectar.ble.david.com.bleconnect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    TextView mtvProgress;
    Button mbtStart;

    BluetoothManager mbtManager;
    BluetoothAdapter mbtAdapter;

    BluetoothLeScanner mbtScanner;
    BluetoothGatt mBluetoothGatt;

    BluetoothGattCharacteristic mBtCharacteristic;
    BluetoothGattService mSimpleService;

    private boolean newDevice= true;
    private boolean mWriten = false;

    private final static String UUID_SIMPLE_PROFILE = "0000fff0-0000-1000-8000-00805f9b34fb";

    private static int STATE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mtvProgress = (TextView)findViewById(R.id.tvProgress);
        mbtStart = (Button)findViewById(R.id.btStart);


        mtvProgress.setText("Espera..");


      /*  mbtStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:

                        //mBluetoothGatt.writeCharacteristic();

                        mBtCharacteristic.setValue(0xFF,BluetoothGattCharacteristic.FORMAT_SINT16,0);
                        mBluetoothGatt.writeCharacteristic(mBtCharacteristic);
                        break;
                    case MotionEvent.ACTION_UP:

                        mBtCharacteristic.setValue(0x33,BluetoothGattCharacteristic.FORMAT_SINT16,0);
                        mBluetoothGatt.writeCharacteristic(mBtCharacteristic);
                        //codigo para cuando el boton es soltado
                        break;
                }
                return false;
            }
        });*/


        mbtManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mbtAdapter = mbtManager.getAdapter();

        if(mbtAdapter == null || !mbtAdapter.isEnabled()){
            Intent btEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btEnable,1);
        }

        mbtScanner = mbtAdapter.getBluetoothLeScanner();

        startScan();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        return;
    }

    private void startScan(){

        mbtScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                BluetoothDevice device = result.getDevice();
                Log.d("SCAn LE", "DEVICE 1 name: " + device.getName());
                Log.d("SCAn LE", "DEVICE 1 address: " + device.getAddress());

                if (device != null) {
                    if (device.getName().equals("SimpleBLEPeripheral"))
                        if (newDevice == true) {
                            mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mBtCallback);
                            newDevice = false;
                        }
                }
            }
        });

        mHandler.postDelayed(mStopRunnable, 2500);


    }

    private void serviceOperation(){

        BluetoothGattService simpleService = mBluetoothGatt.getService(UUID.fromString(UUID_SIMPLE_PROFILE));
        if(simpleService == null){
            Log.d("SERVICE","ES NULL");
        }else{
            Log.d("SERVICE","SERVICIO CONECTADO");
        }


        List<BluetoothGattCharacteristic> characteristics = simpleService.getCharacteristics();
        BluetoothGattCharacteristic charPrueba;

        for(int i =0;i<characteristics.size();i++){
         charPrueba = characteristics.get(i);
           Log.d("UUID Recall ronda " + i, " " + charPrueba.getUuid());
        }
    }

    private void stopScan(){

        mHandler.postDelayed(mStartRunnable, 5000);
    }



    private final BluetoothGattCallback mBtCallback = new BluetoothGattCallback() {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            Log.d("mBtCallback write", "Acabo de escribir");


        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);


            switch(newState){
                case BluetoothProfile.STATE_CONNECTED:

                    Log.d("CONNECTION CHANGED","ESTOY CONECTADO");
                    STATE = BluetoothProfile.STATE_CONNECTED;
                    mBluetoothGatt.discoverServices();
                    //serviceOperation();
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d("CONNECTION CHANGED","ESTOY DESCONECTADO");
                    STATE = BluetoothProfile.STATE_DISCONNECTED;

                    break;

                case BluetoothProfile.STATE_CONNECTING:
                    STATE = BluetoothProfile.STATE_CONNECTING;
                    break;

                case BluetoothProfile.STATE_DISCONNECTING:
                    STATE = BluetoothProfile.STATE_DISCONNECTING;
                    break;

            }


            runOnUiThread(mLED);



        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            List<BluetoothGattService> services = gatt.getServices();
            Log.d("DISCOVER", "Status onServiceDiscovered: " + services);

            for(BluetoothGattService s: services){
                List<BluetoothGattCharacteristic> characteristics = s.getCharacteristics();
                Log.d("SERVICES","Encontrados----> "+s.getUuid());
                for(BluetoothGattCharacteristic c: characteristics) {
                    Log.d("CHARS", "Encontrados----> " + c.getUuid());
                }
            }

            mSimpleService = gatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"));

            if(mSimpleService == null){
                Log.e("SIMPLE SERVICE","Es Null");
            }else {
                Log.e("SIMPLE SERVICE", "Tienes servicio bitch!");
            }

            mBtCharacteristic = mSimpleService.getCharacteristic
                    (UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"));

            if(mBtCharacteristic == null){
                Log.e("SIMPLE CHARACTERISTIC","Es Null");
            }else{
                Log.e("SIMPLE CHARACTERISTIC","Tienes characteristic bitch!");
            }



            if(mWriten == false ) {
                mBtCharacteristic.setValue(0xFF, BluetoothGattCharacteristic.FORMAT_SINT8, 0);
                mBluetoothGatt.writeCharacteristic(mBtCharacteristic);
                mWriten = true;
            }
               // mWriten = true;

            //mHandler.post(mLED);


        }
    };



    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
        stopScan();
        }
    };

    private Runnable mStartRunnable = new Runnable (){

        @Override
        public void run() {
        startScan();
        }
    };


    private Runnable mLED = new Runnable(){
        @Override
        public void run() {

          switch(STATE){

              case BluetoothProfile.STATE_CONNECTED:
                  mtvProgress.setText("Conectado");
                  break;
              case BluetoothProfile.STATE_DISCONNECTED:
                  mtvProgress.setText("Desconectado");
                  break;
              case BluetoothProfile.STATE_CONNECTING:
                  mtvProgress.setText("Conectando...");
                  break;
              case BluetoothProfile.STATE_DISCONNECTING:
                  mtvProgress.setText("Desconectando...");
                  break;
          }


        }
    };


    private Handler mHandler = new Handler(){
        //actualizar el UI
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

        }
    };

}





