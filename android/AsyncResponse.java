package ro.sun.thermostat;

public interface AsyncResponse {
    void refreshed(int success);
    void processFinish(int v);
    void processFinish(String s);
    void processFinish(Boolean b);
    void processFinish(String IP, boolean f);
    void processFinish(boolean finded, String mac);

}
