
clc;
close all;

img = imread('./images/thumb.jpg');
img = imread('./images/18.jpg');
img = imread('./images/12.jpg');

imgGs = grayscaleImage(img);
imgAcc = imgGs;
imgAcc = histeq(imgAcc);
blksze = 16; thresh = 0.05;
gradientsigma = 1; blocksigma = 5; orientsmoothsigma = 5;
[newim, binim, mask] = testfin(imgGs, blksze, thresh, gradientsigma, blocksigma, orientsmoothsigma);

imgAcc = scaleImage(bwmorph(binim, 'thin', 'inf'), 0, 255);
show(imgAcc);

return;







