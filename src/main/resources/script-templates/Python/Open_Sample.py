# Open the sample named 'sample_name' and store it in the variable 'image'.
# 'sample_name' must refer to an image actually present on the ImageJ website;
# they are listed in the File > Open Samples menu.
# For instance:
from ij import IJ
sample_name = 'blobs.gif';
image = IJ.openImage('http://imagej.net/images/'+sample_name)
# then display it.
image.show()
