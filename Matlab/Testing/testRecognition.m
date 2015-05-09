
clc;

img1 = imread('./images/102_3.jpg');
img2 = imread('./images/102_4.jpg');

imgs = {};
imgs{1} = img1;
imgs{2} = img2;

for i=1:2
    imgs{i} = grayscaleImage(imgs{i});
    imgs{i} = histeq(imgs{i});
    u = 3; v = 10; m = 20; d2 = 2;
    [~, ~, imgs{i}] = gaborFeatures(imgs{i},gaborFilterBank(u,v,m,m), d2, d2);
    imgs{i} = adaptiveThresh(imgs{i}, 4, 1, 'gaussian', 'relative');
    imgs{i} = scaleImage(bwmorph(imgs{i}, 'thin', 'inf'), 0, 255);
    [f{i}, d{i}] = vl_sift(im2single(imgs{i}));
end
[matches, scores] = vl_ubcmatch(d{1},d{2});
matches = ransac(f{1}, f{2}, matches);

figure(21); clf;
for i=1:2
    subplot(1,2,i);
    axis equal; axis off; axis tight; hold on;
    imshow(imgs{i});
    set(vl_plotframe(f{i}),'color','k','linewidth',4);
    set(vl_plotframe(f{i}),'color','y','linewidth',3);
end

figure(22); clf;
imagesc(cat(2, imgs{1}, imgs{2}));
colormap(gray);
x1 = f{1}(1,matches(1,:));
x2 = f{2}(1,matches(2,:)) + size(imgs{1},2);
y1 = f{1}(2,matches(1,:));
y2 = f{2}(2,matches(2,:));
hold on;
h = line([x1; x2], [y1; y2]);
set(h,'linewidth', 1, 'color', 'b');
vl_plotframe(f{1}(:,matches(1,:)));
f{2}(1,:) = f{2}(1,:) + size(imgs{1},2);
vl_plotframe(f{2}(:,matches(2,:)));
axis image off;







