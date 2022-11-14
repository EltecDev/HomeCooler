package mx.eltec.homecooler.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import mx.eltec.homecooler.R;
import mx.eltec.homecooler.comunicacionBLE.BluetoothLeService;
import mx.eltec.homecooler.comunicacionBLE.BluetoothServices;
import mx.eltec.homecooler.utility.GetRealDataFromHexaImbera;
import mx.eltec.homecooler.utility.GetRealDataFromHexaOxxoDisplay;
import mx.eltec.homecooler.utility.GlobalTools;

public class BeaconFragment extends Fragment {

    BeaconListener beaconListener;

    BluetoothServices bluetoothServices;
    BluetoothLeService bluetoothLeService;

    ArrayList<String> listData = new ArrayList<String>() ;
    List<String> realDataList = new ArrayList<String>() ;
    List<String> FinalListData2 = new ArrayList<String>() ;
    List<String> FinalListDataRealState = new ArrayList<String>() ;
    List<String> FinalListDataHandshake = new ArrayList<String>() ;
    List<String> FinalListDataTiempo = new ArrayList<String>() ;
    List<String> FinalListDataEvento = new ArrayList<String>() ;

    //Pantalla de peticion inicial de permisos
    SharedPreferences sp;
    SharedPreferences.Editor esp;
    Context context;

    TextView tvhandshake, tvTime, tvEvent, tvRealState, tvReadParam, tvsubtitulo;
    TextView tvLogg,tvLogg1,tvLogg2,tvLogg3;
    Button btnStartMonitoring;

    androidx.appcompat.app.AlertDialog progressdialog=null;
    View dialogViewProgressBar;

    String LOG = "BeaconFragment";

    //private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);

    public BeaconFragment(){}

    public BeaconFragment(BluetoothServices bluetoothServices, Context context){
        this.bluetoothServices = bluetoothServices;
        this.context = context;
        this.sp = context.getSharedPreferences("connection_preferences",Context.MODE_PRIVATE);
        this.esp = sp.edit();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.fragment_beacon, container, false);
        init(view);

