package me.lake.librestreaming.core;

import android.media.MediaFormat;

import java.nio.ByteBuffer;

import me.lake.librestreaming.tools.ByteArrayTools;


public class Packager {
    public static class H264Packager {

        public static byte[] generateAVCDecoderConfigurationRecord(MediaFormat mediaFormat) {
            ByteBuffer SPSByteBuff = mediaFormat.getByteBuffer("csd-0");
            SPSByteBuff.position(4);
            ByteBuffer PPSByteBuff = mediaFormat.getByteBuffer("csd-1");
            PPSByteBuff.position(4);
            int spslength = SPSByteBuff.remaining();
            int ppslength = PPSByteBuff.remaining();
            int length = 11 + spslength + ppslength;
            byte[] result = new byte[length];
            SPSByteBuff.get(result, 8, spslength);
            PPSByteBuff.get(result, 8 + spslength + 3, ppslength);
            /**
             * UB[8]configurationVersion
             * UB[8]AVCProfileIndication
             * UB[8]profile_compatibility
             * UB[8]AVCLevelIndication
             * UB[8]lengthSizeMinusOne
             */
            result[0] = 0x01;
            result[1] = result[9];
            result[2] = result[10];
            result[3] = result[11];
            result[4] = (byte) 0xFF;
            /**
             * UB[8]numOfSequenceParameterSets
             * UB[16]sequenceParameterSetLength
             */
            result[5] = (byte) 0xE1;
            ByteArrayTools.intToByteArrayTwoByte(result, 6, spslength);
            /**
             * UB[8]numOfPictureParameterSets
             * UB[16]pictureParameterSetLength
             */
            int pos = 8 + spslength;
            result[pos] = (byte) 0x01;
            ByteArrayTools.intToByteArrayTwoByte(result, pos + 1, ppslength);

            return result;
        }
    }

    public static class FLVPackager {
        public static final int FLV_TAG_LENGTH = 11;
        public static final int FLV_VIDEO_TAG_LENGTH = 5;
        public static final int FLV_AUDIO_TAG_LENGTH = 2;
        public static final int FLV_TAG_FOOTER_LENGTH = 4;
        public static final int NALU_HEADER_LENGTH = 4;

        public static void fillFlvVideoTag(byte[] dst, int pos, boolean isAVCSequenceHeader, boolean isIDR, int readDataLength) {
            //FrameType&CodecID
            dst[pos] = isIDR ? (byte) 0x17 : (byte) 0x27;
            //AVCPacketType
            dst[pos + 1] = isAVCSequenceHeader ? (byte) 0x00 : (byte) 0x01;
            //LAKETODO CompositionTime
            dst[pos + 2] = 0x00;
            dst[pos + 3] = 0x00;
            dst[pos + 4] = 0x00;
            if (!isAVCSequenceHeader) {
                //NALU HEADER
                ByteArrayTools.intToByteArrayFull(dst, pos + 5, readDataLength);
            }
        }

        public static void fillFlvAudioTag(byte[] dst, int pos, boolean isAACSequenceHeader) {
            /**
             * UB[4] 10=AAC
             * UB[2] 3=44kHz
             * UB[1] 1=16-bit
             * UB[1] 0=MonoSound
             */
            dst[pos] = (byte) 0xAE;
            dst[pos + 1] = isAACSequenceHeader ? (byte) 0x00 : (byte) 0x01;
        }
    }

}
