package com.viliric.javaio;

/**
 * Created by Lenovo on 29.03.2016.
 */
import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.*;

public class SocketServer {

    private static int PORT = 8042;
    public static String SERVER_IP = "";
    static {
        try {
            SERVER_IP = getIpAddress();
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream("../server.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            PORT = Integer.parseInt(properties.getProperty("server_port"));
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    private static HashSet<String> names = new HashSet<String>();
    private static HashSet<PrintWriter> socketWriters = new HashSet<PrintWriter>();
    private static HashMap<String, PrintWriter> activeConnections = new HashMap<>();
    private static HashMap<String, HashMap<String, PrintWriter>> groups = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("The server is running.");
        ServerSocket server = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(server.accept()).start();
            }
        } finally {
            server.close();
        }
    }

    private static class Handler extends Thread {

        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                socketWriters.add(out);
                System.out.println(socket.getRemoteSocketAddress().toString());
                String ip = this.socket.getRemoteSocketAddress().toString();
                ip = ip.replaceAll("/","");
                ip = ip.substring(0,ip.indexOf(":"));
                if (activeConnections.containsKey(ip)) activeConnections.remove(ip);
                activeConnections.put(ip, out);

                while (true) {
                    StringBuffer buffer = new StringBuffer();
                    char[] aa = new char[1024];
                    for(int i = 0; i < aa.length; i++){
                        aa[i] = '^';
                    }

                    in.read(aa);
                    String a = "";
                    for(int i = 0; i < aa.length; i++){
                        if(aa[i] == '^')
                            break;
                        a += Character.toString(aa[i]);
                    }

                    String input = a;

                    System.out.println("new message "+a);

                    if (input == null || input == "") {
                        socketWriters.remove(out);
                        activeConnections.remove(ip);
                        return;
                    }

                    if(input.contains("CLOSE")){
                        out.println("Zakryvau server");
                        System.exit(0);
                    } else
                    if (input.contains("GET ALL IP")){
                        {
                            Set<String> it = activeConnections.keySet();
                            for (String key : it) {
                                out.println(key + "\r\n");
                            }
                            continue;
                        }
                    }else if (input.contains("SGR=")){
                        int begin = input.indexOf("=") + 1;
                        int end = input.indexOf("[");
                        String gname = input.substring(begin,end);
                        if (groups.containsKey(gname)) groups.remove(gname);
                        setGroups(input, gname);
                        continue;
                    }  if (input.contains("GN=")){
                        int begin = input.indexOf("=") + 1;
                        int end = input.indexOf("[");
                        String gname = input.substring(begin,end);
                        input = input.replaceAll("GN=" + gname,"");
                        input = input.substring(input.indexOf("[")+1,input.indexOf("]"));

                        HashMap<String, PrintWriter> groupEquip = groups.get(gname);

                        Iterator it = groupEquip.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry)it.next();
                            PrintWriter pw = groupEquip.get(pair.getKey()) ;

                            try{
                                pw.println(input+"\r\n");
                            }catch (Exception e)
                            {
                                continue;
                            }

                        }

                        continue;
                    } else {
                        for (PrintWriter writer : socketWriters) {
                            writer.println(input + "\r\n");
                        }
                        continue;
                    }

                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    socketWriters.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
    public static String getIpAddress() throws SocketException {
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()&&inetAddress instanceof Inet4Address) {
                        String ipAddress=inetAddress.getHostAddress().toString();
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException ex) {

        }
        return null;
    }

    public static void setGroups(String incomingStr, String gname){
        HashMap<String, PrintWriter> result = new HashMap<>();

        Iterator it = activeConnections.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String ip = (String) pair.getKey();

            if (incomingStr.contains(ip)){
                PrintWriter pw = activeConnections.get(pair.getKey());
                result.put(ip, pw);
            }

        }
        groups.put(gname,result);
        System.out.println(gname + "   =   " + result.size());
    }

}
