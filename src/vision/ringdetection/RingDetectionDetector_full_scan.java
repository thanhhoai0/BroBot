package vision.ringdetection;

import vision.util.*;
import datatypes.*;

import java.awt.image.*;
import java.awt.Point;
import java.util.Random;
import java.util.ArrayList;


public class RingDetectionDetector_full_scan {

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
    private ArrayList<Circle> circles_list; // list of circles

	private int t;		// white threshold (avg. of RGB values > t for a match)
    
    
    // RANSAC Parameters
    private static final int k = 3000;       // number of iterations in search of a circle
    private static final int q = 6;        // number of total circles to be found
    private static final int n = 3;         // randomly selected n points
    private static final double inf = Double.POSITIVE_INFINITY;
    private static final int MIN_RADIUS = 100;   // min pixel radius
	private static final int MAX_RADIUS = 700;	// max pixel radius
    private static final int MIN_SIZE = 300; // min. number of data points to define a circle (circum of min radius)
    private static final double ERROR_CONST = 0.004;
    private static final int ERROR_THRESH = 2;  // base error threshold in pixels for pixel offset
                                                // actual error dependent on radius size
                                                // thresh = ERROR_CONST*radius*error_thresh

	private static final int OFFSET = 10;	//offset for searching for white pixels in bounding box
    


    
    // CONSTRUCTOR METHOD
    public RingDetectionDetector_full_scan() {
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
        // RANSAC ALGORITHM
        //  1. pick three random points from data array
        //  2. fit a circle for these three points
        //  3. run through all points in the array and test fit to line
        //      - if there is a high fit, add to hypothesis inliers
        //      - if there is a high count of inliers, add circle to best circle array
        //  4. Repeat until we find the best circle after many iterations and save the best circle
        //      - remove these points from data array and continue
        //  5. Repeat after we find n circles
        
        this.circles_list = new ArrayList<Circle>();
        int trials = 0;
        
        while (trials < q) {
            // init & reset
            Circle best_model = new Circle();
            ArrayList<Point> best_consensusSet = new ArrayList<Point>();
            
            // begin traversal
            Random randomizer = new Random();
            int iterations = 0;
            while (iterations < k) {
                // coords
                int p1x = randomizer.nextInt(width); int p1y = randomizer.nextInt(height);
                int p2x = randomizer.nextInt(width); int p2y = randomizer.nextInt(height);
                int p3x = randomizer.nextInt(width); int p3y = randomizer.nextInt(height);
                // points
                Point p1 = new Point(p1x, p1y);
                Point p2 = new Point(p2x, p2y);
                Point p3 = new Point(p3x, p3y);
                // test for dissimilar points
                while (p1 == p2 || p2 == p3 || p1 == p3) {
                    // coords
                    p1x = randomizer.nextInt(width); p1y = randomizer.nextInt(height);
                    p2x = randomizer.nextInt(width); p2y = randomizer.nextInt(height);
                    p3x = randomizer.nextInt(width); p3y = randomizer.nextInt(height);
                    // points
                    p1.setLocation(p1x, p1y);
                    p2.setLocation(p2x, p2y);
                    p3.setLocation(p3x, p3y);
                }
                // values
                int p1v = edgesMatrix[p1y][p1x];
                int p2v = edgesMatrix[p2y][p2x];
                int p3v = edgesMatrix[p3y][p3x];                
                // retrieve model
                Circle model = fitCircle(p1, p2, p3);
                
                // test radius
                if (model.getRadius() < MIN_RADIUS || model.getRadius() > MAX_RADIUS || model.getCenter().getX() - model.getRadius() - OFFSET < 0 || model.getCenter().getY() - model.getRadius() - OFFSET < 0 || model.getCenter().getX() + model.getRadius() + OFFSET > width || model.getCenter().getY() + model.getRadius() + OFFSET > height) continue;
                
                // find consensus points
                double squared_error = 0;
                ArrayList<Point> consensusSet = new ArrayList<Point>();
                consensusSet.add(p1); consensusSet.add(p2); consensusSet.add(p3);
                for (int y = (int)(model.getCenter().getY() - model.getRadius() - OFFSET); y < (int)(model.getCenter().getY() + model.getRadius() + OFFSET); y++) {
                    for (int x = (int)(model.getCenter().getX() - model.getRadius() - OFFSET); x < (int)(model.getCenter().getX() + model.getRadius() + OFFSET); x++) {
                        // test if pixel is an edge
                        if (edgesMatrix[y][x] != 1) continue;
                        // test if pixel is within range of radius
                        double error = getCirclePointError(model, x, y);
                        
                        if (error < (ERROR_CONST*model.getRadius()*ERROR_THRESH)) {
                            // test passes, add to consensus set
                            Point in = new Point(x,y);
                            consensusSet.add(in);
							squared_error = squared_error + error*error;
                        }
                    }
                }
                // set score
                model.setScore(squared_error);
                
                // validate consensus set
                if (consensusSet.size() > MIN_SIZE + 0.001*model.getRadius()) {
                    
                    if (model.getScore() < best_model.getScore()) {
                        // current model is better than previous model
                        best_model.setPoint(model.getCenter());
                        best_model.setRadius(model.getRadius());
                        best_model.setScore(model.getScore());
                        best_consensusSet.clear();
                        best_consensusSet.addAll(consensusSet);
                    }
                    
                }
                
                // increment iterations
                iterations++;
            }
            
            // traversal is complete over all iterations
            // retrieve best model and remove points from set
            // add circle properties to circle list
            if (best_consensusSet.size() != 3) {
                // add circle to list
                Circle in = new Circle(best_model.getCenter(), best_model.getRadius());
                in.setScore(best_model.getScore());
                circles_list.add(in);
                System.out.println("Added Circle to List: ");
                System.out.println("Radius: " + in.getRadius());
                System.out.println("Center: " + in.getCenter());
                
                // remove points from set
                for (int i = 0; i < best_consensusSet.size(); i++) {
                    Point cur_point = best_consensusSet.get(i);
                    edgesMatrix[(int)cur_point.getY()][(int)cur_point.getX()] = 0;
					this.edges.setRGB((int)cur_point.getX(), (int)cur_point.getY(), 0xffff0000);
                }
            }
            
            // increment trials
            trials++;

        }
        
		
	}
    
