package th.in.whs.k2ausbkbd.hid;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import th.in.whs.k2ausbkbd.layout.*;

public class Keyboard {
    private static Keyboard instance = null;

    public static final String DEVICE = "/dev/hidg0";

    private Process process;
    private BufferedReader reader;
    private static final String LOG_TAG = "KeyboardShell";

    protected Keyboard() throws UnsupportedOperationException {
        if(!new File(DEVICE).exists()){
            throw new UnsupportedOperationException("Unsupported kernel");
        }

        try {
            process = new ProcessBuilder()
                    .command("su", "-c", "sh")
                    .start();

            inheritIO(process.getInputStream());
            inheritIO(process.getErrorStream());

            if(!process.isAlive()){
                Log.d(LOG_TAG, "Process died with error: " + process.exitValue());
            }

            Log.d(LOG_TAG, "Process Started");
            //process = Runtime.getRuntime().exec(new String[]{"su", "-c", "sh"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void inheritIO(final InputStream src){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "Starting scanner");
                Scanner sc = new Scanner(src);
                while(sc.hasNextLine()){
                    Log.d(LOG_TAG, sc.nextLine());
                }
            }
        }).start();
    }

    public void close() throws IOException {
        if(reader != null)
           reader.close();
        process.destroy();
    }


    public void type(String text, Layout layout) throws IOException {
        for(int i = 0, j = text.length(); i < j; i++){
            char typeChar = text.charAt(i);

            sendChar(typeChar, layout);
        }
        //logRes();
    }

    private void sendChar(char data, Layout layout) throws IOException {
        KeyCode key = layout.getKeycode(data);

        byte[] buffer = new byte[]{
                (key != null) ? (byte)key.modifier : 0,
                0,
                (key != null) ? (byte)key.code : 0,
                0 , 0, 0, 0, 0
        };

        writeToDevice(buffer);
        keyUp();
    }

    private void writeToDevice(byte[] bytes) throws IOException {
        OutputStream stdin = process.getOutputStream();
        stdin.write(("echo -ne " + bytesToEcho(bytes) + " > " + DEVICE + "\n").getBytes());
        stdin.flush();
   }

   private void logRes(){
        if(reader == null){
            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        }
        try {
            String line = "";
            while ((line = reader.readLine()) != null){
                Log.d("KeyboardRes", line);
            }
        }
        catch (IOException ioerr){
            ioerr.printStackTrace();
        }
   }

    private void keyUp() throws IOException {
        OutputStream stdin = process.getOutputStream();
        stdin.write(("echo -ne \\\\x0\\\\x0\\\\x0\\\\x0\\\\x0\\\\x0\\\\x0\\\\x0 > " + DEVICE + "\n").getBytes());
        stdin.flush();
    }

    private String bytesToEcho(byte[] bytes){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < bytes.length; i++){
            builder.append("\\\\x");
            builder.append(Integer.toHexString(new Byte(bytes[i]).intValue()));
        }
        return builder.toString();
    }

    // Singleton
    public static Keyboard getInstance() {
        if(instance == null) {
            instance = new Keyboard();
        }
        return instance;
    }
}
