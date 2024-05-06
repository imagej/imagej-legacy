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

package net.imagej.legacy.convert;

import ij.gui.Overlay;

import net.imagej.roi.ROITree;
import net.imglib2.roi.MaskPredicate;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.TreeNode;

/**
 * Converts a {@link ROITree} to an {@link Overlay}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ROITreeToOverlayConverter extends
	AbstractConverter<ROITree, Overlay>
{

	@Parameter
	private ConvertService convertService;

	@Override
	public Class<ROITree> getInputType() {
		return ROITree.class;
	}

	@Override
	public Class<Overlay> getOutputType() {
		return Overlay.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!getInputType().isInstance(src)) throw new IllegalArgumentException(
			"Unexpected source type: " + src.getClass());
		if (!getOutputType().isAssignableFrom(dest))
			throw new IllegalArgumentException("Unexpected output class: " + dest);

		final ROITree rois = (ROITree) src;
		final Overlay o = new Overlay();
		addROIs(rois, o);
		return (T) o;
	}

	private void addROIs(final TreeNode<?> rois, final Overlay overlay) {
		if (rois.data() instanceof MaskPredicate) {
			final ij.gui.Roi ijRoi = convertService.convert(rois.data(),
				ij.gui.Roi.class);
			overlay.add(ijRoi);
		}
		if (rois.children() == null || rois.children().isEmpty()) return;
		for (final TreeNode<?> roi : rois.children())
			addROIs(roi, overlay);
	}

}
