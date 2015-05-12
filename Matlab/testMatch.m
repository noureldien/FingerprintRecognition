
clc;

img1 = imread('./images/sift_3.jpg');
img2 = imread('./images/sift_4.jpg');

img1 = imread('./images/sift_new_3.jpg');
img2 = imread('./images/sift_new_4.jpg');
%img2 = imread('./images/sift_new_5.jpg');
%img2 = imread('./images/sift_new_2.jpg');

img1 = grayscaleImage(img1);
img2 = grayscaleImage(img2);

[f1,d1] = vl_sift(im2single(img1));
[f2,d2] = vl_sift(im2single(img2));
[matches, scores] = vl_ubcmatch(d1,d2);
matchesOk = ransac(f1, f2, matches);

disp(size(matches,2));
disp(size(matchesOk,2));
matches = matchesOk;

figure(21); clf;
subplot(1,2,1);
axis equal;
axis off;
axis tight;
hold on;
imshow(img1);
h1 = vl_plotframe(f1);
h2 = vl_plotframe(f1);
set(h1,'color','k','linewidth',4);
set(h2,'color','y','linewidth',3);
subplot(1,2,2);
axis equal;
axis off;
axis tight;
hold on;
imshow(img2);
h1 = vl_plotframe(f2);
h2 = vl_plotframe(f2);
set(h1,'color','k','linewidth',4);
set(h2,'color','y','linewidth',3);
axis image off;

figure(22); clf;
imagesc(cat(2, img1, img2));
colormap(gray);
x1 = f1(1,matches(1,:));
x2 = f2(1,matches(2,:)) + size(img2,2);
y1 = f1(2,matches(1,:));
y2 = f2(2,matches(2,:));
hold on;
h = line([x1; x2], [y1; y2]);
set(h,'linewidth', 1, 'color', 'b');
vl_plotframe(f1(:,matches(1,:)));
f2(1,:) = f2(1,:) + size(img2,2);
vl_plotframe(f2(:,matches(2,:)));
axis image off;