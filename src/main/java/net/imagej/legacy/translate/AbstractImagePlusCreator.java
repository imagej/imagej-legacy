/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2014 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.legacy.translate;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.io.FileInfo;
import ij.measure.Calibration;
import io.scif.ImageMetadata;
import io.scif.MetaTable;
import io.scif.Metadata;
import io.scif.img.SCIFIOImgPlus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.imagej.Dataset;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.scijava.AbstractContextual;

/**
 * Abstract superclass for {@link ImagePlusCreator} implementations. Provides
 * general utility methods.
 * 
 * @author Mark Hiner
 */
public abstract class AbstractImagePlusCreator extends AbstractContextual
	implements ImagePlusCreator
{

	/**
	 * Sets the {@link Calibration} data on the provided {@link ImagePlus}.
	 */
	protected void populateCalibrationData(final ImagePlus imp, final Dataset ds)
	{
		final ImgPlus<? extends RealType<?>> imgPlus = ds.getImgPlus();

		final Calibration calibration = imp.getCalibration();
		final int xIndex = imgPlus.dimensionIndex(Axes.X);
		final int yIndex = imgPlus.dimensionIndex(Axes.Y);
		final int zIndex = imgPlus.dimensionIndex(Axes.Z);
		final int tIndex = imgPlus.dimensionIndex(Axes.TIME);

		if (xIndex >= 0) {
			calibration.pixelWidth = imgPlus.averageScale(xIndex);
			calibration.setXUnit(imgPlus.axis(xIndex).unit());
		}
		if (yIndex >= 0) {
			calibration.pixelHeight = imgPlus.averageScale(yIndex);
			calibration.setYUnit(imgPlus.axis(yIndex).unit());
		}
		if (zIndex >= 0) {
			calibration.pixelDepth = imgPlus.averageScale(zIndex);
			calibration.setZUnit(imgPlus.axis(zIndex).unit());
		}
		if (tIndex >= 0) {
			calibration.frameInterval = imgPlus.averageScale(tIndex);
			calibration.setTimeUnit(imgPlus.axis(tIndex).unit());
		}
	}

	protected ImagePlus makeImagePlus(Dataset ds, ImageStack stack) {
		final int[] dimIndices = new int[5];
		final int[] dimValues = new int[5];
		LegacyUtils.getImagePlusDims(ds, dimIndices, dimValues);
		return makeImagePlus(ds, dimValues[2], dimValues[3], dimValues[4], stack);
	}

	protected ImagePlus makeImagePlus(final Dataset ds, final int c, final int z,
		final int t, final ImageStack stack)
	{
		ImagePlus imp = new ImagePlus(ds.getName(), stack);

		imp.setDimensions(c, z, t);

		imp.setOpenAsHyperStack(imp.getNDimensions() > 3);

		final FileInfo fileInfo = new FileInfo();
		final String source = ds.getSource();
		final File file =
			source == null || "".equals(source) ? null : new File(source);

		// We could play games here, if needed.
		fileInfo.fileFormat = FileInfo.UNKNOWN;
		fileInfo.fileType = ds.isRGBMerged() ?
			FileInfo.RGB : ds.getType() instanceof UnsignedShortType ?
				FileInfo.GRAY16_UNSIGNED : FileInfo.GRAY8;
		if (file.exists()) {
			fileInfo.fileName = file.getName();
			fileInfo.directory = file.getParent() + File.separator;
		}
		else {
			fileInfo.url = source;
		}
		fileInfo.width = stack.getWidth();
		fileInfo.height = stack.getHeight();
		// fileInfo.offset = 0;
		// fileInfo.nImages = 1;
		// fileInfo.gapBetweenImages = 0;
		// fileInfo.whiteIsZero = false;
		// fileInfo.intelByteOrder = false;
		// fileInfo.compression = FileInfo.COMPRESSION_NONE;
		// fileInfo.stripOffsets = null;
		// fileInfo.stripLengths = null;
		// fileInfo.rowsPerStrip = 0;
		// fileInfo.lutSize = 0;
		// fileInfo.reds = null;
		// fileInfo.greens = null;
		// fileInfo.blues = null;
		// fileInfo.pixels = null;
		fileInfo.debugInfo = ds.toString();
		// fileInfo.sliceLabels = null;
		// fileInfo.info = "";
		// fileInfo.inputStream = null;
		if (stack instanceof VirtualStack) {
			fileInfo.virtualStack = (VirtualStack) stack;
		}
		populateCalibrationData(imp, ds);
		final Calibration calibration = imp.getCalibration();
		if (calibration != null) {
			fileInfo.pixelWidth = calibration.pixelWidth;
			fileInfo.pixelHeight = calibration.pixelHeight;
			fileInfo.pixelDepth = calibration.pixelDepth;
			fileInfo.unit = calibration.getUnit();
			fileInfo.calibrationFunction = calibration.getFunction();
			fileInfo.coefficients = calibration.getCoefficients();
			fileInfo.valueUnit = calibration.getValueUnit();
			fileInfo.frameInterval = calibration.frameInterval;
		}
		// fileInfo.description = "";
		// fileInfo.longOffset = 0;
		// fileInfo.metaDataTypes = null;
		// fileInfo.metaData = null;
		// fileInfo.displayRanges = null;
		// fileInfo.channelLuts = null;
		// fileInfo.roi = null;
		// fileInfo.overlay = null;
		// fileInfo.samplesPerPixel = 1;
		// fileInfo.openNextDir = null;
		// fileInfo.openNextName = null;

		imp.setFileInfo(fileInfo);

		if (!ds.isRGBMerged()) {
			/*
			 * ImageJ 1.x will use a StackWindow *only* if there is more than one channel.
			 * So unfortunately, we cannot consistently return a composite image here. We
			 * have to continue to deliver different data types that require specific case
			 * logic in any handler.
			 */
			if (imp.getStackSize() > 1) {
				imp = new CompositeImage(imp, CompositeImage.COMPOSITE);
			}
		}

		fillInfo(imp, ds.getImgPlus());

		return imp;
	}

	private void fillInfo(final ImagePlus imp,
		final ImgPlus<? extends RealType<?>> imgPlus)
	{
		if (imgPlus instanceof SCIFIOImgPlus) {
			final SCIFIOImgPlus<?> scifioImgPlus = (SCIFIOImgPlus<?>)imgPlus;

			final Metadata meta = scifioImgPlus.getMetadata();
			if (meta != null) {
				fillImageInfo(imp, meta);

				addInfo(imp, "--- Global Metadata ---");
				fillInfo(imp, meta.getTable());

				addInfo(imp, "--- Image Metadata ---");
				for (final ImageMetadata iMeta : meta.getAll()) {
					fillInfo(imp, iMeta.getTable());
				}
			}
		}
	}

	private void fillImageInfo(final ImagePlus imp, final Metadata meta) {
		addInfo(imp, "--- Dataset Information ---");
		addInfo(imp, "BitsPerPixel = " + meta.get(0).getBitsPerPixel());
		addInfo(imp, "PixelType = " + meta.get(0).getPixelType());
		addInfo(imp, "Dataset name = " + meta.getDatasetName());

		for (int i=0; i<meta.getImageCount(); i++) {
			addInfo(imp, "Image " + i + " Information");
			String dimensionOrder = "";
			String dimensionLengths = "";
			for (int j=0; j<meta.get(i).getAxes().size(); j++) {
				dimensionOrder += meta.get(i).getAxis(j).type().getLabel();
				dimensionLengths += meta.get(i).getAxisLength(j);

				if (j < meta.get(i).getAxes().size() - 1) {
					dimensionOrder += ",";
					dimensionLengths += ",";
				}
			}
			addInfo(imp, "Dimension order = " + dimensionOrder);
			addInfo(imp, "Dimension lengths = " + dimensionLengths);
		}
	}

	private void addInfo(final ImagePlus imp, String newInfo) {
		final String info = (String) imp.getProperty("Info");
		if (info != null) newInfo = info + newInfo;
		imp.setProperty("Info", newInfo + "\n");
	}

	private void fillInfo(final ImagePlus imp, final MetaTable table) {
		String info = (String) imp.getProperty("Info");
		if (info == null) info = "";
		List<String> keySet = new ArrayList<String>(table.keySet());
		Collections.sort(keySet);
		for (final String key : keySet) {
			info += key + " = " + table.get(key) + "\n";
		}
		imp.setProperty("Info", info);
	}
}
