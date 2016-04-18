import java.io.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by yuehw on 16/4/17.
 */
public class AVSummarizer {
    final int frameCnt = 300 * 15, width = 480, height = 270;
    Map.Entry<Integer, Integer> GenerateMotionVector(byte[] last, byte[] now) {
        Map.Entry<Integer, Integer> res = new Map.Entry<>();
        return res;
    }

    public ArrayList<Integer> GenerateVideoCutPoints(String inFile) {
        ArrayList<Integer> res = new ArrayList<Integer>();
        ArrayList<Map.Entry<Integer, Integer>> motionVector = new ArrayList<Map.Entry<Integer, Integer>>();
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

                Map.Entry<Integer, Integer> ret = GenerateMotionVector(last, now);
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
