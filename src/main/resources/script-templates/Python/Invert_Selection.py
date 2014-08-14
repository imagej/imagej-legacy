# Inverse ROI: replaces the current ROI by its inverse.
from ij import IJ
from ij.gui import Roi, ShapeRoi
# Get current ImagePlus
image = IJ.getImage()
# Get current ROI
roi = image.getRoi()
if roi is not None:
  # Convert current roi to a ShapeRoi object
  shape_1 = ShapeRoi(roi)
  # Create a ShapeRoi that grabs the whole image
  shape_2 = ShapeRoi(Roi(0,0, image.getWidth(), image.getHeight()))
  # Compute inverse by XOR operation
  image.setRoi(shape_1.xor(shape_2))
