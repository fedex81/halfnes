/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;
import com.grapeshot.halfnes.PPU;
import java.util.Arrays;
import java.util.prefs.Preferences;
import java.util.zip.CRC32;

public abstract class Mapper {

    protected ROMLoader loader;
    protected int mappertype, submapper, prgsize, prgoff, chroff, chrsize;
    public CPU cpu;
    public CPURAM cpuram;
    public PPU ppu;
    protected int[] prg, chr, chr_map, prg_map, prgram = new int[8192];
    protected MirrorType scrolltype;
    protected boolean haschrram = false, hasprgram = true, savesram = false;
    // PPU nametables
    protected final int[] pput0 = new int[0x400], pput1 = new int[0x400],
            pput2 = new int[0x400], pput3 = new int[0x400];
    //99% of games only use 2 of these, but we have to create 4 and use ptrs to them
    //for those with extra RAM for 4 screen mirror
    protected int[] nt0, nt1, nt2, nt3;
    //and these are pointers to the nametables, so  for singlescreen when we switch
    //and then switch back the data in the other singlescreen NT isn't gone.
    long crc;
    TVType region;
    Preferences prefs = PrefsSingleton.get();

    public boolean supportsSaves() {
        return savesram;
    }

    public void destroy() {
        cpu = null;
        cpuram = null;
        ppu = null;
    }

    public static enum MirrorType {

        H_MIRROR, V_MIRROR, SS_MIRROR0, SS_MIRROR1, FOUR_SCREEN_MIRROR
    };

    public static enum TVType {

        NTSC,
        PAL,
        DENDY;
    }

    public static long crc32(int[] array) {
        CRC32 c = new CRC32();
        for (int i : array) {
            c.update(i);
        }
        return c.getValue();
    }

    public void loadrom() throws BadMapperException {
        loader.parseHeader();
        prgsize = loader.prgsize;
        mappertype = loader.mappertype;
        prgoff = loader.prgoff;
        chroff = loader.chroff;
        chrsize = loader.chrsize;
        scrolltype = loader.scrolltype;
        savesram = loader.savesram;
        prg = loader.load(prgsize, prgoff);
        region = loader.tvtype;
        submapper = loader.submapper;
        crc = crc32(prg);
        //System.err.println(utils.hex(crc));
        //crc "database" for certain impossible-to-recognize games
        if ((crc == 0x41243492L) //low g man (u)
                || (crc == 0x98CCD385L)//low g man (e)
                ) {
            hasprgram = false;
        }
        chr = loader.load(chrsize, chroff);

        if (chrsize == 0) {//chr ram
            haschrram = true;
            chrsize = 8192;
            chr = new int[8192];
        }
        prg_map = new int[32];
        for (int i = 0; i < 32; ++i) {
            prg_map[i] = (1024 * i) & (prgsize - 1);
        }
        chr_map = new int[8];
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
        cpuram = new CPURAM(this);
        cpu = new CPU(cpuram);
        ppu = new PPU(this);
        Arrays.fill(pput0, 0xa0);
        Arrays.fill(pput1, 0xb0);
        Arrays.fill(pput2, 0xc0);
        Arrays.fill(pput3, 0xd0);
        setmirroring(scrolltype);
    }

    public void reset() {
        //this is empty so that mappers w/o some specific instructions
        //on soft reset need not implement this
    }

    //write into the cartridge's address space
    public void cartWrite(final int addr, final int data) {
        //default no-mapper operation just writes if in PRG RAM range
        if (addr >= 0x6000 && addr < 0x8000) {
            prgram[addr & 0x1fff] = data;
        }
    }

    public int cartRead(final int addr) {
        // by default has wram at 0x6000 and cartridge at 0x8000-0xfff
        // but some mappers have different so override for those
        if (addr >= 0x8000) {
            return prg[prg_map[((addr & 0x7fff)) >> 10] + (addr & 1023)];
        } else if (addr >= 0x6000 && hasprgram) {
            return prgram[addr & 0x1fff];
        }
        return addr >> 8; //open bus
    }

    public int ppuRead(int addr) {
        if (addr < 0x2000) {
            return chr[chr_map[addr >> 10] + (addr & 1023)];
        } else {
            switch (addr & 0xc00) {
                case 0:
                    return nt0[addr & 0x3ff];
                case 0x400:
                    return nt1[addr & 0x3ff];
                case 0x800:
                    return nt2[addr & 0x3ff];
                case 0xc00:
                default:
                    if (addr >= 0x3f00) {
                        addr &= 0x1f;
                        if (addr >= 0x10 && ((addr & 3) == 0)) {
                            addr -= 0x10;
                        }
                        return ppu.pal[addr];
                    } else {
                        return nt3[addr & 0x3ff];
                    }
            }
        }
    }

