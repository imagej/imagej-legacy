# Remove any ROI in currently selected image
from ij import IJ
# Get current ImagePlus
image = IJ.getImage()
# Remove ROI from it
image.killRoi()
