package mosaic;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

public class CalcAverages {
	private static int[][] averages;

	public CalcAverages() {
		
	}
	
	class CalcAverage implements Runnable {
		private int imgNum;

		public CalcAverage(int imgNum) {
			this.imgNum = imgNum;
		}

		public void run() {
			BufferedImage image = null;
			try {
				image = ImageIO.read(new File(String.format("img_build/%06d.jpg", imgNum + 1)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			long r = 0, g = 0, b = 0;
			Color c;
			for (int x = 0; x < image.getWidth(); x++) {
				for (int y = 0; y < image.getHeight(); y++) {
					c = new Color(image.getRGB(x, y));
					r += c.getRed();
					g += c.getGreen();
					b += c.getBlue();
				}
			}
			int imgSize = image.getWidth() * image.getHeight();
			synchronized (averages[imgNum]) {
				averages[imgNum][0] = (int) (r / imgSize);
				averages[imgNum][1] = (int) (g / imgSize);
				averages[imgNum][2] = (int) (b / imgSize);
			}
		}
	}

	public void calcAverages() {
		long startTime = System.nanoTime();

		averages = new int[Main.IMG_COUNT][3];
		ExecutorService pool = Executors.newFixedThreadPool(Main.THREAD_COUNT);

		for (int i = 0; i < Main.IMG_COUNT; i++) {
			Runnable run = new CalcAverage(i);
			pool.execute(run);
		}

		pool.shutdown();

		try {
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("avgs/avgs.txt")));
			for (int i = 0; i < Main.IMG_COUNT; i++) {
				bw.write(String.format("%d,%d,%d\n", averages[i][0], averages[i][1], averages[i][2]));
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(String.format("Finished calculating averages (%.1f seconds).",
				(double) (System.nanoTime() - startTime) / 1000000000));
	}
}