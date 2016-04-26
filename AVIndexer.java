import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by yuehw on 16/4/25.
 */



public class AVIndexer {
    public static void main(String[] args) {
        String inVideoPath = args[0];
        String inAudioPath = args[1];
        String inImagePath = args[2];
        String outVideoSumPath = args[3];
        String outAudioSumPath = args[4];

        String outVideoIndexPath = args[5];
        String outAudioIndexPath = args[6];
        IndexingVideo indVideo = new IndexingVideo(inVideoPath, inImagePath);
        int ret = indVideo.findByIndex();

        AVSummarizer summarizer = new AVSummarizer();
        ArrayList<Integer> videoCutPoints = summarizer.GenerateVideoCutPoints(inVideoPath);
        ArrayList<Integer> audioCutPoints = summarizer.GenerateAudioCutPoints(inAudioPath);
        ArrayList<Integer> cutPoints = summarizer.MergeCutPoints(videoCutPoints, audioCutPoints, 9, summarizer.frameCnt);
        //ArrayList<Integer> cutPoints = videoCutPoints;
        for (Integer i : cutPoints) {
            System.out.println("Cut Point == " + i);
        }
        int totLength = 90 * 15; // 90s * 15frames/s
        Map<Integer, Integer> timePeriods = new TreeMap<Integer, Integer>();
        int head = 0;
        for (int i = 0; i < cutPoints.size(); i++) {
            int tail = cutPoints.get(i);
            int len = tail - head;
            int subFrameCnt = len * totLength / summarizer.frameCnt;
            timePeriods.put(head, head + subFrameCnt);
            head = tail;
        }
        MyAVGenerator gen = new MyAVGenerator(inAudioPath, inVideoPath, outAudioSumPath, outVideoSumPath, timePeriods, summarizer.frameCnt);
        gen.generate();



        int first = 0, second = -1;
        for (int i = 0; i < cutPoints.size(); i++) {
            if (ret < cutPoints.get(i)) {
                if (i > 0) {
                    first = cutPoints.get(i - 1);
                }
                second = cutPoints.get(i);
                break;
            }
        }
        timePeriods = new TreeMap<>();
        timePeriods.put(first, second);

        gen = new MyAVGenerator(inAudioPath, inVideoPath, outAudioIndexPath, outVideoIndexPath, timePeriods, summarizer.frameCnt);
        gen.generate();
    }
}
