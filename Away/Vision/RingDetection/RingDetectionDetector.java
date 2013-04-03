package Away.Vision.RingDetection;

import Away.Vision.util.*;

import java.awt.image.*;


public class RingDetectionDetector {

	// const
	// EDGE DETECTION
	private final static float low_thresh = 15.0f;
	private final static float high_thresh = 20.0f;

    // args
    private BufferedImage im;
	private BufferedImage edges;
	private int width;
	private int height;
	private int[][] binarizedImageArray;	// C-ORDERED MATRIX (ROW MAJOR)

	private int t;		// white threshold (avg. of RGB values > t for a match)


    
    // CONSTRUCTOR METHOD
    public RingDetectionDetector() {
		this.width = 0;
		this.height = 0;
        
    }


	// PRIVATE METHODS
	private void binarizeImage() {
		// scan image, test threshold
        int[] imageArray = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                
                int rgb = imageArray[x + y*width];
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = (rgb) & 0xff;
                int avg = (r + g + b) / 3;
                if (avg >= t) {
                    binarizedImageArray[y][x] = 1;
                    //im.setRGB(x,y, 0xff0000ff);   //fill blue
                }
            }
        }

	}

	private void RANSAC_CIRCLES(int[][] edgesMatrix) {
		
	}
    
    
    // RUN DETECTION METHOD
    public void runDetection(BufferedImage im, int thresh) {
        // retrieve image properties & initialize
		this.width = im.getWidth();
		this.height = im.getHeight();
		this.t = thresh;
		this.binarizedImageArray = new int[height][width];

		this.im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); 
        this.im = im;
		
		// run thresholding, signal matches on binarized image
		binarizeImage();
        
        // run edge detection on binarized image
		CannyEdgeDetector detector = new CannyEdgeDetector();
		detector.setLowThreshold(low_thresh);
		detector.setHighThreshold(high_thresh);		
		detector.setSourceImage(this.getBinarizedImage());
		detector.process();
		this.edges = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		this.edges = detector.getEdgesImage();

		// store data points in matrix
		int[][] edgesMatrix = new int[height][width];
		int[] edge_data = ((DataBufferInt) this.edges.getRaster().getDataBuffer()).getData();
		int edge_count = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// edge pixel has value either 1 or 255
				if (((edge_data[x + y*width] >> 16) & 0xff) > 0) {
					edgesMatrix[y][x] = 1;
					edge_count++;
				}
			}
		}
		System.out.println("Edge Count: " + edge_count);

		// run RANSAC Circle Detection on data points
		RANSAC_CIRCLES(edgesMatrix);
        
    }
    
    // ACCESS METHODS
    public BufferedImage getBinarizedImage() {
		// return binarized array
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster raster = (WritableRaster) out.getRaster();
        int[] data = new int[width*height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[x + y*width] = binarizedImageArray[y][x];
            }
        }
        raster.setPixels(0, 0, width, height, data);        
        return out;
        
    }

	public BufferedImage getThinnedImage() {
		return this.edges;

	}

    

}
