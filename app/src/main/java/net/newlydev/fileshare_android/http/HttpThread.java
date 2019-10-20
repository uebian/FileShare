package net.newlydev.fileshare_android.http;

import android.content.*;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.*;

import android.view.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

import net.newlydev.fileshare_android.*;

import org.apache.commons.fileupload.*;
import org.json.JSONObject;

import android.net.*;

import androidx.documentfile.provider.DocumentFile;

public class HttpThread extends Thread {
    private MainService ctx;
    private Socket client;
    private DataInputStream sis;
    private DataOutputStream sos;
    private HttpRespond hr;
    private boolean premissiond;

    public HttpThread(Socket client, MainService ctx) throws IOException {
        this.client = client;
        this.ctx = ctx;
        hr = new HttpRespond(ctx.getApplicationContext(), client.getOutputStream());
    }

    @Override
    public void run() {
        try {
            sis = new DataInputStream(client.getInputStream());
            sos = new DataOutputStream(client.getOutputStream());
            String reqLine = sis.readLine();
            String filename = reqLine.split(" ")[1];
            boolean isget = (reqLine.split(" ")[0]).equals("GET");
            String contenttype = null;
            int postlen = -1;
            String cookies = null;
            String str;
            String fileSystemTypes = PreferenceManager.getDefaultSharedPreferences(ctx).getString("fileSystem", "api");
            while (true) {
                str = sis.readLine();
                if (str.length() == 0) {
                    break;
                }
                if (str.equals("\r\n")) {
                    break;
                }
                if (str.split(":")[0].equals("Content-Length")) {
                    try {
                        String length = str.split(":")[1];
                        postlen = Integer.parseInt(length.trim());
                    } catch (Exception e) {
                    }
                }
                if (str.split(":")[0].equals("Cookie")) {
                    cookies = str.split(":")[1].trim();
                } else if (str.split(":")[0].equals("Content-Type")) {
                    contenttype = str.split(":")[1].trim();
                }
            }
            Session session = null;
            String authType = PreferenceManager.getDefaultSharedPreferences(ctx).getString("authType", "passwd");
            String t = null;
            if (cookies != null) {
                for (String c : cookies.split(";")) {
                    if (c.split("=")[0].trim().equals("token")) {
                        t = c.split("=")[1].trim();
                    }
                }
            }
            if (t != null) {
                session = Session.getSession(t);
            }
            if (isget) {
                if (filename.startsWith("/res")) {
                    hr.sendResFile(filename);
                } else if (session != null) {
                    if (filename.startsWith("/getfiles")) {
                        session.enterDir(URLDecoder.decode(filename.split("\\?")[1].split("=")[1], "UTF-8"));
                        String body = "", tmpf = "";
                        int dirlen = 0;
                        if (fileSystemTypes.equals("api")) {
                            String rootPath = PreferenceManager.getDefaultSharedPreferences(ctx).getString("uriPath", null);
                            if (rootPath == null) {
                                hr.sendContent("no 起始路径未配置，请在开服端FileShare应用上设置");
                            } else {
                                DocumentFile df = DocumentFile.fromTreeUri(ctx, Uri.parse(rootPath));
                                for (String dirname : session.getPath().split("/")) {
                                    if (!dirname.equals("")) {
                                        df = df.findFile(dirname);
                                    }
                                }
                                ArrayList<FastDocumentFile> files = new ArrayList<FastDocumentFile>();
                                if (!df.canRead()) {
                                    hr.sendContent("no 起始路径获取失败，可能是授权已失效。请在FileShare应用中重新配置");
                                } else {
                                    for (DocumentFile tdf : df.listFiles()) {
                                        files.add(new FastDocumentFile(tdf));
                                    }
                                    Collections.sort(files, new Comparator<FastDocumentFile>() {

                                        @Override
                                        public int compare(FastDocumentFile p1, FastDocumentFile p2) {
                                            return p1.getName().compareTo(p2.getName());
                                        }
                                    });
                                    body = body + "\n..";
                                    dirlen++;
                                    for (FastDocumentFile file : files) {
                                        if (file.getDocumentFile().isDirectory()) {
                                            dirlen++;
                                            body = body + "\n" + file.getName();
                                        } else {
                                            tmpf = tmpf + "\n" + file.getName();
                                        }
                                    }
                                    body = "ok " + dirlen + body + tmpf;
                                    hr.sendContent(body);
                                }
                            }
                        } else {
                            throw new RuntimeException("已弃用");
                        }
                    } else if (filename.startsWith("/download")) {
                        String filename_e=filename.split("\\?")[1].split("=")[1];
                        String downloadname = Escape.unescape(filename_e);
                        if (downloadname.indexOf("/") != -1) {
                            hr.sendErrorMsg("因为安全问题，请求被阻止");
                        } else if (fileSystemTypes.equals("api")) {
                            DocumentFile df = DocumentFile.fromTreeUri(ctx, Uri.parse(PreferenceManager.getDefaultSharedPreferences(ctx).getString("uriPath", null)));
                            for (String dirname : session.getPath().split("/")) {
                                if (!dirname.equals("")) {
                                    df = df.findFile(dirname);
                                }
                            }
                            DocumentFile downloadfile = df.findFile(downloadname);
                            InputStream bis = ctx.getContentResolver().openInputStream(downloadfile.getUri());
                            long filesize = downloadfile.length();
                            if (filesize == 0) {
                                hr.sendErrorMsg("非常抱歉，我们暂不支持下载大小为0的文件。<a href=\"/\">返回</a>");
                            } else {
                                String rethead = "HTTP/1.0 200 OK \r\n" +
                                        "Content-Type: application/octet-stream; charset=UTF-8\r\n" +
                                        "Content-Length: " + filesize + "\r\n" +
                                        "Content-Disposition: attachment; filename=" + downloadname + "\r\n" +
                                        "\r\n";
                                sos.write(rethead.getBytes("UTF-8"));
                                byte[] buffer = new byte[1024];
                                int ch = bis.read(buffer);
                                while (ch != -1) {
                                    sos.write(buffer, 0, ch);
                                    ch = bis.read(buffer, 0, 1024);
                                }
                                sos.flush();
                                bis.close();
                            }
                        } else {
                            throw new RuntimeException("已弃用");
                        }
                    }if (filename.equals("/logout")) {
                        Session.sessions.remove(session);
                        hr.sendResFile("refresh.html");
                    }else if (filename.startsWith("/root")) {
                        String realPath = filename.substring("/root".length());
                        boolean error = false, isFile = false;
                        String body = "", tmpf = "";
                        if (fileSystemTypes.equals("api")) {
                            String rootPath = PreferenceManager.getDefaultSharedPreferences(ctx).getString("uriPath", null);
                            if (rootPath == null) {
                                hr.sendContent("error1");
                            } else {
                                DocumentFile df = DocumentFile.fromTreeUri(ctx, Uri.parse(rootPath));
                                for (String dirname : realPath.split("/")) {
                                    if (!dirname.equals("")) {
                                        df = df.findFile(dirname);
                                        if (df == null) {
                                            hr.sendContent("Not Found");
                                            error = true;
                                            break;
                                        }
                                        if (df.isFile()) {
                                            isFile = true;
                                            break;
                                        }
                                    }
                                }
                                if (!error) {
                                    if (isFile) {
                                        InputStream bis = ctx.getContentResolver().openInputStream(df.getUri());
                                        long filesize = df.length();
                                        if (filesize == 0) {
                                            hr.sendContent("非常抱歉，我们暂不支持下载大小为0的文件");
                                        } else {
                                            String rethead = "HTTP/1.0 200 OK \r\n" +
                                                    "Content-Type: application/octet-stream; charset=UTF-8\r\n" +
                                                    "Content-Length: " + filesize + "\r\n" +
                                                    "Content-Disposition: attachment; filename=" + df.getName() + "\r\n" +
                                                    "\r\n";
                                            sos.write(rethead.getBytes("UTF-8"));
                                            byte[] buffer = new byte[1024];
                                            int ch = bis.read(buffer);
                                            while (ch != -1) {
                                                sos.write(buffer, 0, ch);
                                                ch = bis.read(buffer, 0, 1024);
                                            }
                                            sos.flush();
                                            bis.close();
                                        }
                                    } else {
                                        ArrayList<FastDocumentFile> files = new ArrayList<FastDocumentFile>();
                                        for (DocumentFile tdf : df.listFiles()) {
                                            files.add(new FastDocumentFile(tdf));
                                        }
                                        Collections.sort(files, new Comparator<FastDocumentFile>() {

                                            @Override
                                            public int compare(FastDocumentFile p1, FastDocumentFile p2) {
                                                return p1.getName().compareTo(p2.getName());
                                            }
                                        });
                                        for (FastDocumentFile file : files) {
                                            if (file.getDocumentFile().isDirectory()) {
                                                body = body + "\n\r" + file.getName();
                                            } else {
                                                tmpf = tmpf + "\n\r" + file.getName();
                                            }
                                        }
                                        body = body + tmpf;
                                        hr.sendContent(body);
                                    }
                                }
                            }
                        } else {
                            throw new RuntimeException("已弃用");
                        }
                    } else {
                        if (fileSystemTypes.equals("api") && PreferenceManager.getDefaultSharedPreferences(ctx).getString("uriPath", null) == null) {
                            hr.sendErrorMsg("起始路径未配置，请在开服端的FileShare应用上进行配置");
                        } else {
                            hr.sendResFile("main.html");
                        }
                    }
                } else {
                    switch (authType) {
                        case "none": {
                            session = new Session(ctx);
                            InputStream is = ctx.getClassLoader().getResourceAsStream("assets/refresh.html");
                            String rethead = "HTTP/1.0 200 OK \r\n" +
                                    "Content-Type: text/html; charset=UTF-8\r\n" +
                                    "Content-Length: " + is.available() + "\r\n" +
                                    "Set-Cookie: token=" + session.getToken() + "; Path=/;\r\n" +
                                    "\r\n";
                            sos.write(rethead.getBytes("UTF-8"));
                            byte[] buffer = new byte[1024];
                            int ch = is.read(buffer);
                            while (ch != -1) {
                                sos.write(buffer, 0, ch);
                                ch = is.read(buffer, 0, 1024);
                            }
                            break;
                        }
                        case "askme": {
                            if (filename.startsWith("/askpermission")) {
                                premissiond = false;
                                final Object lock = new Object();
                                ctx.handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            AlertDialog.Builder ab = new AlertDialog.Builder(new ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat_Light));
                                            ab.setCancelable(false);
                                            ab.setTitle("FileShare");
                                            ab.setMessage("下列用户请求您的权限来访问您的文件\nip地址:" + client.getInetAddress().toString());
                                            ab.setPositiveButton("授权", new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface p1, int p2) {
                                                    premissiond = true;
                                                    synchronized (lock) {
                                                        lock.notify();
                                                    }
                                                }
                                            });
                                            ab.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface p1, int p2) {
                                                    premissiond = false;
                                                    synchronized (lock) {
                                                        lock.notify();
                                                    }
                                                }
                                            });
                                            ab.setNeutralButton("拒绝并关闭服务器", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    premissiond = false;
                                                    synchronized (lock) {
                                                        lock.notify();
                                                    }
                                                    ctx.stopSelf();
                                                }
                                            });
                                            AlertDialog dlg = ab.create();
                                            dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                                            dlg.show();

                                        }catch(Exception e)
                                        {
                                            e.printStackTrace();
                                            premissiond=false;
                                        }
                                    }
                                });
                                synchronized (lock) {
                                    lock.wait();
                                }
                                String content;
                                String rethead = "HTTP/1.0 200 OK \r\n" +
                                        "Content-Type: text/html; charset=UTF-8\r\n";
                                if (premissiond) {
                                    content=new JSONObject().put("status",0).toString();
                                    String token = new Session(ctx).getToken();
                                    rethead = rethead + "Set-Cookie: token=" + token + "; Path=/;\r\n";
                                } else {
                                    content=new JSONObject().put("status",1).put("message","请求被拒").toString();
                                }
                                rethead=rethead + "Content-Length: " + content.getBytes("utf-8").length + "\r\n\r\n";
                                sos.write(rethead.getBytes("utf-8"));
                                sos.write(content.getBytes("utf-8"));
                                sos.flush();
                            } else {
                                InputStream is = ctx.getClassLoader().getResourceAsStream("assets/waitingforpermission.html");
                                String rethead = "HTTP/1.0 200 OK \r\n" +
                                        "Content-Type: text/html; charset=UTF-8\r\n" +
                                        "Content-Length: " + is.available() + "\r\n" +
                                        "\r\n";
                                sos.write(rethead.getBytes("UTF-8"));
                                byte[] buffer = new byte[1024];
                                int ch = is.read(buffer);
                                while (ch != -1) {
                                    sos.write(buffer, 0, ch);
                                    ch = is.read(buffer, 0, 1024);
                                }
                                sos.flush();
                            }
                            break;
                        }
                        case "passwd": {
                            hr.sendResFile("login.html");
                            break;
                        }
                    }
                }
            } else {
                if (authType.equals("passwd") && filename.equals("/dologin")) {
                    byte[] data = new byte[postlen];
                    sis.read(data);
                    String pwd_md5 = new String(data, "utf-8").split("=")[1];
                    String content;
                    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
                    String real_pwd = p.getString("password", "null");

                    String rethead = "HTTP/1.0 200 OK \r\n" +
                            "Content-Type: text/html; charset=UTF-8\r\n";
                    if (pwd_md5.equals(Utils.EncoderByMd5(real_pwd))) {
                        content=new JSONObject().put("status",0).toString();
                        String token = new Session(ctx).getToken();
                        //tokens.add(token);
                        rethead = rethead + "Set-Cookie: token=" + token + "; Path=/;\r\n";
                    } else {
                        content=new JSONObject().put("status",1).put("message","密码错误").toString();
                    }
                    rethead=rethead + "Content-Length: " + content.getBytes("utf-8").length + "\r\n\r\n";
                    sos.write(rethead.getBytes("utf-8"));
                    sos.write(content.getBytes("utf-8"));
                    sos.flush();
                } else if (filename.startsWith("/upload")) {
                    Thread.sleep(100);
                    if (session == null) {
                        hr.sendErrorMsg("请先登录");
                    } else if (!PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("allowUpload", false)) {
                        hr.sendContent("服务器未开放上传");
                    } else {
                        String uploadfilename = null;
                        String boundary = contenttype.split("boundary=")[1];
                        MultipartStream ms = new MultipartStream(sis, boundary.getBytes("utf-8"));
                        String[] headers = ms.readHeaders().split("\r\n");
                        Matcher matcher = Pattern.compile("Content-Disposition: form-data; name=\".*\"; filename=\"(.*){1}\".*").matcher(headers[1]);
                        if (matcher.matches()) {
                            uploadfilename = new File(matcher.group(1)).getName();
                        }
                        if (fileSystemTypes.equals("api")) {
                            DocumentFile df = DocumentFile.fromTreeUri(ctx, Uri.parse(PreferenceManager.getDefaultSharedPreferences(ctx).getString("uriPath", null)));
                            for (String dirname : session.getPath().split("/")) {
                                if (!dirname.equals("")) {
                                    df = df.findFile(dirname);
                                }
                            }
                            if (df.findFile(uploadfilename) != null) {
                                hr.sendContent("文件名重复，请更换后重试");
                            }
                            DocumentFile uploadcf = df.createFile("application/octet-stream", uploadfilename);
                            OutputStream fos = ctx.getContentResolver().openOutputStream(uploadcf.getUri());
                            InputStream is = ms.newInputStream();
                            byte[] buffer = new byte[1024];
                            int ch = is.read(buffer);
                            while (ch != -1) {
                                fos.write(buffer, 0, ch);
                                ch = is.read(buffer, 0, 1024);
                            }
                            sos.flush();
                            fos.close();
                            is.close();
                            byte[] body = "ok".getBytes("UTF-8");
                            String rethead = "HTTP/1.0 200 OK \r\n" +
                                    "Content-Type: text/html; charset=UTF-8\r\n" +
                                    "Content-Length: " + body.length + "\r\n" +
                                    "\r\n";
                            sos.write(rethead.getBytes("UTF-8"));
                            sos.write(body);
                            sos.flush();
                        } else {
                            throw new RuntimeException("已弃用");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            client.close();
        } catch (IOException e) {
        }
    }
}