import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by yuehw on 16/4/12.
 */



public class MyAVPlayer {

    final int FRAME_PER_SECOND = 15;
    // 0: stop  1: play  2: pause
    public int mode = 0;

    /**
     * Created by daniel on 28/10/2015.
     */
    public class NanoTimer {

        public void busyWaitMicros(long micros){
            long waitUntil = System.nanoTime() + (micros * 1_000);
            while(waitUntil > System.nanoTime()){
                ;
            }
        }
    }

    public class Buttons extends JPanel implements ActionListener {
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		protected JButton b1, b2, b3;
    	public Buttons() {
    		ImageIcon playIcon = createImageIcon("play.png");
    		ImageIcon pauseIcon = createImageIcon("pause.png");
    		ImageIcon stopIcon = createImageIcon("stop.png");
    		
    		b1 = new JButton("Play", playIcon);
    		b1.setVerticalTextPosition(AbstractButton.CENTER);
    		b1.setHorizontalTextPosition(AbstractButton.LEADING);
    		b1.setMnemonic(KeyEvent.VK_P);
    		
    		b2 = new JButton("Pause", pauseIcon);
    		b2.setVerticalTextPosition(AbstractButton.CENTER);
    		b2.setHorizontalTextPosition(AbstractButton.LEADING);
    		b2.setMnemonic(KeyEvent.VK_A);
    		
    		b3 = new JButton("Stop", stopIcon);
    		b3.setVerticalTextPosition(AbstractButton.CENTER);
    		b3.setHorizontalTextPosition(AbstractButton.LEADING);
    		b3.setMnemonic(KeyEvent.VK_S);
    		
    		//Listen for the actions on button 1,2,3
    		b1.addActionListener(this);
    		b2.addActionListener(this);
    		b3.addActionListener(this);
    		System.out.println("action listener");
    		
    		b1.setToolTipText("Click this button to play the video");
    		b2.setToolTipText("Click this button to pause the video");
    		b3.setToolTipText("Click this button to stop the video");
    			
    	}
    	
    	
    	/** Returns an ImageIcon, or null if the path was invalid. */
    	protected ImageIcon createImageIcon(String path) {
    		java.net.URL imgURL = Buttons.class.getResource(path);
    		if (imgURL != null) {
    			ImageIcon icon = new ImageIcon(imgURL);
    			Image img = icon.getImage();
    			Image newimg = img.getScaledInstance(25, 25, java.awt.Image.SCALE_SMOOTH);
    			return new ImageIcon(newimg);
    		} else {
    			System.err.println("Couldn't find file : " + path);
    			return null;
    		}
    	}    	  
    	
		@Override
		public void actionPerformed(ActionEvent e) {
			
			System.out.println("Listening...");
			// 这里写play pause stop方法
			
			if (e.getSource() == b1) {
				mode = 1;
				System.out.println("Play button clicked, mode = " + mode);
			} else if (e.getSource() == b2) {
				mode = 2;
				System.out.println("Pause button clicked, mode = " + mode);
			} else {
				mode = 0;
				System.out.println("Stop button clicked, mode = " + mode);
			}
		}
    }
    public class PlayWaveException extends Exception {

        public PlayWaveException(String message) {
            super(message);
        }

        public PlayWaveException(Throwable cause) {
            super(cause);
        }

        public PlayWaveException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /*
        For 300 seconds audios, there are 7200000 frame positions. 24000 frame positions per second.
        Try to find how many frames in the video.
     */
    public class MyAudioPlayer implements Runnable {
        public String path;
        private FileInputStream inputStream, inputStream1;
        private InputStream waveStream, waveStream1;
        // 0: stop 1: play 2: pause
        public int mode = 0;
        AudioInputStream ais = null, ais1 = null;
        private final int EXTERNAL_BUFFER_SIZE = 3200;
        DataLine.Info info;
        SourceDataLine dataLine = null;
        Object lock;
        long length = 300000000;
        AudioFormat audioFormat;

        MyAudioPlayer() {

        }

