package mManager;

import SyntxAnalizer.SyntaxAnalizer;
import com.java_polytech.pipeline_interfaces.*;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Manager implements IConfigurable {
    RC.RCWho who;
    ManagerGrammar managerGrammar;
    IReader reader;
    IWriter writer;
    boolean isInit = false;
    private FileInputStream in;
    private FileOutputStream out;
    ArrayList<IExecutor> executors;
    private static Logger logger;
    public Manager(){
        who = RC.RCWho.MANAGER;
        managerGrammar = new ManagerGrammar();
        executors = new ArrayList<IExecutor>();
    }
    public static boolean checkRcCode(RC code){
        if(!code.isSuccess()){
            logger.severe("Who: " + code.who + " Error info: " + code.info);
            return false;
        }
        return true;
    }
    public RC setConfig(String s) {
        SyntaxAnalizer sntxAnaliz = new SyntaxAnalizer(who, managerGrammar);
        RC rcCode = sntxAnaliz.Analize(s);
        if(rcCode.isSuccess()){
            try {
                ArrayList<String> data = sntxAnaliz.getConfigData(ManagerGrammar.Fields.INPUT_FILE.getfieldStr());
                if(data.size() != 1){
                    return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "INPUT_FILE must not be an array");
                }
                in = new FileInputStream(data.get(0));
            } catch (FileNotFoundException e) {
                return RC.RC_MANAGER_INVALID_INPUT_FILE;
            } catch (NullPointerException e){
                return new RC(who , RC.RCType.CODE_CUSTOM_ERROR, "INPUT_FILE null ptr");
            }

            try {
                ArrayList<String> data = sntxAnaliz.getConfigData(ManagerGrammar.Fields.OUTPUT_FILE.getfieldStr());
                if(data.size() != 1){
                    return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "OUTPUT_FILE must not be an array");
                }
                out = new FileOutputStream(data.get(0));
            } catch (FileNotFoundException e) {
                return RC.RC_MANAGER_INVALID_OUTPUT_FILE;
            }catch (NullPointerException e){
                return new RC(who , RC.RCType.CODE_CUSTOM_ERROR, "OUTPUT_FILE null ptr");
            }

            try{
                ArrayList<String> data = sntxAnaliz.getConfigData(ManagerGrammar.Fields.READER_NAME.getfieldStr());
                if(data.size() != 1){
                    return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "READER_NAME must not be an array");
                }
                Class<?> tmpReader = Class.forName(data.get(0));
                if (IReader.class.isAssignableFrom(tmpReader))
                    reader = (IReader) tmpReader.getDeclaredConstructor().newInstance();
                else return RC.RC_MANAGER_INVALID_READER_CLASS;
            }
            catch (Exception e) {
                return RC.RC_MANAGER_INVALID_READER_CLASS;
            }

            try{
                ArrayList<String> data = sntxAnaliz.getConfigData(ManagerGrammar.Fields.EXECUTOR_NAME.getfieldStr());
                for(int i = 0; i < data.size();i++) {
                    Class<?> tmpExecutor = Class.forName(data.get(i));
                    if (IExecutor.class.isAssignableFrom(tmpExecutor))
                        executors.add((IExecutor) tmpExecutor.getDeclaredConstructor().newInstance());
                    else return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
                }
            }
            catch (Exception e) {
                return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
            }

            try{
                ArrayList<String> data = sntxAnaliz.getConfigData(ManagerGrammar.Fields.WRITER_NAME.getfieldStr());
                if(data.size() != 1){
                    return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "WRITER_NAME must not be an array");
                }
                Class<?> tmpWriter = Class.forName(data.get(0));
                if (IWriter.class.isAssignableFrom(tmpWriter))
                    writer = (IWriter) tmpWriter.getDeclaredConstructor().newInstance();
                else return RC.RC_MANAGER_INVALID_WRITER_CLASS;
            }
            catch (Exception e) {
                return RC.RC_MANAGER_INVALID_WRITER_CLASS;
            }

            ArrayList<String> data = sntxAnaliz.getConfigData(ManagerGrammar.Fields.READER_CONFIG.getfieldStr());
            if(data.size() != 1){
                return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "READER_CONFIG must not be an array");
            }
            String cnfName = data.get(0);
            if(cnfName == null){
                return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "invalid reader config");
            }
            RC Code = reader.setConfig(cnfName);
            if(!Code.isSuccess()){
                return Code;
            }

            data = sntxAnaliz.getConfigData(ManagerGrammar.Fields.EXECUTOR_CONFIG.getfieldStr());
            if(data.size() != executors.size()){
                return new RC(who , RC.RCType.CODE_CUSTOM_ERROR, "number of executors configs doens't equal to names");
            }
            for(int i = 0 ; i < data.size(); i++){
                cnfName = data.get(i);
                if(cnfName == null){
                    return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "invalid executor config");
                }
                Code = executors.get(i).setConfig(cnfName);
                if(!Code.isSuccess()){
                    return Code;
                }
            }

            data = sntxAnaliz.getConfigData(ManagerGrammar.Fields.WRITER_CONFIG.getfieldStr());
            if(data.size() != 1){
                return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "WRITER_CONFIG must not be an array");
            }
            cnfName = data.get(0);
            if(cnfName == null){
                return new RC(who , RC.RCType.CODE_CUSTOM_ERROR, "invalid writer config");
            }
            Code = writer.setConfig(cnfName);
            if(!Code.isSuccess()){
                return Code;
            }

            Code = reader.setInputStream(in);
            if(!Code.isSuccess()){
                return Code;
            }
            Code = writer.setOutputStream(out);
            if(!Code.isSuccess()){
                return Code;
            }

            Code = reader.setConsumer(executors.get(0));
            if(!Code.isSuccess()){
                return Code;
            }
            for(int i = 0; i < executors.size() - 1;i++){
                Code = executors.get(i).setConsumer(executors.get(i + 1));
                if(!Code.isSuccess()){
                    return Code;
                }
            }

            Code = executors.get(executors.size() - 1).setConsumer(writer);
            if(!Code.isSuccess()){
                return Code;
            }
            isInit = true;

        }
        return rcCode;
    }
    private RC close() {
        try {
            in.close();
        } catch (IOException e) {
            return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "Can't close input");
        } catch(NullPointerException e) {
            return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "Can't close input (null ptr)");
        }

        try {
            out.close();
        } catch (IOException e) {
            return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "Can't close output");
        } catch(NullPointerException e) {
            return new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "Can't close output (null ptr)");
        }
        return RC.RC_SUCCESS;
    }
    public RC Run(){
        if(!isInit){
            return new RC(who , RC.RCType.CODE_CUSTOM_ERROR, "Manager isn't inited");
        }
        RC code = reader.run();
        checkRcCode(close());
        return code;
    }

    private static Logger makeLogger() {
        Logger logger = Logger.getLogger("Logger");
        FileHandler fh;
        try {
            fh = new FileHandler("log.txt");
        } catch (IOException ex) {
            return null;
        }
        SimpleFormatter sf = new SimpleFormatter();
        fh.setFormatter(sf);
        logger.addHandler(fh);
        logger.setUseParentHandlers(false);

        return logger;
    }

    public static void main(String[] args) {
        logger = makeLogger();
        if(logger == null){
            System.out.println("Error, cant create logger");
            return;
        }
        if(args.length != 1) {
            checkRcCode(RC.RC_MANAGER_INVALID_ARGUMENT);
        }
        else {
            logger.severe("Manager start working...");
            Manager manager = new Manager();
            logger.severe("Parse config");
            if(checkRcCode(manager.setConfig(args[0]))){
                logger.severe("Main functiton");
                if(checkRcCode(manager.Run())){
                    System.out.println("Success");
                    return;
                }
            }

        }
        System.out.println("Error");
    }
}
