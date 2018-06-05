/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2018 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
// This macro originated after discussion on the ImageJ forum.
// See the original thread:
//    http://forum.imagej.net/t/how-can-i-obtain-a-table-relating-nucleus-number-and-area-of-each-cell/325
//
// This macro takes a 2-channel image as input.
// The purpose of this macro is to identify objects in each channel.
// The objects in one channel are assumed to contain one or more instances of objects
// in the second channel.
// After object identification (segmentation), this macro will count the number
// of smaller objects contained in each larger object.
//
// This is a non-destructive alternative to the ROI manager's "AND" operation
//
// Search for "TODO" comments for the parts where you need to take some action before running the macro
//
// To help determine the best parameters at the various "TODO" sections, it can be helpful to partially
// execute this macro - up to the TODO line in question. To do this, simply select the code you want to run,
// and choose "Run>Run selected code" from the script editor menu.
//
// For general information on image processing (selecting preprocessing techniques) see:
//  http://imagej.net/Image_Processing_Principles
//
// For general information on macros see:
//  http://imagej.net/Introduction_into_Macro_Programming


// *** Start of macro ***

// TODO
//Before running this macro:
//  - open your dataset (and no other images).
//  - ensure the ROI manager is closed/empty
//  - ensure the results table is closed/empty

// Step 1 - split image into individual channels

// remember the original title of the image
title = getTitle();
run("Split Channels");

//TODO
// the "big channel" contains the larger objects.
// the "small channel" contains the smaller objects
// reverse these numbers if needed for your data
bigChannel = 1;
smallChannel = 2;

//TODO
// These will be the names of our large and small objects in the
// ROI manager. The names are arbitrary, but it is helpful for us
// to remember what's what.
// Update as appropriate for your data
bigObject = "Cells";
smallObject = "Organelles";

// Here we get the title of each image so we can access them later
selectImage("C" + bigChannel + "-" + title);
bigImage = getImageID();
selectImage("C" + smallChannel + "-" + title);
smallImage = getImageID();

// Step 2 - identify the larger objects
selectImage(bigImage);

// Preprocessing - big channel

// TODO 
// The best preprocessing steps are really specific to your data.
// The goal of preprocessing is to eliminate any background and get a mask image that
// can be used later to analyze and identify our objects of interest.
// 
// You can find a threshold that best fits your data by using  "Image>Adjust>AutoThreshold" with "Try all"
//
// The following lines use the "Minimum" threshold, which is fairly aggressive. Then "Despeckles" to
// remove noise, and "Dilates" to increase the size of the particles.
//
// You are encouraged to replace these lines with the preprocessing steps that work for your data.
// You can record the commands for these steps by using "Plugins>Macros>Record..."
setAutoThreshold("Minimum dark");
run("Convert to Mask");
run("Despeckle");
run("Dilate");

// Analysis - big channel

//TODO
// Before analyzing our data we should select what measurements we want to take.
// Run "Analyze>Set Measurements..." and select the features you're interested in.

// TODO
// If your preprocessing is 100% successful in eliminating background noise then you can
// run Analyze Particles without any extra configuration.
// If needed, Analyze Particles has settings to limit the particles it selects based on size,
// roundness, and other options.
//
// Update this call to Analyze Particles as needed.
run("Analyze Particles...", "  show=Outlines display exclude summarize add");

// Analyze Particles creates ROIs around all the objects it identifies and adds them to the ROI manager.
// Since nothing else is in our ROI manager yet, then the total ROI count is the number of larger objects
// we identified.
bigObjectCount = roiManager("count");

// ROI manipulation - big channel

// Any time we want to do some processing of ROIs, we should hide the active image to avoid
// unnecessary overhead
setBatchMode("hide");

// Since we will have ROIs for both our larger and smaller objects of interest
// it is a good idea to rename the ROI itself to remember what object it came from.
//
// The other thing Analyze Particles does is take a measurement of the ROI.
// These measurements show up in the Results Table, which is what we will
// use to record additional information about our objects.
for(i=0;i<bigObjectCount;i++){
	roiManager("select",i);
	cIndex = i+1;
	// this command renames the active ROI
	roiManager("Rename", bigObject + " "+ cIndex);
	// this command links the measurement row in the Results Table
	// to the index we're setting in the ROI manager. This makes it
	// easier to remember which measurements are tied to which ROIs.
	setResult(bigObject + " Index", i, cIndex);
}

// We're done with ROI manipulation or now so we can exit batch mode and show
// our image again.
setBatchMode("exit and display");

// Step 3 - identify the smaller objects

