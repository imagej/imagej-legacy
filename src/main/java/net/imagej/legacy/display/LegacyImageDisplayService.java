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

package net.imagej.legacy.display;

import ij.ImagePlus;
import ij.WindowManager;

import java.util.List;

import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.Position;
import net.imagej.display.DataView;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.LegacyImageMap;
import net.imagej.legacy.LegacyService;

import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.display.DisplayService;
import org.scijava.event.EventService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.service.ServiceIndex;

/**
 * HACK - this whole service exists as a hack to facilitate running commands
 * that require a {@link Dataset} without requiring ImageJ2 synchronization to
 * be enabled via {@code Edit>Options>ImageJ2}.
 * <p>
 * NB: {@link DisplayService} and {@link ObjectService} will NOT be synchronized
 * with the state represented by this service. Use cases querying these services
 * require ImageJ2 synchronization to be enabled.
 * </p>
 * <p>
 * This service will be made obsolete by <a
 * href="https://github.com/imagej/imagej-legacy/issues/105">creating
 * lightweight wrappers</a>.
 * </p>
 *
 * @author Mark Hiner
 */
@Plugin(type = Service.class, priority = Priority.HIGH)
public class LegacyImageDisplayService extends AbstractService implements
	ImageDisplayService
{

	// -- Parameters --

	/* Can't use injection as we want the next lowest priority service. */
	private ImageDisplayService imageDisplayService;

	// -- ImageDispalyService Methods --

	@Override
	public EventService getEventService() {
		return imageDisplayService().getEventService();
	}

	@Override
	public PluginService getPluginService() {
		return imageDisplayService().getPluginService();
	}

	@Override
	public DisplayService getDisplayService() {
		return imageDisplayService().getDisplayService();
	}

	@Override
	public DataView createDataView(final Data data) {
		return imageDisplayService().createDataView(data);
	}

	@Override
	public List<? extends DataView> getDataViews() {
		return imageDisplayService().getDataViews();
	}

	@Override
	public ImageDisplay getActiveImageDisplay() {
		ImageDisplay imageDisplay = null;
		final ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			imageDisplay = getImageMap().registerLegacyImage(imp);
		}
		if (imageDisplay != null)
			return imageDisplay;
		// Try the delegate ImageDisplayService if the WindowManager fails
		return imageDisplayService().getActiveImageDisplay();
	}

	@Override
	public List<ImageDisplay> getImageDisplays() {
		// Register all ImagePlus instances. This will generate ensure no
		// ImagePluses are missed by the standard getImageDisplays.
		final int[] idList = WindowManager.getIDList();
		if (idList != null) {
			for (final int id : idList) {
				final ImagePlus imp = WindowManager.getImage(id);
				getImageMap().registerLegacyImage(imp);
			}
		}

		return imageDisplayService().getImageDisplays();
	}

	// -- Helper methods --

	/**
	 * Lazy initializer for the delegate {@link ImageDisplayService}.
	 */
	private ImageDisplayService imageDisplayService() {
		if (imageDisplayService == null) {
			synchronized (this) {
				if (imageDisplayService == null) {
					final ServiceIndex index = getContext().getServiceIndex();

					// Set datasetService to the next highest priority service
					imageDisplayService =
						index.getNextService(ImageDisplayService.class,
							LegacyImageDisplayService.class);
				}
			}
		}
		return imageDisplayService;
	}

	/**
	 * Helper method to access {@link LegacyImageMap} since we can't have a
	 * {@link LegacyService} parameter.
	 *
	 * @return The {@link LegacyImageMap} for this {@link Context}.
	 */
	private LegacyImageMap getImageMap() {
		LegacyImageMap map = null;
		final LegacyService legacyService =
			getContext().getService(LegacyService.class);
		if (legacyService != null) {
			map = legacyService.getImageMap();
		}
		return map;
	}
}
