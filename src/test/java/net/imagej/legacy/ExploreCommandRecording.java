package net.imagej.legacy;

import ij.plugin.frame.Recorder;
import org.scijava.command.Command;
import org.scijava.command.DefaultCommandService;
import org.scijava.plugin.Parameter;
import org.scijava.widget.Button;

public class ExploreCommandRecording implements Command
{
	@Parameter
	String string = "Hello";

	@Parameter( callback = "callback" )
	Button button;

	@Override
	public void run()
	{

	}

	public void callback()
	{
		System.out.println("Button pressed.");
	}

	public static void main( String[] args )
	{
		// turn on recorder
		new Recorder();

		// execute above command (does not work)
		final DefaultCommandService cs = new DefaultCommandService();
		cs.run( ExploreCommandRecording.class, true );
	}
}