    public void ppuWrite(int addr, final int data) {
        addr &= 0x3fff;
        if (addr < 0x2000) {
            if (haschrram) {
                // Shame on you, Milon's Secret Castle. What possible
                // reason could you have to write to your own chr rom?
                chr[chr_map[addr >> 10] + (addr & 1023)] = data;
                // anyway, only allowing writes when there's actual ram here.
            }
        } else {
            switch (addr & 0xc00) {
                case 0x0:
                    nt0[addr & 0x3ff] = data;
                    break;
                case 0x400:
                    nt1[addr & 0x3ff] = data;
                    break;
                case 0x800:
                    nt2[addr & 0x3ff] = data;
                    break;
                case 0xc00:
                    if (addr >= 0x3f00 && addr <= 0x3fff) {
                        addr &= 0x1f;
                        //System.err.println("wrote "+utils.hex(data)+" to palette index " + utils.hex(addr));
                        if (addr >= 0x10 && ((addr & 3) == 0)) { //0x10,0x14,0x18 etc are mirrors of 0x0, 0x4,0x8 etc
                            addr -= 0x10;
                        }
                        ppu.pal[addr] = (data & 0x3f);
                    } else {
                        nt3[addr & 0x3ff] = data;
                    }
                    break;
                default:
                    System.err.println("where?");
            }
        }
    }

    public void notifyscanline(final int scanline) {
        //this is empty so that mappers w/o a scanline counter need not implement
    }

    public void cpucycle(int cycles) {
        //do it right for once
    }

    public String getrominfo() {
        return ("ROM INFO: \n"
                + "Filename:     " + loader.name + "\n"
                + "Mapper:       " + mappertype + "\n"
                + "PRG Size:     " + prgsize / 1024 + " K\n"
                + "CHR Size:     " + (haschrram ? 0 : chrsize / 1024) + " K\n"
                + "Mirroring:    " + scrolltype.toString() + "\n"
                + "Battery Save: " + ((savesram) ? "Yes" : "No")) + "\n"
                + "CRC:          " + utils.hex(this.crc);
    }

    public boolean hasSRAM() {
        return savesram;
    }

    public void setLoader(final ROMLoader l) {
        loader = l;
    }

    public CPURAM getCPURAM() {
        return cpuram;
    }

    public void checkA12(int addr) {
        //needed for mmc3 irq counter
    }

    public void setPRGRAM(final int[] newprgram) {
        prgram = newprgram;

    }

    public int[] getPRGRam() {
        return prgram;
    }

    public final void setmirroring(final Mapper.MirrorType type) {
        switch (type) {
            case H_MIRROR:
                nt0 = pput0;
                nt1 = pput0;
                nt2 = pput1;
                nt3 = pput1;
                break;
            case V_MIRROR:
                nt0 = pput0;
                nt1 = pput1;
                nt2 = pput0;
                nt3 = pput1;

                break;
            case SS_MIRROR0:
                nt0 = pput0;
                nt1 = pput0;
                nt2 = pput0;
                nt3 = pput0;
                break;
            case SS_MIRROR1:
                nt0 = pput1;
                nt1 = pput1;
                nt2 = pput1;
                nt3 = pput1;
                break;
            case FOUR_SCREEN_MIRROR:
            default:
                nt0 = pput0;
                nt1 = pput1;
                nt2 = pput2;
                nt3 = pput3;
                break;
        }
        scrolltype = type;
    }

    public TVType getTVType() {
        int prefsregion = prefs.getInt("region", 0);
        switch (prefsregion) {
            case 0:
            default://auto detect
                return region;
            case 1: //ntsc
                return TVType.NTSC;
            case 2: //pal
                return TVType.PAL;
            case 3: //dendy
                return TVType.DENDY;
        }
    }

    public void setTVType(int val){
        prefs.putInt("region", val);
    }

    public MirrorType getScrolltype() {
        return scrolltype;
    }

    public static MirrorType getScrolltype(int val) {
        return MirrorType.values()[val];
    }

    public boolean hasChrRam() {
        return haschrram;
    }

    public boolean hasPrgRam() {
        return hasprgram;
    }

    public int[] getChr() {
        return chr;
    }

    public int[] getPputN(int n) {
        switch (n){
            case 0: return pput0;
            case 1: return pput1;
            case 2: return pput2;
            case 3: return pput3;
            default: break;
        }
        return null;
    }

    public void init() {
    } //needed for NSF initialization
}
