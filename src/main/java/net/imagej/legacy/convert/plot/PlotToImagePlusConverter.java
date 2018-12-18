package net.imagej.legacy.convert.plot;

import ij.ImagePlus;
import org.scijava.plot.Plot;
import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Converter plugin, that converts an {@link Plot} to {@link ImagePlus}.
 *
 * @author Matthias Arzt
 * @see ConvertService
 */
@Plugin(type = Converter.class, priority = Priority.NORMAL_PRIORITY)
public class PlotToImagePlusConverter extends AbstractConverter<Plot, ImagePlus> {

	@Parameter
	ConvertService convertService;

	@Override
	public boolean canConvert(ConversionRequest request) {
		return request.destClass().isAssignableFrom( ImagePlus.class ) &&
				Plot.class.isAssignableFrom( request.sourceClass() ) &&
				convertService.supports(new ConversionRequest(
						request.sourceObject(), request.sourceType(), BufferedImage.class));
	}

	@Override
	public <T> T convert(Object o, Class<T> aClass) {
		if(o instanceof Plot && ImagePlus.class.equals(aClass)) {
			@SuppressWarnings("unchecked")
			T t = (T) toImagePlus((Plot) o);
			return t;
		}
		return null;
	}

	private ImagePlus toImagePlus(Plot plot) {
		Image awtImage = convertService.convert(plot, BufferedImage.class);
		//chart.draw(image.createGraphics(), new Rectangle2D.Float(0, 0, imp.getWidth(), imp.getHeight()));
		//ImagePlus imp = IJ.createImage(plot.getTitle(), "RGB", plot.getPreferredWidth(), plot.getPreferredHeight(), 1);
		//BufferedImage image = imp.getBufferedImage();
		//imp.setImage(image);
		return new ImagePlus( plot.getTitle(), awtImage );
	}

	@Override
	public Class<ImagePlus> getOutputType() {
		return ImagePlus.class;
	}

	@Override
	public Class<Plot> getInputType() {
		return Plot.class;
	}
}
