# @DatasetService data
# @DisplayService display
# @OpService ops
# @net.imagej.Dataset inputData

from net.imglib2.meta import ImgPlus
from net.imglib2.img.display.imagej import ImageJFunctions

from jarray import array
from ij import IJ

# create a log kernel
logKernel=ops.logKernel(inputData.numDimensions(), 1.0);

# convolve with log kernel
logFiltered=ops.convolve(inputData, logKernel);

# display log filter result
display.createDisplay("log", ImgPlus(logFiltered));

# otsu threshold and display
thresholded = ops.run("threshold.otsu",logFiltered)
display.createDisplay("thresholded", ImgPlus(thresholded));

# convert to imagej1 imageplus so we can run analyze particles
impThresholded=ImageJFunctions.wrap(thresholded, "wrapped")

# convert to mask and analyze particles
IJ.run(impThresholded, "Convert to Mask", "")
IJ.run(impThresholded, "Analyze Particles...", "display add");
IJ.run("Close");
