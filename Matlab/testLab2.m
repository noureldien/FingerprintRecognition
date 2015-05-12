
clc;
close all;

img = imread('./images/thumb.jpg');
img = imread('./images/18.jpg');
img = imread('./images/12.jpg');
%img = imread('./images/101_2.jpg');
%img = imread('./images/102_3.jpg');
%img = imread('./images/102_4.jpg');
%img = imread('./images/102_5.jpg');

imgGs = grayscaleImage(img);
imgAcc = imgGs;
imgAcc = histeq(imgAcc);
blksze = 36; thresh = 0.05;
gradientsigma = 1; blocksigma = 15; orientsmoothsigma = 13;
[newim, binim, mask] = testfin(imgGs, blksze, thresh, gradientsigma, blocksigma, orientsmoothsigma);

show(newim);
show(binim);

imgAcc = scaleImage(bwmorph(binim, 'thin', 'inf'), 0, 255);
show(imgAcc);

return;







