package omegadrive.savestate;

import com.grapeshot.halfnes.CPU;
import com.grapeshot.halfnes.CPURAM;
import omegadrive.util.Util;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * NesStateHandler
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2020
 */
public class NesStateHandlerImpl implements NesStateHandler {

    private static int FILE_SIZE = 0x5000;
    private final static String fileExtension = "n00";

    private IntBuffer buf;
    private String fileName;
    private Type type;

    @Override
    public BaseStateHandler.Type getType() {
        return type;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public int[] getData() {
        return buf.array();
    }

    public static NesStateHandler createLoadInstance(String fileName){
        return createLoadInstance(fileName, new int[FILE_SIZE]);
    }

    public static NesStateHandler createLoadInstance(String fileName, int[] data){
        NesStateHandlerImpl n = new NesStateHandlerImpl();
        n.fileName = handleFileExtension(fileName);
        n.type = Type.LOAD;
        n.buf = IntBuffer.wrap(data);
        return n;
    }

    public static NesStateHandler createSaveInstance(String fileName){
        NesStateHandlerImpl n = new NesStateHandlerImpl();
        n.fileName = handleFileExtension(fileName);
        n.type = Type.SAVE;
        n.buf = IntBuffer.allocate(FILE_SIZE);
        return n;
    }

    private static String handleFileExtension(String fileName) {
        return fileName + (!fileName.toLowerCase().contains(".n0") ? "." + fileExtension : "");
    }

    @Override
    public void loadCpu(CPU cpu){
        cpu.interrupt = buf.get();
        cpu.bytetoflags(buf.get());
        cpu.setRegA(buf.get());
        cpu.setPC(buf.get());
        cpu.setS(buf.get());
        cpu.setRegX(buf.get());
        cpu.setY(buf.get());
    }

    @Override
    public void saveCpu(CPU cpu){
        buf.put(cpu.interrupt);
        buf.put(cpu.flagstobyte());
        buf.put(cpu.getA());
        buf.put(cpu.getPC());
        buf.put(cpu.getS());
        buf.put(cpu.getX());
        buf.put(cpu.getY());
    }

    @Override
    public void saveRam(CPURAM ram) {
        buf.put(ram.getWram());
    }

    @Override
    public void loadRam(CPURAM ram) {
        buf.get(ram.getWram());
    }
}
