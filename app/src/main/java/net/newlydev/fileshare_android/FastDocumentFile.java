package net.newlydev.fileshare_android;
import androidx.documentfile.provider.DocumentFile;

public class FastDocumentFile
{
	private DocumentFile df;
	private String filename;
	public FastDocumentFile(DocumentFile df)
	{
		this.df=df;
		filename=df.getName();
	}
	public DocumentFile getDocumentFile()
	{
		return df;
	}
	public String getName()
	{
		return filename;
	}
}
