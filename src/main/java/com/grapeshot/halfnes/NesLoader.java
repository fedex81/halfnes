/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes;

import com.grapeshot.halfnes.ui.SwingUI;
import omegadrive.SystemLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;

public class NesLoader {

    private static Logger LOG = LogManager.getLogger(NesLoader.class.getSimpleName());

    private NesLoader() {}

    public static void main(String[] args) throws IOException {
        try {
            SystemLoader.main(args);
        } catch (Exception|Error e) {
            LOG.fatal("Unable to launch, args: " + Arrays.toString(args), e);
            System.err.println("Unable to launch, args: " + Arrays.toString(args));
            e.printStackTrace();
        }
    }

}
