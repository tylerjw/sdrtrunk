package source.file;

import javax.swing.JPanel;

import controller.BaseNode;
import controller.file.File;
import controller.file.FileEditor;

public class FileNode extends BaseNode
{
    private static final long serialVersionUID = 1L;

    public FileNode(File file)
	{
        super( file );
	}
    
    public String toString()
    {
    	return "File Node";
    }
    
    public File getFile() 
    {
    	return (File)getUserObject();
    }
    
    @Override
	public JPanel getEditor()
	{
	    return new FileEditor( this );
	}
}