        MyAudioPlayer(String path_, Object lock_) {
            path = path_;
            lock = lock_;
            try {
                inputStream = new FileInputStream(path);
                inputStream1 = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
            waveStream = new BufferedInputStream(inputStream);
            waveStream1 = new BufferedInputStream(inputStream1);
            try {
                ais = AudioSystem.getAudioInputStream(waveStream);
                ais1 = AudioSystem.getAudioInputStream(waveStream1);
            } catch (UnsupportedAudioFileException e1) {
                //throw new PlayWaveException(e1);
            } catch (IOException e1) {
                //throw new PlayWaveException(e1);
            }

            audioFormat = ais.getFormat();
            /*
            length = (ais.getFrameLength() *
                    audioFormat.getFrameSize() * 8) / audioFormat.getSampleSizeInBits();
            length /= audioFormat.getChannels();
            */
            length = (long)(1000000 * (ais.getFrameLength()) / audioFormat.getSampleRate());
            System.out.println("Audio Length = " + this.length);
        }


        public long getLength() {
            return this.length;
        }

        public synchronized void run() {

            info = new DataLine.Info(SourceDataLine.class, audioFormat);

            try {
                dataLine = (SourceDataLine) AudioSystem.getLine(info);
                dataLine.open(audioFormat, this.EXTERNAL_BUFFER_SIZE);
            } catch (LineUnavailableException e1) {

            }

            dataLine.start();

            int readBytes = 0;
            byte[] audioBuffer = new byte[this.EXTERNAL_BUFFER_SIZE];
            int sumBytes = 0; // 14400000 for 300s, 48000/s, 3200/frame
            float frameRate = audioFormat.getSampleRate();
            long frameSize = audioFormat.getFrameSize();
            System.out.println("FrameRate = " + frameRate);
            System.out.println("FrameSize = " + frameSize);
            long total = (ais.getFrameLength() *
                    audioFormat.getFrameSize() * 8) / audioFormat.getSampleSizeInBits();
            System.out.println("Sample Count = " + total / audioFormat.getChannels());
            while (true) {
                try {
                    do {
                        do {
                            synchronized (lock) {
                                lock.wait();
                            }
                        } while (mode != 1 && mode != 0);
                    } while (mode != 1);

                    if (mode == 0) {
                        //reset the flow
                        continue;
                    }
                    try {
                        //while (readBytes != -1) {
                        if (readBytes != -1) {
                            readBytes = ais.read(audioBuffer, 0, audioBuffer.length);
                            sumBytes += readBytes;
                            System.out.println("readBytes, sumBytes = " + readBytes + ", " + sumBytes);
                            //System.out.println("FramePosition = " + dataLine.getFramePosition());
                            if (readBytes >= 0) {
                                dataLine.write(audioBuffer, 0, readBytes);
                            }
                        }
                    } catch (IOException e) {

                    } finally {
                        if (readBytes == -1) {
                            dataLine.drain();
                            //System.out.println("FramePosition = " + dataLine.getFramePosition());
                            dataLine.close();
                            return;
                        }
                    }
                } catch (InterruptedException e) {

                }

            }

        }

    }
    public class MyVideoPlayer extends Thread {

        public String path;
        // 0: stop   1: play   2: pause
        public int mode = 0;
        final int width = 480;
        final int height = 270;
        BufferedImage img;
        InputStream is;

        JFrame frame;
        JLabel lbIm1;
        private final int EXTERNAL_BUFFER_SIZE = 524288;
        Object lock;
        GridBagConstraints c;
        BufferedImage firstImg = null;

        MyVideoPlayer() {

        }

