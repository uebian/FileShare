package net.newlydev.fileshare_android.http;

import android.app.*;
import android.content.*;

import androidx.preference.*;

import android.view.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import net.newlydev.fileshare_android.*;

import org.apache.commons.fileupload.*;

import android.net.*;

import androidx.documentfile.provider.DocumentFile;

public class HttpThread extends Thread {
    MainService ctx;
    Socket client;
    DataInputStream sis;
    DataOutputStream sos;
    HttpRespond hr;

    public HttpThread(Socket client, MainService ctx) throws IOException{
        this.client = client;
        this.ctx = ctx;
        hr=new HttpRespond(ctx.getApplicationContext(),client.getOutputStream());
    }

    @Override
    public void run() {
        try{
            sis = new DataInputStream(client.getInputStream());
            sos = new DataOutputStream(client.getOutputStream());
            String reqLine = sis.readLine();
            String filename = reqLine.split(" ")[1];
            String contenttype = null;
            boolean isget = (reqLine.split(" ")[0]).equals("GET");
            int postlen = -1;
            String cookies = null;
            String str;
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

            if (isget) {
                if (filename.startsWith("/res")) {
                    hr.sendResFile(filename);
                } else {
                    Session session = null;
                    String authtype = PreferenceManager.getDefaultSharedPreferences(ctx).getString("authtype", "passwd");
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

                    if (session != null) {
                        if (filename.startsWith("/getfiles")) {
                            session.enterdir(URLDecoder.decode(filename.split("\\?")[1].split("=")[1], "UTF-8"));
                            String body = "", tmpf = "";
                            int dirlen = 0;
                            String filesystemtype = PreferenceManager.getDefaultSharedPreferences(ctx).getString("filesystem", "api");
                            if (filesystemtype.equals("api")) {
                                String rootpath = PreferenceManager.getDefaultSharedPreferences(ctx).getString("uripath", null);
                                if (rootpath == null) {
                                    body = "error";
                                    String rethead = "HTTP/1.1 200 OK \r\n" +
                                            "Content-Type: text/html; charset=UTF-8\r\n" +
                                            "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                                            "\r\n";
                                    sos.write(rethead.getBytes("UTF-8"));
                                    session.enterdir("..");
                                    sos.write(body.getBytes("UTF-8"));
                                } else {
                                    DocumentFile df = DocumentFile.fromTreeUri(ctx, Uri.parse(rootpath));
                                    for (String dirname : session.getpath().split("/")) {
                                        if (!dirname.equals("")) {
                                            df = df.findFile(dirname);
                                        }
                                    }
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
                                    String rethead = "HTTP/1.1 200 OK \r\n" +
                                            "Content-Type: text/html; charset=UTF-8\r\n" +
                                            "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                                            "\r\n";
                                    sos.write(rethead.getBytes("UTF-8"));
                                    sos.write(body.getBytes("UTF-8"));
                                }
                            } else {
//                                String sh;
//                                if (filesystemtype.equals("root_shell")) {
//                                    sh = "su";
//                                } else {
//                                    sh = "sh";
//                                }
//                                ArrayList<mFile> files = mFile.listFiles(sh, session.getpath(), ctx.getDataDir() + "/bin/fileutils", ctx.getDataDir().getPath() + "/fifo/");
//                                if (files == null) {
//                                    body = "error";
//                                    String rethead = "HTTP/1.1 200 OK \r\n" +
//                                            "Content-Type: text/html; charset=UTF-8\r\n" +
//                                            "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
//                                            "\r\n";
//                                    sos.write(rethead.getBytes("UTF-8"));
//                                    session.enterdir("..");
//                                    sos.write(body.getBytes("UTF-8"));
//                                } else for (mFile file : files) {
//                                    if (file.isDir()) {
//                                        dirlen++;
//                                        body = body + "\n" + file.getFileName();
//                                    } else {
//                                        tmpf = tmpf + "\n" + file.getFileName();
//                                    }
//                                }
//                                body = "ok " + dirlen + body + tmpf;
//                                String rethead = "HTTP/1.1 200 OK \r\n" +
//                                        "Content-Type: text/html; charset=UTF-8\r\n" +
//                                        "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
//                                        "\r\n";
//                                sos.write(rethead.getBytes("UTF-8"));
//                                sos.write(body.getBytes("UTF-8"));
                           }
                        } else if (filename.startsWith("/download")) {
                            String filesystemtype = PreferenceManager.getDefaultSharedPreferences(ctx).getString("filesystem", "api");
                            String downloadname = URLDecoder.decode(filename.split("\\?")[1].split("=")[1], "UTF-8");
                            if (downloadname.indexOf("/") != -1) {
                                byte body[] = "你想干嘛？".getBytes("UTF-8");
                                String rethead = "HTTP/1.1 200 OK \r\n" +
                                        "Content-Type: text/html; charset=UTF-8\r\n" +
                                        "Content-Length: " + body.length + "\r\n" +
                                        "\r\n";
                                sos.write(rethead.getBytes("UTF-8"));
                                sos.write(body);
                                sos.flush();
                            } else if (filesystemtype.equals("api")) {
                                DocumentFile df = DocumentFile.fromTreeUri(ctx, Uri.parse(PreferenceManager.getDefaultSharedPreferences(ctx).getString("uripath", null)));
                                for (String dirname : session.getpath().split("/")) {
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
                                    String rethead = "HTTP/1.1 200 OK \r\n" +
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
//                                String sh;
//                                if (filesystemtype.equals("root_shell")) {
//                                    sh = "su";
//                                } else {
//                                    sh = "sh";
//                                }
//                                try {
//                                    mFile downloadfile = new mFile(new File(session.getpath(), downloadname));
//                                    InputStream bis = downloadfile.getInputStream(sh, ctx.getDataDir() + "/bin/fileutils", ctx.getDataDir().getPath() + "/fifo/");
//                                    long filesize = downloadfile.getSize(sh, ctx.getDataDir() + "/bin/fileutils");
//                                    if (filesize == 0) {
//                                        byte body[] = "非常抱歉，我们暂不支持下载大小为0的文件。<a href=\"/\">返回</a>".getBytes("UTF-8");
//                                        String rethead = "HTTP/1.1 200 OK \r\n" +
//                                                "Content-Type: text/html; charset=UTF-8\r\n" +
//                                                "Content-Length: " + body.length + "\r\n" +
//                                                "\r\n";
//                                        sos.write(rethead.getBytes("UTF-8"));
//                                        sos.write(body);
//                                        sos.flush();
//                                    } else {
//                                        String rethead = "HTTP/1.1 200 OK \r\n" +
//                                                "Content-Type: application/octet-stream; charset=UTF-8\r\n" +
//                                                "Content-Length: " + filesize + "\r\n" +
//                                                "Content-Disposition: attachment; filename=" + downloadname + "\r\n" +
//                                                "\r\n";
//                                        sos.write(rethead.getBytes("UTF-8"));
//                                        byte[] buffer = new byte[1024];
//                                        int ch = bis.read(buffer);
//                                        while (ch != -1) {
//                                            sos.write(buffer, 0, ch);
//                                            ch = bis.read(buffer, 0, 1024);
//                                        }
//                                        sos.flush();
//                                        bis.close();
//                                    }
//                                } catch (FileNotFoundException e) {
//                                    byte body[] = "文件不存在".getBytes("UTF-8");
//                                    String rethead = "HTTP/1.1 200 OK \r\n" +
//                                            "Content-Type: text/html; charset=UTF-8\r\n" +
//                                            "Content-Length: " + body.length + "\r\n" +
//                                            "\r\n";
//                                    sos.write(rethead.getBytes("UTF-8"));
//                                    sos.write(body);
//                                    sos.flush();
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                    //Runtime.getRuntime().exec("rm -f " + fifoname);
//                                }
                            }
                        }else if (PreferenceManager.getDefaultSharedPreferences(ctx).getString("filesystem", "api").equals("api") && PreferenceManager.getDefaultSharedPreferences(ctx).getString("uripath", null) == null) {
                            hr.sendErrorMsg("起始路径未配置，请在开服端的FileShare应用上进行配置");
                        } else {
                            hr.sendResFile("main.html");
                        }
                    } else {
                        if (authtype.equals("none")) {
                            session = new Session(ctx);
                            InputStream is = ctx.getClassLoader().getResourceAsStream("assets/refresh.html");
                            String rethead = "HTTP/1.1 200 OK \r\n" +
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
                        } else if (authtype.equals("askme")) {
                            if (filename.startsWith("/askpermission")) {
                                final mObject obj = new mObject();
                                obj.obj = -1;
                                ctx.handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
                                        ab.setCancelable(false);
                                        ab.setTitle("FileShare");
                                        ab.setMessage("下列用户请求您的权限来访问您的文件\nip地址:" + client.getInetAddress().toString());
                                        ab.setPositiveButton("授权", new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface p1, int p2) {
                                                obj.obj = 1;
                                            }
                                        });
                                        ab.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface p1, int p2) {
                                                obj.obj = 0;
                                            }
                                        });
                                        AlertDialog dlg = ab.create();
                                        dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                        dlg.show();
                                    }
                                });
                                while (obj.obj.equals(-1)) {
                                    Thread.sleep(500);
                                }

