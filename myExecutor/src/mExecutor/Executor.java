package mExecutor;

import SyntxAnalizer.SyntaxAnalizer;
import com.java_polytech.pipeline_interfaces.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class Executor implements IExecutor {
    enum Mode {
        DECRYPT("DECRYPT"),
        ENCRYPT("ENCRYPT"),
        UNKNOWN("UNKNOWN");
        private String fieldStr;

        Mode(String fieldStr) {
            this.fieldStr = fieldStr;
        }

        public String getfieldStr() {
            return fieldStr;
        }

        static Mode GetEnumNumb(String str) {
            if (str == null) {
                return UNKNOWN;
            }
            if (str.equalsIgnoreCase(DECRYPT.getfieldStr())) {
                return DECRYPT;
            } else if (str.equalsIgnoreCase(ENCRYPT.getfieldStr())) {
                return ENCRYPT;
            } else {
                return UNKNOWN;
            }
        }
    }

    RC.RCWho who;
    ExecutorGrammar executorGrammar;
    IConsumer consumer;
    private Mode mode;
    boolean isConsumerInit = false;
    public static final int MinBigLat = 'A';
    public static final int MaxBigLat = 'Z';
    public static final int MinSmallLat = 'a';
    public static final int MaxSmallLat = 'z';

    public static final int InvMaxBigLat = ~'A';
    public static final int InvMinBigLat =  ~'Z';
    public static final int InvMaxSmallLat = ~'a';
    public static final int InvMinSmallLat = ~'z';

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    private TYPE selectedType;
    IProvider provider;
    IMediator mediator;
    boolean isSelectedType = false;
    private byte[] buffer;
    private int bufferSize;
    private int curLength;

    public Executor() {
        who = RC.RCWho.EXECUTOR;
        executorGrammar = new ExecutorGrammar();
    }
    public RC setProvider(IProvider iProvider) {
        provider = iProvider;
        TYPE[] providedTypes = provider.getOutputTypes();
        for(int i = 0 ; i < supportedTypes.length && !isSelectedType;i++){
            for(int j = 0 ; j < providedTypes.length && !isSelectedType;j++){
                if(providedTypes[j].equals(supportedTypes[i])){
                    selectedType = supportedTypes[i];
                    isSelectedType = true;
                }
            }
        }
        if(!isSelectedType){
            return RC.RC_EXECUTOR_TYPES_INTERSECTION_EMPTY_ERROR;
        }
        mediator = provider.getMediator(selectedType);
        return RC.RC_SUCCESS;
    }


    public RC setConfig(String s) {
        SyntaxAnalizer sntxAnaliz = new SyntaxAnalizer(who, executorGrammar);
        RC rcCode = sntxAnaliz.Analize(s);
        if (rcCode.isSuccess()) {
            ArrayList<String> data = sntxAnaliz.getConfigData(ExecutorGrammar.Fields.MODE.getfieldStr());
            if(data.size() != 1){
                return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "MODE must not be an array");
            }
            mode = Mode.GetEnumNumb(data.get(0));
            if(mode == Mode.UNKNOWN){
                return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
            }

            try {
                data = sntxAnaliz.getConfigData(ExecutorGrammar.Fields.BUFFER_SIZE.getfieldStr());
                if(data.size() != 1){
                    return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "BUFFER_SIZE must not be an array");
                }
                bufferSize = Integer.parseInt(data.get(0));
            } catch (NumberFormatException e) {
                return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
            }

            if(bufferSize <= 0 || bufferSize % 4 != 0)
                return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
            buffer = new byte[bufferSize];
            curLength = 0;
        }
        return rcCode;
    }

    class ByteBufferMediator implements IMediator{
        public Object getData() {
            if(curLength == 0){
                return null;
            }
            byte[] tmp = new byte[curLength];
            System.arraycopy(buffer, 0, tmp, 0, curLength);
            return tmp;
        }
    }
    class CharBufferMediator implements IMediator{
        public Object getData(){
            if(curLength == 0)
                return null;
            byte[] tmp = new byte[curLength];
            System.arraycopy(buffer, 0, tmp, 0, curLength);
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
            System.arraycopy(buffer, 0, tmp, 0, curLength);
            IntBuffer intBuf = ByteBuffer.wrap(tmp).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
            int[] array = new int[intBuf.remaining()];
            intBuf.get(array);
            return array;
        }
    }
    private byte[] getTransformedData() {
        if(selectedType == TYPE.BYTE_ARRAY) {
            return (byte[]) mediator.getData();
        }
        else if(selectedType == TYPE.CHAR_ARRAY){
            char[] chars = (char[])mediator.getData();
            if(chars == null)
                return null;
            ByteBuffer byteBuffer = ByteBuffer.allocate(chars.length * 2);
            CharBuffer charBuffer = byteBuffer.asCharBuffer();
            charBuffer.put(chars);
            return byteBuffer.array();
        }
        else if(selectedType == TYPE.INT_ARRAY){
            int[] ints = (int[])mediator.getData();
            if(ints == null)
                return null;
            ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * 4);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            intBuffer.put(ints);
            return byteBuffer.array();
        }
        else
            return null;
    }
    public RC consume() {
        if (!isConsumerInit) {
            return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "Executor consumer isn't init");
        }
        byte[] bytes = getTransformedData();
        if(bytes == null){
            return bufferEndOfWriting();
        }

        if (mode == Mode.ENCRYPT) {
            Encrypt(bytes);
        } else {
            Decrypt(bytes);
        }

        return RC.RC_SUCCESS;
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

    public IMediator getMediator(TYPE type) {
        if (type.equals(TYPE.BYTE_ARRAY))
            return new ByteBufferMediator();
        else if(type.equals(TYPE.CHAR_ARRAY))
            return new CharBufferMediator();
        else if(type.equals(TYPE.INT_ARRAY))
            return new IntBufferMediator();
        else
            return null;
    }

    private RC Encrypt(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if ((bytes[i] >=MinBigLat && bytes[i] <= MaxBigLat)
                    || (bytes[i] >= MinSmallLat && bytes[i] <= MaxSmallLat)
                    || (bytes[i] >=InvMinBigLat && bytes[i] <= InvMaxBigLat)
                    || (bytes[i] >= InvMinSmallLat && bytes[i] <= InvMaxSmallLat)) {
                bytes[i] = (byte) (~bytes[i]);
            }
            RC rcCode = writeToBuffer(bytes[i]);
            if(!rcCode.isSuccess()){
                return  rcCode;
            }
        }

        return RC.RC_SUCCESS;
    }

    private RC Decrypt(byte[] bytes) {
        int curLength = bytes.length;

        for (int i = 0; i < curLength; i++) {
            if ((bytes[i] >=MinBigLat && bytes[i] <= MaxBigLat)
                    || (bytes[i] >= MinSmallLat && bytes[i] <= MaxSmallLat)
                    || (bytes[i] >=InvMinBigLat && bytes[i] <= InvMaxBigLat)
                    || (bytes[i] >= InvMinSmallLat && bytes[i] <= InvMaxSmallLat)) {
                bytes[i] = (byte) (~bytes[i]);
            }
            RC rcCode = writeToBuffer(bytes[i]);
            if(!rcCode.isSuccess()){
                return  rcCode;
            }
        }
        return RC.RC_SUCCESS;
    }
    private RC writeToBuffer(byte elem){
        buffer[curLength++] = elem;
        if(curLength == bufferSize){
            RC rcCode = consumer.consume();
            curLength = 0;
            return  rcCode;
        }
        return RC.RC_SUCCESS;
    }

    private RC bufferEndOfWriting() {
        RC rcCode = consumer.consume();
        if(!rcCode.isSuccess()){
            return  rcCode;
        }
        curLength = 0;
        buffer = null;
        consumer.consume();
        return RC.RC_SUCCESS;
    }
}
