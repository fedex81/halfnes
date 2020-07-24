/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.ui;

import com.grapeshot.halfnes.PrefsSingleton;
import static com.grapeshot.halfnes.utils.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

/**
 *
 * @author Andrew, Zlika This class uses the JInput Java game controller API
 * (cf. http://java.net/projects/jinput).
 */
public class ControllerImpl implements ControllerInterface, KeyListener {

    public static boolean JINPUT_ENABLE = Boolean.valueOf(System.getProperty("halfnes.jinput.enable", "true"));

    private static Map<String, String> keyMap = new HashMap<>();

    static {
       keyMap.put("U", "keyUp");
       keyMap.put("D","keyDown");
       keyMap.put("L","keyLeft");
       keyMap.put("R","keyRight");
       keyMap.put("A","keyA");
       keyMap.put("B","keyB");
       keyMap.put("M","keySelect");
       keyMap.put("S","keyStart");
    }

    //private final java.awt.Component parent;
    private Controller gameController;
    private Component[] buttons;
    private final ScheduledExecutorService thread = Executors.newSingleThreadScheduledExecutor();
    private int latchbyte = 0, controllerbyte = 0, prevbyte = 0, outbyte = 0, gamepadbyte = 0;
    private final HashMap<Integer, Integer> m = new HashMap<>(10);
    private final int controllernum;

    public static boolean verbose = false;

    //back-compat
    public ControllerImpl(final int controllernum) {
        this(controllernum, Collections.emptyMap());
    }

    public ControllerImpl(final java.awt.Component parent, final int controllernum) {
        this(controllernum, Collections.emptyMap());
        parent.addKeyListener(this);
    }

    public ControllerImpl(final int controllernum, Map<String, Integer> pKeyMap) {
        if ((controllernum != 0) && (controllernum != 1)) {
            throw new IllegalArgumentException("controllerNum must be 0 or 1");
        }
        this.controllernum = controllernum;
        setKeyMappings(controllernum, pKeyMap);
        setButtons();
    }


    @Override
    public void keyPressed(final KeyEvent keyEvent) {
        pressKey(keyEvent.getKeyCode());
    }

    private void pressKey(int keyCode) {
        //enable the byte of whatever is found
        prevbyte = controllerbyte;
        if (!m.containsKey(keyCode)) {
            return;
        }
        //enable the corresponding bit to the key
        controllerbyte |= m.get(keyCode);
        //special case: if up and down are pressed at once, use whichever was pressed previously
        if ((controllerbyte & (BIT4 | BIT5)) == (BIT4 | BIT5)) {
            controllerbyte &= ~(BIT4 | BIT5);
            controllerbyte |= (prevbyte & ~(BIT4 | BIT5));
        }
        //same for left and right
        if ((controllerbyte & (BIT6 | BIT7)) == (BIT6 | BIT7)) {
            controllerbyte &= ~(BIT6 | BIT7);
            controllerbyte |= (prevbyte & ~(BIT6 | BIT7));
        }
    }

    @Override
    public void keyReleased(final KeyEvent keyEvent) {
        releaseKey(keyEvent.getKeyCode());
    }

    private void releaseKey(int keyCode) {
        prevbyte = controllerbyte;
        if (!m.containsKey(keyCode)) {
            return;
        }
        controllerbyte &= ~m.get(keyCode);
    }

    @Override
    public int getbyte() {
        return outbyte;
    }

    @Override
    public int peekOutput() {
        return latchbyte;
    }

    @Override
    public void keyTyped(final KeyEvent arg0) {
        // TODO Auto-generated method stub
    }

    public void strobe() {
        //shifts a byte out
        outbyte = latchbyte & 1;
        latchbyte = ((latchbyte >> 1) | 0x100);
    }

    public void output(final boolean state) {
        latchbyte = gamepadbyte | controllerbyte;
    }

    /**
     * Start in a separate thread the processing of the controller event queue.
     * Must be called after construction of the class to enable the processing
     * of the joystick / gamepad events.
     */
    public void startEventQueue() {
//        if (System.getProperty("java.class.path").contains("jinput")) {
        thread.execute(eventQueueLoop());
//        }
    }
    double threshold = 0.25;

