package com.one.audio_recorder_testing;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Environment;
import android.renderscript.Byte2;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Recorder {

    static final String TAG = "myTest";
    public boolean isRecording = false;
    private AudioRecord mRecorder;

    private static final int RECORDER_SAMPLERATE=44100;
    private static final int RECORDER_CHANNELS= AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private Thread recordingThread;
    private int bufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private int bytesPerElement = 2; // 2 bytes in 16bit format

    private File root;
    private File cache;
    private File rawOutput;



    @RequiresApi(api = Build.VERSION_CODES.Q)
    boolean start(Context context, MediaProjection mProjection){

        if(mRecorder == null){
            AudioPlaybackCaptureConfiguration config;
            try {
                config = new AudioPlaybackCaptureConfiguration.Builder(mProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build();

            }catch (NoClassDefFoundError e){
                Toast.makeText(context,"System Audio Capture is not Supported on this Device",Toast.LENGTH_LONG).show();
                return false;
            }
            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(RECORDER_AUDIO_ENCODING)
                    .setSampleRate(RECORDER_SAMPLERATE)
                    .setChannelMask(RECORDER_CHANNELS)
                    .build();

            mRecorder = new AudioRecord.Builder()
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferElements2Rec * bytesPerElement)
                    .setAudioPlaybackCaptureConfig(config)
                    .build();

            isRecording = true;
            mRecorder.startRecording();

            createAudioFile(context);
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeAudioFile();
                }
            }, "System Audio Capture");

            recordingThread.start();

        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    void createAudioFile(Context context){
        root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "/Audio Capture");
        cache = new File(context.getCacheDir().getAbsolutePath(),"/RawData");
        if(!root.exists()) {
            root.mkdir();
            root.setWritable(true);
        }
        if(!cache.exists()) {
            cache.mkdir();
            cache.setWritable(true);
            cache.setReadable(true);
        }

        rawOutput = new File(cache,"raw.pcm");

        try {
            rawOutput.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "createAudioFile: "+e.toString());
            e.printStackTrace();
        }

        Log.d(TAG, "path: "+ rawOutput.getAbsolutePath());
    }
    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));

            // WAVE header
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, RECORDER_SAMPLERATE); // sample rate
            writeInt(output, RECORDER_SAMPLERATE); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    private byte[] shortToByte(short[] data) {
        int arraySize = data.length;
        byte[] bytes = new byte[arraySize * 2];
        for (int i = 0; i < arraySize; i++) {
            bytes[i * 2] = (byte) (data[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (data[i] >> 8);
            data[i] = 0;
        }
        return bytes;
    }

    void writeAudioFile(){
        try {
            FileOutputStream outputStream = new FileOutputStream(rawOutput.getAbsolutePath());
            short data[] = new short[bufferElements2Rec];

            while(isRecording){
                mRecorder.read(data, 0, bufferElements2Rec);

                Log.d(TAG, "AUDIO: writeAudioFile: "+data.toString());
                ByteBuffer buffer = ByteBuffer.allocate(8*1024);

                outputStream.write(shortToByte(data),
                        0,
                        bufferElements2Rec * bytesPerElement);

            }

            outputStream.close();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "File Not Found: "+e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "IO Exception: "+e.toString());
            e.printStackTrace();
        }
    }

    void startProcessing(){
        isRecording = false;
        mRecorder.stop();
        mRecorder.release();

        String fileName = new SimpleDateFormat("yyyy-MM-dd hh-mm").format(new Date())+".mp3";

        //Convert To mp3 from raw data i.e pcm
        File output = new File(root,fileName);


        try {
            output.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "startProcessing: "+e);
        }

        try {
            rawToWave(rawOutput,output);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            rawOutput.delete();
        }
    }

    void stop(){
        if(mRecorder != null){
            mRecorder = null;
            recordingThread = null;
        }
    }

}
