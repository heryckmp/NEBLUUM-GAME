import javax.sound.sampled.*;

class SoundPlayer {
    boolean available = true;
    long lastSoundTime = 0;

    void playBeep(boolean high) {
        if (!available) return;
        long now = System.currentTimeMillis();
        if (now - lastSoundTime < 50) return;
        lastSoundTime = now;

        try {
            AudioFormat format = new AudioFormat(8000, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, 400);
            line.start();

            int freq = high ? 1000 : 500;
            byte[] buf = new byte[400];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte)(Math.sin(2 * Math.PI * freq * i / 8000.0) * 80);
            }
            line.write(buf, 0, buf.length);
            line.stop();
            line.close();
        } catch (Exception e) { available = false; }
    }

    void playCoin() { playBeep(true); }
    void playHit() { playBeep(false); }
    void playHurt() { playBeep(false); }
    void playDeath() { playBeep(false); }
    void playLevelUp() { playBeep(true); }
}
