/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
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
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;

import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.display.ColorTables;
import net.imagej.display.DataView;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable8;
import net.imglib2.type.numeric.RealType;

/**
 * This class synchronizes {@link ImageDisplay} {@link ColorTable}s with
 * {@link ImagePlus} {@link LUT}s.
 * 
 * @author Barry DeZonia
 */
public class ColorTableHarmonizer implements DisplayHarmonizer {

	private final ImageDisplayService imgDispSrv;

	public ColorTableHarmonizer(ImageDisplayService imgDispSrv) {
		this.imgDispSrv = imgDispSrv;
	}
	
	/**
	 * Sets the ColorTables of the active view of an modern ImageJ ImageDisplay
	 * from the LUTs of a given ImagePlus or CompositeImage.
	 */
	@Override
	public void updateDisplay(final ImageDisplay disp, final ImagePlus imp) {
		final boolean sixteenBitLUTs = imp.getType() == ImagePlus.GRAY16;
		final List<ColorTable> colorTables = colorTablesFromImagePlus(imp);
		assignColorTables(disp, colorTables, sixteenBitLUTs);
		assignChannelMinMax(disp, imp);

		/* BDZ REMOVING on Dec-8-2011. Does not seem to be needed anymore. Resurrect
		 * later if needed again.
		// Force current plane to redraw : HACK to fix bug #668
		final DataView dispView = disp.getActiveView();
		if (dispView == null) return;
		final DatasetView dsView = (DatasetView) dispView;
		dsView.getProjector().map();
		disp.update();
		*/
	}

	/**
	 * Sets LUTs of an ImagePlus or CompositeImage. If given an ImagePlus this
	 * method sets it's single LUT from the first ColorTable of the active view
	 * that displays the active Dataset of the given ImageDisplay. If given a
	 * CompositeImage this method sets all it's LUTs from the ColorTables of the
	 * active view of the given ImageDisplay. In both cases if there is no active
	 * view for the ImageDisplay the LUTs are assigned with sensible default
	 * values.
	 */
	@Override
	public void updateLegacyImage(final ImageDisplay disp, final ImagePlus imp) {
		final DatasetView activeView = (DatasetView) disp.getActiveView();
		if (imp instanceof CompositeImage) {
			final CompositeImage ci = (CompositeImage) imp;
			final List<ColorTable> colorTables =
				(activeView == null) ? null : activeView.getColorTables();
			setCompositeImageLUTs(ci, colorTables);
			final int composChannCount =
				(activeView == null) ? 1 : activeView.getData()
					.getCompositeChannelCount();
			setCompositeImageMode(ci, composChannCount, colorTables);
		}
		else if ( ! (imp.getProcessor() instanceof ColorProcessor) ) {
			// regular noncolor ImagePlus
			// NOTE to fix bug #849 the nonnull case was added below. This reflects
			// a significant behavior change.
			if (activeView == null) {
				final Dataset ds = imgDispSrv.getActiveDataset(disp);
				setImagePlusLUTToFirstInDataset(ds, imp);
			}
			else setImagePlusLUTToFirstInView(activeView, imp);
		}
		assignImagePlusMinMax(disp, imp);
	}

	// -- private interface --

	/**
	 * For each channel in CompositeImage, sets LUT to one from default
	 * progression
	 */
	private void setCompositeImageLUTsToDefault(final CompositeImage ci) {
		for (int i = 0; i < ci.getNChannels(); i++) {
			final ColorTable cTable = ColorTables.getDefaultColorTable(i);
			final LUT lut = make8BitLUT(cTable);
			ci.setChannelLut(lut, i + 1);
		}
	}

	/**
	 * For each channel in CompositeImage, sets LUT to one from given ColorTables
	 */
	private void setCompositeImageLUTs(final CompositeImage ci,
		final List<ColorTable> cTables)
	{
		if (cTables == null || cTables.size() == 0) {
			setCompositeImageLUTsToDefault(ci);
		}
		else {
			for (int i = 0; i < ci.getNChannels(); i++) {
				final ColorTable cTable = cTables.get(i);
				final LUT lut = make8BitLUT(cTable);
				ci.setChannelLut(lut, i + 1);
			}
		}
	}

