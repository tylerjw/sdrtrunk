package source.file;

import gui.control.JFrequencyControl;
import source.SourceEditor;
import source.config.SourceConfigFile;
import source.config.SourceConfiguration;

public class FileSourceEditor extends SourceEditor 
{
    private static final long serialVersionUID = 1L;
    private JFrequencyControl mFrequencyControl;
    
	public FileSourceEditor( SourceConfiguration config )
	{
		super( config );
		
		initGUI();
	}

	public void reset()
	{
		mFrequencyControl.setFrequency( 
				((SourceConfigFile)mConfig).getFrequency(), false );
	}
	
	public void save()
	{
		((SourceConfigFile)mConfig).setFrequency( mFrequencyControl.getFrequency() );
	}
	
	private void initGUI()
	{
		
		mFrequencyControl = new JFrequencyControl();
		
		mFrequencyControl.setFrequency( 
				((SourceConfigFile)mConfig).getFrequency(), false );
		
		add( mFrequencyControl );
	}
}
