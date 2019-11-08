/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.styling;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.LiteShape;
import org.geotools.geometry.jts.WKTReader2;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.operation.overlay.snap.GeometrySnapper;

public class MarkAlongLine implements Stroke {

    Stroke delegate;

    static final WKTReader2 reader = new WKTReader2();
//    static final GeometryFactory geometryFactory = new GeometryFactory();
//    static final GeometryBuilder geometryBuolder = new GeometryFactory();
    LiteShape wktShape;
    AffineTransform at;
    // default
    double size = 20;

    public MarkAlongLine(Stroke delegate) {
        this.delegate = delegate;
    }

    public MarkAlongLine(Stroke delegate, double size, String wkt) {
        this.delegate = delegate;
        this.size = size;
        setWKT(wkt);
    }

    public void setWKT(String wkt) {
        //  this.sloppiness = sloppiness; // How sloppy should we be?
        try {
            AffineTransformation at = new AffineTransformation();
            at.setToScale(size, size);
            this.wktShape = new LiteShape(reader.read(wkt), null, false);
            this.wktShape.setGeometry(at.transform(this.wktShape.getGeometry()));
            String simpleWkt=this.wktShape.getGeometry().toText();
            //simplify complex geom to simple geometry
            if(!simpleWkt.equalsIgnoreCase(wkt)) {
                this.wktShape = new LiteShape(reader.read(simpleWkt), null, false);
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //    public void setAffineTransform(AffineTransform at) {
    //        this.at = at;
    //    }

    @Override
    public Shape createStrokedShape(Shape shape) {
        GeneralPath newshape = new GeneralPath(); // Start with an empty shape

        float[] coords = new float[6];
        float[] prevcoords = new float[6];
        boolean newSegment = true;
        
        LiteShape drapeMe;
        double angle;
        for (PathIterator i = shape.getPathIterator(null); !i.isDone(); i.next()) {
            int type = i.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    // perturb(coords, 2);

                    // remember where segment is starting from
                    // lineAt.setToTranslation(coords[0], coords[1]);
                    newshape.moveTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    // get shape for drapping
                    if(!newSegment) {
                        //mark line from where drawing of previous shape stopped
                        //to the point where 
                     //   newshape.lineTo(prevcoords[0], prevcoords[1]);
                    } 
                    drapeMe =
                            getShapeForSegment(coords[0], prevcoords[0], coords[1], prevcoords[1]);
                    // rotate
                    angle = getAngle(prevcoords[0], coords[0], prevcoords[1], coords[1]);
                    // draw
                    drape(newshape, drapeMe, prevcoords[0], prevcoords[1], angle);
                    newSegment = false;
                    break;
                case PathIterator.SEG_QUADTO:
                    newshape.quadTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    newshape.curveTo(
                            coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    newshape.closePath();
                    break;
            }
            prevcoords = coords.clone();
        }
        // newshape.closePath();
        // Finally, stroke the perturbed shape and return the result
        return delegate.createStrokedShape(newshape);
    }

    private LiteShape getClone(AffineTransform at) {

        return new LiteShape(wktShape.getGeometry(), at, false);
    }

    private int getSegmentLength(double x1, double x2, double y1, double y2) {
        // Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
        return (int) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    private double getAngle(double x1, double x2, double y1, double y2) {
        float angle = (float) Math.toDegrees(Math.atan2((y2 - y1), (x2 - x1)));

        if (angle < 0) {
            angle += 360;
        }

        return Math.toRadians(angle);
    }

    private LiteShape getShapeForSegment(double x1, double x2, double y1, double y2) {
        double length = getSegmentLength(x1, x2, y1, y2);
        // float angle = getAngle(x1,x2,y1,y2);

        //        Geometry line = geometryFactory.createLineString(new Coordinate[] {
        //                new Coordinate(x1,y1),
        //                new Coordinate(x2,y2)
        //        });
        return getExtendedShape(length);
        // return null;
    }

    // this method returns the wkt geometry repeated in cycles
    // required to cover the line segment
    private LiteShape getExtendedShape(double length) {
        // e.g repetition = 2.5 means drape the shape completey twice and once half
        float repetition = (float) (length / this.wktShape.getBounds2D().getWidth());
        LiteShape repeatedShape = this.getClone(null); // first shape
        // AT to translate copies of geometry and union them into result shape
        AffineTransformation at = new AffineTransformation();
        
        
        for (int i = 1; i < repetition; i++) {
            // translate along x according to the width and repetition number
            at.setToTranslation((i * this.wktShape.getBounds2D().getWidth()), 0);
            Geometry geom = repeatedShape.getGeometry();
            Geometry translatedGeom = at.transform(this.getClone(null).getGeometry());
            
            repeatedShape.setGeometry(geom.union(translatedGeom));
        }        
        Rectangle clip = null;
        //shape is longer than length of line segment
        //clip it with box with width = line length and height = shape height
        if (repeatedShape.getBounds().width > length) {
            GeometrySnapper snapper = new GeometrySnapper(repeatedShape.getGeometry());
            Geometry env=repeatedShape.getGeometry().getEnvelope();
            double scaleX=length/repeatedShape.getBounds().width;
            at = new AffineTransformation();
            at.setToScale(scaleX, 1d);            
            //create Polygon from Rectangle bounds and cut the final shape down to size in length
//            repeatedShape.setGeometry(at.transform(env).intersection(repeatedShape.getGeometry()));
            repeatedShape.setGeometry(repeatedShape.getGeometry().intersection(at.transform(env)));
            //repeatedShape.setGeometry(snapper.snapTo(at.transform(env), 10));          
            
        }
        // draw part last partial segmnet of shape if exists
        // float decimalPart=repetition-new Float(repetition).intValue();
        //repeatedShape = new LiteShape(repeatedShape.getGeometry(), null, false, length);
        return repeatedShape;
    }
    
    //OPTION 2
    private LiteShape getExtendedTransformedShape(double length) {
        // e.g repetition = 2.5 means drape the shape completey twice and once half
        float repetition = (float) (length / this.wktShape.getBounds2D().getWidth());
        LiteShape repeatedShape = this.getClone(null); // first shape
        // AT to translate copies of geometry and union them into result shape
        AffineTransformation at = new AffineTransformation();
        
        Geometry geom;
        Geometry cloneGeom;
        Geometry translatedGeom;
        for (int i = 1; i < repetition; i++) {
            // translate along x according to the width and repetition number
            at.setToTranslation((i * this.wktShape.getBounds2D().getWidth()), 0);
            geom = repeatedShape.getGeometry();
            cloneGeom = this.getClone(null).getGeometry();
            translatedGeom = at.transform(cloneGeom);            
            repeatedShape.setGeometry(geom.union(translatedGeom));
        }     
        
        
        //length adjustment
        if(repeatedShape.getBounds().width != length) {
            double scaleX=length/repeatedShape.getBounds().width;
            at = new AffineTransformation();
            at.setToScale(scaleX, 1d);
            repeatedShape.setGeometry(at.transform(repeatedShape.getGeometry()));
            // draw part last partial segmnet of shape if exists
            // float decimalPart=repetition-new Float(repetition).intValue();
            //repeatedShape = new LiteShape(repeatedShape.getGeometry(), null, false, length);
        }
        
        return repeatedShape;
    }

    private float[] drape(
            GeneralPath newshape,
            LiteShape wktShape,
            double tx,
            double ty,
            double rotationRadians) {
        // setting AT to anchor wktShape to start of line segnment
        // and rotate to line segment angle
        AffineTransformation at = new AffineTransformation();
        at.rotate(rotationRadians);
        at.translate(tx, ty);
        wktShape.setGeometry(at.transform(wktShape.getGeometry()));
        
        float[] coords = new float[6];

        for (PathIterator i = wktShape.getPathIterator(null); !i.isDone(); i.next()) {
            int type = i.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    newshape.moveTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    newshape.lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    newshape.quadTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    newshape.curveTo(
                            coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    newshape.closePath();
                    break;
            }
        }
        // return last drawn
    //    newshape.moveTo(coords[0], coords[1]);
        return coords;
    }
}
