package omegadrive.savestate;

import com.grapeshot.halfnes.CPU;
import com.grapeshot.halfnes.CPURAM;
import com.grapeshot.halfnes.NES;

/**
 * NesStateHandler
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2020
 */
public interface NesStateHandler extends BaseStateHandler {

    void loadCpu(CPU cpu);

    void saveCpu(CPU cpu);

    void saveRam(CPURAM ram);

    void loadRam(CPURAM ram);

    default void processState(NES nes){
        CPU cpu = nes.getCPU();
        CPURAM ram = nes.getCPURAM();
        if(getType() == Type.LOAD){
            loadCpu(cpu);
            loadRam(ram);
        } else {
            saveCpu(cpu);
            saveRam(ram);
        }
    }

    NesStateHandler EMPTY_STATE = new NesStateHandler() {
        @Override
        public void loadCpu(CPU cpu) {

        }

        @Override
        public void saveCpu(CPU cpu) {

        }

        @Override
        public void saveRam(CPURAM ram) {

        }

        @Override
        public void loadRam(CPURAM ram) {

        }

        @Override
        public Type getType() {
            return null;
        }

        @Override
        public String getFileName() {
            return null;
        }

        @Override
        public int[] getData() {
            return new int[0];
        }
    };
}
