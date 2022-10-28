package mReader;

import SyntxAnalizer.SyntaxAnalizer;
import com.java_polytech.pipeline_interfaces.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class Reader implements IReader {
    RC.RCWho who;
    ReaderGrammar readerGrammar;
    IConsumer consumer;
    InputStream inputStream;
    boolean isConsumerInit = false;
    boolean isInputStreamInit = false;
    boolean isConfigInit = false;

    private byte[] bytes;
    private int bufferSize;
    private int curLength;
    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    public Reader(){
        who = RC.RCWho.READER;
        readerGrammar = new ReaderGrammar();
    }

    public RC setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        isInputStreamInit = true;
        return RC.RC_SUCCESS;
    }

    public RC run() {
        curLength = 0;
        RC rcCode;
        if(!isInputStreamInit){
            return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "InputStream isn't init");
        }
        if(!isConfigInit){
            return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "reader Config isn't init");
        }
        if(!isConsumerInit){
            return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "Consumer isn't init");
        }

        try {
            curLength = inputStream.read(bytes, 0, bufferSize);
        } catch (IOException e) {
            return RC.RC_READER_FAILED_TO_READ;
        }
        while ((curLength > 0)) {
            rcCode = consumer.consume();
            if(!rcCode.isSuccess()){
                bytes  = null;
                curLength = 0;
                consumer.consume();
                return rcCode;
            }

            try {
                curLength = inputStream.read(bytes, 0, bufferSize);
            } catch (IOException e) {
                return RC.RC_READER_FAILED_TO_READ;
            }
        }
        curLength = 0;
        consumer.consume();
        return RC.RC_SUCCESS;
    }

    public RC setConfig(String s) {
        SyntaxAnalizer sntxAnaliz = new SyntaxAnalizer(who, readerGrammar);
        RC rcCode = sntxAnaliz.Analize(s);
        if(rcCode.isSuccess()){
            try {
                ArrayList<String> data = sntxAnaliz.getConfigData(ReaderGrammar.Fields.BUFFER_SIZE.getfieldStr());
                if(data.size() != 1){
                    return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "BUFFER_SIZE must not be an array");
                }
                bufferSize = Integer.parseInt(data.get(0));
            } catch (NumberFormatException e) {
                return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
            }
        }
        if(bufferSize <= 0 || bufferSize % 4 != 0)
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        bytes = new byte[bufferSize];
        curLength = 0;
        isConfigInit = true;
        return rcCode;
    }

    public RC setConsumer(IConsumer iConsumer) {
        consumer = iConsumer;
        RC rcCode = iConsumer.setProvider(this);
        if(!rcCode.isSuccess())
            return rcCode;
        isConsumerInit = true;
        return RC.RC_SUCCESS;
    }

    public TYPE[] getOutputTypes() {
        return supportedTypes;
    }
    class ByteBufferMediator implements IMediator{
        public Object getData() {
            if(curLength == 0)
                return null;
            byte[] tmp = new byte[curLength];
            System.arraycopy(bytes, 0, tmp, 0, curLength);
            return tmp;
        }
    }
    class CharBufferMediator implements IMediator{
        public Object getData(){
            if(curLength == 0)
                return null;
            byte[] tmp = new byte[curLength];
            System.arraycopy(bytes, 0, tmp, 0, curLength);
            CharBuffer charBuf = ByteBuffer.wrap(tmp).order(ByteOrder.BIG_ENDIAN).asCharBuffer();
            char[] array = new char[charBuf.remaining()];
            charBuf.get(array);
            return array;
        }
    }
    class IntBufferMediator implements IMediator{
        public Object getData(){
            if(curLength == 0)
                return null;
            byte[] tmp = new byte[curLength];
            System.arraycopy(bytes, 0, tmp, 0, curLength);
            IntBuffer intBuf = ByteBuffer.wrap(tmp).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
            int[] array = new int[intBuf.remaining()];
            intBuf.get(array);
            return array;
        }
    }
    public IMediator getMediator(TYPE type) {
        if (type.equals(TYPE.BYTE_ARRAY))
            return new ByteBufferMediator();
        else if(type.equals((TYPE.CHAR_ARRAY)))
            return new CharBufferMediator();
        else if(type.equals((TYPE.INT_ARRAY)))
            return new IntBufferMediator();
        else
            return null;
    }
}
