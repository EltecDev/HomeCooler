package mx.eltec.homecooler.Fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import mx.eltec.homecooler.R;
import mx.eltec.homecooler.comunicacionBLE.BluetoothLeService;
import mx.eltec.homecooler.comunicacionBLE.BluetoothServices;
import mx.eltec.homecooler.utility.GetHexFromRealDataImbera;
import mx.eltec.homecooler.utility.GetRealDataFromHexaImbera;

public class HomeFragment extends Fragment implements BluetoothServices.BLEServicesInterface{

    String TAG="HomeFragment";
    BluetoothServices bluetoothServices;
    BluetoothLeService bluetoothLeService;

    ImageButton imgbtnonOffLampara, imgbtnOnOff, imgbtnRefresco, imgbtnCerveza;
    Button btnRojo, btnAzul, btnVerde, btnRojo_azul,btnRojo_verde, btnVerde_azul,btnTransicion;
    StringBuilder nuevaPlantillaEnviar;
    TextView tvTemperatura, tvPuerta, tvVoltaje;
    TextView tvconnectionstate, tvfwversion, tvconnectionstate0;

    boolean isplantillaColorAzulPushed=false;
    boolean isplantillaColorRojoPushed=false;
    boolean isplantillaColorVerdePushed=false;
    boolean isplantillaColorVerdeAzulPushed=false;
    boolean isplantillaColorRojo_azulPushed=false;
    boolean isplantillaColorRojo_verdePushed=false;
    boolean isplantillaColorTransicionPushed=false;
    boolean isplantillaColorApagadaPushed=false;

    boolean isOnOffButtonPushed=false;
    boolean isRefrescoButtonPushed=false;
    boolean isCervezaButtonPushed=false;
    boolean isOnOffLamparaButtonPushed=false;

    String colorActualLeido;
    String modoActualLeido;

    //Solo cambio el param CD (color estático)
    String plantillaDefault = "";
    String status="";
    final String plantillaColorRojo =       "00";
    final String plantillaColorVerde =      "01";
    final String plantillaColorAzul=        "02";
    final String plantillaColorRojo_verde = "03";
    final String plantillaColorRojo_azul =  "04";
    final String plantillaColorVerdeAzul =  "05";
    final String plantillaColorTransicion = "06";
    final String plantillaColorApagada =    "07";
    final String plantillaComandoNoReinicio="70";
    final String plantillaComandoOnOff =    "AA";
    final String plantillaFuncionRefresco = "FFEC";
    final String plantillaFuncionCerveza =  "000A";

    int espacioHexColor =188;//+2. Espacio donde se sustituye el dato del color
    int espacioValorNoReinicioD7 =240;//+2. Espacio donde se coloca el valor 01110000 en hexa para evitar el reinicio del control al mandar plantilla
    int espacioOnOffD5 =236;//+2. Espacio donde se coloca el valor AA en hexa para "encender y apagar" el equipo, es solo por ahora
    int espacioT0 =5;//+2. Espacio donde se sustituye el dato T0

    List<String> listData = new ArrayList<String>() ;
    List<String> dataListPlantilla = new ArrayList<String>() ;
    List<String> listDataStatus = new ArrayList<String>() ;
    List<String> FinalListData = new ArrayList<String>() ;

    //Pantalla de peticion inicial de permisos
    SharedPreferences sp;
    SharedPreferences.Editor esp;
    Context context;

    androidx.appcompat.app.AlertDialog progressdialog=null;
    View dialogViewProgressBar;

    HomeFragmentInterface homeFragmentInterface;



    public HomeFragment(BluetoothServices bluetoothServices, Context context, TextView tvconstate, TextView tvfw){
        this.bluetoothServices = bluetoothServices;
        bluetoothServices.bleListener(this);
        this.context = context;
        this.sp = context.getSharedPreferences("connection_preferences",Context.MODE_PRIVATE);
        this.esp = sp.edit();
        this.tvconnectionstate = tvconstate;
        this.tvfwversion = tvfw;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.fragment_home, container, false);
        init(view);

