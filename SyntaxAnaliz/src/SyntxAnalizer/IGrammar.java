package SyntxAnalizer;
public interface IGrammar {
    String getMainStringDelimeter();
    String getArrayStringDelimeter();
    String getCommentSymbol();
    boolean isGrammarKey(String s);

}
