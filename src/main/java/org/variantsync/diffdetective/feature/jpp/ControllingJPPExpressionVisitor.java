package org.variantsync.diffdetective.feature.jpp;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.prop4j.Node;
import org.variantsync.diffdetective.feature.antlr.JPPExpressionParser;
import org.variantsync.diffdetective.feature.antlr.JPPExpressionVisitor;
import org.variantsync.diffdetective.util.fide.FormulaUtils;

import java.util.List;
import java.util.function.Function;

import static org.variantsync.diffdetective.util.fide.FormulaUtils.negate;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.var;

/**
 * Transform a parse tree into a {@link org.prop4j.Node formula}.
 * Non-boolean sub-expressions are abstracted using
 * {@link org.antlr.v4.runtime.tree.ParseTree#getText}
 * which essentially removes all irrelevant whitespace.
 */
public class ControllingJPPExpressionVisitor extends AbstractParseTreeVisitor<Node> implements JPPExpressionVisitor<Node> {
    // expression
    //    :   logicalOrExpression
    //    ;
    @Override
    public Node visitExpression(JPPExpressionParser.ExpressionContext ctx) {
        return ctx.logicalOrExpression().accept(this);
    }

    // logicalOrExpression
    //    :   logicalAndExpression (OR logicalAndExpression)*
    //    ;
    @Override
    public Node visitLogicalOrExpression(JPPExpressionParser.LogicalOrExpressionContext ctx) {
        return visitLogicalExpression(ctx, FormulaUtils::or);
    }

    // logicalAndExpression
    //    :   primaryExpression (AND primaryExpression)*
    //    ;
    @Override
    public Node visitLogicalAndExpression(JPPExpressionParser.LogicalAndExpressionContext ctx) {
        return visitLogicalExpression(ctx, FormulaUtils::and);
    }

    // primaryExpression
    //    :   definedExpression
    //    |   undefinedExpression
    //    |   comparisonExpression
    //    ;
    @Override
    public Node visitPrimaryExpression(JPPExpressionParser.PrimaryExpressionContext ctx) {
        if (ctx.definedExpression() != null) {
            return ctx.definedExpression().accept(this);
        }
        if (ctx.undefinedExpression() != null) {
            return ctx.undefinedExpression().accept(this);
        }
        if (ctx.comparisonExpression() != null) {
            return ctx.comparisonExpression().accept(this);
        }
        throw new IllegalStateException("Unreachable code");
    }

    // comparisonExpression
    //    :   operand ((LT|GT|LEQ|GEQ|EQ|NEQ) operand)?
    //    ;
    @Override
    public Node visitComparisonExpression(JPPExpressionParser.ComparisonExpressionContext ctx) {
        return var(ctx.getText());
    }

    // operand
    //    :   propertyExpression
    //    |   Constant
    //    |   StringLiteral+
    //    |   unaryOperator Constant
    //    ;
    @Override
    public Node visitOperand(JPPExpressionParser.OperandContext ctx) {
        return var(ctx.getText());
    }

    // definedExpression
    //    :   'defined' '(' Identifier ')'
    //    ;
    @Override
    public Node visitDefinedExpression(JPPExpressionParser.DefinedExpressionContext ctx) {
        return var(String.format("defined(%s)", ctx.Identifier().getText()));
    }

    // undefinedExpression
    //    :   NOT 'defined' '(' Identifier ')'
    //    ;
    @Override
    public Node visitUndefinedExpression(JPPExpressionParser.UndefinedExpressionContext ctx) {
        return negate(var(String.format("defined(%s)", ctx.Identifier().getText())));
    }

    // propertyExpression
    //    :   '${' Identifier '}'
    //    ;
    @Override
    public Node visitPropertyExpression(JPPExpressionParser.PropertyExpressionContext ctx) {
        throw new IllegalStateException("Unreachable code");
    }

    // unaryOperator
    //    : U_PLUS
    //    | U_MINUS
    //    ;
    @Override
    public Node visitUnaryOperator(JPPExpressionParser.UnaryOperatorContext ctx) {
        throw new IllegalStateException("Unreachable code");
    }

    // logicalOrExpression
    //    :   logicalAndExpression (OR logicalAndExpression)*
    //    ;
    // logicalAndExpression
    //    :   primaryExpression (AND primaryExpression)*
    //    ;
    private Node visitLogicalExpression(ParserRuleContext expressionContext, Function<Node[], Node> newLogicNode) {
        if (expressionContext.getChildCount() == 1) {
            return expressionContext.getChild(0).accept(this);
        } else {
            List<ParseTree> subtrees = expressionContext.children;

            // Skip every second node. These nodes are either "&&" or "||".
            Node[] children = new Node[1 + (subtrees.size() - 1) / 2];
            for (int i = 0; i < children.length; ++i) {
                children[i] = subtrees.get(2 * i).accept(this);
            }

            return newLogicNode.apply(children);
        }
    }
}
