package net.imagej.legacy.convert;

import java.awt.Frame;
import java.util.Collection;

import org.scijava.Priority;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

@Plugin(type = Converter.class, priority = Priority.LOW)
public class StringToResultTableConverter extends
AbstractLegacyConverter<String, ResultsTable> {

	@Override
	public boolean canConvert(Object src, Class<?> dest) {
		return super.canConvert(src, dest) && convert(src, getOutputType()) != null;
	}

	@Override
	public <T> T convert(Object src, Class<T> dest) {
		String title = (String) src;
		@SuppressWarnings("unchecked")
		T rt = (T) ResultsTable.getResultsTable(title);
		return rt;
	}

	@Override
	public void populateInputCandidates(Collection<Object> objects) {
		Frame[] nonImageWindows = WindowManager.getNonImageWindows();
		for (Frame frame : nonImageWindows) {
			if (frame instanceof TextWindow) {
				ResultsTable rt = ((TextWindow) frame).getTextPanel().getResultsTable();
				if (rt != null) objects.add(rt);
			}
		}
	}

	@Override
	public Class<String> getInputType() {
		return String.class;
	}

	@Override
	public Class<ResultsTable> getOutputType() {
		return ResultsTable.class;
	}

}
