package com.rev.webctl;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebService extends Service {
    static final int PORT = 8080;
    private HttpServer server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (server == null) {
            server = new HttpServer(getApplicationContext(), PORT);
            server.start();
        }
        Notification n = new Notification.Builder(this)
                .setContentTitle("Simian Web Control")
                .setContentText("http://" + ip() + ":" + PORT + "  (PIN: " + readPin(this) + ")")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .build();
        startForeground(1, n);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (server != null) { server.stopServer(); server = null; }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    static String ip() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                NetworkInterface ni = ifs.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (!a.isLoopbackAddress() && a instanceof Inet4Address) return a.getHostAddress();
                }
            }
        } catch (Exception e) {}
        return "0.0.0.0";
    }

    // PIN stored in files/pin.txt; default "simian". Change the file to change the PIN (no rebuild).
    static String readPin(Context c) {
        try {
            File f = new File(c.getFilesDir(), "pin.txt");
            if (!f.exists()) {
                FileOutputStream fo = new FileOutputStream(f);
                fo.write("simian".getBytes("UTF-8"));
                fo.close();
                return "simian";
            }
            FileInputStream fi = new FileInputStream(f);
            byte[] buf = new byte[64];
            int n = fi.read(buf);
            fi.close();
            String s = new String(buf, 0, n > 0 ? n : 0, "UTF-8").trim();
            return s.length() > 0 ? s : "simian";
        } catch (Exception e) { return "simian"; }
    }

    // ---------------- embedded HTTP server ----------------
    static class HttpServer extends Thread {
        private final Context ctx;
        private final int port;
        private ServerSocket ss;
        private volatile boolean running = false;

        HttpServer(Context c, int p) { ctx = c; port = p; }

        void stopServer() {
            running = false;
            try { if (ss != null) ss.close(); } catch (Exception e) {}
        }

        @Override
        public void run() {
            try {
                ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress("0.0.0.0", port));
                running = true;
                while (running) {
                    final Socket s = ss.accept();
                    new Thread(new Runnable() { public void run() { handle(s); } }).start();
                }
            } catch (Exception e) {}
        }

        private void handle(Socket s) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String line = in.readLine();
                if (line == null) { s.close(); return; }
                String[] parts = line.split(" ");
                String target = parts.length > 1 ? parts[1] : "/";
                String hdr; while ((hdr = in.readLine()) != null && hdr.length() > 0) {}

                String path = target; String query = "";
                int q = target.indexOf('?');
                if (q >= 0) { path = target.substring(0, q); query = target.substring(q + 1); }
                Map<String, String> p = parseQuery(query);

                String body; String ctype = "text/plain";
                if (path.startsWith("/api/") && !checkTok(p)) {
                    body = "FORBIDDEN";
                } else if (path.equals("/")) {
                    body = page(); ctype = "text/html; charset=utf-8";
                } else if (path.equals("/api/apps")) {
                    body = appsJson(); ctype = "application/json";
                } else if (path.equals("/api/kill")) {
                    body = root("am force-stop " + p.get("pkg"));
                } else if (path.equals("/api/start")) {
                    body = root("am start -n " + p.get("pkg") + "/" + p.get("cls"));
                } else if (path.equals("/api/key")) {
                    body = root("input keyevent " + p.get("code"));
                } else if (path.equals("/api/setting")) {
                    body = root("settings put " + p.get("ns") + " " + p.get("k") + " " + p.get("v"));
                } else if (path.equals("/api/killall")) {
                    body = root("for x in $(pm list packages -3 | sed 's/package://'); do am force-stop $x; done; echo done");
                } else if (path.equals("/api/cmd")) {
                    body = root(p.get("c"));
                } else {
                    body = "not found";
                }

                byte[] data = body.getBytes("UTF-8");
                OutputStream out = s.getOutputStream();
                StringBuilder h = new StringBuilder();
                h.append("HTTP/1.1 200 OK\r\n");
                h.append("Content-Type: ").append(ctype).append("\r\n");
                h.append("Content-Length: ").append(data.length).append("\r\n");
                h.append("Access-Control-Allow-Origin: *\r\n");
                h.append("Connection: close\r\n\r\n");
                out.write(h.toString().getBytes("UTF-8"));
                out.write(data);
                out.flush();
                s.close();
            } catch (Exception e) {
                try { s.close(); } catch (Exception e2) {}
            }
        }

        private boolean checkTok(Map<String, String> p) {
            String k = p.get("k");
            return k != null && k.equals(readPin(ctx));
        }

        private Map<String, String> parseQuery(String qs) {
            Map<String, String> m = new HashMap<String, String>();
            if (qs == null || qs.length() == 0) return m;
            for (String pr : qs.split("&")) {
                int e = pr.indexOf('=');
                try {
                    if (e >= 0) m.put(URLDecoder.decode(pr.substring(0, e), "UTF-8"), URLDecoder.decode(pr.substring(e + 1), "UTF-8"));
                    else m.put(URLDecoder.decode(pr, "UTF-8"), "");
                } catch (Exception ex) {}
            }
            return m;
        }

        private String root(String cmd) {
            if (cmd == null) return "ERR: empty";
            try {
                Process pr = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String l; while ((l = br.readLine()) != null) sb.append(l).append("\n");
                pr.waitFor();
                return sb.length() > 0 ? sb.toString() : "OK";
            } catch (Exception e) { return "ERR: " + e.getMessage(); }
        }

        private String appsJson() {
            PackageManager pm = ctx.getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> l = pm.queryIntentActivities(main, 0);
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (ResolveInfo ri : l) {
                String pkg = ri.activityInfo.packageName;
                String cls = ri.activityInfo.name;
                String label;
                try { label = ri.loadLabel(pm).toString(); } catch (Exception e) { label = pkg; }
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"label\":\"").append(esc(label)).append("\",\"pkg\":\"").append(esc(pkg)).append("\",\"cls\":\"").append(esc(cls)).append("\"}");
            }
            sb.append("]");
            return sb.toString();
        }

        private String esc(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        private String page() {
            StringBuilder b = new StringBuilder();
            b.append("<!doctype html><html><head><meta charset='utf-8'>");
            b.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
            b.append("<title>Simian Web Control</title><style>");
            b.append("body{background:#0e1216;color:#e6e6e6;font-family:sans-serif;margin:0;padding:12px}");
            b.append("h1{color:#4fc3f7;font-size:20px;margin:6px 0}");
            b.append("h2{color:#ffb300;font-size:13px;margin:16px 0 6px;text-transform:uppercase}");
            b.append("button{background:#1b2530;color:#e6e6e6;border:1px solid #2e3e4e;border-radius:8px;padding:12px;margin:4px;font-size:15px}");
            b.append("button:active{background:#2a3a4a}.danger{border-color:#a33;color:#ff8a80}");
            b.append(".row{display:flex;align-items:center;gap:6px;border-bottom:1px solid #1b2530;padding:4px 0}.row .nm{flex:1;font-size:14px}");
            b.append("#out{background:#000;color:#7CFC00;padding:8px;border-radius:8px;white-space:pre-wrap;font-family:monospace;font-size:12px;min-height:36px;margin-top:8px}");
            b.append("input{background:#1b2530;color:#fff;border:1px solid #2e3e4e;border-radius:8px;padding:10px;width:65%}");
            b.append("</style></head><body>");
            b.append("<h1>&#128018; Simian Web Control</h1>");
            b.append("<div style='font-size:12px;color:#888'>Echo Show &middot; root via Magisk &middot; LAN only &middot; <a href='#' style='color:#888' onclick='chpin()'>change PIN</a></div>");
            b.append("<h2>Quick Controls</h2>");
            b.append("<button onclick=\"key(223)\">&#127769; Display Off</button>");
            b.append("<button onclick=\"key(224)\">&#9728;&#65039; Wake</button>");
            b.append("<button onclick=\"key(24)\">&#128266; Vol +</button>");
            b.append("<button onclick=\"key(25)\">&#128263; Vol -</button>");
            b.append("<button onclick=\"key(164)\">&#128277; Mute</button>");
            b.append("<button onclick=\"key(3)\">&#127968; Home</button>");
            b.append("<button onclick=\"key(4)\">&#8617;&#65039; Back</button>");
            b.append("<button class='danger' onclick=\"if(confirm('Reboot device?'))cmd('reboot')\">&#9851;&#65039; Reboot</button>");
            b.append("<h2>Apps <button onclick='load()'>refresh</button> <button class='danger' onclick=\"if(confirm('Kill all 3rd-party apps?'))killall()\">kill all</button></h2>");
            b.append("<div id='apps'>enter PIN to load...</div>");
            b.append("<h2>Root Console</h2>");
            b.append("<input id='c' placeholder='shell command (root)'/> <button onclick='runc()'>Run</button>");
            b.append("<div id='out'>ready.</div>");
            b.append("<script>");
            b.append("var PIN=localStorage.getItem('pin')||'';");
            b.append("function ask(){PIN=prompt('Web Control PIN:')||'';localStorage.setItem('pin',PIN);}");
            b.append("function chpin(){localStorage.removeItem('pin');ask();load();}");
            b.append("if(!PIN)ask();");
            b.append("function api(path){var u=path+(path.indexOf('?')>=0?'&':'?')+'k='+encodeURIComponent(PIN);return fetch(u).then(function(r){return r.text();}).then(function(t){if(t==='FORBIDDEN'){ask();return 'bad PIN - tap again';}return t;});}");
            b.append("function flash(t){document.getElementById('out').textContent=t;}");
            b.append("function key(c){api('/api/key?code='+c).then(flash);}");
            b.append("function kill(p){api('/api/kill?pkg='+encodeURIComponent(p)).then(flash);}");
            b.append("function open2(p,c){api('/api/start?pkg='+encodeURIComponent(p)+'&cls='+encodeURIComponent(c)).then(flash);}");
            b.append("function killall(){flash('killing...');api('/api/killall').then(flash);}");
            b.append("function cmd(c){api('/api/cmd?c='+encodeURIComponent(c)).then(flash);}");
            b.append("function runc(){cmd(document.getElementById('c').value);}");
            b.append("function load(){api('/api/apps').then(function(t){var a;try{a=JSON.parse(t);}catch(e){document.getElementById('apps').textContent=t;return;}a.sort(function(x,y){return x.label.localeCompare(y.label);});var box=document.getElementById('apps');box.innerHTML='';a.forEach(function(o){var row=document.createElement('div');row.className='row';var nm=document.createElement('span');nm.className='nm';nm.textContent=o.label;row.appendChild(nm);var bo=document.createElement('button');bo.textContent='open';bo.onclick=function(){open2(o.pkg,o.cls);};row.appendChild(bo);var bk=document.createElement('button');bk.className='danger';bk.textContent='kill';bk.onclick=function(){kill(o.pkg);};row.appendChild(bk);box.appendChild(row);});});}");
            b.append("load();");
            b.append("</script></body></html>");
            return b.toString();
        }
    }
}
