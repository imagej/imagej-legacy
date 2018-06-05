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
# Set current image properties. WARNING!! There is no check that what you enter is correct
from ij import IJ
from ij.measure import Calibration
# Set dimensions
n_channels  = 1
n_slices  = 1    # Z slices
n_frames  = 1    # time frames
# Get current image
image = IJ.getImage()
# Check that we have correct dimensions
stack_size = image.getImageStackSize() # raw number of images in the stack
if n_channels * n_slices * n_frames == stack_size:
  image.setDimensions(n_channels, n_slices, n_frames)
else:
  IJ.log('The product of channels ('+str(n_channels)+'), slices ('+str(n_slices)+')')
  IJ.log('and frames ('+str(n_frames)+') must equal the stack size ('+str(stack_size)+').')
# Set calibration
pixel_width   = 1
pixel_height  = 1
pixel_depth   = 1
space_unit    = 'µm'
frame_interval  = 1
time_unit     = 's'
calibration = Calibration() # new empty calibration
calibration.pixelWidth    = pixel_width
calibration.pixelHeight   = pixel_height
calibration.pixelDepth    = pixel_depth
calibration.frameInterval   = frame_interval
calibration.setUnit(space_unit)
calibration.setTimeUnit(time_unit)
image.setCalibration(calibration)
image.repaintWindow()

