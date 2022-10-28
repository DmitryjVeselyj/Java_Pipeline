package mWriter;
import SyntxAnalizer.SyntaxAnalizer;
import com.java_polytech.pipeline_interfaces.*;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class Writer implements IWriter {
    RC.RCWho who;
    WriterGrammar writerGrammar;
    OutputStream outputStream;

    boolean isOutputStreamInit = false;
    boolean isConfigInit = false;
    boolean isSelectedType = false;

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    private TYPE selectedType;
    IProvider provider;
    IMediator mediator;
    private int bufferSize;
    private BufferedOutputStream bufferedOutputStream;
    public Writer(){
        who = RC.RCWho.WRITER;
        writerGrammar = new WriterGrammar();
    }

    public RC setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        isOutputStreamInit = true;
        if(bufferedOutputStream == null  && bufferSize > 0){
            bufferedOutputStream = new BufferedOutputStream(this.outputStream, bufferSize);
        }
        return RC.RC_SUCCESS;
    }

    public RC setConfig(String s) {
        SyntaxAnalizer sntxAnaliz = new SyntaxAnalizer(who, writerGrammar);
        RC rcCode = sntxAnaliz.Analize(s);
        if(rcCode.isSuccess()){
            try {
                ArrayList<String> data = sntxAnaliz.getConfigData(WriterGrammar.Fields.BUFFER_SIZE.getfieldStr());
                if(data.size() != 1){
                    return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "BUFFER_SIZE must not be an array");
                }
                bufferSize = Integer.parseInt(data.get(0));
            } catch (NumberFormatException e) {
                return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;
            }
        }
        if(bufferSize <= 0 || bufferSize % 4 != 0)
            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;
        if(bufferedOutputStream == null && outputStream != null){
            bufferedOutputStream = new BufferedOutputStream(outputStream, bufferSize);
        }
        isConfigInit = true;
        return rcCode;
    }
    private byte[] getTransformedData(){
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
        if(!isOutputStreamInit){
            return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "OutputStream isn't init");
        }
        if(!isConfigInit){
            return new RC(who , RC.RCType.CODE_CUSTOM_ERROR, "writer config isn't init");
        }
        byte[] bytes = getTransformedData();

        if(bytes == null) {
            try {
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            } catch (IOException e) {
                return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "bufferedStream flush error");
            }
            return RC.RC_SUCCESS;
        }
        else{
            try {
                bufferedOutputStream.write(bytes);
            } catch (IOException e) {
                return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "bufferedStream write error");
            }
        }
        return RC.RC_SUCCESS;
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
            return RC.RC_WRITER_TYPES_INTERSECTION_EMPTY_ERROR;
        }
        mediator = provider.getMediator(selectedType);
        return RC.RC_SUCCESS;
    }
}
