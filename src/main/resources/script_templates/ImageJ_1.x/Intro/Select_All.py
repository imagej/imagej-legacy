# Create a ROI around the whole image
from ij import IJ
from ij.gui import Roi
# Get current ImagePlus
image = IJ.getImage()
# Create ROI
roi = Roi(0, 0, image.getWidth(), image.getHeight())
# Assign it to the image and display it 
image.setRoi(roi, True)
