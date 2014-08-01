package controller.file;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.TreePath;

import net.miginfocom.swing.MigLayout;
import source.file.FileNode;
import controller.system.SystemEditor;

public class FileEditor  extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private FileNode mFileNode;
    
    private JLabel mLabelFilename;
    private JTextField mTextFilename;

	public FileEditor( FileNode fileNode )
	{
		mFileNode = fileNode;
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout() );
		
		setBorder( BorderFactory.createTitledBorder( "System" ) );

		mLabelFilename = new JLabel( "File Name:" );
		add( mLabelFilename, "align right" );
		
		mTextFilename = new JTextField( mFileNode.getFile().getName(), 20);
		add( mTextFilename, "grow, wrap" );
		
		JButton btnBrowse = new JButton( "Browse" );
		btnBrowse.addActionListener( FileEditor.this );
		add( btnBrowse );
//
//		JButton btnReset = new JButton( "Reset" );
//		btnReset.addActionListener( SystemEditor.this );
//		add( btnReset, "wrap" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Browse" ) )
		{
			JFileChooser fileChooser = new JFileChooser();
			
			int result = fileChooser.showOpenDialog(null);
			
			switch(result) {
			case JFileChooser.APPROVE_OPTION:
				String filename = fileChooser.getSelectedFile().getPath();
				mTextFilename.setText(filename);
				mFileNode.getFile().setName(filename);
				break;
			case JFileChooser.ERROR_OPTION:
				JOptionPane.showMessageDialog(null, "An error occurred.");
				break;
			default:
				break;
			}
		}
    }
}
