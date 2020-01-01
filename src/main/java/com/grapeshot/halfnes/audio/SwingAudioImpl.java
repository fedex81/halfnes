/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.audio;

import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.PrefsSingleton;
import com.grapeshot.halfnes.mappers.Mapper;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author Andrew
 */
public class SwingAudioImpl implements AudioOutInterface {

    private boolean soundEnable;
    private SourceDataLine sdl;
    private volatile byte[] audiobuf;
    private volatile int bufptr = 0;
    protected float outputvol;
    protected int samplesPerFrame;

    final static int CHANNELS = 1;
    final static int SAMPLE_SIZE = 16;
    final static int BYTES_PER_SAMPLE = SAMPLE_SIZE/8;


    public SwingAudioImpl(final NES nes, final int samplerate, Mapper.TVType tvtype) {
        soundEnable = PrefsSingleton.get().getBoolean("soundEnable", true);
        outputvol = (float) (PrefsSingleton.get().getInt("outputvol", 13107) / 16384.);
        double fps;
        switch (tvtype) {
            case NTSC:
            default:
                fps = 60.;
                break;
            case PAL:
            case DENDY:
                fps = 50.;
                break;
        }
        if (soundEnable) {
            samplesPerFrame = (int) Math.ceil((samplerate * CHANNELS) / fps);
            audiobuf = new byte[samplesPerFrame * BYTES_PER_SAMPLE];
            try {
                AudioFormat af = new AudioFormat(
                        samplerate,
                        SAMPLE_SIZE,//bit
                        CHANNELS,//channel
                        true,//signed
                        false //little endian
                //(works everywhere, afaict, but macs need 44100 sample rate)
                );
                sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af, samplesPerFrame * 2 * CHANNELS * BYTES_PER_SAMPLE);
                //create 4 frame audio buffer
                sdl.start();
            } catch (LineUnavailableException a) {
                System.err.println(a);
                nes.messageBox("Unable to inintialize sound.");
                soundEnable = false;
            } catch (IllegalArgumentException a) {
                System.err.println(a);
                nes.messageBox("Unable to inintialize sound.");
                soundEnable = false;
            }
        }
    }

    @Override
    public void flushFrame(final boolean waitIfBufferFull) {
        if (soundEnable) {

//            if (sdl.available() == sdl.getBufferSize()) {
//                System.err.println("Audio is underrun");
//            }
            if (sdl.available() < bufptr) {
//                System.err.println("Audio is blocking");
                if (waitIfBufferFull) {

                    //write to audio buffer and don't worry if it blocks
                    sdl.write(audiobuf, 0, bufptr);
                }
                //else don't bother to write if the buffer is full
            } else {
                sdl.write(audiobuf, 0, bufptr);
            }
        }
        bufptr = 0;

    }

    @Override
    public void outputSample(int sample) {
        if (soundEnable) {
            sample *= outputvol;
            if (sample < -32768) {
                sample = -32768;
                //System.err.println("clip");
            }
            if (sample > 32767) {
                sample = 32767;
                //System.err.println("clop");
            }
            //mono
            audiobuf[bufptr] = (byte) (sample & 0xff);
            audiobuf[bufptr + 1] = (byte) ((sample >> 8) & 0xff);
            bufptr += 2;
        }
    }

    @Override
    public void pause() {
        if (soundEnable) {
            sdl.flush();
            sdl.stop();
        }
    }

    @Override
    public void resume() {
        if (soundEnable) {
            sdl.start();
        }
    }

    @Override
    public final void destroy() {
        if (soundEnable) {
            sdl.stop();
            sdl.close();
        }
    }

    public final boolean bufferHasLessThan(final int samples) {
        //returns true if the audio buffer has less than the specified amt of samples remaining in it
        return (sdl == null) ? false : ((sdl.getBufferSize() - sdl.available()) <= samples);
    }
}
