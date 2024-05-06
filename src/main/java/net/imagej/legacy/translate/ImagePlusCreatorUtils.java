/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2024 ImageJ2 developers.
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
import java.util.Map;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * Utility class for {@link ImagePlusCreator}.
 *
 * @author Mark Hiner
 * @author Matthias Arzt
 */
public final class ImagePlusCreatorUtils {

	private ImagePlusCreatorUtils() {
		// prevent from instantiation
	}

	static void setMetadata( Dataset ds, ImagePlus imp )
	{
		imp.setOpenAsHyperStack(imp.getNDimensions() > 3);
		final FileInfo fileInfo = getFileInfo( ds, imp );
		imp.setFileInfo(fileInfo);
		setSliceLabels( imp, fileInfo );
		fillInfo(imp, ds.getImgPlus());
	}

	private static FileInfo getFileInfo( Dataset ds, ImagePlus imp )
	{
		final FileInfo fileInfo = new FileInfo();
		final String source = ds.getSource();
		final File file =
			source == null || "".equals(source) ? null : new File(source);

		// We could play games here, if needed.
		fileInfo.fileFormat = FileInfo.UNKNOWN;
		fileInfo.fileType = ds.isRGBMerged() ?
			FileInfo.RGB : ds.getType() instanceof UnsignedShortType ?
				FileInfo.GRAY16_UNSIGNED : FileInfo.GRAY8;
		if (file != null && file.exists()) {
			fileInfo.fileName = file.getName();
			fileInfo.directory = file.getParent() + File.separator;
		}
		else {
			fileInfo.url = source;
		}
		fileInfo.width = imp.getWidth();
		fileInfo.height = imp.getHeight();
		fileInfo.debugInfo = ds.toString();
		if (imp.getStack() instanceof VirtualStack ) {
			fileInfo.virtualStack = (VirtualStack) imp.getStack();
		}
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

		fileInfo.sliceLabels = getSliceLabels(ds);
		return fileInfo;
	}

	private static void setSliceLabels( ImagePlus imp, FileInfo fileInfo )
	{
		if (fileInfo.sliceLabels != null) {
			if (imp.getStackSize() < 2) {
				if (fileInfo.sliceLabels.length > 0) {
					imp.setProperty("Label", fileInfo.sliceLabels[0]);
				}
			}
			else {
				ImageStack stack = imp.getStack();
				for (int i = 0; i < fileInfo.sliceLabels.length && i < stack.getSize(); i++) {
					stack.setSliceLabel(fileInfo.sliceLabels[i], i + 1);
				}
			}
		}
	}

	// TODO remove usage of SCIFIO classes after migrating ImageMetadata
	// framework to imagej-common
	private static String[] getSliceLabels(final Dataset ds) {
		final Map<String, Object> properties = ds.getImgPlus().getProperties();
		if (properties == null) return null;
		final Object metadata = properties.get("scifio.metadata.image");
		if (metadata == null || !(metadata instanceof ImageMetadata)) return null;
		final MetaTable table = ((ImageMetadata) metadata).getTable();
		if (table == null) return null;
		// HACK - temporary until SCIFIO metadata API supports per-plane
		// metadata.
		final Object sliceLabels = table.get("SliceLabels");
		return sliceLabels != null && sliceLabels instanceof String[]
			? (String[]) sliceLabels : null;
	}

	// TODO remove usage of SCIFIO classes after migrating ImageMetadata
	// framework to imagej-common
	private static void fillInfo(final ImagePlus imp,
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

	// TODO remove usage of SCIFIO classes after migrating ImageMetadata
	// framework to imagej-common
	private static void fillImageInfo(final ImagePlus imp, final Metadata meta) {
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

	private static void addInfo(final ImagePlus imp, String newInfo) {
		final String info = (String) imp.getProperty("Info");
		if (info != null) newInfo = info + newInfo;
		imp.setProperty("Info", newInfo + "\n");
	}

	// TODO remove usage of SCIFIO classes after migrating ImageMetadata
	// framework to imagej-common
	private static void fillInfo(final ImagePlus imp, final MetaTable table) {
		String info = (String) imp.getProperty("Info");
		if (info == null) info = "";
		List<String> keySet = new ArrayList<>(table.keySet());
		Collections.sort(keySet);
		for (final String key : keySet) {
			info += key + " = " + table.get(key) + "\n";
		}
		imp.setProperty("Info", info);
	}

}