                                String content = "no";
                                String rethead = "HTTP/1.1 200 OK \r\n" +
                                        "Content-Type: text/html; charset=UTF-8\r\n" +
                                        "Content-Length: " + content.getBytes("utf-8").length + "\r\n";
                                if (obj.obj.equals(1)) {
                                    content = "ok";
                                    String token = new Session(ctx).getToken();
                                    //tokens.add(token);
                                    rethead = rethead + "Set-Cookie: token=" + token + "; Path=/;\r\n\r\n";
                                } else {
                                    rethead = rethead + "\r\n";
                                }

                                sos.write(rethead.getBytes("utf-8"));
                                sos.write(content.getBytes("utf-8"));
                            } else {
                                InputStream is = ctx.getClassLoader().getResourceAsStream("assets/waitingforpermission.html");
                                String rethead = "HTTP/1.1 200 OK \r\n" +
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

                            }
                        } else {
                            hr.sendResFile("login.html");
                        }
                    }
                }

            } else {
                if (filename.equals("/dologin")) {
                    byte[] data = new byte[postlen];
                    sis.read(data);
                    String pwd_md5 = new String(data, "utf-8").split("=")[1];
                    String content = "no";
                    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
                    String real_pwd = p.getString("password", "null");

                    String rethead = "HTTP/1.1 200 OK \r\n" +
                            "Content-Type: text/html; charset=UTF-8\r\n" +
                            "Content-Length: " + content.getBytes("utf-8").length + "\r\n";
                    if (pwd_md5.equals(Utils.EncoderByMd5(real_pwd))) {
                        content = "ok";
                        String token = new Session(ctx).getToken();
                        //tokens.add(token);
                        rethead = rethead + "Set-Cookie: token=" + token + "; Path=/;\r\n\r\n";
                    } else {
                        rethead = rethead + "\r\n";
                    }

                    sos.write(rethead.getBytes("utf-8"));
                    sos.write(content.getBytes("utf-8"));
                    //Log.v("fileshare","post");

                } else if (filename.startsWith("/upload")) {
                    String filesystemtype = PreferenceManager.getDefaultSharedPreferences(ctx).getString("filesystem", "api");
                    Session session = null;
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
                    if (session == null) {
                        byte[] body = "请先登录".getBytes("utf-8");
                        String rethead = "HTTP/1.1 200 OK \r\n" +
                                "Content-Type: text/html; charset=UTF-8\r\n" +
                                "Content-Length: " + body.length + "\r\n" +
                                "\r\n";
                        sos.write(rethead.getBytes("UTF-8"));
                        sos.write(body);
                        sos.flush();
                    } else {
                        String uploadfilename = null;
                        String boundary = contenttype.split("boundary=")[1];
                        MultipartStream ms = new MultipartStream(sis, boundary.getBytes("utf-8"));
                        String[] headers = ms.readHeaders().split("\r\n");
                        Matcher matcher = Pattern.compile("Content-Disposition: form-data; name=\".*\"; filename=\"(.*){1}\".*").matcher(headers[1]);
                        if (matcher.matches()) {
                            uploadfilename = new File(matcher.group(1)).getName();
                        }
                        File uploadfile = new File(session.getpath(), uploadfilename);
                        if (uploadfile.exists()) {

                            byte[] body = "文件名重复，请更换后重试".getBytes("utf-8");
                            String rethead = "HTTP/1.1 200 OK \r\n" +
                                    "Content-Type: text/html; charset=UTF-8\r\n" +
                                    "Content-Length: " + body.length + "\r\n" +
                                    "\r\n";
                            sos.write(rethead.getBytes("UTF-8"));
                            sos.write(body);
                            sos.flush();
                        } else if (filesystemtype.equals("api")) {
                            DocumentFile df = DocumentFile.fromTreeUri(ctx, Uri.parse(PreferenceManager.getDefaultSharedPreferences(ctx).getString("uripath", null)));
                            for (String dirname : session.getpath().split("/")) {
                                if (!dirname.equals("")) {
                                    df = df.findFile(dirname);
                                }
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
                            //fos.write(baos.toByteArray());
                            sos.flush();
                            fos.close();
                            is.close();
                            //baos.close();
                            byte[] body = "ok".getBytes("utf-8");
                            String rethead = "HTTP/1.1 200 OK \r\n" +
                                    "Content-Type: text/html; charset=UTF-8\r\n" +
                                    "Content-Length: " + body.length + "\r\n" +
                                    "\r\n";
                            sos.write(rethead.getBytes("UTF-8"));
                            sos.write(body);
                            sos.flush();
                        } else {
//                            String sh;
//                            if (filesystemtype.equals("root_shell")) {
//                                sh = "su";
//                            } else {
//                                sh = "sh";
//                            }
//                            OutputStream fos = new mFile(uploadfile).getOutputStream(sh, ctx.getDataDir() + "/bin/fileutils", ctx.getDataDir().getPath() + "/fifo/");
//                            InputStream is = ms.newInputStream();
//                            byte[] buffer = new byte[1024];
//                            int ch = is.read(buffer);
//                            while (ch != -1) {
//                                fos.write(buffer, 0, ch);
//                                ch = is.read(buffer, 0, 1024);
//                            }
//                            //fos.write(baos.toByteArray());
//                            sos.flush();
//                            fos.close();
//                            is.close();
//                            //baos.close();
//                            byte[] body = "ok".getBytes("utf-8");
//                            String rethead = "HTTP/1.1 200 OK \r\n" +
//                                    "Content-Type: text/html; charset=UTF-8\r\n" +
//                                    "Content-Length: " + body.length + "\r\n" +
//                                    "\r\n";
//                            sos.write(rethead.getBytes("UTF-8"));
//                            sos.write(body);
//                            sos.flush();
                            //sos.close();
                            //}
                        }
                    }
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        try {
            client.close();
        } catch (IOException e) {
        }
    }
}