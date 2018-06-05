###
# #%L
# ImageJ software for multidimensional image processing and analysis.
# %%
# Copyright (C) 2009 - 2018 Board of Regents of the University of
# Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
# Institute of Molecular Cell Biology and Genetics.
# %%
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 
# 1. Redistributions of source code must retain the above copyright notice,
#    this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions and the following disclaimer in the documentation
#    and/or other materials provided with the distribution.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# #L%
###
# @DatasetService data
# @DisplayService display
# @OpService ops
# @net.imagej.Dataset inputData

from net.imglib2.meta import ImgPlus
from net.imglib2.img.display.imagej import ImageJFunctions

from jarray import array
from ij import IJ

# create a log kernel
logKernel=ops.create().kernelLog(inputData.numDimensions(), 1.0);

# convolve with log kernel
logFiltered=ops.filter().convolve(inputData, logKernel);

# display log filter result
display.createDisplay("log", ImgPlus(logFiltered));

# otsu threshold and display
thresholded = ops.threshold().otsu(logFiltered)
display.createDisplay("thresholded", ImgPlus(thresholded));

# convert to imagej1 imageplus so we can run analyze particles
impThresholded=ImageJFunctions.wrap(thresholded, "wrapped")

# convert to mask and analyze particles
IJ.run(impThresholded, "Convert to Mask", "")
IJ.run(impThresholded, "Analyze Particles...", "display add");
IJ.run("Close");