	/**
	 * Sets the correct legacy ImageJ CompositeImage display mode based upon input
	 * data values.
	 */
	private void setCompositeImageMode(final CompositeImage ci,
		final int composCount, final List<ColorTable> cTables)
	{
		if ((composCount > 1) || (cTables == null) || (cTables.size() == 0)) ci
			.setMode(CompositeImage.COMPOSITE);
		else {
			boolean allGrayTables = true;
			for (int i = 0; i < ci.getNChannels(); i++) {
				final ColorTable cTable = cTables.get(i);
				if ((allGrayTables) && (!ColorTables.isGrayColorTable(cTable))) {
					allGrayTables = false;
				}
			}
			if (allGrayTables) {
				ci.setMode(CompositeImage.GRAYSCALE);
			}
			else {
				ci.setMode(CompositeImage.COLOR);
			}
		}
	}

	/** Sets the single LUT of an ImagePlus to the first ColorTable of a Dataset */
	private void setImagePlusLUTToFirstInDataset(final Dataset ds,
		final ImagePlus imp)
	{
		ColorTable cTable = ds.getColorTable(0);
		if (cTable == null) cTable = ColorTables.GRAYS;
		final LUT lut = make8BitLUT(cTable);
		imp.getProcessor().setColorModel(lut);
		// or imp.getStack().setColorModel(lut);
	}

	/** Sets the single LUT of an ImagePlus to the first ColorTable of a Dataset */
	private void setImagePlusLUTToFirstInView(final DatasetView view,
		final ImagePlus imp)
	{
		ColorTable cTable = view.getColorTables().get(0);
		if (cTable == null) cTable = ColorTables.GRAYS;
		final LUT lut = make8BitLUT(cTable);
		imp.getProcessor().setColorModel(lut);
		// or imp.getStack().setColorModel(lut);
	}

	/**
	 * Assigns the given ImagePlus's per-channel min/max values to the active view
	 * of the specified ImageDisplay.
	 */
	private void assignImagePlusMinMax(final ImageDisplay disp,
		final ImagePlus imp)
	{
		final DataView dataView = disp.getActiveView();
		if (!(dataView instanceof DatasetView)) return;
		final DatasetView view = (DatasetView) dataView;
		final int channelCount = view.getChannelCount();
		final double[] min = new double[channelCount];
		final double[] max = new double[channelCount];
		double overallMin = Double.POSITIVE_INFINITY;
		double overallMax = Double.NEGATIVE_INFINITY;
		for (int c = 0; c < channelCount; c++) {
			double lo = view.getChannelMin(c);
			double hi = view.getChannelMax(c);
			// ShortProcessor backed data cannot handle negative display range
			if ((imp.getBitDepth() == 16) && (imp.getCalibration().isSigned16Bit())) {
				lo += 32768;
				hi += 32768;
			}
			min[c] = lo;
			max[c] = hi;
			overallMin = Math.min(overallMin,lo);
			overallMax = Math.max(overallMax,hi);
		}

		if (imp instanceof CompositeImage) {
			// set each channel's display range
			final CompositeImage ci = (CompositeImage) imp;
			if (channelCount != ci.getNChannels()) {
				throw new IllegalArgumentException("Channel mismatch: " +
					channelCount + " vs. " + ci.getNChannels());
			}
			// NB
			//   Originally I used ci.getLUTs() and modified each LUT's min and max.
			//   This cannot work as getLUTs() returns copies rather than originals.
			//   Unfortunately setLUTs() does not use min/max of passed in LUTs. So
			//   can't tweak and set back. So we'll cycle through the channels setting
			//   min/max and make sure channel set where it started when we're done.
			int origC = ci.getC();
			for (int i = 0; i < channelCount; i++) {
				ci.setC(i+1);
				ci.setDisplayRange(min[i], max[i]);
			}
			ci.setC(origC);
		}
		else { // regular ImagePlus
			
			// NB - for color data imp.setDisplayRange() will reset pixel data from
			// the snapshot buffer!! So manipulate snapshot buffer to avoid this.
			// BDZ reported the behavior of ColorProcessor::setDisplayRange() to Wayne
			// as a possible bug on 7-27-12. If he removes reset() call from that
			// method then the manipulation of snapshot pixels can be removed.

			// save info
			ImageProcessor proc = imp.getProcessor();
			Object snapshot = proc.getSnapshotPixels();
			proc.setSnapshotPixels(null);
			
			// Actually set the display range here
			imp.setDisplayRange(overallMin, overallMax);
			
			// restore info
			proc.setSnapshotPixels(snapshot);
		}
	}

