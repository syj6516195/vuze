/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

/**
 * @author Olivier
 * 
 */
public class ImageRepository {

  private static HashMap images;

  static {
    images = new HashMap();
  }

  public static void loadImages(Display display) {
    loadImage(display, "org/gudy/azureus2/ui/icons/close.png", "close");
    loadImage(display, "org/gudy/azureus2/ui/icons/tray.png", "tray");
    loadImage(display, "org/gudy/azureus2/ui/icons/azureus.png", "azureus");
    loadImage(display, "org/gudy/azureus2/ui/icons/dragger.gif", "dragger");
    loadImage(display, "org/gudy/azureus2/ui/icons/folder.gif", "folder");
  }

  private static void loadImage(Display display, String res, String name) {
    InputStream is;
    Image im;
    is = ClassLoader.getSystemResourceAsStream(res);
    im = new Image(display, is);
    images.put(name, im);
  }

  public static void unLoadImages() {
    Iterator iter = images.values().iterator();
    while (iter.hasNext()) {
      Image im = (Image) iter.next();
      im.dispose();
    }
  }

  public static Image getImage(String name) {
    return (Image) images.get(name);
  }

  /**
     * Gets an image for a file associated with a given program
     *
     * @param program the Program
     */
  public static Image getIconFromProgram(Program program) {
    Image image = (Image) images.get(program);
    if (image == null) {
      if (program != null) {

        ImageData imageData = program.getImageData();
        if (imageData != null) {
          image = new Image(null, imageData,imageData.getTransparencyMask());
          images.put(program, image);
        }
      }
    }
    if (image == null) {
      image = (Image) images.get("folder");
    }
    return image;
  }
}
