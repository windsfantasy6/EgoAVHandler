import java.io.*;
import java.util.ArrayList;

/**
 * Created by yuehw on 16/4/17.
 */
public class AVSummarizer {
    final int frameCnt = 300 * 15, width = 480, height = 270;
    int diff(int x, int y) {
        return Math.abs(x - y);
        //return (x - y) * (x - y);
    }

    int[] GenerateMotionVector(byte[] last, byte[] now) {
        int[] res = new int[2];
        int sumX = 0, sumY = 0;
        int range = 10;
        final int subX = 30, subY = 30;
        for (int i = 0; i < height; i += subY) {
            for (int j = 0; j < width; j += subX) {
                int optX = 0, optY = 0;
                long opt = 1l << 62;
                for (int di = -range; di <= range; di++) {
                    for (int dj = -range; dj <= range; dj++) {
                        int u = i + di, d = i + di + subY, l = j + dj, r = j + dj + subX;
                        if (l < 0 || r >= width || u < 0 || d >= height) {
                            continue;
                        }
                        long diffSum = 0;
                        for (int ii = u; ii < d; ii++) {
                            for (int jj = l; jj < r; jj++) {
                                byte lr = last[];
                                byte lg = last[];
                                byte lb = last[];
                                byte nr = now[];
                                byte ng = now[];
                                byte nb = now[];
                                diffSum += diff(lr, nr) + diff(lg, ng) + diff(lb, nb);
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public ArrayList<Integer> GenerateVideoCutPoints(String inFile) {
        ArrayList<Integer> res = new ArrayList<Integer>();
        ArrayList<int[]> motionVector = new ArrayList<int[]>();
        try {
            File inVideoFile = new File(inFile);
            InputStream is = new FileInputStream(inVideoFile);
            byte[] last = null;
            for (int frameIdx = 0; frameIdx < this.frameCnt; frameIdx++) {

                long len = width*height*3;
                byte[] now = new byte[(int)len];

                int offset = 0;
                int numRead = 0;
                while (offset < now.length && (numRead=is.read(now, offset, now.length-offset)) >= 0) {
                    offset += numRead;
                }

                int[] ret = GenerateMotionVector(last, now);
                motionVector.add(ret);
            }
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }
        return res;
    }
    public static void main(String args[]) {
        String inVideoPath = args[0];
        String inAudioPath = args[1];
        AVSummarizer summarizer = new AVSummarizer();
        ArrayList<Integer> videoCutPoints = summarizer.GenerateVideoCutPoints(inVideoPath);
        int totLength = 90 * 15; // ms
    }
}
