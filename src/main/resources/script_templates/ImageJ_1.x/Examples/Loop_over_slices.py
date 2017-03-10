# Loop over all slices of a stack.
from ij import IJ
image = IJ.getImage()
stack = image.getStack() # get the stack within the ImagePlus
n_slices = stack.getSize() # get the number of slices
for index in range(1, n_slices+1):
  ip = stack.getProcessor(index)    
  IJ.log(ip.toString()) # output info on current slice
