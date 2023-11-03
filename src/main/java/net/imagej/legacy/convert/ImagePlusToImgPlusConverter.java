package net.imagej.legacy.convert;

import net.imagej.Dataset;
import net.imagej.ImgPlus;

import org.scijava.convert.AbstractDelegateConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;

@SuppressWarnings("rawtypes")
@Plugin(type = Converter.class)
public class ImagePlusToImgPlusConverter extends AbstractDelegateConverter<ImagePlus, Dataset, ImgPlus>{

	@Override
	public Class<ImgPlus> getOutputType() {
		return ImgPlus.class;
	}

	@Override
	public Class<ImagePlus> getInputType() {
		return ImagePlus.class;
	}

	@Override
	protected Class<Dataset> getDelegateType() {
		return Dataset.class;
	}

}
