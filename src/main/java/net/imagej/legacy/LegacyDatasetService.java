package net.imagej.legacy;

import ij.ImagePlus;
import ij.WindowManager;

import java.io.IOException;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.service.ServiceIndex;

/**
 * HACK - this whole service exists as a hack to facilitate running commands
 * that require a list of datasets using {@link getDataset} without requiring
 * ImageJ2 synchronization to be enabled via {@code Edit>Options>ImageJ2}.
 * <p>
 * </p>
 * <p>
 * This service will be made obsolete by <a
 * href="https://github.com/imagej/imagej-legacy/issues/105">creating
 * lightweight wrappers</a>.
 * </p>
 * 
 * @author Mark Hiner, Brian Northan
 */
@Plugin(type = Service.class, priority = Priority.HIGH_PRIORITY)
public class LegacyDatasetService extends AbstractService implements
		DatasetService {

	/* Can't use injection as we want the next lowest priority service. */
	private DatasetService datasetService;

	@Override
	public boolean canOpen(String arg0) {

		return datasetService().canOpen(arg0);
	}

	@Override
	public boolean canSave(String arg0) {

		return datasetService().canSave(arg0);
	}

	@Override
	public <T extends RealType<T>> Dataset create(ImgPlus<T> arg0) {

		return datasetService().create(arg0);
	}

	@Override
	public <T extends RealType<T>> Dataset create(
			RandomAccessibleInterval<T> arg0) {

		return datasetService().create(arg0);
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> Dataset create(T arg0,
			long[] arg1, String arg2, AxisType[] arg3) {

		return datasetService().create(arg0, arg1, arg2, arg3);
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> Dataset create(T arg0,
			long[] arg1, String arg2, AxisType[] arg3, boolean arg4) {

		return datasetService().create(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public <T extends RealType<T>> Dataset create(ImgFactory<T> arg0, T arg1,
			long[] arg2, String arg3, AxisType[] arg4) {

		return datasetService().create(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public Dataset create(long[] arg0, String arg1, AxisType[] arg2, int arg3,
			boolean arg4, boolean arg5) {

		return datasetService().create(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	@Override
	public Dataset create(long[] arg0, String arg1, AxisType[] arg2, int arg3,
			boolean arg4, boolean arg5, boolean arg6) {

		return datasetService()
				.create(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	@Override
	public List<Dataset> getDatasets() {

		registerDatasets();

		return datasetService().getDatasets();
	}

	@Override
	public List<Dataset> getDatasets(ImageDisplay arg0) {

		registerDatasets();

		return datasetService().getDatasets(arg0);
	}

	@Override
	public ObjectService getObjectService() {

		return datasetService().getObjectService();
	}

	@Override
	public Dataset open(String arg0) throws IOException {

		return datasetService().open(arg0);
	}

	@Override
	public Dataset open(String arg0, Object arg1) throws IOException {

		return datasetService().open(arg0, arg1);
	}

	@Override
	public void revert(Dataset arg0) throws IOException {
		datasetService().revert(arg0);

	}

	@Override
	public Object save(Dataset arg0, String arg1) throws IOException {

		return datasetService().save(arg0, arg1);
	}

	@Override
	public Object save(Dataset arg0, String arg1, Object arg2)
			throws IOException {

		return datasetService().save(arg0, arg1, arg2);
	}

	// -- Helper methods --

	/**
	 * Register all ImagePlus instances
	 */
	private void registerDatasets() {
		// Register all ImagePlus instances. This will generate ensure no
		// ImagePluses are missed by the standard getImageDisplays.
		final int[] idList = WindowManager.getIDList();
		if (idList != null) {
			for (final int id : idList) {
				final ImagePlus imp = WindowManager.getImage(id);
				getImageMap().registerLegacyImage(imp);
			}
		}

	}

	/**
	 * Lazy initializer for the delegate {@link DatasetService}.
	 */
	private DatasetService datasetService() {
		if (datasetService == null) {
			synchronized (this) {
				if (datasetService == null) {
					final ServiceIndex index = getContext().getServiceIndex();

					// Set datasetService to the next highest priority service
					datasetService = index.getNextService(DatasetService.class,
							LegacyDatasetService.class);
				}
			}
		}
		return datasetService;
	}

	/**
	 * Helper method to access {@link LegacyImageMap} since we can't have a
	 * {@link LegacyService} parameter.
	 * 
	 * @return The {@link LegacyImageMap} for this {@link Context}.
	 */
	private LegacyImageMap getImageMap() {
		LegacyImageMap map = null;
		final LegacyService legacyService = getContext().getService(
				LegacyService.class);
		if (legacyService != null) {
			map = legacyService.getImageMap();
		}
		return map;
	}
}
