package edu.cmu.cs.diamond.snapfind2;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import edu.cmu.cs.diamond.snapfind2.LogicExpressionParser.expr_return;

public class TestLogic {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // TODO Auto-generated method stub
        String logic = "and(NOT(NOT(OR($1,AND(OR($2,AND(NOT($3),$4)),OR($1,$2))))),$3)";

        ANTLRStringStream stream = new ANTLRStringStream(logic);

        LogicExpressionLexer lexer = new LogicExpressionLexer(stream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        LogicExpressionParser parser = new LogicExpressionParser(tokens);

        expr_return expr = parser.expr();

        CommonTree ct = (CommonTree) expr.getTree();

        System.out.println(ct.toStringTree());

        String opcodes = createStackMachineCode(ct);

        System.out.println(opcodes);
    }

    private static String createStackMachineCode(Tree ct) {
        // depth-first traversal
        StringBuilder sb = new StringBuilder();

        createStackMachineCode(ct, sb);

        return sb.toString();
    }

    private static void createStackMachineCode(Tree t, StringBuilder sb) {
        int type = t.getType();

        // System.out.println(type);

        switch (type) {
        case LogicExpressionParser.OP_AND:
            createStackMachineCode(t.getChild(0), sb);
            createStackMachineCode(t.getChild(1), sb);
            sb.append(" &");
            break;
        case LogicExpressionParser.OP_OR:
            createStackMachineCode(t.getChild(0), sb);
            createStackMachineCode(t.getChild(1), sb);
            sb.append(" |");
            break;
        case LogicExpressionParser.OP_NOT:
            createStackMachineCode(t.getChild(0), sb);
            sb.append(" !");
            break;
        case LogicExpressionParser.NUMBER:
            sb.append(" " + t.getText());
            break;
        default:
            throw new IllegalStateException("Invalid node: " + t);
        }
    }
}
