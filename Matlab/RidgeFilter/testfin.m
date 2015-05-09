% TESTFIN
%
% Function to demonstrate use of fingerprint code
%
% Usage:  [newim, binim, mask, reliability] =  testfin(im);
%
% Argument:   im -  Fingerprint image to be enhanced.
%
% Returns:    newim - Ridge enhanced image.
%             binim - Binary version of enhanced image.
%             mask  - Ridge-like regions of the image
%             reliability - 'Reliability' of orientation data

% Peter Kovesi
% School of Computer Science & Software Engineering
% The University of Western Australia
% pk at csse uwa edu au
% http://www.csse.uwa.edu.au/~pk
%
% January 2005


function [newim, binim, mask] =  testfin(im, blksze, thresh, gradientsigma, blocksigma, orientsmoothsigma)

% identify ridge-like regions and normalise image
[normim, mask] = ridgesegment(im, blksze, thresh);
show(normim);

% determine ridge orientations
orientim = ridgeorient(normim, gradientsigma, blocksigma, orientsmoothsigma);
plotridgeorient(orientim, 20, im, 2);

% determine ridge frequency values across the image
blksze = 36;
[freq, medfreq] = ridgefreq(normim, mask, orientim, blksze, 5, 5, 15);

% the median frequency value used across the whole 
% fingerprint gives a more satisfactory result
freq = medfreq.*mask;

% apply filters to enhance the ridge pattern
newim = ridgefilter(normim, orientim, freq, 0.5, 0.5, 0);
%show(newim,4);

% binarise, ridge/valley threshold is 0
binim = newim > 0;
show(binim,5);

% Display binary image for where the mask values are one and where
% the orientation reliability is greater than 0.5
%show(binim.*mask.*(reliability>0.5), 7)

end





