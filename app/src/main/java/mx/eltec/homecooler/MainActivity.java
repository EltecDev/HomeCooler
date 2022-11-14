package mx.eltec.homecooler;
/**
 * todo
 * Hay un detalle que se vio que tiene que ver con dificultades para
 * actualización en primer plano cada 6min
 *
 *
 * */
import static android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
import static android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mx.eltec.homecooler.Fragment.BeaconFragment;
import mx.eltec.homecooler.comunicacionBLE.BluetoothLeService;
import mx.eltec.homecooler.comunicacionBLE.BluetoothServices;
import mx.eltec.homecooler.Fragment.HomeFragment;
import mx.eltec.homecooler.utility.GetRealDataFromHexaImbera;
import mx.eltec.homecooler.utility.GlobalTools;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener, MonitorNotifier, BeaconFragment.BeaconListener, HomeFragment.HomeFragmentInterface {
    BottomNavigationView bottomNavigationView;
    TextView tvConnectionState, tvfwversion , tvUsuarioActual, tvappversion;

    androidx.appcompat.app.AlertDialog progressDialog;
    View dialogViewProgressBar;


    List<String> listData = new ArrayList<String>() ;
    List<String> listDataStatus = new ArrayList<String>() ;
    List<String> FinalListData = new ArrayList<String>() ;
    List<String> dataListPlantilla = new ArrayList<String>() ;

    BeaconManager beaconManager;
    RangeNotifier rangeNotifier;

    private BluetoothAdapter mBluetoothAdapter;
    String name, mac;
    boolean isActivityAlive=false;

    public boolean insideRegion;
    //public static final Region wildcardRegion = new Region("TREFP-iBeacon-Region", Identifier.parse("FDA50693-A4E2-4FB1-AFCF-C6EB07647825"), null, null);
    public static final Region wildcardRegion = new Region("TREFP-iBeacon-Region", null, null, null);

    //Pantalla de peticion inicial de permisos
    SharedPreferences sp;
    SharedPreferences.Editor esp;

    //Fragments
    HomeFragment homeFragment;
    BeaconFragment beaconFragment;
    Fragment active;

    FragmentManager fragmentManager ;
    //Bluetooth Services
    private BluetoothManager mBluetoothManager;
    BluetoothServices bluetoothServices;
    private BluetoothLeService bluetoothLeService;
    Activity activityMain;

    private BroadcastReceiver minuteUpdateReceiver=null;
    private int counter;


    final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        initBeacon(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityAlive = true;
        initOnResume();
    }

    private void initOnResume() {
        askPermission();
        BluetoothLeService BLE= bluetoothServices.getBluetoothLeService();
        if (BLE==null){
            esp.putBoolean("isconnected",false);
            esp.putString("mac","");
            esp.putString("trefpVersionName","");
            esp.apply();
            //disconnectBLE();
            GlobalTools.changeScreenConnectionStatus(tvConnectionState,sp);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopMonitoring();
        //startMonitoring();
        /*if (minuteUpdateReceiver!=null){
            unregisterReceiver(minuteUpdateReceiver);
            minuteUpdateReceiver=null;
        }*/


        isActivityAlive = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityAlive = false;
    }

    private void initBeacon(Context context){
        this.beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(context);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconManager.setDebug(true);

        // Uncomment the code below to use a foreground service to scan for beacons. This unlocks
        // the ability to continually scan for long periods of time in the background on Andorid 8+
        // in exchange for showing an icon at the top of the screen and a always-on notification to
        // communicate to users that your app is using resources in the background.

        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_baseline_bluetooth_searching_24_white);
        builder.setContentTitle("Buscando Home Cooler");
        /*Intent intent = new Intent(this, MonitoringActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("My Notification Channel ID",
                    "My Notification Name", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("My Notification Channel Description");
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channel.getId());
        }
        beaconManager.enableForegroundServiceScanning(builder.build(), 456);

        // For the above foreground scanning service to be useful, you need to disable
        // JobScheduler-based scans (used on Android 8+) and set a fast background scan
        // cycle that would otherwise be disallowed by the operating system.
        //
        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setBackgroundBetweenScanPeriod(90000L);
        beaconManager.setBackgroundScanPeriod(10000L);
        //beaconManager.setDebug(true);


        Log.d(TAG, "setting up background monitoring in app onCreate");
        //beaconManager.addMonitorNotifier(this);

        // If we were monitoring *different* regions on the last run of this app, they will be
        // remembered.  In this case we need to disable them here
        /*for (Region region: beaconManager.getMonitoredRegions()) {
            beaconManager.stopMonitoring(region);
        }*/



        rangeNotifier = new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                //if (!sp.getBoolean("isconnected",false)){
                    if (beacons.size() > 0) {

                        Beacon firstBeacon = beacons.iterator().next();
                        logToDisplay("RANGE-BEACON:"+firstBeacon.getBluetoothAddress());
                        stopRanging();
                        stopRangingMainActivity(firstBeacon.getBluetoothAddress(), firstBeacon.getBluetoothName());
                    }
                //}

            }

        };


    }

    private void init(){
        activityMain = this;
        /*minuteUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //actualizar datos
                new MyAsyncTaskGetPlantillaCommandForeground().execute();

            }
        };*/


        sp = getSharedPreferences("connection_preferences", Context.MODE_PRIVATE);
        esp = sp.edit();

        //FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        // To log a message to a crash report, use the following syntax:
        //crashlytics.log("E/TAG: my message testt");

        //Vista para controlar el cambio de color en la interfaz
        View backgroundView = findViewById(R.id.welcomeActivityBackgroundLayout);

        // Ajuste visual de la barra de notificaciones para android 6.0+:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = backgroundView.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            backgroundView.setSystemUiVisibility(flags);
            getWindow().setStatusBarColor(Color.parseColor("#f4f4f4"));
        }

        // Ajuste visual de la barra de navegación para android 8.0+:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            getWindow().setNavigationBarColor(Color.parseColor("#f4f4f4"));
        }

        //campos
        tvConnectionState = findViewById(R.id.tvconnectionstate);
        tvUsuarioActual = findViewById(R.id.tvUsuarioActual);
        tvfwversion = findViewById(R.id.tvfwversion);
        tvappversion = findViewById(R.id.tvappVersion);

        tvappversion.setText("Versión: "+BuildConfig.VERSION_NAME);

        //servicios bluetooth
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        fragmentManager= getSupportFragmentManager();
        bluetoothServices = new BluetoothServices(this, fragmentManager,tvConnectionState,tvfwversion, minuteUpdateReceiver,activityMain);

        if (bluetoothServices.BLESupport()){
            if (bluetoothServices.isBluetoothAdapterEnabled()){
                homeFragment = new HomeFragment(bluetoothServices,  this, tvConnectionState, tvfwversion);
                beaconFragment = new BeaconFragment(bluetoothServices,  this);
                active = homeFragment;
            }else {
                Toast.makeText(MainActivity.this, "El dispositivo tiene problemas con el Bluetooth o está apagado", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(MainActivity.this, "El dispositivo no soporta deteccion de BLE", Toast.LENGTH_SHORT).show();
        }

        //inicializar interfaces
        beaconFragment.beaconFragmentListener(this);
        homeFragment.HomeFragmentListener(this);

        //Inicializar interfaz MainActivity
        bottomNavigationView = (BottomNavigationView)findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);

        //if (!sp.getBoolean("permissionGiven",false))



        //listaBLEFragment.setconnectListenerListener(this);
        //plantillaFragment.setListener(this);

        fragmentManager.beginTransaction().add(R.id.flFragment,homeFragment, "").hide(homeFragment).commit();
        fragmentManager.beginTransaction().add(R.id.flFragment,beaconFragment, "").hide(beaconFragment).commit();

        fragmentManager.beginTransaction().show(beaconFragment).commit();
        active= beaconFragment;
    }

    private void askPermission(){

        if (Build.VERSION.SDK_INT >= 31){
            String[] perms = {  Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT};
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                esp.putBoolean("permissionGiven",true);
                esp.apply();
            } else {
                requestPermissions(perms,100);
            }
        }else{
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_FINE_LOCATION  }, 1666);
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            boolean         locationEnabled = false;
            try {
                locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch(Exception ignored) {}
            try {
                locationEnabled |= locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch(Exception ignored) {}
            if(!locationEnabled)
                Log.d("PERMISSION LOCATION ENABLED","CORREWCTO");
            else
                Log.d("PERMISSION LOCATION ENABLED","IJNININCORREWCTO");
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bottom_menu_item1:
                fragmentManager.beginTransaction().hide(active).show(homeFragment).commit();
                active=homeFragment;
                return true;
            case R.id.bottom_menu_item2:
                fragmentManager.beginTransaction().hide(active).show(beaconFragment).commit();
                active=beaconFragment;
                return true;
        }
        return false;
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "did enter region:"+region.getBluetoothAddress());
        insideRegion = true;
        // Send a notification to the user whenever a Beacon
        // matching a Region (defined above) are first seen.
        Log.d(TAG, "Sending notification.");
        startRanging();
        //stopMonitoringMainActivity(region.getBluetoothAddress(),region.getUniqueId());
    }

    @Override
    public void didExitRegion(Region region) {
        insideRegion = false;
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        startRanging();
        // do nothing here. logging happens in MonitoringActivity
        //logToDisplay("didDetermineStateForRegion called with state: " + (state == 1 ? "INSIDE ("+state+")" : "OUTSIDE ("+state+")"));

    }

    private void sendNotification() {
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Beacon Reference Notifications",
                    "Beacon Reference Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
            builder = new Notification.Builder(this, channel.getId());
        }
        else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_IMMUTABLE
                );
        builder.setSmallIcon(R.drawable.ic_baseline_bluetooth_searching_24_white);
        builder.setContentTitle("Se encontró un TREFP-Beacon");
        builder.setContentText("Pulsa aquí para ver detalles en la app");
        builder.setContentIntent(resultPendingIntent);
        notificationManager.notify(1, builder.build());
    }

    private void sendNotification(String datos) {
        /*NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText("Handshake:\n"+datos );


        //Now create the notification.  We must use the NotificationCompat or it will not work on the wearable.
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, "1")
                        .setSmallIcon(R.drawable.ic_baseline_bluetooth_searching_24_white)
                        .setContentTitle("Hola "+getLocalBluetoothName()+" encontré tu NeveraGame")
                        .setContentText("Estado actual:")
                        //.setContentIntent(viewPendingIntent)
                        .setChannelId("1")
                        .setStyle(bigStyle);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(1, notificationBuilder.build());*/


        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Beacon Reference Notifications",
                    "Beacon Reference Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
            builder = new Notification.Builder(this, channel.getId());
        }
        else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        /*TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_IMMUTABLE
                );*/
        builder.setSmallIcon(R.drawable.ic_baseline_bluetooth_searching_24_white);
        builder.setContentTitle("Hola "+getLocalBluetoothName()+" encontré tu Home Cooler");
        builder.setContentText("Estado actual:"+datos);
        //builder.setContentIntent(resultPendingIntent);
        notificationManager.notify(1, builder.build());
        /*
        Notification notification = new NotificationCompat.Builder(this, "Imberag")
                .setSmallIcon(R.drawable.ic_baseline_bluetooth_searching_24_white)
                .setContentTitle("Hola "+getLocalBluetoothName()+" encontré tu NeveraGame")
                .setContentText("Estado actual:")
                //.setLargeIcon()
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Mac:"+datos.substring(0,18)))
                .build();
        notification.notify();*/
    }

    private void sendNotification(List<String> datos) {
        /*NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText("Handshake:\n"+datos );


        //Now create the notification.  We must use the NotificationCompat or it will not work on the wearable.
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, "1")
                        .setSmallIcon(R.drawable.ic_baseline_bluetooth_searching_24_white)
                        .setContentTitle("Hola "+getLocalBluetoothName()+" encontré tu NeveraGame")
                        .setContentText("Estado actual:")
                        //.setContentIntent(viewPendingIntent)
                        .setChannelId("1")
                        .setStyle(bigStyle);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(1, notificationBuilder.build());*/
        logToDisplay("SENDNOTIFICATION:"+datos);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Beacon Reference Notifications",
                    "Beacon Reference Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
            builder = new Notification.Builder(this, channel.getId());
        }
        else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        /*TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_IMMUTABLE
                );*/
        builder.setSmallIcon(R.drawable.ic_baseline_bluetooth_searching_24_white);
        builder.setContentTitle("Hola "+getLocalBluetoothName()+" encontré tu NeveraGame");
        builder.setContentText("Estado actual de temperatura:"+datos.get(0));
        //builder.setContentIntent(resultPendingIntent);
        notificationManager.notify(1, builder.build());

        if (!sp.getString("mac","").equals(sp.getString("HomeCoolerMAC","")))//si el home cooler que se encontró es diferente al registrado actualmente
            showOptionsPopup("Home Cooler encontrado","¿Quieres agregar este Home Cooler como tuyo? (permitirá reconectarse a él cuando quieras)");

        stopMonitoring();

        /*
        Notification notification = new NotificationCompat.Builder(this, "Imberag")
                .setSmallIcon(R.drawable.ic_baseline_bluetooth_searching_24_white)
                .setContentTitle("Hola "+getLocalBluetoothName()+" encontré tu NeveraGame")
                .setContentText("Estado actual:")
                //.setLargeIcon()
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Mac:"+datos.substring(0,18)))
                .build();
        notification.notify();*/
    }

    private void desconectarBLE(){
        bluetoothServices.disconnect();
        GlobalTools.changeScreenConnectionStatus(tvConnectionState,sp);
    }

    public String getLocalBluetoothName(){
        if(mBluetoothAdapter == null){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        String name = mBluetoothAdapter.getName();
        if(name == null){
            System.out.println("Name is null!");
            name = mBluetoothAdapter.getAddress();
        }
        return name;
    }

    public void startBLEConnectionBackground(){
        bluetoothServices.connect(name,mac);
        try {
            Thread.sleep(5000);
            bluetoothLeService = bluetoothServices.getBluetoothLeService();
            if (bluetoothLeService.sendFirstComando("4021")){
                Log.d("","dataChecksum total:7");
                Thread.sleep(400);
                List<String> listData = new ArrayList<String>();
                List<String> FinalListData = new ArrayList<String>();
                listData = bluetoothLeService.getDataFromBroadcastUpdate();

                if (!listData.isEmpty()){
                    Log.d("listdata",":"+listData);
                    if(name.equals("IMBERA-TREFP")){
                        String isChecksumOk = GlobalTools.checkChecksumImberaTREFPB(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                        if (isChecksumOk.equals("ok")){
                            FinalListData = GetRealDataFromHexaImbera.convert(listData, "Handshake");
                            listData = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Handshake");
                            tvfwversion.setText("Modelo TREFPB:" + listData.get(1)
                                    + "\nVersión:" + listData.get(2)
                                    + "\nPlantilla:" + listData.get(3));
                            esp.putString("modelo",listData.get(1));
                            esp.putString("numversion",listData.get(2));
                            esp.putString("plantillaVersion",listData.get(3));
                            esp.putString("trefpVersionName",name);
                            esp.apply();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int i=0;i<listData.size();i++)
                                stringBuilder.append(listData.get(i));
                            sendNotification(stringBuilder.toString());
                        }else if (isChecksumOk.equals("notFirmware")){
                            //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                            GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",getApplicationContext());
                            esp.putString("trefpVersionName","");
                            esp.putString("numversion","");
                            esp.putString("plantillaVersion","");
                            esp.apply();
                            desconectarBLE();
                        }else if (isChecksumOk.equals("notok")){
                            GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getApplicationContext());
                            esp.putString("trefpVersionName","");
                            esp.putString("numversion","");
                            esp.putString("plantillaVersion","");
                            esp.apply();
                            desconectarBLE();
                        }
                    }else if (name.equals("IMBERA-OXXO")){
                        String isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                        if (isChecksumOk.equals("ok")){
                            FinalListData = GetRealDataFromHexaImbera.convert(listData, "Handshake");
                            listData = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Handshake");
                            tvfwversion.setText("Modelo TREFPB:" + listData.get(1)
                                    + "\nVersión:" + listData.get(2)
                                    + "\nPlantilla:" + listData.get(3));
                            esp.putString("modelo",listData.get(1));
                            esp.putString("numversion",listData.get(2));
                            esp.putString("plantillaVersion",listData.get(3));
                            esp.putString("trefpVersionName",name);
                            esp.apply();
                            Log.d("","");
                        }else if (isChecksumOk.equals("notFirmware")){
                            //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                            GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",getApplicationContext());
                            esp.putString("trefpVersionName","");
                            esp.putString("numversion","");
                            esp.putString("plantillaVersion","");
                            esp.apply();
                            desconectarBLE();
                        }else if (isChecksumOk.equals("notok")){
                            GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getApplicationContext());
                            esp.putString("trefpVersionName","");
                            esp.putString("numversion","");
                            esp.putString("plantillaVersion","");
                            esp.apply();
                            desconectarBLE();
                        }
                    }


                }else{
                    Toast.makeText(getApplicationContext(), "No se pudo obtener primera comunicación", Toast.LENGTH_SHORT).show();
                    esp.putString("trefpVersionName","");
                    esp.putString("numversion","");
                    esp.putString("plantillaVersion","");
                    esp.apply();
                    desconectarBLE();
                }
            }else{
                Log.d("","dataChecksum total:8");
                desconectarBLE();
            }



        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopMonitoringMainActivity(String mac,String name){
        Log.d(TAG, "MAC beacon:"+mac);
        //beaconManager.stopRangingBeacons(wildcardRegion);
        //beaconManager.removeAllRangeNotifiers();
        this.name = name;
        this.mac = mac;
        //startBLEConnectionBackground();
        new MyAsyncTaskConnectBLEBackground().execute();
    }

    private void stopRangingMainActivity(String mac,String name){
        Log.d(TAG, "MAC beacon:"+mac);
        //beaconManager.stopRangingBeacons(wildcardRegion);
        //beaconManager.removeAllRangeNotifiers();
        this.name = name;
        this.mac = mac;
        //startBLEConnectionBackground();
        new MyAsyncTaskConnectBLEBackground().execute();
    }

    @Override
    public void startRanging() {
        beaconManager.addRangeNotifier(rangeNotifier);
        try {
            beaconManager.startRangingBeaconsInRegion(wildcardRegion);
        } catch (RemoteException e) {
            Log.d(TAG,":"+e.getStackTrace());
            e.printStackTrace();
        }
    }

    @Override
    public void stopRanging() {
        beaconManager.stopRangingBeacons(wildcardRegion);
        beaconManager.removeRangeNotifier(rangeNotifier);
    }

    @Override
    public void startMonitoring() {
        Toast.makeText(this, "Monitoreo de HomeCooler iniciado", Toast.LENGTH_SHORT).show();
        beaconManager.addMonitorNotifier(this);
        beaconManager.startMonitoring(wildcardRegion);
    }

    @Override
    public void stopMonitoring() {
        Toast.makeText(this, "Monitoreo de HomeCooler detenido", Toast.LENGTH_SHORT).show();
        beaconManager.stopMonitoring(wildcardRegion);
        beaconManager.removeMonitorNotifier(this);
    }

    private void logToDisplay(String line) {
        Log.d(TAG,"LINE:"+line);
        /*cumulativeLog += line+"\n";
        runOnUiThread(new Runnable() {
            public void run() {
                EditText editText = (EditText) MonitoringActivity.this
                        .findViewById(R.id.monitoringText);
                editText.setText(cumulativeLog);
            }
        });*/
    }

    @Override
    public void connectHomeCooler(TextView tvdisplay) {
        this.name = sp.getString("HomeCoolerMAC","");
        this.mac = sp.getString("HomeCoolerMAC","");
        new MyAsyncTaskConnectBLEFromHome().execute();
    }

    @Override
    public void desconectar() {
        bluetoothServices.disconnect();
        /*if (minuteUpdateReceiver!=null){
            unregisterReceiver(minuteUpdateReceiver);
            minuteUpdateReceiver=null;
        }*/

    }

    class MyAsyncTaskConnectBLEBackground extends AsyncTask<Integer, Integer, String> {

        public MyAsyncTaskConnectBLEBackground() {
        }

        @Override
        protected String doInBackground(Integer... params) {
            bluetoothServices.connect(name,mac);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "resp";
        }

        @Override
        protected void onPostExecute(String result) {

            if (isActivityAlive)
                //GlobalTools.closeProgressDialog(progressDialog,dialogViewProgressBar);
            new MyAsyncTaskGetHandshakeBackground().execute();
            //esp.putBoolean("isconnected",true);
            //esp.apply();
        }

        @Override
        protected void onPreExecute() {
            if (isActivityAlive){
                //progressDialog = GlobalTools.createProgressDialog("TREFP encontrado, conectando con el dispositivo...",MainActivity.this,progressDialog,dialogViewProgressBar);
            }

        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    class MyAsyncTaskConnectBLEFromHome extends AsyncTask<Integer, Integer, String> {
        public MyAsyncTaskConnectBLEFromHome() {
        }
        @Override
        protected String doInBackground(Integer... params) {
            bluetoothServices.connect(name,mac);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "resp";
        }

        @Override
        protected void onPostExecute(String result) {

            if (isActivityAlive)
                progressDialog.dismiss();
                //GlobalTools.closeProgressDialog(progressDialog,dialogViewProgressBar);

                //new MyAsyncTaskGetHandshake().execute();
            //esp.putBoolean("isconnected",true);
            //esp.apply();
        }

        @Override
        protected void onPreExecute() {
            if (isActivityAlive){
                progressDialog = GlobalTools.createProgressDialog("TREFP encontrado, conectando con el dispositivo...",MainActivity.this,progressDialog,dialogViewProgressBar);
            }

        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    class MyAsyncTaskGetHandshakeBackground extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            if (sp.getBoolean("isconnected",false)){
                bluetoothLeService = bluetoothServices.getBluetoothLeService();
                if (bluetoothLeService.sendFirstComando("4021")){
                    Log.d("","dataChecksum total:7");
                    return "ok";
                }else
                    Log.d("","dataChecksum total:8");
                return "not";
            }else
                return "noconnected";
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            GlobalTools.closeProgressDialog(progressDialog,dialogViewProgressBar);

            if (result.equals("noconnected")) {
                Toast.makeText(getApplicationContext(), "Problemas de conexión, reconecta a tu BLE", Toast.LENGTH_SHORT).show();
            }else {
                if (result.equals("ok")){
                    List<String> listData = new ArrayList<String>();
                    List<String> FinalListData = new ArrayList<String>();
                    listData = bluetoothLeService.getDataFromBroadcastUpdate();

                    if (!listData.isEmpty()){
                        Log.d("listdata",":"+listData);
                        if(name.equals("IMBERA-TREFP")){
                            String isChecksumOk = GlobalTools.checkChecksumImberaTREFPB(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                            if (isChecksumOk.equals("ok")){
                                FinalListData = GetRealDataFromHexaImbera.convert(listData, "Handshake");
                                listData = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Handshake");
                                tvfwversion.setText("Modelo TREFPB:" + listData.get(1)
                                        + "\nVersión:" + listData.get(2)
                                        + "\nPlantilla:" + listData.get(3));
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i=0;i<listData.size();i++)
                                    stringBuilder.append(listData.get(i));
                                new MyAsyncTaskRealtimeStatusBackground().execute();
                                //sendNotification(stringBuilder.toString());
                            }else if (isChecksumOk.equals("notFirmware")){
                                //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",getApplication());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }else if (isChecksumOk.equals("notok")){
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getApplication());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }
                        }else if (name.equals("IMBERA-OXXO")){
                            String isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                            if (isChecksumOk.equals("ok")){
                                FinalListData = GetRealDataFromHexaImbera.convert(listData, "Handshake");
                                listData = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Handshake");
                                tvfwversion.setText("Modelo TREFPB:" + listData.get(1)
                                        + "\nVersión:" + listData.get(2)
                                        + "\nPlantilla:" + listData.get(3));
                                esp.putString("modelo",listData.get(1));
                                esp.putString("numversion",listData.get(2));
                                esp.putString("plantillaVersion",listData.get(3));
                                esp.putString("trefpVersionName",name);
                                esp.apply();
                                Log.d("","");
                            }else if (isChecksumOk.equals("notFirmware")){
                                //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",getApplicationContext());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }else if (isChecksumOk.equals("notok")){
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getApplicationContext());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }
                        }


                    }else{
                        Toast.makeText(getApplicationContext(), "No se pudo obtener primera comunicación", Toast.LENGTH_SHORT).show();
                        esp.putString("trefpVersionName","");
                        esp.putString("numversion","");
                        esp.putString("plantillaVersion","");
                        esp.apply();
                        desconectarBLE();
                    }

                }else{
                    Toast.makeText(getApplicationContext(), "Fallo al conectar a un BLE", Toast.LENGTH_SHORT).show();
                    desconectarBLE();
                }

            }

        }

        @Override
        protected void onPreExecute() {
            progressDialog = GlobalTools.createProgressDialog("Obteniendo primera comunicación...",MainActivity.this,progressDialog,dialogViewProgressBar);
        }
        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    class MyAsyncTaskGetHandshake extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            if (sp.getBoolean("isconnected",false)){
                bluetoothLeService = bluetoothServices.getBluetoothLeService();
                if (bluetoothLeService.sendFirstComando("4021")){
                    Log.d("","dataChecksum total:7");
                    return "ok";
                }else
                    Log.d("","dataChecksum total:8");
                return "not";
            }else
                return "noconnected";
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            GlobalTools.closeProgressDialog(progressDialog,dialogViewProgressBar);
            if (result.equals("noconnected")) {
                Toast.makeText(getApplicationContext(), "Problemas de conexión, reconecta a tu BLE", Toast.LENGTH_SHORT).show();
            }else {
                if (result.equals("ok")){
                    List<String> listData = new ArrayList<String>();
                    List<String> FinalListData = new ArrayList<String>();
                    listData = bluetoothLeService.getDataFromBroadcastUpdate();
                    if (!listData.isEmpty()){

                        if(sp.getString("HomeCoolerNAME","").equals("IMBERA-TREFP")){
                            String isChecksumOk = GlobalTools.checkChecksumImberaTREFPB(GetRealDataFromHexaImbera.cleanSpace(listData).toString());

                            if (isChecksumOk.equals("ok")){
                                Log.d("listdatalolo",":"+listData);
                                FinalListData = GetRealDataFromHexaImbera.convert(listData, "Handshake");
                                listData = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Handshake");
                                tvfwversion.setText("Modelo TREFPB:" + listData.get(1)
                                        + "\nVersión:" + listData.get(2)
                                        + "\nPlantilla:" + listData.get(3));
                                new MyAsyncTaskGetPlantillaCommand().execute();
                            }else if (isChecksumOk.equals("notFirmware")){
                                //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",getApplication());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }else if (isChecksumOk.equals("notok")){
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getApplication());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }
                        }else if (name.equals("IMBERA-OXXO")){
                            String isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                            if (isChecksumOk.equals("ok")){
                                FinalListData = GetRealDataFromHexaImbera.convert(listData, "Handshake");
                                listData = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Handshake");
                                tvfwversion.setText("Modelo TREFPB:" + listData.get(1)
                                        + "\nVersión:" + listData.get(2)
                                        + "\nPlantilla:" + listData.get(3));
                                esp.putString("modelo",listData.get(1));
                                esp.putString("numversion",listData.get(2));
                                esp.putString("plantillaVersion",listData.get(3));
                                esp.putString("trefpVersionName",name);
                                esp.apply();
                                Log.d("","");
                            }else if (isChecksumOk.equals("notFirmware")){
                                //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",getApplicationContext());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }else if (isChecksumOk.equals("notok")){
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getApplicationContext());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }
                        }
                    }else{
                        Toast.makeText(getApplicationContext(), "No se pudo obtener primera comunicación", Toast.LENGTH_SHORT).show();
                        esp.putString("trefpVersionName","");
                        esp.putString("numversion","");
                        esp.putString("plantillaVersion","");
                        esp.apply();
                        desconectarBLE();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Fallo al conectar a un BLE", Toast.LENGTH_SHORT).show();
                    desconectarBLE();
                }
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog = GlobalTools.createProgressDialog("Obteniendo primera comunicación...",MainActivity.this,progressDialog,dialogViewProgressBar);
        }
        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    class MyAsyncTaskRealtimeStatusBackground extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            if (sp.getBoolean("isconnected",false)){
                bluetoothLeService = bluetoothServices.getBluetoothLeService();
                if (bluetoothLeService.sendFirstComando("4053")){
                    Log.d("","dataChecksum total:7");
                    return "ok";
                }else
                    Log.d("","dataChecksum total:8");
                return "not";
            }else
                return "noconnected";
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            GlobalTools.closeProgressDialog(progressDialog,dialogViewProgressBar);

            if (result.equals("noconnected")) {
                Toast.makeText(getApplicationContext(), "Problemas de conexión, reconecta a tu BLE", Toast.LENGTH_SHORT).show();
            }else {
                if (result.equals("ok")){
                    List<String> listData = new ArrayList<String>();
                    List<String> FinalListData = new ArrayList<String>();
                    listData = bluetoothLeService.getDataFromBroadcastUpdate();

                    if (!listData.isEmpty()){
                        Log.d("listdata",":"+listData);
                        if(name.equals("IMBERA-TREFP")){
                            String isChecksumOk = GlobalTools.checkChecksumImberaTREFPB(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                            if (isChecksumOk.equals("ok")){
                                FinalListData = GetRealDataFromHexaImbera.convert(listData, "Lectura de datos tipo Tiempo real");
                                listData = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Lectura de datos tipo Tiempo real");
                                sendNotification(listData);
                                new MyAsyncTaskGetPlantillaCommand().execute();
                            }else if (isChecksumOk.equals("notFirmware")){
                                //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",getApplication());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }else if (isChecksumOk.equals("notok")){
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getApplication());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }
                        }else if (name.equals("IMBERA-OXXO")){
                            String isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                            if (isChecksumOk.equals("ok")){
                                FinalListData = GetRealDataFromHexaImbera.convert(listData, "Lectura de datos tipo Tiempo real");
                                listData = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Lectura de datos tipo Tiempo real");
                                tvfwversion.setText("Modelo TREFPB:" + listData.get(1)
                                        + "\nVersión:" + listData.get(2)
                                        + "\nPlantilla:" + listData.get(3));
                                esp.putString("modelo",listData.get(1));
                                esp.putString("numversion",listData.get(2));
                                esp.putString("plantillaVersion",listData.get(3));
                                esp.putString("trefpVersionName",name);
                                esp.apply();
                                Log.d("","");
                            }else if (isChecksumOk.equals("notFirmware")){
                                //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",getApplicationContext());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }else if (isChecksumOk.equals("notok")){
                                GlobalTools.showInfoPopup("Información del equipo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getApplicationContext());
                                esp.putString("trefpVersionName","");
                                esp.putString("numversion","");
                                esp.putString("plantillaVersion","");
                                esp.apply();
                                desconectarBLE();
                            }
                        }


                    }else{
                        Toast.makeText(getApplicationContext(), "No se pudo obtener primera comunicación", Toast.LENGTH_SHORT).show();
                        esp.putString("trefpVersionName","");
                        esp.putString("numversion","");
                        esp.putString("plantillaVersion","");
                        esp.apply();
                        desconectarBLE();
                    }

                }else{
                    Toast.makeText(getApplicationContext(), "Fallo al conectar a un BLE", Toast.LENGTH_SHORT).show();
                    desconectarBLE();
                }

            }

        }

        @Override
        protected void onPreExecute() {
            progressDialog = GlobalTools.createProgressDialog("Obteniendo primera comunicación...",MainActivity.this,progressDialog,dialogViewProgressBar);
        }
        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    class MyAsyncTaskGetPlantillaCommandForeground extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            bluetoothServices.sendCommand("readParam");

            try {
                Thread.sleep(280);
                bluetoothLeService = bluetoothServices.getBluetoothLeService();
                if (bluetoothLeService == null){
                    dataListPlantilla.clear();
                    listData.clear();
                    return "";
                }else {
                    listData = bluetoothLeService.getDataFromBroadcastUpdate();
                    bluetoothLeService.sendFirstComando("4053");
                    Thread.sleep(200);
                    listDataStatus= bluetoothLeService.getDataFromBroadcastUpdate();
                    FinalListData = GetRealDataFromHexaImbera.convert(listDataStatus,"Lectura de datos tipo Tiempo real");
                    dataListPlantilla = GetRealDataFromHexaImbera.GetRealData(FinalListData,"Lectura de datos tipo Tiempo real");
                    return "resp";
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return "";
            }

        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Thread.sleep(800);
                /*if (progressdialog != null)progressdialog.dismiss();
                progressdialog=null;*/
                if (result.equals("")){
                    Log.d(TAG,":No se pudo recuperar plantilla res MAIN FOREg");
                }else {
                    if (result.equals("resp")){
                        Log.d(TAG,":Si se pudo recuperar plantilla MAIN FOREg");
                        //guardarPlantillaActual();

                        //createCommandtoSend();
                        //counterText.setText("" + counter);

                        //tvTemperatura.setText(sp.getString("Temp1Estatus","")+"°C");
                    }else{
                        Log.d(TAG,":No se pudo recuperar plantilla noresp");
                    }

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
            //createProgressDialog("Obteniendo estado de tu Home Cooler...");
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    class MyAsyncTaskGetPlantillaCommand extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            bluetoothServices.sendCommand("readParam");
            dataListPlantilla.clear();
            listData.clear();
            listDataStatus.clear();
            try {
                Thread.sleep(300);
                if (bluetoothLeService == null){

                    return "";
                }else {
                    listData = bluetoothLeService.getDataFromBroadcastUpdate();
                    bluetoothLeService.sendFirstComando("4053");
                    Thread.sleep(200);
                    listDataStatus= bluetoothLeService.getDataFromBroadcastUpdate();
                    FinalListData = GetRealDataFromHexaImbera.convert(listDataStatus,"Lectura de datos tipo Tiempo real");
                    dataListPlantilla = GetRealDataFromHexaImbera.GetRealData(FinalListData,"Lectura de datos tipo Tiempo real");

                    return "resp";
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return "exception";
            }



        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Thread.sleep(300);
                if (result.equals("")){
                    Log.d(TAG,":No se pudo recuperar plantilla");
                }else {
                    if (result.equals("resp")){
                        Log.d(TAG,":Si se pudo recuperar plantilla");
                        guardarPlantillaActual();
                    }else{
                        Log.d(TAG,":No se pudo recuperar plantilla");
                    }

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    private void guardarPlantillaActual() {
        StringBuilder s = GetRealDataFromHexaImbera.cleanSpace(listData);
        StringBuilder s2 = GetRealDataFromHexaImbera.cleanSpace(dataListPlantilla);
        esp.putString("currentPlantilla",s.toString());
        esp.putString("currentStatusCompleto",s2.toString());

        esp.putString("Temp1Estatus",dataListPlantilla.get(0));
        esp.putString("Temp2Estatus",dataListPlantilla.get(1));
        esp.putString("VoltajeEstatus",dataListPlantilla.get(2));
        esp.putString("ActuadoresEstatus",dataListPlantilla.get(3));
        if (dataListPlantilla.size()==5)
            esp.putString("AlarmasEstatus",dataListPlantilla.get(4));

        esp.apply();
        homeFragment.updateInterfazFromPlantilla();
        /*if (minuteUpdateReceiver==null)
            startMinuteUpdater();*/

    }

    private void showOptionsPopup(String tittle, String content){
        final AlertDialog alexaDialog;
        LayoutInflater inflater = getLayoutInflater();//getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.popup_option, null, false);
        AlertDialog.Builder adb = new AlertDialog.Builder(this,R.style.Theme_AppCompat_Light_Dialog_Alert_eltc);
        adb.setView(dialogView);

        TextView tv1 = (TextView) dialogView.findViewById(R.id.tvtitutloPopupOption);
        tv1.setText(tittle);
        TextView tv2 = (TextView) dialogView.findViewById(R.id.tvcontentePopupOption);
        tv2.setText(content);

        alexaDialog = adb.create();
        alexaDialog.setCanceledOnTouchOutside(false);
        alexaDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alexaDialog.show();
        dialogView.findViewById(R.id.btnAceptarPopupOption).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                esp.putString("HomeCoolerMAC",sp.getString("mac",""));
                esp.putString("HomeCoolerNAME",sp.getString("name",""));
                esp.apply();
                alexaDialog.dismiss();
            }
        });
        dialogView.findViewById(R.id.btnCancelarPopupOption).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alexaDialog.dismiss();
            }
        });

    }

    public void startMinuteUpdater() {



    }
}