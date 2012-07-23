package org.openscience.cdk.renderer;


import org.openscience.cdk.renderer.elements.AtomSymbolElement;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.elements.LineElement;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.font.IFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.visitor.IDrawVisitor;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Writer;

/**
 * A very quick Draw Vistor for the HTML 5 canvas. This is more a proof of concept
 * and simply writes javascript which will draw on a 2D context.
 *
 * @author John May
 */
public class HTMLCanvasDrawVisitor implements IDrawVisitor {

    // should use a different font manager
    private AWTFontManager  fontManager;
    private RendererModel   rendererModel;
    private AffineTransform transform;

    private final Writer writer;

    /**
     * Method copied from AbstractAWTDrawVistor which transforms a point using the
     * provided affine transform. An example would be scaling the molecule.
     *
     * @param xCoord an x coordinate from an IRenderingElement
     * @param yCoord an y coordinate from an IRenderingElement
     *
     * @return transformed coordinates
     */
    public int[] transformPoint(double xCoord, double yCoord) {
        double[] src = new double[]{xCoord,
                                    yCoord};
        double[] dest = new double[2];
        this.transform.transform(src, 0, dest, 0, 1);
        return new int[]{(int) dest[0],
                         (int) dest[1]};
    }

    /**
     * Creates the draw visitor with the specified writer. The writer will
     * have java script written to it which can then be pasted in a HTML page
     * to draw the structure
     *
     * @param writer the writer to use
     */
    public HTMLCanvasDrawVisitor(Writer writer) {
        this.writer = writer;
    }