    private Runnable eventQueueLoop() {
        return new Runnable() {
            @Override
            public void run() {
                if (gameController != null) {
                    Event event = new Event();
                    while (!Thread.interrupted()) {
                        gameController.poll();
                        EventQueue queue = gameController.getEventQueue();
                        while (queue.getNextEvent(event)) {
                            Component component = event.getComponent();
                            if (component.getIdentifier() == Component.Identifier.Axis.X) {
                                if (event.getValue() > threshold) {
                                    gamepadbyte |= BIT7;//left on, right off
                                    gamepadbyte &= ~BIT6;

                                } else if (event.getValue() < -threshold) {
                                    gamepadbyte |= BIT6;
                                    gamepadbyte &= ~BIT7;
                                } else {
                                    gamepadbyte &= ~(BIT7 | BIT6);
                                }
                            } else if (component.getIdentifier() == Component.Identifier.Axis.Y) {
                                if (event.getValue() > threshold) {
                                    gamepadbyte |= BIT5;//up on, down off
                                    gamepadbyte &= ~BIT4;
                                } else if (event.getValue() < -threshold) {
                                    gamepadbyte |= BIT4;//down on, up off
                                    gamepadbyte &= ~BIT5;
                                } else {
                                    gamepadbyte &= ~(BIT4 | BIT5);
                                }
                            } else if (component == buttons[0]) {
                                if (isPressed(event)) {
                                    gamepadbyte |= BIT0;
                                } else {
                                    gamepadbyte &= ~BIT0;
                                }
                            } else if (component == buttons[1]) {
                                if (isPressed(event)) {
                                    gamepadbyte |= BIT1;
                                } else {
                                    gamepadbyte &= ~BIT1;
                                }
                            } else if (component == buttons[2]) {
                                if (isPressed(event)) {
                                    gamepadbyte |= BIT2;
                                } else {
                                    gamepadbyte &= ~BIT2;
                                }
                            } else if (component == buttons[3]) {
                                if (isPressed(event)) {
                                    gamepadbyte |= BIT3;
                                } else {
                                    gamepadbyte &= ~BIT3;
                                }
                            }
                        }

                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            // Preserve interrupt status
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        };
    }

    private boolean isPressed(Event event) {
        Component component = event.getComponent();
        if (component.isAnalog()) {
            if (Math.abs(event.getValue()) > 0.2f) {
                return true;
            } else {
                return false;
            }
        } else if (event.getValue() == 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Stop the controller event queue thread. Must be called before closing the
     * application.
     */
    public void stopEventQueue() {
        thread.shutdownNow();
    }

    /**
     * This method detects the available joysticks / gamepads on the computer
     * and return them in a list.
     *
     * @return List of available joysticks / gamepads connected to the computer
     */
    private static Controller[] getAvailablePadControllers() {
        List<Controller> gameControllers = new ArrayList<>();
        if(!JINPUT_ENABLE){
            return new Controller[0];
        }
        // Get a list of the controllers JInput knows about and can interact
        // with
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        // Check the useable controllers (gamepads or joysticks with at least 2
        // axis and 2 buttons)
        for (Controller controller : controllers) {
            if ((controller.getType() == Controller.Type.GAMEPAD) || (controller.getType() == Controller.Type.STICK)) {
                int nbOfAxis = 0;
                // Get this controllers components (buttons and axis)
                Component[] components = controller.getComponents();
                // Check the availability of X/Y axis and at least 2 buttons
                // (for A and B, because select and start can use the keyboard)
                for (Component component : components) {
                    if ((component.getIdentifier() == Component.Identifier.Axis.X)
                            || (component.getIdentifier() == Component.Identifier.Axis.Y)) {
                        nbOfAxis++;
                    }
                }
                if ((nbOfAxis >= 2) && (getButtons(controller).length >= 2)) {
                    // Valid game controller
                    gameControllers.add(controller);
                }
            }
        }
        return gameControllers.toArray(new Controller[0]);
    }

    /**
     * Return the available buttons on this controller (by priority order).
     */
    private static Component[] getButtons(Controller controller) {
        List<Component> buttons = new ArrayList<>();
        // Get this controllers components (buttons and axis)
        Component[] components = controller.getComponents();
        for (Component component : components) {
            if (component.getIdentifier() instanceof Component.Identifier.Button) {
                buttons.add(component);
            }
        }
        return buttons.toArray(new Component[0]);
    }


    private void setKeyMappings(int pNum, Map<String, Integer> pKeyMap) {
        Preferences prefs = PrefsSingleton.get();
        String tail = "" + (pNum+1); //1-based
        StringBuilder sb = new StringBuilder();
        pKeyMap.entrySet().forEach(e -> {
            String k = keyMap.getOrDefault(e.getKey(), null);
            if( k != null){
                prefs.putInt(k+tail, e.getValue());
                sb.append(k + tail + "->" + KeyEvent.getKeyText(e.getValue()) + ", ");
            }
        });
        if(verbose){
            System.out.println("Setting player " + tail + " key mappings: " + sb.toString());
        }
    }

    public final void setButtons() {
        Preferences prefs = PrefsSingleton.get();
        //reset the buttons from prefs
        m.clear();
        switch (controllernum) {
            case 0:
                m.put(prefs.getInt("keyUp1", KeyEvent.VK_UP), BIT4);
                m.put(prefs.getInt("keyDown1", KeyEvent.VK_DOWN), BIT5);
                m.put(prefs.getInt("keyLeft1", KeyEvent.VK_LEFT), BIT6);
                m.put(prefs.getInt("keyRight1", KeyEvent.VK_RIGHT), BIT7);
                m.put(prefs.getInt("keyA1", KeyEvent.VK_I), BIT0);
                m.put(prefs.getInt("keyB1", KeyEvent.VK_O), BIT1);
                m.put(prefs.getInt("keySelect1", KeyEvent.VK_L), BIT2);
                m.put(prefs.getInt("keyStart1", KeyEvent.VK_ENTER), BIT3);
                break;
            case 1:
            default:
                m.put(prefs.getInt("keyUp2", KeyEvent.VK_W), BIT4);
                m.put(prefs.getInt("keyDown2", KeyEvent.VK_S), BIT5);
                m.put(prefs.getInt("keyLeft2", KeyEvent.VK_A), BIT6);
                m.put(prefs.getInt("keyRight2", KeyEvent.VK_D), BIT7);
                m.put(prefs.getInt("keyA2", KeyEvent.VK_C), BIT0);
                m.put(prefs.getInt("keyB2", KeyEvent.VK_V), BIT1);
                m.put(prefs.getInt("keySelect2", KeyEvent.VK_N), BIT2);
                m.put(prefs.getInt("keyStart2", KeyEvent.VK_SPACE), BIT3);
                break;

        }
        if(JINPUT_ENABLE) {
            Controller[] controllers = getAvailablePadControllers();
            if (controllers.length > controllernum) {
                this.gameController = controllers[controllernum];
                PrefsSingleton.get().put("controller" + controllernum, gameController.getName());
                System.err.println(controllernum + 1 + ". " + gameController.getName());
                this.buttons = getButtons(controllers[controllernum]);
            } else {
                PrefsSingleton.get().put("controller" + controllernum, "");
                this.gameController = null;
                this.buttons = null;
            }
        }
    }
}
