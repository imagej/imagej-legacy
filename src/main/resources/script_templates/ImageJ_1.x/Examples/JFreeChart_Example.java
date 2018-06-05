/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2018 Board of Regents of the University of
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

import ij.IJ;
import ij.ImagePlus;

import ij.plugin.PlugIn;

import java.awt.Font;

import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

import org.jfree.chart.plot.PiePlot;

import org.jfree.data.general.DefaultPieDataset;

/**
 * A basic example how to use JFreeChart in a Fiji plugin.
 */
public class JFreeChart_Example implements PlugIn {
	/**
	 * Paint a pie chart into an ImagePlus.
	 *
	 * @param arg can be specified in plugins.config
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String arg) {
		DefaultPieDataset dataset = new DefaultPieDataset();
		dataset.setValue("Apples", 63);
		dataset.setValue("Oranges", 36);

		JFreeChart chart = ChartFactory.createPieChart("Comparison", dataset, true, true, false);
		PiePlot pie = (PiePlot)chart.getPlot();
		pie.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
		pie.setLabelGap(0.05);
		pie.setCircular(true);

		ImagePlus imp = IJ.createImage("Comparison", "RGB", 512, 512, 1);
		BufferedImage image = imp.getBufferedImage();
		chart.draw(image.createGraphics(),
			new Rectangle2D.Float(0, 0, imp.getWidth(), imp.getHeight()));
		imp.setImage(image);
		imp.show();
	}
}