selectImage(smallImage);

// Preprocessing - small channel

// TODO
// Our preprocessing goals here are the same as with the big channel:
// remove the background so we can identify all our small objects with Analyze Particles.
// Find what works best for your data and put those commands here.
setAutoThreshold("Default dark");
run("Convert to Mask");
run("Dilate");
run("Despeckle");

// Analysis - small channel

// TODO
// See the large object analysis section for more information about customizing
// the call to Analyze Particles.
//
// In this example, we have increased the minimum size of our particles to distinguish
// them from background noise that was left over after preprocessing.
run("Analyze Particles...", "size=6-Infinity display exclude summarize add");

// - ROI manipulation - small channel

// hide the UI for this computation to avoid unnecessary overhead of ROI selection
setBatchMode("hide"); 

// Again we want to label our small object ROIs and provide them with
// a unique identifier in the results table.
//
// NOTE: since we have big object and small object ROIs in the same ROI
//            manager, and big objects come first, we want to start our loop
//            at the bigObjectCount.
//            The ROI manager is "0-indexed", which means that the last
//            big object ROI actually appears at index "bigObjectCount - 1"
//            However, it would be confusing to start the small object indices
//            at a large number.
smallObjectCount = 0;
for(i=bigObjectCount;i<roiManager("count");i++) {
	smallObjectCount++;

	roiManager("select", i);
	//Rename the ROI
	roiManager("Rename", smallObject +" "+ smallObjectCount);
	// Set the index in the results table
	setResult(smallObject + " Index",i, smallObjectCount);
}

// Step 4 - determine overlap

// We will be adding two new columns to the results table.
// For small objects, we want to record which larger object contained them
// For big objects, we want to record a count of how many smaller objects are contained by them.
// By storing the column names in variables here we can ensure we don't make mistakes when using them later!
pairedColumn = "Paired " + bigObject;
smallCountColumn = smallObject + " Count";

// Now that we have our large and small object ROIs, we can figure out
// which overlap with each other.
//
// We have to use "nested" loops here - we need to look at each big object,
//  then we need to look at each pixel in these big objects, and for each pixel
// we need to look at each small object to determine if they overlap.
// Because the bigger objects contain more pixels, we want to limit how many
// times we scan them - for a given pixel, it is much faster to determine if
// it's contained in a small ROI than a large ROI.
// So our "outer" loop will go through the big objects, and in our "inner" loop
// we will check for overlap with the small objects.
for (i=0; i<bigObjectCount; i++) {
	roiManager("select", i); // select ROI for the next big object

	// Depending on how many big and small objects you have, this step could take a while.
	// By printing status information we can keep the user informed about what's happening
	print("Checking " + bigObject + " number " + (i+1) + " of " + bigObjectCount);

	// This creates two new variables: xPoints and yPoints, which contain the
	// coordinates that make up our current big object.
	Roi.getCoordinates(xPoints, yPoints);

	// Now we need to check each pixel position in our big object
	for (j=0; j<xPoints.length; j++) {
		// X and Y are the actual coordinates of the current pixel we're looking at
		x = xPoints[j];
		y = yPoints[j];

		// Now we can look at each small object. If it contains the current pixel
		// we know it overlaps with the current big object!
		for(k=bigObjectCount;k<roiManager("count");k++) {
			// Note that i and k are also the indices for big and small objects, repsectively, in the results table.

			// Check if the small object already has a paired big object. If so, we don't care if it contains this pixel or not.
			result = getResult(pairedColumn, k);

			// Since the pairedColumn may not exist yet, we could get back a NaN result, or a 0 if it exists but
			// hasn't been set yet.
			if (isNaN(result) || result ==0) {
				// Whether NaN or 0, this small object doesn't have a paired big object. So we make it
				// active in the ROI manager.
				roiManager("select", k);

				// Check if the small object contains our current point
				if (Roi.contains(x, y)) {
					// If it does we found a match! So we record the pairing
					// Note that we have to add 1 to the big object index (i) because we start all our
					// object indices at 1 -  to differentiate them from the "0" default entry.
					setResult(pairedColumn, k, (i+1));

					// Now we want to increase the count in the paired big object's results table row
					count = getResult(smallCountColumn, i);

					// Check for NaN in case the column doesn't exist yet
					if (isNaN(count)) count = 0;

					// Increase the count
					count++;

					// Update the count to its new value.
					setResult(smallCountColumn, i, count);
				}
			}
		}
	}
}

// we're done with ROI manipulation now so we can exit batch mode
setBatchMode("exit and display");

// *** End of macro ***
