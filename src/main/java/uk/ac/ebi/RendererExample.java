package uk.ac.ebi;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.HTMLCanvasDrawVisitor;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.font.IFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;

import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple main method that takes a mol file, loads it and prints to STDOUT the JS to render on
 * a HTML 5 canvas.
 *
 * Very basic piece of code before we can put some GWT goodness in.
 *
 * @author John May
 */
public class RendererExample {

    public static void main(String[] args) {

        IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();

        try {

            MDLV2000Reader reader = new MDLV2000Reader(new FileInputStream(args[0]));
            IAtomContainer container = reader.read(builder.newInstance(IAtomContainer.class));
            reader.close();

            List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();


            generators.add(new BasicSceneGenerator());
            generators.add(new BasicBondGenerator());
            generators.add(new BasicAtomGenerator());

            IFontManager manager = new AWTFontManager();

            AtomContainerRenderer renderer = new AtomContainerRenderer(generators,
                                                                       manager);

            StringWriter stringWriter = new StringWriter();
            renderer.paint(container, new HTMLCanvasDrawVisitor(stringWriter), new Rectangle2D.Double(0, 0, 512, 512), true);
            stringWriter.close();

            // js output
            System.out.println(stringWriter.toString());

        } catch (FileNotFoundException e) {
            System.err.println("File was not found: " + e.getMessage());
        } catch (CDKException e) {
            System.err.println("Unable to load molecule from file" + e.getMessage());
        } catch (IOException e) {
            System.out.println("Low-level IO exception occurred: " + e.getMessage());
        }

    }

}