        MyVideoPlayer(String path_, Object lock_) {
            path = path_;
            lock = lock_;
            //Create and set up the window
            frame = new JFrame("AVPlayer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            
            GridBagLayout gLayout = new GridBagLayout();
            frame.getContentPane().setLayout(gLayout);
            
            //Display the window
            //frame.pack();
            frame.setVisible(true);
            c = new GridBagConstraints();
            JLabel lbText1 = new JLabel("Video: ");
            lbText1.setHorizontalAlignment(SwingConstants.LEFT);
            JLabel lbText2 = new JLabel("Audio: ");
            lbText2.setHorizontalAlignment(SwingConstants.LEFT);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.5;
            c.gridx = 0;
            c.gridy = 0;
            frame.getContentPane().add(lbText1, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.5;
            c.gridx = 0;
            c.gridy = 1;
            frame.getContentPane().add(lbText2, c);
            
            //Create and set up the content pane
            Buttons newButton = new Buttons();
            newButton.setOpaque(true);//content panes must be opaque                   

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 1;
            c.weightx = 1;
            c.ipady = 10;
            frame.getContentPane().add(newButton.b1, c);
            
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 1;
            c.weightx = 1;
            c.ipady = 10;
            frame.getContentPane().add(newButton.b2, c);
            
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 2;
            c.gridy = 2;
            c.gridwidth = 1;
            c.weightx = 1;
            c.ipady = 10;
            frame.getContentPane().add(newButton.b3, c);
            frame.pack();
            
        }

        public void run() {
            try {
                File file = new File(path);
                is = new FileInputStream(file);
                long len = width * height * 3;
                byte[] bytes = new byte[(int)len];
                int offset = 0, numRead = 0;
                while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                    offset += numRead;
                }

                //wait();
                img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                
                
                
                System.out.println(mode);
                
                while (true) {
                    do {
                        do {
                            synchronized (lock) {
                                lock.wait();
                            }
                            if (mode == 0) {
                                if (lbIm1 != null) {
                                    frame.getContentPane().remove(lbIm1);
                                }
                                img = firstImg;
                                lbIm1 = new JLabel(new ImageIcon(img));
                                c.fill = GridBagConstraints.HORIZONTAL;
                                c.gridx = 0;
                                c.gridy = 3;
                                c.ipady = 50;
                                c.ipadx = 50;
                                //c.weighty = 1;
                                c.gridwidth = 3;
                                frame.getContentPane().add(lbIm1, c);
                                frame.pack();
                            }
                        } while (mode != 1);
                    } while (mode != 1 && mode != 0);
                    int ind = 0;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            byte a = 0;
                            byte r = bytes[ind];
                            byte g = bytes[ind + height * width];
                            byte b = bytes[ind + height * width * 2];
                            //System.out.println("r, g, b = " + r + ", " + g + ", " + b);
                            int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                            img.setRGB(x, y, pix);
                            ind++;
                        }
                    }
                    
                    if (lbIm1 != null) {
                    	frame.getContentPane().remove(lbIm1);
                    }

                    if (firstImg == null) {
                        firstImg = img;
                    }
                    lbIm1 = new JLabel(new ImageIcon(img));
                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.gridx = 0;
                    c.gridy = 3;
                    c.ipady = 50;
                    c.ipadx = 50;
                    //c.weighty = 1;
                    c.gridwidth = 3;
                    frame.getContentPane().add(lbIm1, c);                  
                    frame.pack();
                    
                    offset = 0;
                    while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                        offset += numRead;
                    }
                    
                }

            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            } catch (InterruptedException e) {

            }


        }
    }

    public void play(String videoPath, String audioPath) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        final Object lock = new Object();
        MyAudioPlayer aPlayer = new MyAudioPlayer(audioPath, lock);
        MyVideoPlayer vPlayer = new MyVideoPlayer(videoPath, lock);
        Thread aThread = new Thread(aPlayer);
        Thread vThread = new Thread(vPlayer);
        aThread.start();
        vThread.start();

        //vThread.start();

        mode = 1;
        try {
            Thread.sleep(1000);
            //length is decided by audio, micro second
            long length = aPlayer.getLength();
            System.out.println("length = " + length);
            long frameCnt = (length / 1000000) * FRAME_PER_SECOND;

            long sleepTime = length / frameCnt;
            System.out.println("Length1 = " + length);
            System.out.println("SleepTime = " + sleepTime);
            System.out.println("Length2 = " + sleepTime * frameCnt);
            for (long i = 0; i < frameCnt; i++) {
                if (mode == 0) {
                    i = 0;
                    aPlayer.mode = this.mode;
                    vPlayer.mode = this.mode;
                    aThread.interrupt();
                    vThread.interrupt();
                    aPlayer = new MyAudioPlayer(audioPath, lock);
                    //vPlayer = new MyVideoPlayer(videoPath, lock);
                    aThread = new Thread(aPlayer);
                    vThread = new Thread(vPlayer);
                    aThread.start();
                    vThread.start();
                }
                aPlayer.mode = this.mode;
                vPlayer.mode = this.mode;

                synchronized (lock) {
                    lock.notifyAll();
                }
                NanoTimer timer = new NanoTimer();
                timer.busyWaitMicros(sleepTime);
                if (i == frameCnt - 1) {
                    mode = 0;
                    i = 0;
                }
            }
        } catch (InterruptedException e) {

        }


    }
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: java -jar AVPlayer.jar [RGB file] [WAV file]");
            return;
        }
        //JFrame frame = new JFrame();
        MyAVPlayer player = new MyAVPlayer();
        try {
            player.play(args[0], args[1]);
        } catch (LineUnavailableException e) {

        } catch (IOException e) {

        } catch (UnsupportedAudioFileException e) {

        }
    }
}
