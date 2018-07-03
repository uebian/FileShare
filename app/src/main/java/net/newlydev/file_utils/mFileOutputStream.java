package net.newlydev.file_utils;
import java.io.*;

public class mFileOutputStream extends FileOutputStream
{
	private BufferedWriter cmdos;
	Process p;
	public mFileOutputStream(String file,BufferedWriter cmdos,Process p) throws FileNotFoundException
	{
		super(file);
		this.cmdos=cmdos;
		this.p=p;
	}

	@Override
	public void close() throws IOException
	{
		// TODO: Implement this method
		super.close();
		cmdos.write("exit");
		cmdos.close();
		p.destroy();
	}
}
