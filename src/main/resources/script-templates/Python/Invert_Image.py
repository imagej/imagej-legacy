# Invert the image under the current selection.
from ij import IJ
# Get current ImagePlus
image = IJ.getImage()
# Get current ImageProcessor (i.e. the current displayed slice)
ip = image.getProcessor()
# Invert what is under the ROI, or the whole image if there is none
ip.invert()
# Refresh display
image.updateAndDraw()

