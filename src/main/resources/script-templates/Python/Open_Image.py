# Open the image given its path and store it in the variable 'image'
from ij import IJ
image = IJ.openImage('/path/to/image.tiff')
# then display it.
image.show()
 
