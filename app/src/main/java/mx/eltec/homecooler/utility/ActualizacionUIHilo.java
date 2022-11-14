package mx.eltec.homecooler.utility;

public class ActualizacionUIHilo extends Thread{
    private boolean isConnectedToBLE;

    public ActualizacionUIHilo(boolean isconnected){
        this.isConnectedToBLE = isconnected;
    }



    public void changeStatus(boolean b){
        this.isConnectedToBLE = b;
    }

}

/*
t = new Thread(){
@Override
public void run() {
        super.run();
        try {
        while (!isInterrupted()){

        Thread.sleep(10000);
        activityMain.runOnUiThread(new Runnable() {
@Override
public void run() {
        if (sp.getBoolean("h",false)){
        t.interrupt();
        }else{
        Log.d("THE SKU","rain frie");
        }


        //guardarPlantillaActual(10);
        }
        });
        }
        }catch (Exception e){}
        }
        };
        t.start();*/
