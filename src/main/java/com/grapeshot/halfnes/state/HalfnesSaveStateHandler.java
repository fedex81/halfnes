package com.grapeshot.halfnes.state;

import com.grapeshot.halfnes.*;
import com.grapeshot.halfnes.mappers.Mapper;
import com.grapeshot.halfnes.mappers.MapperHelper;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * HalfnesSavestateHandler
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2020
 */
public class HalfnesSaveStateHandler {

    private static int FILE_SIZE = 0x20_000;

    private ByteBuffer buf = ByteBuffer.allocate(FILE_SIZE);

    private static void putInt(ByteBuffer buffer, int[] data) {
        Arrays.stream(data).forEach(d -> buffer.putInt(d));
    }

    private static void getInt(ByteBuffer buffer, int[] data) {
        IntStream.range(0, data.length).forEach(i -> data[i] = buffer.getInt());
    }

    public byte[] getSaveStateData(NES nes) {
        buf.clear();
        processState(Type.SAVE, nes);
        return buf.array();
    }

    public void setSaveStateData(NES nes, byte[] data) {
        buf = ByteBuffer.wrap(data);
        processState(Type.LOAD, nes);
    }

    public void processState(Type type, CPU cpu, PPU ppu, APU apu, CPURAM ram) {
        if (type == Type.LOAD) {
            loadCpu(cpu);
            loadPpu(ppu);
            loadApu(apu);
            loadRam(ram);
        } else {
            saveCpu(cpu);
            savePpu(ppu);
            saveApu(apu);
            saveRam(ram);
        }
    }

    private void processState(Type type, NES nes) {
        CPU cpu = nes.getCPU();
        PPU ppu = nes.getPpu();
        APU apu = nes.getApu();
        CPURAM ram = nes.getCPURAM();
        processState(type, cpu, ppu, apu, ram);
    }

    private void loadCpu(CPU cpu) {
        cpu.setRegA(buf.getInt());
        cpu.setPC(buf.getInt());
        cpu.setS(buf.getInt());
        cpu.setRegX(buf.getInt());
        cpu.setY(buf.getInt());
        cpu.bytetoflags(buf.getInt());
        cpu.interrupt = buf.getInt();
        cpu.interruptDelay = buf.getInt() != 0;
        cpu.nmiNext = buf.getInt() != 0;
        cpu.clocks = buf.getInt();
        cpu.cycles = buf.getInt();
    }

    private void saveCpu(CPU cpu) {
        buf.putInt(cpu.getA());
        buf.putInt(cpu.getPC());
        buf.putInt(cpu.getS());
        buf.putInt(cpu.getX());
        buf.putInt(cpu.getY());
        buf.putInt(cpu.flagstobyte());
        buf.putInt(cpu.interrupt);
        buf.putInt(cpu.interruptDelay ? 1 : 0);
        buf.putInt(cpu.nmiNext ? 1 : 0);
        buf.putInt(cpu.clocks);
        buf.putInt(cpu.cycles);
    }

    private void loadPpu(PPU ppu) {
        int[] ppuReg = ppu.getPpuReg();
        IntStream.range(0, ppuReg.length).forEach(i -> ppuReg[i] = buf.getInt());
        ppu.even = buf.getInt() != 0;
        ppu.openbus = buf.getInt();
        ppu.readbuffer = buf.getInt();
        ppu.vblankflag = buf.getInt() != 0;
        ppu.loopyX = buf.getInt();
        ppu.loopyV = buf.getInt();
        int loopyT = buf.getInt();
        getInt(buf, ppu.getOAM());
        getInt(buf, ppu.getPalette());
        loadMapper(ppu.mapper);

        ppu.setParameters();
        ppu.init();
        ppu.loopyT = loopyT;
    }

    private void savePpu(PPU ppu) {
        putInt(buf, ppu.getPpuReg());
        buf.putInt(ppu.even ? 1 : 0);
        buf.putInt(ppu.openbus);
        buf.putInt(ppu.readbuffer);
        buf.putInt(ppu.vblankflag ? 1 : 0);
        buf.putInt(ppu.loopyX);
        buf.putInt(ppu.loopyV);
        buf.putInt(ppu.loopyT);
        putInt(buf, ppu.getOAM());
        putInt(buf, ppu.getPalette());
        saveMapper(ppu.mapper);
    }

    private void loadMapper(Mapper mapper) {
        mapper.setTVType(buf.getInt());
        Mapper.MirrorType type = Mapper.getScrolltype(buf.getInt());
        if (mapper.hasPrgRam()) {
            getInt(buf, mapper.getPRGRam());
        }
        if (mapper.hasChrRam()) {
            getInt(buf, mapper.getChr());
        }
        getInt(buf, mapper.getPputN(0));
        getInt(buf, mapper.getPputN(1));
        getInt(buf, mapper.getPputN(2));
        getInt(buf, mapper.getPputN(3));

        if (mapper instanceof MapperHelper.MapperState) {
            MapperHelper.MapperState<Integer> mapperState = (MapperHelper.MapperState<Integer>) mapper;
            mapperState.loadState(buf.getInt());
        }

        mapper.setmirroring(type);
    }

    private void saveMapper(Mapper mapper) {
        buf.putInt(mapper.getTVType().ordinal());
        buf.putInt(mapper.getScrolltype().ordinal());
        if (mapper.hasPrgRam()) {
            putInt(buf, mapper.getPRGRam());
        }
        if (mapper.hasChrRam()) {
            putInt(buf, mapper.getChr());
        }
        putInt(buf, mapper.getPputN(0));
        putInt(buf, mapper.getPputN(1));
        putInt(buf, mapper.getPputN(2));
        putInt(buf, mapper.getPputN(3));

        if (mapper instanceof MapperHelper.MapperState) {
            MapperHelper.MapperState<Integer> mapperState = (MapperHelper.MapperState<Integer>) mapper;
            buf.putInt(mapperState.saveState());
        }
    }

    public void setBuf(ByteBuffer buf) {
        this.buf = buf;
    }

    private void saveRam(CPURAM ram) {
        putInt(buf, ram.getWram());
    }

    private void loadRam(CPURAM ram) {
        getInt(buf, ram.getWram());
    }

    private void loadApu(APU cpu) {

    }

    private void saveApu(APU cpu) {

    }

    public enum Type {SAVE, LOAD}


}
