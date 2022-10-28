package SyntxAnalizer;


import com.java_polytech.pipeline_interfaces.RC;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SyntaxAnalizer {
    public boolean isCorrect = true;
    final private Map<String, ArrayList<String>> configData = new HashMap<String, ArrayList<String>>();
    IGrammar myGrammar;
    RC.RCWho who = RC.RCWho.UNKNOWN;
    public SyntaxAnalizer(RC.RCWho owner, IGrammar grammar){
        who = owner;
        myGrammar = grammar;
    }

    private boolean isComment(String str){
        str = str.trim();
        if(str.length() >= 1) {
            if(myGrammar.getCommentSymbol().equals(str.substring(0, myGrammar.getCommentSymbol().length())))
                return true;
        }
        return false;
    }
    private boolean isEmptyStr(String str){
        str = str.trim();
        return str.isEmpty();
    }
    public RC Analize(String fileName) {
        BufferedReader reader = null;
        try {
            File file = new File(fileName);
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException ex) {
            isCorrect = false;
            return new RC(who , RC.RCType.CODE_CONFIG_FILE_ERROR,"Can't open config");
        }
        String[] subStr;
        String line;
        try {
            while (isCorrect && (line = reader.readLine()) != null) {
                if(isEmptyStr(line) || isComment(line))
                    continue;
                subStr = line.split(myGrammar.getMainStringDelimeter());
                if (subStr.length != 2) {
                    isCorrect = false;
                    return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "Number of parameters isn't equal to 2");
                }
                if (myGrammar.isGrammarKey(subStr[0])) {
                    if (configData.containsKey(getConfigData(subStr[0]))) {
                        isCorrect = false;
                        return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "The key has already been met");
                    }
                    String[] elementsOfArray = subStr[1].split(myGrammar.getArrayStringDelimeter());
                    ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(elementsOfArray));
                    configData.put(subStr[0], arrayList);
                }
                else{
                    isCorrect = false;
                    return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "No equal grammar key");
                }
            }
        } catch (IOException e) {
            isCorrect = false;
            return new RC(who, RC.RCType.CODE_FAILED_TO_READ, "Errors in reading the config");
        }
        return RC.RC_SUCCESS;
    }

    public ArrayList<String> getConfigData(String field) {
        return configData.get(field);
    }

}
