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
# Straighten: like the command in Edit > Selection menu, 
# this snippet creates a new image by taking some pixels along a
# line ROI. It is typically used to make a straight image from
# a bent selection.
from ij import IJ, ImagePlus
width = 20 # how many pixels should we fetch from around the ROI?
# Get current ImagePlus
image = IJ.getImage()
if image is not None:
  roi = image.getRoi()
  if roi is not None and roi.isLine(): # we can only do it for line ROIs
    # Instantiate plugin
    straightener = Straightener()
    # Are we dealing with a stack?
    stack_size = image.getStackSize()
    if stack_size > 1:
      new_stack = straightener.straightenStack(image, roi, width)
      new_image = ImagePlus( image.getTitle()+"-straightened", new_stack)
    else:
      new_ip = straightener.straighten(image, roi, width)
      new_image = ImagePlus( image.getTitle()+"-straightened", new_ip)
    # Display result
    new_image.show()

