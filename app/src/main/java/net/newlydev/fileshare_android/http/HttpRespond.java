package net.newlydev.fileshare_android.http;

import android.content.Context;

import net.newlydev.fileshare_android.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class HttpRespond {
    Context ctx;
    OutputStream os;

    public HttpRespond(Context ctx, OutputStream os) {
        this.ctx = ctx;
        this.os = os;
    }

    public void sendResFile(String fileName) throws UnsupportedEncodingException, IOException {
        if (!fileName.startsWith("/")) {
            fileName = "/" + fileName;
        }
        InputStream is = ctx.getClassLoader().getResourceAsStream("assets" + fileName);
        String rethead = "HTTP/1.0 200 OK \r\n" +
                "Content-Length: " + is.available() + "\r\n" +
                "Content-Type: " + Utils.getContentTypeByExpansion(Utils.getExtensionByCutStr(fileName)) + "; charset=UTF-8\r\n" +
                "\r\n";
        os.write(rethead.getBytes("UTF-8"));
        byte[] buffer = new byte[1024];
        int ch = is.read(buffer);
        while (ch != -1) {
            os.write(buffer, 0, ch);
            ch = is.read(buffer, 0, 1024);
        }
        os.flush();
    }

    public void sendErrorMsg(String msgHtml) throws IOException {
        StringBuilder sb=new StringBuilder();
        String tmp;
        BufferedReader is = new BufferedReader(new InputStreamReader(ctx.getAssets().open("error.html"), "UTF-8"));
        while ((tmp = is.readLine()) != null) {
            sb.append(tmp + "\n");
        }
        String body=sb.toString().replaceFirst("contentarea",msgHtml);
        String rethead = "HTTP/1.0 200 OK \r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                "\r\n";
        os.write(rethead.getBytes("UTF-8"));
        os.write(body.getBytes("UTF-8"));
        os.flush();
    }
    public void sendContent(String content) throws IOException
    {
        String rethead = "HTTP/1.0 200 OK \r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + content.getBytes("UTF-8").length + "\r\n" +
                "\r\n";
        os.write(rethead.getBytes("UTF-8"));
        os.write(content.getBytes("UTF-8"));
        os.flush();
    }
}
