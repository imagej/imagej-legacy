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
import ij.measure.Calibration;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;
import net.imglib2.imagej.display.CalibrationUtils;

/**
 * Synchronizes metadata bidirectionally between a {@link Dataset} and an
 * {@link ImagePlus}.
 * 
 * @author Barry DeZonia
 */
public class MetadataHarmonizer implements DataHarmonizer {

	/** Sets a {@link Dataset}'s metadata to match a given {@link ImagePlus}. */
	@Override
	public void updateDataset(final Dataset ds, final ImagePlus imp) {
		ds.setName(imp.getTitle());
		// copy calibration info where possible
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		final int cIndex = ds.dimensionIndex(Axes.CHANNEL);
		final int zIndex = ds.dimensionIndex(Axes.Z);
		final int tIndex = ds.dimensionIndex(Axes.TIME);
		final Calibration cal = imp.getCalibration();
		CalibratedAxis axis = null;
		if (xIndex >= 0) {
			axis = ds.axis(xIndex);
			if (axis instanceof LinearAxis) {
				((LinearAxis) axis).setScale(cal.pixelWidth);
				((LinearAxis) axis).setOrigin(cal.xOrigin);
			}
			axis.setUnit(cal.getXUnit());
		}
		if (yIndex >= 0) {
			axis = ds.axis(yIndex);
			if (axis instanceof LinearAxis) {
				((LinearAxis) axis).setScale(cal.pixelHeight);
				((LinearAxis) axis).setOrigin(cal.yOrigin);
			}
			axis.setUnit(cal.getYUnit());
		}
		if (cIndex >= 0) {
			// OLD and likely bad
			// ds.setCalibration(1, cIndex);
		}
		if (zIndex >= 0) {
			axis = ds.axis(zIndex);
			if (axis instanceof LinearAxis) {
				((LinearAxis) axis).setScale(cal.pixelDepth);
				((LinearAxis) axis).setOrigin(cal.zOrigin);
			}
			axis.setUnit(cal.getZUnit());
		}
		if (tIndex >= 0) {
			axis = ds.axis(tIndex);
			if (axis instanceof LinearAxis) {
				((LinearAxis) axis).setScale(cal.frameInterval);
			}
			axis.setUnit(cal.getTimeUnit());
		}
		// no need to ds.update() - these calls should track that themselves
	}

	/** Sets an {@link ImagePlus}' metadata to match a given {@link Dataset}. */
	@Override
	public void updateLegacyImage(final Dataset ds, final ImagePlus imp) {
		imp.setTitle(ds.getName());
		CalibrationUtils.copyCalibrationToImagePlus( ds.getImgPlus(), imp );
	}
}
