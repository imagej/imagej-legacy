package net.imagej.legacy;

import ij.IJ;
import ij.WindowManager;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import static org.junit.Assert.assertArrayEquals;

/**
 * Test if a {@link Command} that returns a {@link Dataset} behaves as expected
 * when a IJ1 macro is run in batch mode.
 */
public class BatchModeTest {

	static {
		LegacyInjector.preinit();
	}

	private Context context;

	@Before
	public void setUp() {
		context = new Context();
		UIService service = context.service(UIService.class);
		service.showUI();
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	/** Just for comparison, see how plain IJ1 behaves in batch mode. */
	@Test
	public void testIJ1() {
		WindowManager.closeAllWindows();
		IJ.runMacro(
			"setBatchMode(true);" +
			"newImage(\"imageA\", \"8-bit black\", 2, 2, 1);" +
			"newImage(\"imageB\", \"8-bit black\", 2, 2, 1);" +
			"setBatchMode(\"exit & display\");"
		);
		assertArrayEquals(new String[] {"imageA", "imageB"}, WindowManager.getImageTitles());
	}

	@Test
	public void testBatchMode() {
		WindowManager.closeAllWindows();
		IJ.runMacro(
			"setBatchMode(true);" +
			"run(\"Output Dataset\", \"title=imageA\");" +
			"run(\"Output Dataset\", \"title=imageB\");" +
			"setBatchMode(\"exit & display\");"
		);
		assertArrayEquals(new String[] {"imageA", "imageB"}, WindowManager.getImageTitles());
	}

	/**
	 * Simple command that outputs a {@link Dataset}. Used for testing.
	 *
	 * @author Matthias Arzt
	 */
	@Plugin(type = Command.class, menuPath = "ImageJ Legacy Test > Output Dataset")
	public static class OutputDataset implements Command {

		@Parameter
		private DatasetService datasetService;

		@Parameter(required = false)
		public String title = "title";

		@Parameter(type = ItemIO.OUTPUT)
		public Dataset output;

		@Override
		public void run() {
			output = datasetService
				.create(new UnsignedByteType(), new long[] { 2, 2 }, title,
					new AxisType[] { Axes.X, Axes.Y });
		}
	}

}
