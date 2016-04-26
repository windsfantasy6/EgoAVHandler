import java.io.*;

/**
 * Created by yuehw on 16/4/20.
 */
public class MyAVStapler {
    final int frameCnt = 300 * 15, width = 480, height = 270;
    int idx(int i, int j, int m) {
        return i * m + j;
    }
    int diff(int x, int y) {
        if (x < 0) {
            x += 256;
        }
        if (y < 0) {
            y += 256;
        }
        return Math.abs(x - y);
        //return (x - y) * (x - y);
    }

    boolean diff2(double x, double y, double err) {
        return Math.abs(x - y) <= err;
    }

    public String inVideoPath, outVideoPath;
    public MyAVStapler(String inVideoPath, String outVideoPath) {
        this.inVideoPath = inVideoPath;
        this.outVideoPath = outVideoPath;
    }

    double[] GenerateMotionVectorAngle(byte[] last, byte[] now) {
        int Xrange = 3, Yrange = 2;
        double angRange = 2.0 * Math.PI / 180.0;
        double subAng = angRange / 5;
        double[] res = new double[3];
        long opt = 1 << 62;
        int threshold = 1000000;
        double[] cosTable = new double[11], sinTable = new double[11];
        for (int i = 0; i < 11; i++) {
            double ang = (i - 5) * subAng;
            cosTable[i] = Math.cos(ang);
            sinTable[i] = Math.sin(ang);
        }
        for (int dx = -Xrange; dx <= Xrange; dx++) {
            for (int dy = -Yrange; dy <= Yrange; dy++) {
                for (int angIdx = 0; angIdx < 11; angIdx++) {
                    long diffSum = 0;
                    for (int y = 20; y < height - 20; y++) {
                        for (int x = 20; x < width - 20; x++) {
                            int newx = x + dx, newy = y + dy;
                            double vx = newx - width / 2, vy = newy - height / 2;
                            int nvx = width / 2 + (int)Math.round(cosTable[angIdx] * vx - sinTable[angIdx] * vy);
                            int nvy = height / 2 + (int)Math.round(sinTable[angIdx] * vx + cosTable[angIdx] * vy);
                            if (nvx < 0 || nvy < 0 || nvx >= width || nvy >= width) {
                                System.out.println("y, x = " + y + ", " + x);
                                continue;
                            }
                            byte lr = last[idx(nvy, nvx, width)];
                            byte lg = last[idx(nvy, nvx, width) + width * height];
                            byte lb = last[idx(nvy, nvx, width) + 2 * width * height];
                            byte nr = now[idx(y, x, width)];
                            byte ng = now[idx(y, x, width) + width * height];
                            byte nb = now[idx(y, x, width) + 2 * width * height];
                            diffSum += diff(lr, nr) + diff(lg, ng) + diff(lb, nb);
                            if (diffSum >= opt || diffSum >= threshold) {
                                break;
                            }
                        }
                        if (diffSum >= opt || diffSum >= threshold) {
                            break;
                        }
                    }
                    if (diffSum < opt) {
                        opt = diffSum;
                        res[0] = dy;
                        res[1] = dx;
                        res[2] = angIdx;
                    }
                }
            }
        }
        System.out.println("opt = " + opt);
        if (opt >= threshold) {
            res = new double[0];
        }

        return res;
    }

    /////////byte is signed, but the pic is not. Compare from a true rubric
    private byte[] rotate(byte[] now, double[] motions) {
        byte[] res = new byte[now.length];
        for (int i = 0; i < now.length; i++) {
            res[i] = now[i];
        }


        double angRange = 2.0 * Math.PI / 180.0;
        double subAng = angRange / 5;
        double[] cosTable = new double[11], sinTable = new double[11];
        for (int i = 0; i < 11; i++) {
            double ang = (i - 5) * subAng;
            cosTable[i] = Math.cos(ang);
            sinTable[i] = Math.sin(ang);
        }
        int dy = (int)Math.round(motions[0]), dx = (int)Math.round(motions[1]);
        int angIdx = (int)Math.round(motions[2]);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int newx = x + dx, newy = y + dy;
                double vx = newx - width / 2.0, vy = newy - height / 2.0;
                int nvx = width / 2 + (int)Math.round(cosTable[angIdx] * vx - sinTable[angIdx] * vy);
                int nvy = height / 2 + (int)Math.round(sinTable[angIdx] * vx + cosTable[angIdx] * vy);
                if (nvx < 0 || nvy < 0 || nvx >= width || nvy >= height) {
                    continue;
                }
                res[idx(nvy, nvx, width)] = now[idx(y, x, width)];
                res[idx(nvy, nvx, width) + width * height] = now[idx(y, x, width) + width * height];
                res[idx(nvy, nvx, width) + 2 * width * height] = now[idx(y, x, width) + 2 * width * height];
            }
        }
        return res;
    }

    public void GenerateStapleVideo() {
        try {
            File inVideoFile = new File(this.inVideoPath);
            File outVideoFile = new File(this.outVideoPath);
            InputStream is = new FileInputStream(inVideoFile);
            OutputStream os = new FileOutputStream(outVideoFile);
            byte[] last = null;
            int slowCnt = 0;
            for (int frameIdx = 0; frameIdx < this.frameCnt; frameIdx++) {
                System.out.println("FrameIdx = " + frameIdx);

                long len = width*height*3;
                byte[] bytes = new byte[(int)len];

                int offset = 0;
                int numRead = 0;
                while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                    offset += numRead;
                }
                if (last != null) {
                    double[] ret = GenerateMotionVectorAngle(last, bytes);
                    if (ret.length >= 2) {
                        System.out.println("Slow Frame: " + frameIdx);
                        slowCnt++;
                        bytes = rotate(bytes, ret);
                    }
                }
                os.write(bytes, 0, offset);
                last = bytes;
            }
            System.out.println("Slow Count = " + slowCnt);
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }
    }
    public static void main(String args[]) {
        String inVideoPath = args[0];
        String outVideoPath = args[1];
        MyAVStapler stapler = new MyAVStapler(inVideoPath, outVideoPath);
        stapler.GenerateStapleVideo();
    }
}
