package net.newlydev.file_utils;
import org.apache.commons.io.input.*;
import java.io.*;

public class FifoInputStream extends FileInputStream
{
	private BufferedWriter cmdos;
	String fifo;
	Process p;
	public FifoInputStream(String fifo,BufferedWriter cmdos,Process p) throws FileNotFoundException
	{
		super(fifo);
		this.fifo=fifo;
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
		Runtime.getRuntime().exec("rm -f " + fifo);
	}
	
}
