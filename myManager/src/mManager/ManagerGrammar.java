package mManager;

import SyntxAnalizer.IGrammar;

public class ManagerGrammar implements IGrammar {
    public enum Fields{
        INPUT_FILE("INPUT_FILE"),
        OUTPUT_FILE("OUTPUT_FILE"),
        READER_NAME("READER_NAME"),
        EXECUTOR_NAME("EXECUTOR_NAME"),
        WRITER_NAME("WRITER_NAME"),
        READER_CONFIG("READER_CONFIG"),
        EXECUTOR_CONFIG("EXECUTOR_CONFIG"),
        WRITER_CONFIG("WRITER_CONFIG");

        private String fieldStr;
        Fields(String fieldStr){
            this.fieldStr = fieldStr;
        }
        public String getfieldStr() {
            return fieldStr;
        }
    }
    private final String mainStringDelimeter = " = ";
    private final String arrayStringDelimeter = ", ";
    private final String commentString = "#";

    public String getMainStringDelimeter() {
        return mainStringDelimeter;
    }

    public String getArrayStringDelimeter() {return arrayStringDelimeter;}

    public String getCommentSymbol() {
        return commentString;
    }

    public boolean isGrammarKey(String s) {
        for(Fields elem: Fields.values()){
            if(s.equalsIgnoreCase(elem.getfieldStr())){
                return true;
            }
        }
        return false;
    }
}