    // RANSAC HELPER METHODS
    private Circle fitCircle(Point p1, Point p2, Point p3) {
        // given three points, fit a circle
        // return the radius and center coordinate point
        // int array result = [Point c, int radius] (floored)
        // Point c Calculations:
        //      ~ Done by equating distance from all three points to xr and yr
        //      ~ Algebraic expression
        //
        // Radius Calculations:
        //      ~ Done by calculating distance from any input point to center point
        // 
        
        // CENTER POINT CALCULATIONS        
        double A = p1.getX() - p3.getX();
        double B = p1.getX() - p2.getX();
        double C = A*(p2.getX()*p2.getX() + p2.getY()*p2.getY()) + B*(p1.getX()*p1.getX() + p1.getY()*p1.getY()) - A*(p1.getX()*p1.getX() + p1.getY()*p1.getY()) - B*(p3.getX()*p3.getX() + p3.getY()*p3.getY());
        double D = 2*(A*(p2.getY()-p1.getY()) - B*(p3.getY() - p1.getY()));
        
        int yr = (int)Math.floor(C / D);
        
        int xr = (int)Math.floor(((p1.getX()*p1.getX() + p1.getY()*p1.getY()) - (p2.getX()*p2.getX() + p2.getY()*p2.getY()) + 2*yr*(p2.getY() - p1.getY())) / (2*(p1.getX() - p2.getX())));
        
        Point center = new Point(xr, yr);
        
        // RADIUS CALCULATIONS
        double r = Math.sqrt((p1.getX() - center.getX())*(p1.getX() - center.getX()) + (p1.getY() - center.getY())*(p1.getY() - center.getY()));
        
        // return
        Circle out = new Circle(center, r);
        return out;
    }
    
    private double getCirclePointError(Circle model, int x, int y) {
        // Assess validity of test point x and y on circle model
        //  ~ calculate distance from point to center
        //  ~ compare distance with radius
        //  ~ return difference in distance
        Point c = model.getCenter();
        double r = model.getRadius();        
        double dist = Math.sqrt((x - c.getX())*(x - c.getX()) + (y - c.getY())*(y - c.getY()));
        
        return Math.abs(dist - r);
        
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
		for (int i = 0; i < circles_list.size(); i++) {
			Circle cur_circle = circles_list.get(i);
			Point p = cur_circle.getCenter();
			if (p.getX() > 0 && p.getX() < width && p.getY() > 0 && p.getY() < height) {
				this.edges.setRGB((int)p.getX(), (int)p.getY(), 0xff0000ff);
				this.edges.setRGB((int)p.getX()-1, (int)p.getY(), 0xff0000ff);
				this.edges.setRGB((int)p.getX(), (int)p.getY()-1, 0xff0000ff);
				this.edges.setRGB((int)p.getX()+1, (int)p.getY(), 0xff0000ff);
				this.edges.setRGB((int)p.getX(), (int)p.getY()+1, 0xff0000ff);
				this.edges.setRGB((int)p.getX()-1, (int)p.getY()-1, 0xff0000ff);
				this.edges.setRGB((int)p.getX()+1, (int)p.getY()+1, 0xff0000ff);
				this.edges.setRGB((int)p.getX()-1, (int)p.getY()+1, 0xff0000ff);
				this.edges.setRGB((int)p.getX()+1, (int)p.getY()-1, 0xff0000ff);
			}
		}
		return this.edges;

	}

    

}
