import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuehw on 16/4/15.
 */
public class MyAVGenerator {
    private String inAudioPath, inVideoPath, outAudioPath, outVideoPath;
    public Map<Integer, Integer> timePeriods;
    public int frameCnt;
    public int width = 480, height = 270;
    public int AudioBufferSize = 3200;
    public int FramePerSecondInVideo = 15;
    public MyAVGenerator(String inAudioPath, String inVideoPath, String outAudioPath, String outVideoPath,
                         Map<Integer, Integer> timePeriods, int frameCnt) {
        this.inAudioPath = inAudioPath;
        this.inVideoPath = inVideoPath;
        this.outAudioPath = outAudioPath;
        this.outVideoPath = outVideoPath;
        this.timePeriods = timePeriods;
        this.frameCnt = frameCnt;
    }

    public void generate() {
        int selectedFrameCnt = 0;
        try {
            File inVideoFile = new File(this.inVideoPath);
            File outVideoFile = new File(this.outVideoPath);
            InputStream is = new FileInputStream(inVideoFile);
            OutputStream os = new FileOutputStream(outVideoFile);
            for (int frameIdx = 0; frameIdx < this.frameCnt; frameIdx++) {
                System.out.println("FrameIdx = " + frameIdx);
                boolean shouldIncluded = false;

                long len = width*height*3;
                byte[] bytes = new byte[(int)len];

                int offset = 0;
                int numRead = 0;
                while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                    offset += numRead;
                }

                for (Map.Entry<Integer, Integer> entry : this.timePeriods.entrySet()) {
                    int head = entry.getKey(), tail = entry.getValue();
                    if (head <= frameIdx && frameIdx < tail) {
                        shouldIncluded = true;
                        break;
                    }
                }
                if (shouldIncluded) {
                    selectedFrameCnt++;
                    os.write(bytes, 0, offset);
                }
            }
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }

        AudioSampleReader sampleReader = null;
        try {
            sampleReader = new AudioSampleReader(new File(inAudioPath));
            long nbSamples = sampleReader.getSampleCount();  // 7200000
            double[] samples = new double[(int)nbSamples];
            sampleReader.getInterleavedSamples(0, nbSamples, samples);
            float frameRate = sampleReader.getFormat().getSampleRate(); //24000.0
            // 24000 samples per second, 1600 samples per frame
            System.out.println("FrameRate = " + frameRate);

            int iFrameRate = (int)(frameRate + 0.5f);
            double[] newSamples = new double[(int)(nbSamples * selectedFrameCnt / frameCnt)];
            int idx = 0;

            long samplePerVideoFrame = nbSamples / frameCnt;
            for (Map.Entry<Integer, Integer> entry : this.timePeriods.entrySet()) {
                int head = entry.getKey(), tail = entry.getValue();
                long lb = head * nbSamples / frameCnt, ub = tail * nbSamples / frameCnt;
                System.out.println("lb, ub = " + lb + ", " + ub);
                for (long i = lb; i < ub; i++) {
                    newSamples[idx++] = samples[(int)(i)];
                }
            }
            assert(idx == (int)(nbSamples * selectedFrameCnt / frameCnt));

            AudioSampleWriter sampleWriter = new AudioSampleWriter(new File(outAudioPath),
                                                                    sampleReader.getFormat(),
                                                                    AudioFileFormat.Type.WAVE);
            sampleWriter.write(newSamples);
            sampleWriter.close();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String inVideoPath = "./data/Alin_Day1_002/Alin_Day1_002.rgb";
        String inAudioPath = "./data/Alin_Day1_002/Alin_Day1_002.wav";
        String outVideoPath = "./tryVideo.rgb";
        String outAudioPath = "./tryAudio.wav";
        Map<Integer, Integer> timePeriods = new HashMap<Integer, Integer>();
        timePeriods.put(0, 15 * 15);
        timePeriods.put(280 * 15, 295 * 15);
        MyAVGenerator test = new MyAVGenerator(inAudioPath, inVideoPath, outAudioPath, outVideoPath, timePeriods, 300 * 15);
        test.generate();
    }
}
