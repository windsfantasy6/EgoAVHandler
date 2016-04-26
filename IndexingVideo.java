import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wuyunxin on 16/4/20.
 */
public class IndexingVideo {
    public String videoPath;
    public String imgPath;
    public IndexingVideo(String videoPath, String imgPath) {
        this.videoPath = videoPath;
        this.imgPath = imgPath;
    }
    public final int width = 480;
    public final int height = 270;
    public final int imgWidth = 1280;
    public final int imgHeight = 720;

    public int[] resizePixels(int[] pixels, int w1, int h1, int w2, int h2) {
        int[] temp = new int[w2 * h2 * 3];
        // EDIT: added +1 to account for an early rounding problem
        int x_ratio = (int)((w1 << 16) / w2) + 1;
        int y_ratio = (int)((h1 << 16) / h2) + 1;

        int x2, y2 ;
        for (int i = 0; i < h2; i++) {
            for (int j = 0; j < w2; j++) {
                x2 = ((j * x_ratio) >> 16);
                y2 = ((i * y_ratio) >> 16);
                temp[(i * w2) + j] = pixels[(y2 * w1) + x2];
                temp[(i * w2) + j + h2 * w2] = pixels[(y2 * w1) + x2 + h1 * w1];
                temp[(i * w2) + j + 2 * h2 * w2] = pixels[(y2 * w1) + x2 + 2 * h1 * w1];
            }
        }
        return temp;
    }

    int Byte2Int(byte b) {
        if (b < 0) {
            return (int)b + 256;
        }
        return (int)b;
    }

    public int findByIndex() {
        int selectedFrame = -1;
        File file = new File(imgPath);
        File vFile = new File(videoPath);


        try {
            //BufferedImage image = ImageIO.read(file);
            //System.out.println(image.getHeight() + " " + image.getWidth());
            InputStream is = new FileInputStream(file);
            byte[] bytes = new byte[imgHeight * imgWidth * 3];
            int offset = 0, numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            int[] img = new int[imgHeight * imgWidth * 3];
            //int[] img = getPixel(image, imgHeight, imgWidth);
            for (int i = 0; i < img.length; i++) {
                img[i] = Byte2Int(bytes[i]);
            }

            is = new FileInputStream(vFile);
            byte[] video = new byte[480 * 270 * 3];
            /*
            for (int frame = 0; frame < 4500; frame++) {

            }
            */
            long opt = 1l << 31;


            int val4 = width * height;
            for (int newWidth = width; newWidth >= width * 5 / 6; newWidth--) {
                if ((newWidth * height) % width != 0) continue;
                int newHeight = newWidth * height / width;
                int val2 = newWidth * newHeight;
                System.out.println("newWidth, newHeight = " + newWidth + ", " + newHeight);
                int[] newImg = resizePixels(img, imgWidth, imgHeight, newWidth, newHeight);
                long imgSum = 0;
                for (int i : newImg) {
                    imgSum += i;
                }
                long ratio = newWidth * newHeight;
                int lb = 0, ub = 4500;
                if (selectedFrame != -1) {
                    lb = Math.max(lb, selectedFrame - 300);
                    ub = Math.min(ub, selectedFrame + 300);
                }


                for (int frame = 0; frame < 4500; frame++) {
                    offset = 0;
                    numRead = 0;
                    while (offset < video.length && (numRead = is.read(video, offset, video.length - offset)) >= 0) {
                        offset += numRead;
                    }
                    if (frame < lb || frame >= ub) {
                        continue;
                    }
                    if (frame % 50 == 0) {
                        System.out.println("Frame = " + frame);
                    }
                    long[][] sum = new long[height + 1][width + 1];
                    for (int y = 0; y < height; y++) {
                        int val5 = y * width;
                        for (int x = 0; x < width; x++) {
                            sum[y + 1][x + 1] = sum[y + 1][x] + sum[y][x + 1] - sum[y][x]
                                    + Byte2Int(video[val5 + x])
                                    + Byte2Int(video[val5 + x + val4])
                                    + Byte2Int(video[val5 + x + (val4 << 1)]);
                        }
                    }
                    for (int yy = 0; yy <= height - newHeight; yy++) {
                        for (int xx = 0; xx <= width - newWidth; xx++) {
                            long videoSum = sum[yy + newHeight][xx + newWidth] - sum[yy][xx + newWidth] - sum[yy + newHeight][xx] + sum[yy][xx];
                            long val = Math.abs(videoSum - imgSum);
                            //System.out.println("vid, img, val = " + videoSum + ", " + imgSum + ", " + val);
                            if (val >= 10 * val2) {
                                continue;
                            }
                            //detailed match
                            //System.out.println("Detailed Search! Frame = " + frame);
                            long diff = 0;
                            for (int dy = 0; dy < newHeight; dy++) {
                                for (int dx = 0; dx < newWidth; dx++) {
                                    int val1 = dy * newWidth;
                                    int val3 = (yy + dy) * width;
                                    diff += Math.abs(newImg[val1 + dx] - Byte2Int(video[val3 + xx + dx]));
                                    diff += Math.abs(newImg[val1 + dx + val2] - Byte2Int(video[val3 + xx + dx + val4]));
                                    diff += Math.abs(newImg[val1 + dx + (val2 << 1)] - Byte2Int(video[val3 + xx + dx + (val4 << 1)]));
                                    if (diff >= opt * ratio) {
                                        break;
                                    }
                                }
                                if (diff >= opt * ratio) {
                                    break;
                                }
                            }
                            if (diff >= opt * ratio) {
                                continue;
                            }
                            diff /= ratio;
                            opt = diff;
                            selectedFrame = frame;
                            System.out.println("Opt, frame = " + opt + ", " + frame);
                        }
                    }
                }
                System.out.println("SelectedFrame = " + selectedFrame);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("SelectedFrame = " + selectedFrame);
        return selectedFrame;
    }

/*
        public static void main(String[] args) {
            IndexingVideo video = new IndexingVideo(args[1], args[0]);
            int f = video.findByIndex();
        }
        */
}
