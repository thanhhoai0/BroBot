package datatypes;

import java.awt.Point;

public class Circle {

    // args
    Point c;    // center point of circle
    double r;   // radius of circle
    double score;   // "fit" score
    
    // CONSTRUCTOR METHODS
    public Circle() {
        this.c = new Point(0,0);
        this.r = Double.POSITIVE_INFINITY;
        this.score = Double.POSITIVE_INFINITY;
    }
    
    public Circle(Point p, double r) {
        this.c = p;
        this.r = r;
        this.score = Double.POSITIVE_INFINITY;  // init at infinity
    }
    
    public void setPoint(Point p) {
        this.c = p;
    }
    
    public void setRadius(double r) {
        this.r = r;
    }
    
    public void setScore(double s) {
        this.score = s;
    }
    
    // ACCESSOR METHODS
    public double getRadius() {
        return r;
    }
    
    public Point getCenter() {
        return c;
    }
    
    public double getScore() {
        return score;
    }


}