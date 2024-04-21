package net.newlydev.fileshare_android.http;

import android.content.DialogInterface;
import android.content.UriPermission;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import net.newlydev.fileshare_android.FastDocumentFile;
import net.newlydev.fileshare_android.MainService;
import net.newlydev.fileshare_android.R;
import net.newlydev.fileshare_android.Session;
import net.newlydev.fileshare_android.Utils;

import org.apache.commons.fileupload.MultipartStream;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpThread implements Runnable {
	private MainService ctx;
	private Socket client;
	private DataInputStream sis;
	private DataOutputStream sos;
	private HttpRespond hr;
	private boolean premissiond;
	public static final int BUFSIZE=8192;

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
			String contentType = null;
			int postlen = -1;
			String cookies = null;
			String str;
			String multiThread = "";
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
				String headerType = str.split(":")[0];
				if (headerType.equals("Cookie")) {
					cookies = str.split(":")[1].trim();
				} else if (headerType.equals("Content-Type")) {
					contentType = str.split(":")[1].trim();
				}else if (headerType.equals("Range") && filename.startsWith("/download") && PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("multiThread", false)) {
					multiThread = str.split(":")[1].trim();
				}
			}
			Session session = null;
			Session.AuthType authType=Session.AuthType.get(PreferenceManager.getDefaultSharedPreferences(ctx).getString("authType", "passwd"));
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
				} else if (filename.startsWith("/download")) {
					String downloadToken = filename.split("\\?")[1].split("=")[1];
					String downloadFilePath = Session.getRealPath(downloadToken);
					String downloadFileName = new File(downloadFilePath).getName();
					downloadFilePath = new File(downloadFilePath).getParent();
					if (fileSystemTypes.equals("api")) {
						DocumentFile df = DocumentFile.fromTreeUri(ctx,ctx.getContentResolver().getPersistedUriPermissions().get(0).getUri());
						for (String dirname : downloadFilePath.split("/")) {
							if (!dirname.equals("")) {
								df = df.findFile(dirname);
							}
						}
						DocumentFile downloadFile = df.findFile(downloadFileName);
						InputStream bis = ctx.getContentResolver().openInputStream(downloadFile.getUri());
						long fileSize = downloadFile.length();
						if (fileSize == 0) {
							hr.sendErrorMsg("非常抱歉，我们暂不支持下载大小为0的文件。<a href=\"/\">返回</a>");
						} else {
							if (multiThread.equals("")) {
								String retHead = "HTTP/1.0 200 OK \r\n"
										+ "Content-Type: application/octet-stream; charset=UTF-8\r\n"
										+ "Content-Length: " + fileSize + "\r\n"
										+ "Content-Disposition: attachment; filename="
										+ URLEncoder.encode(downloadFileName,"UTF-8") + "\r\n" + "\r\n";
								sos.write(retHead.getBytes("UTF-8"));
								byte[] buffer = new byte[8192];
								int ch = bis.read(buffer);
								while (ch != -1) {
									sos.write(buffer, 0, ch);
									ch = bis.read(buffer, 0, 8192);
								}
								sos.flush();
								bis.close();
							} else {
								multiThread = multiThread.substring(multiThread.indexOf("bytes") + 6).trim();
								long start = Long.parseLong(multiThread.split("-")[0]);
								long end;
								try {
									end = Long.parseLong(multiThread.split("-")[1].split("/")[0]);
								} catch (Exception e) {
									end = fileSize - 1;
								}
								if (end == fileSize) {
									end--;
								}
								long lastlength = end - start + 1;
								String rethead = "HTTP/1.1 206 Partial Content \r\n"
										+ "Content-Type: application/octet-stream; charset=UTF-8\r\n"
										+ "Content-Length: " + lastlength + "\r\n" + "Content-Range: bytes " + start
										+ "-" + end + "/" + fileSize + "\r\n"
										+ "Content-Disposition: attachment; filename="
										+ URLEncoder.encode(downloadFileName, "UTF-8") + "\r\n" + "\r\n";
								sos.write(rethead.getBytes("UTF-8"));
								bis.skip(start);
								try {
									byte[] buffer = new byte[BUFSIZE];
									int ch;
									do {
										ch = bis.read(buffer, 0, (int) Math.min(lastlength, BUFSIZE));
										sos.write(buffer, 0, ch);
										lastlength = lastlength - ch;
									} while (ch != -1 && lastlength > 0);
								} catch (Exception e) {
									e.printStackTrace();
									System.out.printf("start=%d,end=%d\n", start, end);
								}
								sos.flush();
								bis.close();
							}
						}
					} else {
						throw new RuntimeException("已弃用");
					}
				} else if (session != null) {
					if (filename.startsWith("/getfiles")) {
						session.enterDir(URLDecoder.decode(filename.split("\\?")[1].split("=")[1], "UTF-8"));
						JSONArray filesJsonObject = new JSONArray();
						JSONArray dirJsonObject = new JSONArray();
						dirJsonObject.put("..");
						if (fileSystemTypes.equals("api")) {
							List<UriPermission> rootPath = ctx.getContentResolver().getPersistedUriPermissions();
							if (rootPath.size()==0) {
								hr.sendContent(new JSONObject().put("status", 1).put("message", "起始路径未配置，请在开服端FileShare应用上设置").toString());
							} else {
								DocumentFile df = DocumentFile.fromTreeUri(ctx, rootPath.get(0).getUri());
								for (String dirname : session.getPath().split("/")) {
									if (!dirname.equals("")) {
										df = df.findFile(dirname);
									}
								}
								ArrayList<FastDocumentFile> files = new ArrayList<FastDocumentFile>();
								if (!df.canRead()) {
									hr.sendContent(new JSONObject().put("status", 1).put("message", "起始路径获取失败，可能是授权已失效。请在FileShare应用中重新配置").toString());
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
									for (FastDocumentFile file : files) {
										if (file.getDocumentFile().isDirectory()) {
											dirJsonObject.put(file.getName());
										} else {
											filesJsonObject.put(file.getName());
										}
									}
									JSONObject resultJsonObject = new JSONObject();
									resultJsonObject.put("status", 0);
									resultJsonObject.put("dir", dirJsonObject);
									resultJsonObject.put("files", filesJsonObject);
									hr.sendContent(resultJsonObject.toString());
								}
							}
						} else {
							throw new RuntimeException("已弃用");
						}
					} else if (filename.startsWith("/createDownloadLink")) {
						String downloadFileName = URLDecoder.decode(filename.split("\\?")[1].split("=")[1]);
						if (!downloadFileName.contains("/")) {
							String downloadToken = session.createDownloadToken(session.getPath() + downloadFileName);
							hr.sendContent(new JSONObject().put("status", 0).put("token", downloadToken).toString());
						} else {
							hr.sendContent(new JSONObject().put("status", 1).toString());
						}
					} else if (filename.equals("/logout")) {
						Session.sessions.remove(session);
						hr.sendResFile("refresh.html");
					} else if (filename.startsWith("/root")) {
						String realPath = filename.substring("/root".length());
						boolean error = false, isFile = false;
						String body = "", tmpf = "";
						if (fileSystemTypes.equals("api")) {
							List<UriPermission> rootPath = ctx.getContentResolver().getPersistedUriPermissions();
							if(rootPath.size()==0) {
								hr.sendContent("error1");
							} else {
								DocumentFile df = DocumentFile.fromTreeUri(ctx, rootPath.get(0).getUri());
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
						if (fileSystemTypes.equals("api") && ctx.getContentResolver().getPersistedUriPermissions().size()==0) {
							hr.sendErrorMsg("起始路径未配置，请在开服端的FileShare应用上进行配置");
						} else {
							hr.sendResFile("main.html");
						}
					}
				} else {
					switch (authType) {
						case NONE: {
							session = new Session(ctx);
							InputStream is = ctx.getClassLoader().getResourceAsStream("assets/refresh.html");
							String retHead = "HTTP/1.0 200 OK \r\n" +
									"Content-Type: text/html; charset=UTF-8\r\n" +
									"Content-Length: " + is.available() + "\r\n" +
									"Set-Cookie: token=" + session.getToken() + "; Path=/;\r\n" +
									"\r\n";
							sos.write(retHead.getBytes("UTF-8"));
							byte[] buffer = new byte[1024];
							int ch = is.read(buffer);
							while (ch != -1) {
								sos.write(buffer, 0, ch);
								ch = is.read(buffer, 0, 1024);
							}
							break;
						}
						case ASKME: {
							if (filename.startsWith("/askpermission")) {
								premissiond = false;
								final Object lock = new Object();
								ctx.handler.post(new Runnable() {
									@Override
									public void run() {
										try {
											AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
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
										} catch (Exception e) {
											e.printStackTrace();
											premissiond = false;
										}
									}
								});
								synchronized (lock) {
									lock.wait();
								}
								String content;
								String retHead = "HTTP/1.0 200 OK \r\n" +
										"Content-Type: text/html; charset=UTF-8\r\n";
								if (premissiond) {
									content = new JSONObject().put("status", 0).toString();
									String token = new Session(ctx).getToken();
									retHead = retHead + "Set-Cookie: token=" + token + "; Path=/;\r\n";
								} else {
									content = new JSONObject().put("status", 1).put("message", "请求被拒").toString();
								}
								retHead = retHead + "Content-Length: " + content.getBytes("utf-8").length + "\r\n\r\n";
								sos.write(retHead.getBytes("utf-8"));
								sos.write(content.getBytes("utf-8"));
								sos.flush();
							} else {
								InputStream is = ctx.getClassLoader().getResourceAsStream("assets/waitingforpermission.html");
								String retHead = "HTTP/1.0 200 OK \r\n" +
										"Content-Type: text/html; charset=UTF-8\r\n" +
										"Content-Length: " + is.available() + "\r\n" +
										"\r\n";
								sos.write(retHead.getBytes("UTF-8"));
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
						case PASSWORD: {
							hr.sendResFile("login.html");
							break;
						}
					}
				}
			} else {
				if (authType== Session.AuthType.PASSWORD && filename.equals("/dologin")) {
					byte[] data = new byte[postlen];
					sis.read(data);
					String pwd_md5 = new String(data, "utf-8").split("=")[1];
					String content;
					String real_pwd = PreferenceManager.getDefaultSharedPreferences(ctx).getString("password", "null");

					String retHead = "HTTP/1.0 200 OK \r\n" +
							"Content-Type: text/html; charset=UTF-8\r\n";
					if (pwd_md5.equals(Utils.EncoderByMd5(real_pwd))) {
						content = new JSONObject().put("status", 0).toString();
						String token = new Session(ctx).getToken();
						//tokens.add(token);
						retHead = retHead + "Set-Cookie: token=" + token + "; Path=/;\r\n";
					} else {
						content = new JSONObject().put("status", 1).put("message", "密码错误").toString();
					}
					retHead = retHead + "Content-Length: " + content.getBytes("utf-8").length + "\r\n\r\n";
					sos.write(retHead.getBytes("utf-8"));
					sos.write(content.getBytes("utf-8"));
					sos.flush();
				} else if (filename.startsWith("/upload")) {
					if (session == null) {
						hr.sendErrorMsg("请先登录");
					} else {
						long startTime=System.currentTimeMillis();
						String uploadFileName = null;
						String boundary = contentType.split("boundary=")[1];
						MultipartStream ms = new MultipartStream(sis, boundary.getBytes("utf-8"));
						String[] headers = ms.readHeaders().split("\r\n");
						Matcher matcher = Pattern.compile("Content-Disposition: form-data; name=\".*\"; filename=\"(.*){1}\".*").matcher(headers[1]);
						if (matcher.matches()) {
							uploadFileName = new File(matcher.group(1)).getName();
						}
						if (!PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("allowUpload", false)) {
							ms.discardBodyData();
							hr.sendContent(new JSONObject().put("status",1).put("message","服务器未开放上传，请在开服端FileShare应用上配置").toString());
						} else if (fileSystemTypes.equals("api")) {
							DocumentFile df = DocumentFile.fromTreeUri(ctx, ctx.getContentResolver().getPersistedUriPermissions().get(0).getUri());
							for (String dirname : session.getPath().split("/")) {
								if (!dirname.equals("")) {
									df = df.findFile(dirname);
								}
							}
							if (df.findFile(uploadFileName) != null) {
								ms.discardBodyData();
								hr.sendContent(new JSONObject().put("status",1).put("message","文件名重复，请更换后重试").toString());
							}else {
								DocumentFile uploadcf = df.createFile("application/octet-stream", uploadFileName);
								OutputStream fos = ctx.getContentResolver().openOutputStream(uploadcf.getUri());
								ms.readBodyData(fos);
								JSONObject result = new JSONObject();
								result.put("status", 0);
								result.put("time", System.currentTimeMillis() - startTime);
								byte[] body = result.toString().getBytes("UTF-8");
								String rethead = "HTTP/1.0 200 OK \r\n" +
										"Content-Type: text/html; charset=UTF-8\r\n" +
										"Content-Length: " + body.length + "\r\n" +
										"\r\n";
								sos.write(rethead.getBytes("UTF-8"));
								sos.write(body);
								sos.flush();
							}
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
		} catch (IOException ignored) {
		}
	}
}