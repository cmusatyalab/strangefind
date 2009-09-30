package edu.cmu.cs.diamond.strangefind;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import edu.cmu.cs.diamond.strangefind.LogicExpressionParser.expr_return;

public class LogicEngine {

    private LogicEngine() {
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // TODO Auto-generated method stub
        // String logic =
        // "and(NOT(NOT(OR($1,AND(OR($2,AND(NOT($3),$4)),OR($1,$2))))),$3)";
        String logic = args[0];

        System.out.println(getMachineCodeForExpression(logic));
    }

    public static String getMachineCodeForExpression(String expression) {
        if (expression.trim().equals("")) {
            return "F"; // false
        }

        try {
            ANTLRStringStream stream = new ANTLRStringStream(expression);

            LogicExpressionLexer lexer = new LogicExpressionLexer(stream);

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            LogicExpressionParser parser = new LogicExpressionParser(tokens);

            expr_return expr = parser.expr();

            CommonTree ct = (CommonTree) expr.getTree();

            System.out.println(ct.toStringTree());

            return createStackMachineCode(ct);
        } catch (RecognitionException e) {
            e.printStackTrace();
        }

        // fail
        return "";
    }

    private static String createStackMachineCode(Tree ct) {
        // depth-first traversal
        StringBuilder sb = new StringBuilder();

        createStackMachineCode(ct, sb);

        String opcodes = sb.toString();
        return opcodes.substring(0, opcodes.length() - 1);
    }

    final private static String DELIMITER = "_";

    private static void createStackMachineCode(Tree t, StringBuilder sb) {
        int type = t.getType();

        // System.out.println(type);

        switch (type) {
        case LogicExpressionParser.OP_AND:
            createStackMachineCode(t.getChild(0), sb);
            createStackMachineCode(t.getChild(1), sb);
            sb.append("&" + DELIMITER);
            break;
        case LogicExpressionParser.OP_OR:
            createStackMachineCode(t.getChild(0), sb);
            createStackMachineCode(t.getChild(1), sb);
            sb.append("|" + DELIMITER);
            break;
        case LogicExpressionParser.OP_NOT:
            createStackMachineCode(t.getChild(0), sb);
            sb.append("!" + DELIMITER);
            break;
        case LogicExpressionParser.NUMBER:
            int literal = Integer.parseInt(t.getText()) - 1;
            sb.append(literal + DELIMITER);
            break;
        default:
            throw new IllegalStateException("Invalid node: " + t);
        }
    }
}