	/**
	 * Makes a ColorTable8 from an IndexColorModel. Note that legacy ImageJ LUT's
	 * are a kind of IndexColorModel.
	 */
	private ColorTable8 make8BitColorTable(final IndexColorModel icm) {
		final byte[] reds = new byte[256];
		final byte[] greens = new byte[256];
		final byte[] blues = new byte[256];
		icm.getReds(reds);
		icm.getGreens(greens);
		icm.getBlues(blues);
		return new ColorTable8(reds, greens, blues);
	}

	/**
	 * Makes an 8-bit LUT from a ColorTable.
	 * <p>
	 * ColorTable16's are merely down-sampled.  If this is a non-false-color
	 * image (i.e. the data is in the palette entries) data will be lost.  For
	 * false-color images (data is in the indices, palette is just for 
	 * visualization) the default palette is typically a ramp so the ColorTable8
	 * version is functionally equivalent.
	 */
	private LUT make8BitLUT(final ColorTable cTable) {
		final byte[] reds = new byte[256];
		final byte[] greens = new byte[256];
		final byte[] blues = new byte[256];

		for (int i = 0; i < 256; i++) {
			reds  [i] = (byte) cTable.getResampled(ColorTable.RED,   256, i);
			greens[i] = (byte) cTable.getResampled(ColorTable.GREEN, 256, i);
			blues [i] = (byte) cTable.getResampled(ColorTable.BLUE,  256, i);
		}
		return new LUT(reds, greens, blues);
	}

	/** Assigns the color tables of the active view of a ImageDisplay. */
	private void assignColorTables(final ImageDisplay disp,
		final List<ColorTable> colorTables,
		final boolean sixteenBitLUTs)
	{
		// FIXME HACK
		// Grab the active view of the given ImageDisplay and set it's default
		// channel
		// luts. When we allow multiple views of a Dataset this will break. We
		// avoid setting a Dataset's per plane LUTs because it would be expensive
		// and also IJ1 LUTs are not model space constructs but rather view space
		// constructs.
		final DataView dispView = disp.getActiveView();
		if (dispView == null) return;
		final DatasetView dsView = (DatasetView) dispView;

		// TODO - removing this old code allows color tables to be applied to
		// gray images. Does this break anything? Note that avoiding this code
		// fixes #550, #765, #768, and #774.
		// final ColorMode currMode = dsView.getColorMode();
		// if (currMode == ColorMode.GRAYSCALE) return;

		// either we're given one color table for whole dataset
		if (colorTables.size() == 1) {
			final ColorTable newTable = colorTables.get(0);
			final List<ColorTable> existingColorTables = dsView.getColorTables();
			for (int i = 0; i < existingColorTables.size(); i++)
				dsView.setColorTable(newTable, i);
		}
		else { // or we're given one per channel
			/* debugging hacks
			// FIXME - temp debugging hack - this should be removed if it somehow gets
			// into master code. It avoids a race condition between two threads
			// manipulating the color tables
			//try { Thread.sleep(3000); } catch (Exception e) {}
			// FIXME - more debugging hacks - resize color tables to fit
			//int numColorTablesInView = dsView.getColorTables().size();
			//if (colorTables.size() > numColorTablesInView)
			//	dsView.resetColorTables(false); // TODO - when to use "true"?
			*/
			for (int i = 0; i < colorTables.size(); i++)
				dsView.setColorTable(colorTables.get(i), i);
		}
		// TODO : note Dec 20, 2011 BDZ  See bug #915
		// we should tell the dsView that it needs a redraw (projector.map()) to be
		// done soon. We don't mess with a Dataset here. So a DatasetUpdatedEvent is
		// not generated. And thus no redraw happens. Because of this the last LUT
		// is not applied. For Blobs this means it does not display with a white
		// background. We need to notify the view. Soon the display event updates
		// will be modified and when that happens an update call of some kind needs
		// to go here. Note that a clearly marked blobs workaround is located in
		// Harmonizer that should go away when this issue here is resolved.
	}