    /**
     * Set the font manager (for now we're just using the AWT one)
     *
     * @inheritDoc
     */
    @Override
    public void setFontManager(IFontManager fontManager) {
        this.fontManager = (AWTFontManager) fontManager;
        try {
            setContextFont();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setRendererModel(RendererModel rendererModel) {
        this.rendererModel = rendererModel;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setTransform(AffineTransform transform) {
        this.transform = transform;
    }

    /**
     * Main visit method that delegates line and atom symbol rendering to the correct
     * 'visit' methods. Currently only ElementGroup, AtomSymbolElement and LineElement
     * are supported.
     *
     * @param element
     *
     * @see #visit(org.openscience.cdk.renderer.elements.AtomSymbolElement)
     * @see #visit(org.openscience.cdk.renderer.elements.LineElement)
     */
    @Override
    public void visit(IRenderingElement element) {

        if (element instanceof ElementGroup)
            ((ElementGroup) element).visitChildren(this);
        else if (element instanceof AtomSymbolElement)
            visit((AtomSymbolElement) element);
        else if (element instanceof LineElement)
            visit((LineElement) element);

    }


    /**
     * Creates the javascript that will draw the AtomSymbolElement on a canvas.
     * The js is written to the writer provided in the constructor.
     *
     * @param element an instance of an AtomSymbolElement
     */
    public void visit(AtomSymbolElement element) {

        StringBuilder builder = new StringBuilder();


        // context.fillText("C", 20, 20);
        int[] coordinates = transformPoint(element.xCoord, element.yCoord);


        try {
            writer.write("context.fillStyle=\"#FFFFFF\";");
            drawCircle(coordinates[0],
                       coordinates[1],
                       getBackingRadius(element.text, coordinates[0], coordinates[1], fontManager.getFont()));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // set the color context.fillStyle="#444444";
        builder.append("context.fillStyle=\"").append(toHexString(element.color)).append("\"\n");

        Point2D point = getTextBasePoint(element.text, element.xCoord, element.yCoord, fontManager.getFont());

        builder.append("context.fillText(\"").append(element.text).append("\",")
               .append(Integer.toString((int) point.getX())).append(",").append(Integer.toString((int) point.getY())).append(")\n");

        try {
            writer.write(builder.toString());
        } catch (IOException e) {
            System.err.println("Could not write to writer: " + e.getMessage());
        }

    }

    /**
     * uses the context.font to set the font on the canvas. The font is set
     * from the IFontManager.
     */
    private void setContextFont() throws IOException {
        Font font = fontManager.getFont();
        writer.write("context.font=\"");
        writer.write(Integer.toString(font.getSize()));
        writer.write("pt ");
        writer.write(font.getName());
        writer.write("\";");
    }

    /**
     * Creates the js to render a line element. The js is written to the writer provided in the
     * constructor.
     *
     * @param element instance of a line element
     */
    public void visit(LineElement element) {

        // transform the coordinates
        int[] first = transformPoint(element.firstPointX, element.firstPointY);
        int[] second = transformPoint(element.secondPointX, element.secondPointY);

        // draw the line using moveTo and lineTo
        try {
            writer.write("context.strokeStyle=\"#444444\";\n");
            int width = (int) (element.width * this.rendererModel.getParameter(BasicSceneGenerator.Scale.class).getValue());
            writer.write("context.lineWidth=" + Integer.toString(width < 0 ? 1 : width) + ";\n");
            drawLine(first[0], first[1], second[0], second[1]);
        } catch (IOException e) {
            System.err.println("Unable to draw line: " + e.getMessage());
        }

    }

    /**
     * Utility to draw a line from 4 points
     *
     * @param x1 first x coordinate
     * @param y1 first y coordinate
     * @param x2 second x coordinate
     * @param y2 second y coordinate
     *
     * @throws IOException low-level io exception if the writer could not write
     */
    public void drawLine(int x1, int y1, int x2, int y2) throws IOException {

        writer.write("context.moveTo(");
        writer.write(Integer.toString(x1));
        writer.write(",");
        writer.write(Integer.toString(y1));
        writer.write("); ");

        writer.write("context.lineTo(");
        writer.write(Integer.toString(x2));
        writer.write(",");
        writer.write(Integer.toString(y2));
        writer.write("); ");

        writer.write("context.stroke();\n");

    }

    /**
     * Utility to draw a circle (which is actually a filled arc).
     *
     * @param x      the x coordinate (centre)
     * @param y      the y coordinate (centre)
     * @param radius of the circle
     *
     * @throws IOException low-level io exception if the writer could not write
     */
    public void drawCircle(int x, int y, int radius) throws IOException {

        //        ctx.fillStyle="#FF0000";
//        ctx.beginPath();
//        ctx.arc(70,18,15,0,Math.PI*2,true);
//        ctx.closePath();
//        ctx.fill();

        writer.write("context.beginPath();");
        writer.write("context.arc(");
        writer.write(Integer.toString(x));
        writer.write(",");
        writer.write(Integer.toString(y));
        writer.write(",");
        writer.write(Integer.toString(radius));
        writer.write(",0,Math.PI*2,true");
        writer.write(");");
        writer.write("context.closePath();");
        writer.write("context.fill();");

    }

    /**
     * Used to draw a backing circle for atom element symbols.
     *
     * @param text   the symbol of the atom
     * @param xCoord centre x
     * @param yCoord centre y
     * @param font   the font it will be drawn in
     *
     * @return radius of the circle which will back the text
     */
    private int getBackingRadius(String text, double xCoord, double yCoord,
                                 Font font) {

        FontRenderContext context = new FontRenderContext(transform, true, false);

        LineMetrics metrics = font.getLineMetrics(text, context);

        return Math.max((int) font.getStringBounds(text, context).getWidth(), (int) metrics.getHeight()) / 2;
    }

    /**
     * Used to get the point where text should be drawn from (bottom left). This isn't perfect at the moment
     * and is still using Java Font Metrics to do the calculations.
     *
     * @param text   the value which will be drawn
     * @param xCoord x coordinate
     * @param yCoord y coordinate
     * @param font   the font which will be used
     *
     * @return adjusted location to bottom left of string.
     */
    private Point getTextBasePoint(String text, double xCoord, double yCoord,
                                   Font font) {
        FontRenderContext context = new FontRenderContext(transform, true, false);
        Rectangle2D stringBounds = font.getStringBounds(text, context);
        int[] point = this.transformPoint(xCoord, yCoord);
        int baseX = (int) (point[0] - (stringBounds.getWidth() / 2));

        LineMetrics metrics = font.getLineMetrics(text, context);

        // correct the baseline by the ascent
        int baseY = (int) (point[1] +
        (metrics.getAscent() - stringBounds.getHeight() / 2));
        return new Point(baseX, baseY);
    }

    /**
     * Converts a java color instance into it's HTML hex code equivalent.
     * For example Color.RED = #FF0000.
     *
     * @param color instance of color to convert
     *
     * @return the hex string value
     */
    private String toHexString(Color color) {

        StringBuilder hexColor = new StringBuilder("#");

        int[] components = new int[]{color.getRed(),
                                     color.getGreen(),
                                     color.getBlue()};

        for (int component : components) {

            if (component < 16)
                hexColor.append('0');

            hexColor.append(Integer.toHexString(component));

        }

        return hexColor.toString();

    }

}
