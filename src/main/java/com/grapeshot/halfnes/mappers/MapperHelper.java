package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.ROMLoader;

import java.util.Optional;

/**
 * MapperHelper
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2020
 */
public class MapperHelper {

    public static interface MapperState<T> {
        default void loadState(T state){/*DO NOTHING*/}

        default T saveState(){ return null;}
    }

    public static Mapper getCorrectMapper(final ROMLoader l) throws BadMapperException {
        int type = l.mappertype;
        boolean haschr = (l.chrsize == 0);
        switch (type) {
            case -1:
                return new NSFMapper();
            case 0:
                return new NromMapper();
            case 1:
                return new MMC1Mapper();
            case 2:
                return new UnromMapper();
            case 3:
                return new CnromMapper();
            case 4:
                return new MMC3Mapper();
            case 5:
                return new MMC5Mapper();
            case 7:
                return new AnromMapper();
            case 9:
                return new MMC2Mapper();
            case 10:
                return new MMC4Mapper();
            case 11:
                return new ColorDreamsMapper();
            case 15:
            case 169:
                return new Mapper15();
            case 19:
                return new NamcoMapper();
            case 21:
            case 23:
            case 25:
                //VRC4 has three different mapper numbers for six different address line layouts
                //some of which really should be VRC2
                //but they're all handled in the same file
                //there's a proposal for submapper #s in iNES 2.0
                return new VRC4Mapper(type);
            case 22:
                return new VRC2Mapper();
            case 24:
            case 26:
                return new VRC6Mapper(type);
            case 31:
                return new Mapper31();
            case 33:
                return new Mapper33();
            case 34:
                if (haschr) {
                    return new BnromMapper();
                } else {
                    return new NINA_001_Mapper();
                }
            case 36:
                return new Mapper36();
            case 38:
                return new CrimeBustersMapper();
            case 41:
                return new CaltronMapper();
            case 47:
                return new Mapper47();
            case 48:
                return new Mapper48();
            case 58:
                return new Mapper58();
            case 60:
                return new Mapper60();
            case 61:
                return new Mapper61();
            case 62:
                return new Mapper62();
            case 64:
                return new TengenRamboMapper();
            case 65:
                return new IremH3001Mapper();
            case 66:
                return new GnromMapper();
            case 67:
                return new Sunsoft03Mapper();
            case 68:
                return new AfterburnerMapper();
            case 69:
                return new FME7Mapper();
            case 70:
                return new Mapper70();
            case 71:
                return new CodemastersMapper();
            case 72:
                return new Mapper72();
            case 73:
                return new VRC3Mapper();
            case 75:
                return new VRC1Mapper();
            case 76:
                return new Mapper76();
            case 78:
                return new Mapper78();
            case 79:
            case 113:
                return new NINA_003_006_Mapper(type);
            case 85:
                return new VRC7Mapper();
            case 86:
                return new Mapper86();
            case 87:
                return new Mapper87();
            case 88:
            case 154:
                return new Namcot34x3Mapper(type);
            case 89:
            case 93:
                return new Sunsoft02Mapper(type);
            case 92:
                return new Mapper92();
            case 94:
                return new Mapper94();
            case 97:
                return new Mapper97();
            case 107:
                return new Mapper107();
            case 112:
                return new Mapper112();
            case 119:
                return new Mapper119();
            case 140:
                return new Mapper140();
            case 152:
                return new Mapper152();
            case 180:
                return new CrazyClimberMapper();
            case 182:
                return new Mapper182();
            case 184:
                return new Sunsoft01Mapper();
            case 185:
                return new Mapper185();
            case 200:
                return new Mapper200();
            case 201:
                return new Mapper201();
            case 203:
                return new Mapper203();
            case 206:
                return new MIMICMapper();
            case 212:
                return new Mapper212();
            case 213:
                return new Mapper213();
            case 214:
                return new Mapper214();
            case 225:
                return new Mapper225();
            case 226:
                return new Mapper226();
            case 228:
                return new Action52Mapper();
            case 229:
                return new Mapper229();
            case 231:
                return new Mapper231();
            case 240:
                return new Mapper240();
            case 241:
                return new Mapper241();
            case 242:
                return new Mapper242();
            case 244:
                return new Mapper244();
            case 246:
                return new Mapper246();
            case 255:
                return new Mapper255();
            default:
                System.err.println("unsupported mapper # " + type);
                throw new BadMapperException("Unsupported mapper: " + type);
        }
    }
}
