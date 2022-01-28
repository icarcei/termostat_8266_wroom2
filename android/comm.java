package ro.sun.thermostat;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class comm extends AsyncTask<String, Void, String> {
    private byte[] key = {0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30};
    private boolean local = true;
    private Thermo thermo;

    public comm(Thermo thermo) {
        this.thermo = thermo;
    }

    private static String padString(String source) {
        char paddingChar = 0;
        int size = 16;
        int x = source.length() % size;
        int padLength = size - x;
        for (int i = 0; i < padLength; i++) {
            source += paddingChar;
        }
        return source;
    }


    private byte[] encrypt(String clear)throws Exception{
        SecretKeySpec secretKeySpec= new SecretKeySpec(key, "AES");
        byte[] ivstring = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        IvParameterSpec iv = new IvParameterSpec(ivstring);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
        byte[] encrypted = cipher.doFinal(padString(clear).getBytes());
        return encrypted;
    }

    private String decrypt( byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        byte[] ivstring = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        IvParameterSpec iv = new IvParameterSpec(ivstring);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted,"UTF-8");
    }

    public String docomm(String ask, String ip, int port) throws IOException {
        Socket socket = new Socket();
        try{
            socket.setReuseAddress(true);
            socket.setTcpNoDelay(true);
            if(thermo.isLocal()&&(!thermo.isUselocalap())){
                socket.connect(new InetSocketAddress(ip,port), 5000);
                socket.setSoTimeout(5000);
            }else{//bonjour
                socket.connect(new InetSocketAddress(ip,port), 15000);
                socket.setSoTimeout(15000);
            }
            DataInputStream input = new DataInputStream(socket.getInputStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] data = encrypt(ask);

            socket.getOutputStream().write(data);
            socket.getOutputStream().flush();

            byte[] rdata = new byte[1000];
            int length ;
            if(true) {
                //input.readFully(rdata);
                length = input.read(rdata,0, 1000);
                byte[] recdata = new byte[length];
                out.close();
                input.close();
                socket.close();
                System.arraycopy(rdata, 0, recdata,0, length);
                return decrypt(recdata);
            }
            out.close();
            input.close();
            socket.close();

            return "\0";

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(thermo.isLocal()){
            thermo.setDistant();
            return docomm(ask, thermo.getBonjourserver(), thermo.getBonjourport()+1);

        }else {
            return "";
        }
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            if(thermo.isUselocalap()){
                return docomm(strings[0], thermo.getLocalipap(), thermo.getPort());
            }else {
                if (thermo.isLocal()) {
                    //return docomm(strings[0],"192.168.43.221", 5001);
                    return docomm(strings[0], thermo.getIP(), thermo.getPort());//""192.168.43.111", 4321);
                } else {
                    return docomm(strings[0], thermo.getBonjourserver(), thermo.getBonjourport() + 1);//""192.168.43.111", 4321);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String s) {
       // if(!thermo.isLocal()){
       //     thermo.decodeMessage(s);
       // }
            thermo.decodeMessage(s);
        //else{}
            //
        super.onPostExecute(s);

    }
}