        view.findViewById(R.id.btnIniciarRanging).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beaconListener.startRanging();
                //beaconListener.stopRanging();
            }
        });
        view.findViewById(R.id.btnDetenerRanging).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beaconListener.stopRanging();
                //beaconListener.stopRanging();
            }
        });

        view.findViewById(R.id.btnIniciarMonitoreo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beaconListener.startMonitoring();
                //beaconListener.stopRanging();
            }
        });
        view.findViewById(R.id.btnDetenerMonitoreo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beaconListener.stopMonitoring();
                //beaconListener.stopRanging();
            }
        });

        return view;
    }


    private void requestStatus(){
        bluetoothLeService = bluetoothServices.getBluetoothLeService();
        if (bluetoothLeService!=null)
            new MyAsyncTaskGetActualStatus().execute();
        else
            Toast.makeText(getContext(), "No estás conectado a BLE", Toast.LENGTH_SHORT).show();
    }

    private boolean convertInfo(){
        List<String> FinalListData = new ArrayList<String>() ;
        listData.clear();
        FinalListDataRealState.clear();
        try {
            if (sp.getString("trefpVersionName","").equals("IMBERA-TREFP")){
                bluetoothServices.sendCommand("handshake","4021");
                Thread.sleep(450);
                listData.add(bluetoothLeService.getDataFromBroadcastUpdateString());
                if(GetRealDataFromHexaImbera.cleanSpace(listData).toString().length() == 0){
                    return false;
                }else{
                    String isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                    Log.d("ishandshake",":"+isChecksumOk);
                    if (isChecksumOk.equals("ok")){
                        if (listData.get(0).length() == 0){
                            FinalListDataHandshake.clear();
                        }else{
                            FinalListData = GetRealDataFromHexaImbera.convert(listData, "Handshake");
                            FinalListDataHandshake = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Handshake");
                        }
                    }else{
                        GlobalTools.showInfoPopup("Información del equipo:Handshake","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getContext());
                    }

                    isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                    Log.d("istiempo",":"+isChecksumOk);
                    if (isChecksumOk.equals("ok")){
                        FinalListData.clear();
                        FinalListData2.clear();
                        listData.clear();
                        bluetoothServices.sendCommand("time","4060");
                        Thread.sleep(3050);
                        listData.add(bluetoothLeService.getDataFromBroadcastUpdateString());
                        if (listData.get(0).length() == 0){
                            FinalListDataTiempo.clear();
                        }else{
                            FinalListData = GetRealDataFromHexaImbera.convert(listData, "Lectura de datos tipo Tiempo");
                            FinalListDataTiempo = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Lectura de datos tipo Tiempo");
                        }
                    }else{
                        GlobalTools.showInfoPopup("Información del equipo:Tiempo","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getContext());
                    }

                    isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                    Log.d("isevento",":"+isChecksumOk);
                    if (isChecksumOk.equals("ok")){
                        FinalListData.clear();
                        FinalListData2.clear();
                        listData.clear();
                        bluetoothServices.sendCommand("event","4061");
                        Thread.sleep(1050);
                        listData.add(bluetoothLeService.getDataFromBroadcastUpdateString());
                        if (listData.get(0).length() == 0){
                            FinalListDataEvento.clear();
                        }else{
                            FinalListData = GetRealDataFromHexaImbera.convert(listData, "Lectura de datos tipo Evento");
                            FinalListDataEvento = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Lectura de datos tipo Evento");
                        }
                    }else{
                        GlobalTools.showInfoPopup("Información del equipo:Evento","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getContext());
                    }

                    isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                    Log.d("isrealtime",":"+isChecksumOk);
                    if (isChecksumOk.equals("ok")){
                        FinalListData.clear();
                        FinalListData2.clear();
                        listData.clear();
                        bluetoothServices.sendCommand("realState","4053");
                        Thread.sleep(450);
                        listData.add(bluetoothLeService.getDataFromBroadcastUpdateString());
                        if (listData.get(0).length() == 0){
                            FinalListDataRealState.clear();
                        }else{
                            FinalListData = GetRealDataFromHexaImbera.convert(listData, "Lectura de datos tipo Tiempo real");
                            FinalListDataRealState = GetRealDataFromHexaImbera.GetRealData(FinalListData, "Lectura de datos tipo Tiempo real");
                        }
                    }else{
                        GlobalTools.showInfoPopup("Información del equipo:Estado real","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getContext());
                    }
                    return false;
                }


            }else if( sp.getString("trefpVersionName","").equals("IMBERA-OXXO")){
                bluetoothServices.sendCommand("handshake","4021");
                Thread.sleep(250);
                listData.add(bluetoothLeService.getDataFromBroadcastUpdateString());
                if(GetRealDataFromHexaImbera.cleanSpace(listData).toString().length() == 0){
                    return false;
                }else{
                    String isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                    Log.d("ishandshake",":"+isChecksumOk);
                    if (isChecksumOk.equals("ok")){
                        if (listData.get(0).length() == 0){
                            FinalListDataHandshake.clear();
                        }else{
                            FinalListData = GetRealDataFromHexaOxxoDisplay.convert(listData, "Handshake");
                            FinalListDataHandshake = GetRealDataFromHexaOxxoDisplay.GetRealData(FinalListData, "Handshake");
                        }
                    }else{
                        GlobalTools.showInfoPopup("Información del equipo:Handshake","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getContext());
                    }

                    isChecksumOk = GlobalTools.checkChecksum(GetRealDataFromHexaImbera.cleanSpace(listData).toString());
                    Log.d("isresltatus",":"+isChecksumOk);
                    if (isChecksumOk.equals("ok")){
                        FinalListData.clear();
                        FinalListData2.clear();
                        listData.clear();

                        bluetoothServices.sendCommand("realState","4053");
                        Thread.sleep(250);
                        listData.add(bluetoothLeService.getDataFromBroadcastUpdateString());
                        if (listData.get(0).length() == 0){
                            FinalListDataRealState.clear();
                        }else{
                            FinalListData = GetRealDataFromHexaOxxoDisplay.convert(listData, "Lectura de datos tipo Tiempo real");
                            FinalListDataRealState = GetRealDataFromHexaOxxoDisplay.GetRealData(FinalListData, "Lectura de datos tipo Tiempo real");
                        }
                    }else{
                        GlobalTools.showInfoPopup("Información del equipo:Estado real","Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",getContext());
                    }

                }


            }

        } catch (InterruptedException e) {
            e.printStackTrace();

        }

        return true;

    }

    class MyAsyncTaskGetActualStatus extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            if (convertInfo()){
                return "true";
            }else{
                return "false";
            }

        }

        @Override
        protected void onPostExecute(String result) {
            if (progressdialog != null)progressdialog.dismiss();
            progressdialog=null;

            if (result.equals("true")){
                if (sp.getString("trefpVersionName","").equals("IMBERA-TREFP")){
                    tvsubtitulo.setText("Aquí se muestra el estado actual de tu dispositivo IMBERA-TREFPB");

                    tvhandshake.setVisibility(View.VISIBLE);
                    tvLogg.setVisibility(View.VISIBLE);
                    tvLogg1.setVisibility(View.VISIBLE);
                    tvLogg2.setVisibility(View.VISIBLE);
                    tvLogg3.setVisibility(View.VISIBLE);
                    tvEvent.setVisibility(View.VISIBLE);
                    tvTime.setVisibility(View.VISIBLE);
                    tvRealState.setVisibility(View.VISIBLE);

                    tvhandshake.setText("MAC:" + FinalListDataHandshake.get(0)
                            + "\nModelo TREFPB:" + FinalListDataHandshake.get(1)
                            + "\nVersión:" + FinalListDataHandshake.get(2)
                            + "\nPlantilla:" + FinalListDataHandshake.get(3));



                    StringBuilder stringb = new StringBuilder();
                    int j=1;
                    for (int i=0;i<FinalListDataTiempo.size(); i+=4){
                        if (i+3>FinalListDataTiempo.size()){
                            break;
                        }   else {
                            stringb.append("\nIteración "+j+
                                    "\nTimeStamp:" + FinalListDataTiempo.get(i)
                                    +"\nTemperatura 1:" + FinalListDataTiempo.get(i+1) + " °C"
                                    + "\nTemperatura 2:" + FinalListDataTiempo.get(i+2) + " °C"
                                    + "\nVoltaje:" + FinalListDataTiempo.get(i+3)+ "\n");
                            j++;
                        }
                    }
                    tvTime.setText(stringb.toString());

                    StringBuilder stringa = new StringBuilder();
                    j=1;
                    for (int i=0;i<FinalListDataEvento.size(); i+=6){
                        if (i+5>FinalListDataEvento.size()){
                            break;
                        }   else {
                            stringa.append("\nIteración "+j+
                                    "\nTimeStamp START:" + FinalListDataEvento.get(i)
                                    +"\nTimeStamp END:" + FinalListDataEvento.get(i+1)
                                    +"\nTipo de evento:\n" + FinalListDataEvento.get(i+2)
                                    +"\nTemperatura 1I:" + FinalListDataEvento.get(i+3) + " °C"
                                    + "\nTemperatura 2F:" + FinalListDataEvento.get(i+4) + " °C"
                                    + "\nVoltaje:" + FinalListDataEvento.get(i+5)+ "\n");
                            j++;
                        }
                    }
                    tvEvent.setText(stringa.toString());

                    tvRealState.setText("\nTemperatura 1:" + FinalListDataRealState.get(0) + " °C"
                            + "\nTemperatura 2:" + FinalListDataRealState.get(1) + " °C"
                            + "\nVoltage:" + FinalListDataRealState.get(2)
                            + "\nActuadores:" + FinalListDataRealState.get(3)
                            + "\nAlarmas:" + FinalListDataRealState.get(4)+ "\n"
                    );
                }else if (sp.getString("trefpVersionName","").contains("IMBERA-OXXO")){
                    tvsubtitulo.setText("Aquí se muestra el estado actual de tu dispositivo IMBERA-OXXO");

                    //OXXO no tiene LOGGER
                    tvhandshake.setVisibility(View.VISIBLE);
                    tvLogg.setVisibility(View.GONE);
                    tvLogg1.setVisibility(View.GONE);
                    tvLogg2.setVisibility(View.GONE);
                    tvLogg3.setVisibility(View.GONE);
                    tvEvent.setVisibility(View.GONE);
                    tvTime.setVisibility(View.GONE);
                    tvRealState.setVisibility(View.VISIBLE);
                    if (!FinalListDataHandshake.isEmpty()){
                        tvhandshake.setText("MAC:" + FinalListDataHandshake.get(0)
                                + "\nModelo TREFPB:" + FinalListDataHandshake.get(1)
                                + "\nVersión:" + FinalListDataHandshake.get(2)
                                + "\nPlantilla:" + FinalListDataHandshake.get(3)+ "\n");
                    }else{
                        tvhandshake.setText("No se pudo obtener información Handshake de tu Trefp, por favor intenta reconectarte");
                    }
                    if (!FinalListDataRealState.isEmpty()){
                        if (FinalListDataRealState.get(0).equals("8")){
                            tvRealState.setText("\nTemperatura 1:" + FinalListDataRealState.get(1) + " °C"
                                    + "\nTemperatura 2:" + FinalListDataRealState.get(2) + " °C"
                                    + "\nVoltage:" + FinalListDataRealState.get(3)
                                    + "\nActuadores:" + FinalListDataRealState.get(4)
                                    + "\nAlarmas:" + FinalListDataRealState.get(5)+ "\n"
                            );
                        }else{
                            tvRealState.setText("\nTemperatura 1:" + FinalListDataRealState.get(1) + " °C"
                                    + "\nTemperatura 2:" + FinalListDataRealState.get(2) + " °C"
                                    + "\nTemperatura 3:" + FinalListDataRealState.get(3) + " °C"
                                    + "\nVoltage:" + FinalListDataRealState.get(4)
                                    + "\nActuadores:" + FinalListDataRealState.get(5)
                                    + "\nAlarmas:" + FinalListDataRealState.get(6)+ "\n"
                            );
                        }

                    }else
                        tvRealState.setText("No se pudo obtener información Estado en tiempo real de tu Trefp, por favor intenta reconectarte");

                }
            }else if(result.equals("false")){
                GlobalTools.showInfoPopup("Información","La comunicación no fue exitosa, reintenta o reconecta con el equipo TREFP",getContext());
            }



        }

        @Override
        protected void onPreExecute() {
            tvhandshake.setText("");
            tvRealState.setText("");
            tvEvent.setText("");
            tvTime.setText("");
            createProgressDialog("Obteniendo estado actual...");
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    public void init(View v){
        btnStartMonitoring = (Button)v.findViewById(R.id.btnIniciarMonitoreo);
    }

    public void createProgressDialog(String string){
        if(progressdialog == null){
            //Crear dialogos de "pantalla de carga" y "popups if"
            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);//getLayoutInflater();
            dialogViewProgressBar = inflater.inflate(R.layout.show_progress_bar, null, false);
            androidx.appcompat.app.AlertDialog.Builder adb = new androidx.appcompat.app.AlertDialog.Builder(getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert_eltc);
            adb.setView(dialogViewProgressBar);
            progressdialog = adb.create();
            progressdialog.setCanceledOnTouchOutside(false);
            progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            TextView txt = ((TextView) dialogViewProgressBar.findViewById(R.id.txtInfoProgressBar));
            txt.setText(string);
        }
        progressdialog.show();
    }

    private void logToDisplay(String line) {
        Log.d(LOG,":"+line);
        /*cumulativeLog += line+"\n";
        runOnUiThread(new Runnable() {
            public void run() {
                EditText editText = (EditText) MonitoringActivity.this
                        .findViewById(R.id.monitoringText);
                editText.setText(cumulativeLog);
            }
        });*/
    }

    public interface BeaconListener{
        public void startRanging();
        public void stopRanging();
        public void startMonitoring();
        public void stopMonitoring();
    }

    public void beaconFragmentListener(BeaconListener beaconListener){
        this.beaconListener = beaconListener;

    }
}
