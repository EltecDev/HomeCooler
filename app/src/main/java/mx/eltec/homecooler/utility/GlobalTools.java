package mx.eltec.homecooler.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import mx.eltec.homecooler.R;

import java.util.Locale;

public class GlobalTools {

    public static void startDisplayUpdateUI(){

    }

    public static void changeScreenConnectionStatus(TextView tv, SharedPreferences sp){
        if (sp.getBoolean("isconnected",false)){
            tv.setText("Conectado a:"+sp.getString("mac",""));
            tv.setTextColor(Color.parseColor("#00a135"));
        }else{
            tv.setText("Desconectado");
            tv.setTextColor(Color.BLACK);
        }
    }

    public static androidx.appcompat.app.AlertDialog createProgressDialog(String string, Context context, androidx.appcompat.app.AlertDialog progressDialog, View dialogViewProgressBar){
        //androidx.appcompat.app.AlertDialog progressdialog=null;
        //View dialogViewProgressBar;
        //if(progressdialog == null){
            //Crear dialogos de "pantalla de carga" y "popups if"
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);//getLayoutInflater();
        dialogViewProgressBar = inflater.inflate(R.layout.show_progress_bar, null, false);
        androidx.appcompat.app.AlertDialog.Builder adb = new androidx.appcompat.app.AlertDialog.Builder(context,R.style.Theme_AppCompat_Light_Dialog_Alert_eltc);
        adb.setView(dialogViewProgressBar);
        progressDialog = adb.create();
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView txt = ((TextView) dialogViewProgressBar.findViewById(R.id.txtInfoProgressBar));
        txt.setText(string);
        //}
        progressDialog.show();
        return progressDialog;
    }

    public static androidx.appcompat.app.AlertDialog closeProgressDialog(androidx.appcompat.app.AlertDialog progressDialog, View dialogViewProgressBar){
        if (progressDialog != null){
            progressDialog.dismiss();
            progressDialog=null;
        }
        return progressDialog;
    }

    public static void showInfoPopup(String tittle, String content, Context context){
        final AlertDialog alexaDialog;
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);//getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.popup_info, null, false);
        AlertDialog.Builder adb = new AlertDialog.Builder(context,R.style.Theme_AppCompat_Light_Dialog_Alert_eltc);
        adb.setView(dialogView);

        TextView tv1 = (TextView) dialogView.findViewById(R.id.tvTituloData);
        tv1.setText(tittle);
        TextView tv2 = (TextView) dialogView.findViewById(R.id.tvsubtitulo);
        tv2.setText(content);

        alexaDialog = adb.create();
        alexaDialog.setCanceledOnTouchOutside(false);
        alexaDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alexaDialog.show();
        dialogView.findViewById(R.id.welcomeAlexaButtonLater).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alexaDialog.dismiss();
            }
        });

    }

    public static String checkChecksumImberaTREFPB(String data){
        //if (data.length()!=50){//la comunicación es erronea, es posible que no tenga fw instalado
        //    return "notFirmware";
        //}else{
            String checksumData = data.substring(data.length()-8);
            int checksumTotal=0;
            String c="";
            for (int p=0; p<data.length()-9 ; p+=2) {
                checksumTotal = checksumTotal + GetRealDataFromHexaOxxoDisplay.getDecimal(data.substring(p, p + 2));
            }

            c = Integer.toHexString(checksumTotal);
            String finalchecksum = "";
            if (c.length() == 1)
                finalchecksum = "0000000" + c;
            else if (c.length() == 2)
                finalchecksum = "000000" + c;
            else if (c.length() == 3)
                finalchecksum = "00000" + c;
            else if (c.length() == 4)
                finalchecksum = "0000" + c;
            else if (c.length() == 5)
                finalchecksum = "000" + c;
            else if (c.length() == 6)
                finalchecksum = "00" + c;
            else if (c.length() == 7)
                finalchecksum = "0" + c;
            else
                finalchecksum = c;
            finalchecksum = finalchecksum.toUpperCase(Locale.ROOT);

            if(checksumData.equals(finalchecksum)){
                return "ok";
            }else{
                return "notok";
            }
       // }
    }

    public static String checkChecksum(String data){
        String checksumData = data.substring(data.length()-8);
        int checksumTotal=0;
        String c="";
        for (int p=0; p<data.length()-9 ; p+=2) {
            checksumTotal = checksumTotal + GetRealDataFromHexaOxxoDisplay.getDecimal(data.substring(p, p + 2));
        }
        c = Integer.toHexString(checksumTotal);
        String finalchecksum = "";
        if (c.length() == 1)
            finalchecksum = "0000000" + c;
        else if (c.length() == 2)
            finalchecksum = "000000" + c;
        else if (c.length() == 3)
            finalchecksum = "00000" + c;
        else if (c.length() == 4)
            finalchecksum = "0000" + c;
        else if (c.length() == 5)
            finalchecksum = "000" + c;
        else if (c.length() == 6)
            finalchecksum = "00" + c;
        else if (c.length() == 7)
            finalchecksum = "0" + c;
        else
            finalchecksum = c;
        finalchecksum = finalchecksum.toUpperCase(Locale.ROOT);
        if(checksumData.equals(finalchecksum)){
            return "ok";
        }else{
            return "notok";
        }
    }


}
