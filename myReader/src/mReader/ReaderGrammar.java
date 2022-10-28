package mReader;
import SyntxAnalizer.IGrammar;

public class ReaderGrammar implements IGrammar {
    public enum Fields{
        BUFFER_SIZE("BUFFER_SIZE");
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
