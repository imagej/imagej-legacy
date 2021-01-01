package net.imagej.legacy.convert;

import static org.junit.Assert.*;
import static org.scijava.test.TestUtils.createTemporaryDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.junit.Test;
import org.scijava.Context;

import ij.ImagePlus;
import ij.WindowManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import net.imagej.Dataset;

public class ParameterInputConversionTest {

	private String title1 = "Image 1";
	private String title2 = "Image 2";

	private int id1;
	private int width1 = 100;
	private int height1 = 200;
	private int width2 = 20;
	private int height2 = 10;

	private String scriptImagePlus = "#@ ImagePlus inputImage\n"
			+ "ijDir = System.getProperty(\"imagej.dir\")\n"
			+ "resultFile = new File(ijDir, \"result.txt\")\n"
			+ "System.err.println(\"ImagePlus: \" + inputImage.getTitle())\n"
			+ "resultFile.write(inputImage.getTitle())";

	private String scriptDataset = "#@ Dataset inputImage\n"
			+ "ijDir = System.getProperty(\"imagej.dir\")\n"
			+ "resultFile = new File(ijDir, \"result.txt\")\n"
			+ "System.err.println(\"Dataset: \" + inputImage.getName())\n"
			+ "resultFile.write(inputImage.getName())";

	/**
	 * Assert we can set an {@link ImagePlus} input using the image title in an
	 * option string.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testImagePlusByTitle() throws IOException {
		runScriptInContext(scriptImagePlus, false);
	}

	/**
	 * Assert we can set an {@link ImagePlus} input using the image ID in an
	 * option string.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testImagePlusByID() throws IOException {
		runScriptInContext(scriptImagePlus, true);
	}
	
	/**
	 * Assert we can set a {@link Dataset} input using the image title in an
	 * option string.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDatasetByTitle() throws IOException {
		runScriptInContext(scriptDataset, false);
	}

	/**
	 * Assert we can set a {@link Dataset} input using the image ID in an
	 * option string.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDatasetByID() throws IOException {
		runScriptInContext(scriptDataset, true);
	}

	// --- Helper Function ---

	private void runScriptInContext(String script, boolean useID) throws IOException {
		final File tmp = createTemporaryDirectory("macro-");
		final String imagejDirKey = "imagej.dir";
		final String imagejDir = System.getProperty(imagejDirKey);
		try {
			System.setProperty(imagejDirKey, tmp.getPath());
			final File plugins = new File(tmp, "plugins");
			final File scripts = new File(plugins, "Scripts");
			assertTrue(scripts.mkdirs());

			final File addArguments = new File(scripts, "Analyze_Image.groovy");
			try (final FileWriter writer = new FileWriter(addArguments)) {
				writer.write(script);
			}

			Context myContext = new Context();
			try {
				ImageProcessor ip1 = new FloatProcessor(width1, height1);
				ImagePlus imp1 = new ImagePlus(title1, ip1);
				imp1.show();
				id1 = imp1.getID();

				ImageProcessor ip2 = new FloatProcessor(width2, height2);
				ImagePlus imp2 = new ImagePlus(title2, ip2);
				imp2.show();

				assertEquals(imp2, WindowManager.getCurrentImage());

				String optionString = useID ? "inputimage=" + id1 : "inputimage=[" + title1 + "]";

				// Prevent the test class from loading the ij.IJ class
				new Runnable() {
					@Override
					public void run() {
						ij.IJ.run("Analyze Image", optionString);
					}
				}.run();
			} finally {
				myContext.dispose();
			}
			final File result = new File(tmp, "result.txt");
			assertTrue(result.exists());
			String resultString;
			try (Scanner scanner = new Scanner(result)) {
				resultString = scanner.nextLine();
			}
			assertEquals(title1, resultString);
		}
		finally {
			if (imagejDir == null) System.clearProperty(imagejDirKey);
			else System.setProperty(imagejDirKey, imagejDir);
		}
	}
}
