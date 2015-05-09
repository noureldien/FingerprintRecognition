clc;

im1=imread('page.png');
im2=imread('tshape.png');
%bwim1=adaptiveThreshold(im1,11,0.03,0);
%bwim2=adaptiveThreshold(im2,20,0.02,0);
bwim1 = adaptiveThresh(im1, 15, 15, 'gaussian', 'relative');
bwim2 = adaptiveThresh(im2, 8, 10, 'gaussian', 'relative');
figure(2);clf;
subplot(2,2,1);
imshow(im1);
subplot(2,2,2);
imshow(bwim1);
subplot(2,2,3);
imshow(im2);
subplot(2,2,4);
imshow(bwim2);