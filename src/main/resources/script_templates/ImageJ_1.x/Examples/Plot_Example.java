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
import ij.gui.Plot;
import ij.gui.PlotWindow;

import ij.plugin.PlugIn;

import java.awt.Color;

/**
 * An example how to use ImageJ's Plot class
 */
public class Plot_Example implements PlugIn {
	/**
	 * This method gets called by ImageJ / Fiji.
	 *
	 * @param arg can be specified in plugins.config
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String arg) {
		// Some data to show
		double[] x = { 1, 3, 4, 5, 6, 7, 8, 9, 11 };
		double[] y = { 20, 5, -2, 3, 10, 12, 8, 3, 0 };
		double[] y2 = { 18, 10, 3, 1, 7, 11, 11, 5, 2 };
		double[] x3 = { 2, 10 };
		double[] y3 = { 13, 3 };

		// Initialize the plot with x/y
		Plot plot = new Plot("Example plot", "x", "y", x, y);

		// make some margin (xMin, xMax, yMin, yMax)
		plot.setLimits(0, 12, -3, 21);

		// Add x/y2 in blue; need to draw the previous data first
		plot.draw();
		plot.setColor(Color.BLUE);
		plot.addPoints(x, y2, Plot.LINE);

		// Add x3/y3 as circles instead of connected lines
		plot.draw();
		plot.setColor(Color.BLACK);
		plot.addPoints(x3, y3, Plot.CIRCLE);

		// Finally show it, but remember the window
		PlotWindow window = plot.show();

		// Wait 5 seconds
		try { Thread.sleep(5000); } catch (InterruptedException e) {}

		// Make a new plot and update the window
		plot = new Plot("Example plot2", "x", "y", x, y2);
		plot.setLimits(0, 12, -3, 21);
		plot.draw();
		plot.setColor(Color.GREEN);
		plot.addPoints(x, y, Plot.CROSS);
		plot.draw();
		plot.setColor(Color.RED);
		plot.addPoints(x3, y3, Plot.LINE);
		window.drawPlot(plot);
	}
}