        view.findViewById(R.id.btnConnectHomeCooler).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeFragmentInterface.connectHomeCooler(tvTemperatura);
            }
        });
        view.findViewById(R.id.btndesconectar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MyAsyncTaskDesconnectBLE().execute();
            }
        });

        view.findViewById(R.id.btnOnOff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createCommandtoSendPowerOff();
            }
        });
        view.findViewById(R.id.btnOnOffLampara).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnoffLampara(v);
            }
        });
        view.findViewById(R.id.btnRefresco).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //createCommandtoSendTipoDeshielo();
                isRefrescoButtonPushed=true;
                isCervezaButtonPushed=false;
                checkActualConfFromPlantillaActualTest();
            }
        });
        view.findViewById(R.id.btnCerveza).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //createCommandtoSendTipoDeshielo();
                isRefrescoButtonPushed=false;
                isCervezaButtonPushed=true;
                checkActualConfFromPlantillaActualTest();
            }
        });

        view.findViewById(R.id.btnColorRojo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorRojo(v);
            }
        });
        view.findViewById(R.id.btnColorVerde).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorVerde(v);
            }
        });
        view.findViewById(R.id.btnColorAzul).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorAzul(v);
            }
        });
        view.findViewById(R.id.btnColorMorado).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorRojo_azul(v);
            }
        });
        view.findViewById(R.id.btnColorAmarillo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorRojo_verde(v);
            }
        });
        view.findViewById(R.id.btnColorAzulCeleste).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorVerdeAzul(v);
            }
        });
        view.findViewById(R.id.btnColorNegro).setOnClickListener(new View.OnClickListener() {//todo TRANSICION DE COLORES
            @Override
            public void onClick(View v) {
                ColorTransicion(v);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //todo resumen de la interfaz según el último estatus correcto obtenido
    }

    private void init(View v){
        //bluetoothLeService = bluetoothServices.getBluetoothLeService();
        //inicializar variables de UI
        //interface ble


        isRefrescoButtonPushed = false;
        isOnOffButtonPushed = true;
        isCervezaButtonPushed = false;
        isOnOffLamparaButtonPushed = false;
        nuevaPlantillaEnviar = new StringBuilder();
        //botones
        imgbtnonOffLampara = v.findViewById(R.id.btnOnOffLampara);
        imgbtnOnOff = v.findViewById(R.id.btnOnOff);
        imgbtnCerveza = v.findViewById(R.id.btnCerveza);
        imgbtnRefresco = v.findViewById(R.id.btnRefresco);

        btnRojo = v.findViewById(R.id.btnColorRojo);
        btnAzul = v.findViewById(R.id.btnColorAzul);
        btnVerde = v.findViewById(R.id.btnColorVerde);
        btnRojo_verde = v.findViewById(R.id.btnColorAmarillo);
        btnRojo_azul = v.findViewById(R.id.btnColorMorado);
        btnVerde_azul = v.findViewById(R.id.btnColorAzulCeleste);
        btnTransicion = v.findViewById(R.id.btnColorNegro);

        tvPuerta = v.findViewById(R.id.tvPuerta);
        tvTemperatura = v.findViewById(R.id.tvDisplay);
        tvVoltaje = v.findViewById(R.id.tvVoltaje);
    }

    //fnción de botones
    public void ColorRojo(View view){
        if (isConnected()){
            //se carga la interfaz desde la plantilla que se obtendrá al momento
            isplantillaColorRojoPushed=true;
            isplantillaColorVerdePushed = false;
            isplantillaColorAzulPushed = false;
            isplantillaColorVerdeAzulPushed = false;
            isplantillaColorRojo_verdePushed = false;
            isplantillaColorRojo_azulPushed = false;
            isplantillaColorTransicionPushed = false;
            checkActualConfFromPlantillaActual();
        }else {
            //updateUIFromPlantillaLast(view);
            //se carga la interfaz con el último estado que se obtuvo
            Toast.makeText(context, "No estás conectado a tu Home Cooler", Toast.LENGTH_SHORT).show();
            //checkActualConfFromPlantillaLast();
        }
    }
    public void ColorAzul(View view){
        if (isConnected()){
            //se carga la interfaz desde la plantilla que se obtendrá al momento
            isplantillaColorAzulPushed=true;
            isplantillaColorRojoPushed = false;
            isplantillaColorVerdePushed = false;
            isplantillaColorVerdeAzulPushed = false;
            isplantillaColorRojo_verdePushed = false;
            isplantillaColorRojo_azulPushed = false;
            isplantillaColorTransicionPushed = false;
            checkActualConfFromPlantillaActual();

        }else {
            //updateUIFromPlantillaLast(view);
            //se carga la interfaz con el último estado que se obtuvo
            Toast.makeText(context, "No estás conectado a tu Home Cooler", Toast.LENGTH_SHORT).show();
            //checkActualConfFromPlantillaLast();
        }
    }
    public void ColorVerde(View view){
        if (isConnected()){
            //se carga la interfaz desde la plantilla que se obtendrá al momento
            isplantillaColorVerdePushed=true;
            isplantillaColorRojoPushed = false;
            isplantillaColorAzulPushed = false;
            isplantillaColorVerdeAzulPushed = false;
            isplantillaColorRojo_verdePushed = false;
            isplantillaColorRojo_azulPushed = false;
            isplantillaColorTransicionPushed = false;
            checkActualConfFromPlantillaActual();

        }else {
            //updateUIFromPlantillaLast(view);
            //se carga la interfaz con el último estado que se obtuvo
            Toast.makeText(context, "No estás conectado a tu Home Cooler", Toast.LENGTH_SHORT).show();
            //checkActualConfFromPlantillaLast();
        }
    }
    public void ColorRojo_azul(View view){
        if (isConnected()){
            //se carga la interfaz desde la plantilla que se obtendrá al momento
            isplantillaColorRojo_azulPushed=true;
            isplantillaColorRojoPushed = false;
            isplantillaColorVerdePushed = false;
            isplantillaColorAzulPushed = false;
            isplantillaColorVerdeAzulPushed = false;
            isplantillaColorRojo_verdePushed = false;
            isplantillaColorTransicionPushed = false;
            checkActualConfFromPlantillaActual();

        }else {
            //updateUIFromPlantillaLast(view);
            //se carga la interfaz con el último estado que se obtuvo
            Toast.makeText(context, "No estás conectado a tu Home Cooler", Toast.LENGTH_SHORT).show();
            //checkActualConfFromPlantillaLast();
        }
    }
    public void ColorRojo_verde(View view){
        if (isConnected()){
            //se carga la interfaz desde la plantilla que se obtendrá al momento
            isplantillaColorRojo_verdePushed=true;
            isplantillaColorRojoPushed = false;
            isplantillaColorVerdePushed = false;
            isplantillaColorAzulPushed = false;
            isplantillaColorVerdeAzulPushed = false;
            isplantillaColorRojo_azulPushed = false;
            isplantillaColorTransicionPushed = false;
            checkActualConfFromPlantillaActual();

        }else {
            //updateUIFromPlantillaLast(view);
            //se carga la interfaz con el último estado que se obtuvo
            Toast.makeText(context, "No estás conectado a tu Home Cooler", Toast.LENGTH_SHORT).show();
            //checkActualConfFromPlantillaLast();
        }
    }
    public void ColorVerdeAzul(View view){
        if (isConnected()){
            //se carga la interfaz desde la plantilla que se obtendrá al momento
            isplantillaColorVerdeAzulPushed=true;
            isplantillaColorRojoPushed = false;
            isplantillaColorVerdePushed = false;
            isplantillaColorAzulPushed = false;
            isplantillaColorRojo_verdePushed = false;
            isplantillaColorRojo_azulPushed = false;
            isplantillaColorTransicionPushed = false;
            checkActualConfFromPlantillaActual();

        }else {
            //updateUIFromPlantillaLast(view);
            //se carga la interfaz con el último estado que se obtuvo
            Toast.makeText(context, "No estás conectado a tu Home Cooler", Toast.LENGTH_SHORT).show();
            //checkActualConfFromPlantillaLast();
        }
    }
    public void ColorTransicion(View view){
        if (isConnected()){
            //se carga la interfaz desde la plantilla que se obtendrá al momento
            isplantillaColorTransicionPushed=true;
            isplantillaColorRojoPushed=false;
            isplantillaColorVerdePushed = false;
            isplantillaColorAzulPushed = false;
            isplantillaColorVerdeAzulPushed = false;
            isplantillaColorRojo_verdePushed = false;
            isplantillaColorRojo_azulPushed = false;
            checkActualConfFromPlantillaActual();

        }else {
            //updateUIFromPlantillaLast(view);
            //se carga la interfaz con el último estado que se obtuvo
            Toast.makeText(context, "No estás conectado a tu Home Cooler", Toast.LENGTH_SHORT).show();
            //checkActualConfFromPlantillaLast();
        }
    }

    public void OnoffLampara(View view){
        if (isConnected()){
            //se carga la interfaz desde la plantilla que se obtendrá al momento

            if (isOnOffLamparaButtonPushed){
                isplantillaColorTransicionPushed=false;
                isplantillaColorRojoPushed=false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
            }
            checkActualConfFromPlantillaActual();
        }else {
            //updateUIFromPlantillaLast(view);
            //se carga la interfaz con el último estado que se obtuvo
            Toast.makeText(context, "No estás conectado a tu Home Cooler", Toast.LENGTH_SHORT).show();
            //checkActualConfFromPlantillaLast();
        }
    }

    public void stopUpdateUIService(Handler handler){
        handler.removeCallbacksAndMessages(null);
    }

    public void startUpdateUIService(Handler handler){
        final int TIEMPO = 5000;
        handler.postDelayed(new Runnable() {
            public void run() {
                Log.d("INFANT","ANIHILATOR");
                if (sp.getBoolean("isconnected",false)){
                    bluetoothServices.sendCommand("readParam");

                    /*if (bluetoothLeService == null){
                        dataListPlantilla.clear();
                        listData.clear();
                    }else {*/

                        try {
                            Thread.sleep(300);
                            bluetoothLeService = bluetoothServices.getBluetoothLeService();
                            listData = bluetoothLeService.getDataFromBroadcastUpdate();
                            bluetoothLeService.sendFirstComando("4053");
                            Thread.sleep(200);
                            listDataStatus= bluetoothLeService.getDataFromBroadcastUpdate();
                            FinalListData = GetRealDataFromHexaImbera.convert(listDataStatus,"Lectura de datos tipo Tiempo real");
                            dataListPlantilla = GetRealDataFromHexaImbera.GetRealData(FinalListData,"Lectura de datos tipo Tiempo real");
                            StringBuilder s2 = GetRealDataFromHexaImbera.cleanSpace(dataListPlantilla);
                            esp.putString("currentStatusCompleto",s2.toString());

                            esp.putString("Temp1Estatus",dataListPlantilla.get(0));
                            esp.putString("Temp2Estatus",dataListPlantilla.get(1));
                            esp.putString("VoltajeEstatus",dataListPlantilla.get(2));
                            esp.putString("ActuadoresEstatus",dataListPlantilla.get(3));
                            if (dataListPlantilla.size()==5)
                                esp.putString("AlarmasEstatus",dataListPlantilla.get(4));

                            esp.apply();
                            if (listData.isEmpty()){
                                Log.d(TAG,":No se pudo recuperar plantilla res11s");
                            }else {
                                Log.d(TAG,":Si se pudo recuperar plantilla HOME2");
                                guardarPlantillaActual();
                                updateInterfazFromPlantilla();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    //}
                }/*else{
                    stopUpdateUIService();
                }*/
                handler.postDelayed(this, TIEMPO);
            }
        }, TIEMPO);
    }

    //lógica de UI
    public void updateUIFromPlantillaLast(View view){
        plantillaDefault = sp.getString("currentPlantilla","");
        StringBuilder nuevaPlantillaEnviar = new StringBuilder();
        nuevaPlantillaEnviar.append(plantillaDefault.substring(0,espacioHexColor));
    }

    public void resetUIfalse(){
        isRefrescoButtonPushed = false;
        isOnOffButtonPushed = true;
        isCervezaButtonPushed = false;
        isOnOffLamparaButtonPushed = false;

        isplantillaColorAzulPushed=false;
        isplantillaColorRojoPushed=false;
        isplantillaColorVerdePushed=false;
        isplantillaColorVerdeAzulPushed=false;
        isplantillaColorRojo_azulPushed=false;
        isplantillaColorRojo_verdePushed=false;
        isplantillaColorTransicionPushed=false;
    }


    public void updateInterfazFromPlantilla(){
        Log.d("LLL",sp.getString("currentPlantilla",""));
        Log.d("LLL2",sp.getString("currentStatusCompleto",""));
        plantillaDefault = sp.getString("currentPlantilla","").substring(18);//s.substring(18).toString();
        status = sp.getString("currentStatus","");
        resetUIfalse();
        //todo actualizar el estatus actual
        tvTemperatura.setText("");
        tvTemperatura.setText(sp.getString("Temp1Estatus","")+"°C");
        tvPuerta.setText(sp.getString("ActuadoresEstatus",""));
        tvVoltaje.setText("Voltaje:"+sp.getString("VoltajeEstatus",""));
        //todo se comparan los datos resibidos en la plantilla para saber que funciones se encuentran activas y actualizar la UI

        if (plantillaDefault.substring(espacioOnOffD5,espacioOnOffD5+2).equals(plantillaComandoOnOff)){
            imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            imgbtnCerveza.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            imgbtnRefresco.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            btnRojo.setText("");
            btnAzul.setText("");
            btnVerde.setText("");
            btnRojo_verde.setText("");
            btnRojo_azul.setText("");
            btnVerde_azul.setText("");
            btnTransicion.setText("");
            isOnOffButtonPushed=false;
        }else{
            //funciones
            isOnOffButtonPushed=true;
            imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            if (plantillaDefault.substring(0,4).equals(plantillaFuncionRefresco)) {
                imgbtnRefresco.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                isRefrescoButtonPushed = true;
                isCervezaButtonPushed=false;
            }
            if (plantillaDefault.substring(0,4).equals(plantillaFuncionCerveza)) {
                imgbtnCerveza.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                isCervezaButtonPushed=true;
                isRefrescoButtonPushed = false;
            }
            //colores
            if (plantillaDefault.substring(espacioHexColor,espacioHexColor+2).equals(plantillaColorRojo)) {
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                btnRojo.setText("S");
                isplantillaColorRojoPushed=true;
            }
            if (plantillaDefault.substring(espacioHexColor,espacioHexColor+2).equals(plantillaColorVerde)) {
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                btnVerde.setText("S");
                isplantillaColorVerdePushed=true;
            }
            if (plantillaDefault.substring(espacioHexColor,espacioHexColor+2).equals(plantillaColorAzul)) {
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                btnAzul.setText("S");
                isplantillaColorAzulPushed=true;
            }
            if (plantillaDefault.substring(espacioHexColor,espacioHexColor+2).equals(plantillaColorRojo_azul)) {
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                btnRojo_azul.setText("S");
                isplantillaColorRojo_azulPushed=true;
            }
            if (plantillaDefault.substring(espacioHexColor,espacioHexColor+2).equals(plantillaColorRojo_verde)) {
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                btnRojo_verde.setText("S");
                isplantillaColorRojo_verdePushed=true;
            }
            if (plantillaDefault.substring(espacioHexColor,espacioHexColor+2).equals(plantillaColorVerdeAzul)) {
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                btnVerde_azul.setText("S");
                isplantillaColorVerdeAzulPushed=true;
            }
            if (plantillaDefault.substring(espacioHexColor,espacioHexColor+2).equals(plantillaColorTransicion)) {
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                btnTransicion.setText("S");
                isplantillaColorTransicionPushed=true;
            }
            if (plantillaDefault.substring(espacioHexColor,espacioHexColor+2).equals(plantillaColorApagada)) {
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
                btnRojo.setText("");
                btnAzul.setText("");
                btnVerde.setText("");
                btnRojo_verde.setText("");
                btnRojo_azul.setText("");
                btnVerde_azul.setText("");
                btnTransicion.setText("");
            }


        }
    }

    public void createCommandtoSendPowerup(){
        nuevaPlantillaEnviar = null;
        nuevaPlantillaEnviar = new StringBuilder();
        nuevaPlantillaEnviar.append("4050AA");

        btnRojo.setText("");
        btnAzul.setText("");
        btnVerde.setText("");
        btnRojo_verde.setText("");
        btnRojo_azul.setText("");
        btnVerde_azul.setText("");
        btnTransicion.setText("");

        //todo se llena la nueva plantilla
        if(isRefrescoButtonPushed){
            imgbtnRefresco.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            imgbtnCerveza.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            nuevaPlantillaEnviar.append(plantillaFuncionRefresco);
            nuevaPlantillaEnviar.append(plantillaDefault.substring(4,espacioHexColor));//se completa la cadena hasta el comando de color
            isRefrescoButtonPushed=true;
            isCervezaButtonPushed=false;
        }else if(isCervezaButtonPushed){
            imgbtnRefresco.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            imgbtnCerveza.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            nuevaPlantillaEnviar.append(plantillaFuncionCerveza);
            nuevaPlantillaEnviar.append(plantillaDefault.substring(4,espacioHexColor));//se completa la cadena hasta el comando de color
            isRefrescoButtonPushed=false;
            isCervezaButtonPushed=true;
        }else{
            nuevaPlantillaEnviar.append(plantillaDefault.substring(0,espacioHexColor));//se completa la cadena hasta el comando de color
        }

        /***/


        if (isOnOffLamparaButtonPushed){
            //revisar si alguno de los colores fue pulsado o si se quiere apagar la luz
            if (isplantillaColorRojoPushed){
                btnRojo.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorVerdePushed){
                btnVerde.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorVerde);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorAzulPushed){
                btnAzul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorAzul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorRojo_azulPushed){
                btnRojo_azul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo_azul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorRojo_verdePushed){
                btnRojo_verde.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo_verde);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorVerdeAzulPushed){
                btnVerde_azul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorVerdeAzul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorTransicionPushed){
                btnTransicion.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorTransicion);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                isplantillaColorVerdeAzulPushed=false;
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else{
                //btnRojo.setText("S");
                //no hay ningún color clickeado, entonces se poderde a apagar
                nuevaPlantillaEnviar.append(plantillaColorApagada);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isOnOffLamparaButtonPushed = false;
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }
        }else{
            if (isplantillaColorRojoPushed){
                btnRojo.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
            }else if (isplantillaColorVerdePushed){
                btnVerde.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorVerde);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorAzulPushed){
                btnAzul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorAzul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorRojo_azulPushed){
                btnRojo_azul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo_azul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorRojo_verdePushed){
                btnRojo_verde.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo_verde);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorVerdeAzulPushed){
                btnVerde_azul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorVerdeAzul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorTransicionPushed){
                btnTransicion.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorTransicion);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                isplantillaColorVerdeAzulPushed=false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else{
                //no se pulsó ningún color pero se pulsó el encendido, entonces mando el color default (rojo)
                btnRojo.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
            }
            isOnOffLamparaButtonPushed = true;
        }

        /*if (isOnOffLamparaButtonPushed){
            isOnOffLamparaButtonPushed = false;
            view.findViewById(R.id.btnOnOffLampara).setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_yellow));

        }else{
            isOnOffLamparaButtonPushed = true;
            view.findViewById(R.id.btnOnOffLampara).setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
        }*/

        //se obtiene el CKSM
        String check =GetHexFromRealDataImbera.calculateChacksumString(nuevaPlantillaEnviar.toString());
        nuevaPlantillaEnviar.append(check);
        Log.d(TAG,"Plantilla A ENVIAR:"+nuevaPlantillaEnviar);
        new MyAsyncTaskSendNewPlantilla(nuevaPlantillaEnviar).execute();
    }

    public void createCommandtoSendPowerOff(){
        nuevaPlantillaEnviar = null;
        nuevaPlantillaEnviar = new StringBuilder();
        nuevaPlantillaEnviar.append("4050AA");

        btnRojo.setText("");
        btnAzul.setText("");
        btnVerde.setText("");
        btnRojo_verde.setText("");
        btnRojo_azul.setText("");
        btnVerde_azul.setText("");
        btnTransicion.setText("");

        //todo se llena la nueva plantilla

        if (isOnOffButtonPushed){
            nuevaPlantillaEnviar.append(plantillaDefault.substring(0,espacioHexColor));//se completa la cadena hasta el comando de color
            nuevaPlantillaEnviar.append(plantillaColorApagada);
            nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
            nuevaPlantillaEnviar.append(plantillaComandoOnOff);
            nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
            nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
            nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
            //apagar todos los colores/funciones
            imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            imgbtnCerveza.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            imgbtnRefresco.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            isOnOffButtonPushed=false;
            //se obtiene el CKSM
            String check =GetHexFromRealDataImbera.calculateChacksumString(nuevaPlantillaEnviar.toString());
            nuevaPlantillaEnviar.append(check);
            Log.d(TAG,"Plantilla A ENVIAR:"+nuevaPlantillaEnviar);
            new MyAsyncTaskSendNewPlantilla(nuevaPlantillaEnviar).execute();
        }else{
            createCommandtoSendPowerup();
            isOnOffButtonPushed=true;
        }


    }

    public void createCommandtoSendTipoDeshielo(){
        nuevaPlantillaEnviar = null;
        nuevaPlantillaEnviar = new StringBuilder();
        nuevaPlantillaEnviar.append("4050AA");

        //todo se llena la nueva plantilla
        Log.d("refe",":"+isRefrescoButtonPushed);
        Log.d("cerve",":"+isCervezaButtonPushed);
        Log.d("cerve",":"+plantillaDefault);
        if(isRefrescoButtonPushed){
            imgbtnRefresco.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            imgbtnCerveza.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            nuevaPlantillaEnviar.append(plantillaFuncionRefresco);
            nuevaPlantillaEnviar.append(plantillaDefault.substring(4,plantillaDefault.length()-8));//se completa la cadena hasta el comando de color

        }else if(isCervezaButtonPushed){
            imgbtnRefresco.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            imgbtnCerveza.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            nuevaPlantillaEnviar.append(plantillaFuncionCerveza);
            nuevaPlantillaEnviar.append(plantillaDefault.substring(4,plantillaDefault.length()-8));//se completa la cadena hasta el comando de color

        }else{
            nuevaPlantillaEnviar.append(plantillaDefault.substring(0,plantillaDefault.length()-8));//se completa la cadena hasta el comando de color
        }

        //se obtiene el CKSM
        String check =GetHexFromRealDataImbera.calculateChacksumString(nuevaPlantillaEnviar.toString());
        nuevaPlantillaEnviar.append(check);
        Log.d(TAG,"Plantilla A ENVIAR:"+nuevaPlantillaEnviar);
        new MyAsyncTaskSendNewPlantilla(nuevaPlantillaEnviar).execute();
    }

    public void createCommandtoSend(){
        nuevaPlantillaEnviar = null;
        nuevaPlantillaEnviar = new StringBuilder();
        nuevaPlantillaEnviar.append("4050AA");

        btnRojo.setText("");
        btnAzul.setText("");
        btnVerde.setText("");
        btnRojo_verde.setText("");
        btnRojo_azul.setText("");
        btnVerde_azul.setText("");
        btnTransicion.setText("");

        //todo se llena la nueva plantilla


        if(isRefrescoButtonPushed){
            imgbtnRefresco.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            imgbtnCerveza.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            nuevaPlantillaEnviar.append(plantillaFuncionRefresco);
            nuevaPlantillaEnviar.append(plantillaDefault.substring(4,espacioHexColor));//se completa la cadena hasta el comando de color
            isRefrescoButtonPushed=true;
            isCervezaButtonPushed=false;
        }else if(isCervezaButtonPushed){
            imgbtnRefresco.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
            imgbtnCerveza.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            nuevaPlantillaEnviar.append(plantillaFuncionCerveza);
            nuevaPlantillaEnviar.append(plantillaDefault.substring(4,espacioHexColor));//se completa la cadena hasta el comando de color
            isRefrescoButtonPushed=false;
            isCervezaButtonPushed=true;
        }else{
            nuevaPlantillaEnviar.append(plantillaDefault.substring(0,espacioHexColor));//se completa la cadena hasta el comando de color
        }

        if (isOnOffLamparaButtonPushed){
            //revisar si alguno de los colores fue pulsado o si se quiere apagar la luz
            if (isplantillaColorRojoPushed){
                btnRojo.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorVerdePushed){
                btnVerde.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorVerde);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorAzulPushed){
                btnAzul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorAzul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorRojo_azulPushed){
                btnRojo_azul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo_azul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorRojo_verdePushed){
                btnRojo_verde.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo_verde);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorVerdeAzulPushed){
                btnVerde_azul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorVerdeAzul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorTransicionPushed){
                btnTransicion.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorTransicion);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                isplantillaColorVerdeAzulPushed=false;
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else{
                //btnRojo.setText("S");
                //no hay ningún color clickeado, entonces se poderde a apagar
                nuevaPlantillaEnviar.append(plantillaColorApagada);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isOnOffLamparaButtonPushed = false;
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_btnwhite));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }
        }else{
            if (isplantillaColorRojoPushed){
                btnRojo.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
            }else if (isplantillaColorVerdePushed){
                btnVerde.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorVerde);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorAzulPushed){
                btnAzul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorAzul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorRojo_azulPushed){
                btnRojo_azul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo_azul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorRojo_verdePushed){
                btnRojo_verde.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo_verde);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorVerdeAzulPushed){
                btnVerde_azul.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorVerdeAzul);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else if (isplantillaColorTransicionPushed){
                btnTransicion.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorTransicion);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                isplantillaColorRojoPushed = false;
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
                isplantillaColorVerdeAzulPushed=false;
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
            }else{
                //no se pulsó ningún color pero se pulsó el encendido, entonces mando el color default (rojo)
                btnRojo.setText("S");
                nuevaPlantillaEnviar.append(plantillaColorRojo);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioHexColor+2, espacioOnOffD5));
                nuevaPlantillaEnviar.append("BB");
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioOnOffD5+2,espacioOnOffD5+4));
                nuevaPlantillaEnviar.append(plantillaComandoNoReinicio);
                nuevaPlantillaEnviar.append(plantillaDefault.substring(espacioValorNoReinicioD7+2, plantillaDefault.length()-8));
                imgbtnonOffLampara.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                imgbtnOnOff.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corner_azulimbera));
                isplantillaColorVerdePushed = false;
                isplantillaColorAzulPushed = false;
                isplantillaColorVerdeAzulPushed = false;
                isplantillaColorRojo_verdePushed = false;
                isplantillaColorRojo_azulPushed = false;
            }
            isOnOffLamparaButtonPushed = true;
        }

        //se obtiene el CKSM
        String check =GetHexFromRealDataImbera.calculateChacksumString(nuevaPlantillaEnviar.toString());
        nuevaPlantillaEnviar.append(check);
        Log.d(TAG,"Plantilla A ENVIAR:"+nuevaPlantillaEnviar);
        new MyAsyncTaskSendNewPlantilla(nuevaPlantillaEnviar).execute();
    }

    public void checkActualConfFromPlantillaActual(){
        //pedir entonces la plantilla actual
        new MyAsyncTaskGetPlantillaCommand().execute();
    }

    public void checkActualConfFromPlantillaActualTest(){
        //pedir entonces la plantilla actual
        new MyAsyncTaskGetPlantillaCommandTest().execute();
    }

    @Override
    public void startServiceUpdateUI(Handler handler) {
        startUpdateUIService(handler);
    }

    @Override
    public void stopServiceUpdateUI(Handler handler) {
        stopUpdateUIService(handler);
    }

    class MyAsyncTaskGetPlantillaCommandTest extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            bluetoothServices.sendCommand("readParam");

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bluetoothLeService = bluetoothServices.getBluetoothLeService();
            if (bluetoothLeService == null){
                dataListPlantilla.clear();
                listData.clear();
                return "";
            }else {
                listData = bluetoothLeService.getDataFromBroadcastUpdate();
                //FinalListData = GetRealDataFromHexaImbera.convert(listData,"Lectura de parámetros de operación");
                //dataListPlantilla = GetRealDataFromHexaImbera.GetRealData(FinalListData,"Lectura de parámetros de operación");
                return "resp";
            }

        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Thread.sleep(800);
                if (progressdialog != null)progressdialog.dismiss();
                progressdialog=null;
                if (result.equals("")){
                    Log.d(TAG,":No se pudo recuperar plantilla res");
                }else {
                    if (result.equals("resp")){
                        Log.d(TAG,":Si se pudo recuperar plantilla");
                        guardarPlantillaActual();
                        createCommandtoSendTipoDeshielo();
                        //createCommandtoSend();
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
            createProgressDialog("Obteniendo estado de tu Home Cooler...");
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    class MyAsyncTaskGetPlantillaCommand extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            bluetoothServices.sendCommand("readParam");

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bluetoothLeService = bluetoothServices.getBluetoothLeService();
            if (bluetoothLeService == null){
                dataListPlantilla.clear();
                listData.clear();
                return "";
            }else {
                listData = bluetoothLeService.getDataFromBroadcastUpdate();
                //FinalListData = GetRealDataFromHexaImbera.convert(listData,"Lectura de parámetros de operación");
                //dataListPlantilla = GetRealDataFromHexaImbera.GetRealData(FinalListData,"Lectura de parámetros de operación");
                return "resp";
            }

        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Thread.sleep(800);
                if (progressdialog != null)progressdialog.dismiss();
                progressdialog=null;
                if (result.equals("")){
                    Log.d(TAG,":No se pudo recuperar plantilla res");
                }else {
                    if (result.equals("resp")){
                        Log.d(TAG,":Si se pudo recuperar plantilla HOME");
                        guardarPlantillaActual();
                        createCommandtoSend();
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
            createProgressDialog("Obteniendo estado de tu Home Cooler...");
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    class MyAsyncTaskSendNewPlantilla extends AsyncTask<Integer, Integer, String> {
        String comando;
        MyAsyncTaskSendNewPlantilla(StringBuilder sb){
            this.comando = sb.toString();
        }
        @Override
        protected String doInBackground(Integer... params) {
            bluetoothServices.sendCommand("writeRealParam",comando);
            dataListPlantilla.clear();
            try {
                Thread.sleep(800);
                if (bluetoothLeService != null){
                    dataListPlantilla = bluetoothLeService.getDataFromBroadcastUpdate();
                    if (dataListPlantilla.isEmpty()){
                        return "empty";
                    }else {
                        Log.d("RESP",":"+dataListPlantilla.get(0));
                        if (dataListPlantilla.get(0).equals("F1 3D ")){
                            return "ok";
                        }else{
                            return "notok";
                        }
                    }
                }else {
                    return "noconnected";
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
                if (progressdialog != null)progressdialog.dismiss();
                progressdialog=null;
                if (result.equals("empty"))
                    Toast.makeText(getContext(), "Acabas de enviar plantilla, intenta reconectarte a BLE", Toast.LENGTH_SHORT).show();
                if (result.equals("ok")){
                    Toast.makeText(getContext(), "Actualización de Home Cooler correcta", Toast.LENGTH_SHORT).show();
                    //listenermain.printExcel(getOriginalList(),"imbera");
                }if (result.equals("noconnected")){
                    Toast.makeText(getContext(), "No te has conectado a un Home Cooler", Toast.LENGTH_SHORT).show();
                }if (result.equals("exception")){
                    Toast.makeText(getContext(), "Ha ocurrido un error inesperado", Toast.LENGTH_SHORT).show();
                }if (result.equals("notok"))
                    Toast.makeText(getContext(), "Actualización de Home Cooler incorrecta", Toast.LENGTH_SHORT).show();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
            createProgressDialog("Actualizando tu Home Cooler...");
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    class MyAsyncTaskDesconnectBLE extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            try {
                Thread.sleep(400);
                if (bluetoothServices.getBluetoothLeService()!=null){
                    homeFragmentInterface.desconectar();
                    return "resp";
                }else
                    return "noconexion";
            } catch (InterruptedException e) {
                e.printStackTrace();
                return "resp";
            }


        }

        @Override
        protected void onPostExecute(String result) {
            if (progressdialog != null)progressdialog.dismiss();
            progressdialog=null;
            if (result.equals("noconexion")){
                Toast.makeText(getContext(), "No estás conectado a ningún BLE", Toast.LENGTH_SHORT).show();
                esp.putBoolean("isconnected",false);
                esp.apply();
            }else{
                //Toast.makeText(getContext(), "Te has desconectado de BLE", Toast.LENGTH_SHORT).show();
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvfwversion.setText("");
                        tvconnectionstate.setText("Desconectado");
                        tvconnectionstate.setTextColor(Color.BLACK);
                        esp.putString("trefpVersionName","");
                        esp.apply();
                    }
                });

                esp.putBoolean("isconnected",false);
                esp.apply();
            }
        }

        @Override
        protected void onPreExecute() {
            createProgressDialog("Desconectando del dispositivo...");
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    public void createProgressDialog(String string){
        if(progressdialog == null){
            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);//getLayoutInflater();
            dialogViewProgressBar = inflater.inflate(R.layout.show_progress_bar, null, false);
            androidx.appcompat.app.AlertDialog.Builder adb = new androidx.appcompat.app.AlertDialog.Builder(getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert_eltc);
            adb.setView(dialogViewProgressBar);
            progressdialog = adb.create();
            progressdialog.setCanceledOnTouchOutside(false);
            progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            TextView txt = ((TextView) dialogViewProgressBar.findViewById(R.id.txtInfoProgressBar));
            txt.setText(string);
            progressdialog.show();
        }

    }

    private boolean isConnected(){
        return sp.getBoolean("isconnected",false);
    }

    private void guardarPlantillaActual() {
        StringBuilder s = GetRealDataFromHexaImbera.cleanSpace(listData);
        plantillaDefault = s.substring(18).toString();
        esp.putString("currentPlantilla",s.substring(18));//no necesito el header por eso substring
        esp.apply();
    }

    public interface HomeFragmentInterface{
        public void connectHomeCooler(TextView tvdisplay);
        public void desconectar();
    }

    public void HomeFragmentListener(HomeFragmentInterface homeFragmentInterface){
        this.homeFragmentInterface = homeFragmentInterface;
    }


}