	/**
	 * Assigns the per-channel min/max values of active view of given ImageDisplay
	 * to the specified ImagePlus/CompositeImage range(s).
	 */
	private void
		assignChannelMinMax(final ImageDisplay disp, final ImagePlus imp)
	{
		final DataView dataView = disp.getActiveView();
		if (!(dataView instanceof DatasetView)) return;
		final DatasetView view = (DatasetView) dataView;
		final int channelCount = view.getChannelCount();
		final double[] min = new double[channelCount];
		final double[] max = new double[channelCount];

		if (imp instanceof CompositeImage) {
			final CompositeImage ci = (CompositeImage) imp;
			for (int c = 0; c < channelCount; c++) {
				// some image modes return null for this call
				final ImageProcessor ip = ci.getProcessor(c + 1);
				if (ip != null) {
					// use the data min max when possible
					min[c] = ip.getMin();
					max[c] = ip.getMax();
				}
				else { // ip == null
					// use best estimate we last had
					LUT[] luts = ci.getLuts();
					min[c] = luts[c].min;
					max[c] = luts[c].max;
				}
			}
		}
		else {
			double mn = imp.getDisplayRangeMin();
			double mx = imp.getDisplayRangeMax();
			if ((imp.getBitDepth() == 16) && (imp.getCalibration().isSigned16Bit())) {
				mn -= 32768.0;
				mx -= 32768.0;
			}
			for (int c = 0; c < channelCount; c++) {
				min[c] = mn;
				max[c] = mx;
			}
		}

		Dataset dataset = imgDispSrv.getActiveDataset(disp);
		RealType<?> type = dataset.getType();
		for (int c = 0; c < channelCount; c++) {
			double mn = outOfBounds(min[c], type) ? type.getMinValue() : min[c];
			double mx = outOfBounds(max[c], type) ? type.getMaxValue() : max[c];
			if (mn > mx) {
				throw new IllegalArgumentException("Bad display range setting");
			}
			view.setChannelRange(c, mn, mx);
		}
	}

	private boolean outOfBounds(double val, RealType<?> type) {
		if (val < type.getMinValue()) return true;
		if (val > type.getMaxValue()) return true;
		return false;
	}

	/** Creates a list of ColorTables from an ImagePlus. */
	private List<ColorTable> colorTablesFromImagePlus(final ImagePlus imp) {
		final List<ColorTable> colorTables = new ArrayList<>();
		final LUT[] luts = imp.getLuts();
		if (luts == null) { // not a CompositeImage
			if (imp.getType() == ImagePlus.COLOR_RGB) {
				for (int i = 0; i < imp.getNChannels() * 3; i++) {
					final ColorTable cTable = ColorTables.getDefaultColorTable(i);
					colorTables.add(cTable);
				}
			}
			else { // not a direct color model image
				final IndexColorModel icm =
					(IndexColorModel) imp.getProcessor().getColorModel();
				ColorTable cTable;
//				if (icm.getPixelSize() == 16) // is 16 bit table
//					cTable = make16BitColorTable(icm);
//				else // 8 bit color table
				cTable = make8BitColorTable(icm);
				colorTables.add(cTable);
			}
		}
		else { // we have multiple LUTs from a CompositeImage, 1 per channel
			ColorTable cTable;
			for (int i = 0; i < luts.length; i++) {
				//TODO ARG what about 16-bit LUTs also?
//				if (luts[i].getPixelSize() == 16) // is 16 bit table
//					cTable = make16BitColorTable(luts[i]);
//				else // 8 bit color table
				cTable = make8BitColorTable(luts[i]);
				colorTables.add(cTable);
			}
		}

		return colorTables;
	}

}
