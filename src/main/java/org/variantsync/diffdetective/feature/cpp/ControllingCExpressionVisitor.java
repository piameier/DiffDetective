package org.variantsync.diffdetective.feature.cpp;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.prop4j.Node;
import org.variantsync.diffdetective.feature.antlr.CExpressionParser;
import org.variantsync.diffdetective.feature.antlr.CExpressionVisitor;
import org.variantsync.diffdetective.util.fide.FormulaUtils;

import java.util.List;
import java.util.function.Function;

import static org.variantsync.diffdetective.util.fide.FormulaUtils.negate;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.var;

/**
 * Visitor that controls how formulas given as an ANTLR parse tree are abstracted.
 * To this end, the visitor traverses the parse tree, searching for subtrees that should be abstracted.
 * If such a subtree is found, the visitor calls an {@link AbstractingCExpressionVisitor} to abstract the entire subtree.
 * Only those parts of a formula are abstracted that require abstraction, leaving ancestors in the tree unchanged.
 */
@SuppressWarnings("CheckReturnValue")
public class ControllingCExpressionVisitor extends AbstractParseTreeVisitor<Node> implements CExpressionVisitor<Node> {
    private final ParseTreeVisitor<StringBuilder> abstractingVisitor = new AbstractingCExpressionVisitor();

    public ControllingCExpressionVisitor() {
    }

    // conditionalExpression
    //    :   logicalOrExpression ('?' expression ':' conditionalExpression)?
    //    ;
    @Override
    public Node visitConditionalExpression(CExpressionParser.ConditionalExpressionContext ctx) {
        // We have to abstract the expression if it is a ternary expression
        return recurseOnSingleChild(ctx);
    }

    // primaryExpression
    //    :   macroExpression
    //    |   Identifier
    //    |   Constant
    //    |   StringLiteral+
    //    |   '(' expression ')'
    //    |   unaryOperator primaryExpression
    //    |   specialOperator
    //    ;
    @Override
    public Node visitPrimaryExpression(CExpressionParser.PrimaryExpressionContext ctx) {
        // macroExpression
        if (ctx.macroExpression() != null) {
            return abstractToLiteral(ctx);
        }
        // Identifier
        if (ctx.Identifier() != null) {
            return abstractToLiteral(ctx);
        }
        // Constant
        if (ctx.Constant() != null) {
            return abstractToLiteral(ctx);
        }
        // StringLiteral+
        if (!ctx.StringLiteral().isEmpty()) {
            return abstractToLiteral(ctx);
        }
        // '(' expression ')'
        if (ctx.expression() != null) {
            return ctx.expression().accept(this);
        }
        // unaryOperator primaryExpression
        if (ctx.unaryOperator() != null) {
            // Negation can be modeled in the formula.
            // All other unary operators need to be abstracted.
            if (ctx.unaryOperator().getText().equals("!")) {
                return negate(ctx.primaryExpression().accept(this));
            } else {
                return abstractToLiteral(ctx);
            }
        }
        // specialOperator
        if (ctx.specialOperator() != null) {
            return abstractToLiteral(ctx.specialOperator());
        }

        throw new IllegalStateException("Unreachable code");
    }

    // unaryOperator
    //    :   '&' | '*' | '+' | '-' | '~' | '!'
    //    ;
    @Override
    public Node visitUnaryOperator(CExpressionParser.UnaryOperatorContext ctx) {
        throw new IllegalStateException("Unreachable code");
    }


    // namespaceExpression
    //    :   primaryExpression (':' primaryExpression)*
    //    ;
    @Override
    public Node visitNamespaceExpression(CExpressionParser.NamespaceExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // multiplicativeExpression
    //    :   primaryExpression (('*'|'/'|'%') primaryExpression)*
    //    ;
    @Override
    public Node visitMultiplicativeExpression(CExpressionParser.MultiplicativeExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // additiveExpression
    //    :   multiplicativeExpression (('+'|'-') multiplicativeExpression)*
    //    ;
    @Override
    public Node visitAdditiveExpression(CExpressionParser.AdditiveExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // shiftExpression
    //    :   additiveExpression (('<<'|'>>') additiveExpression)*
    //    ;
    @Override
    public Node visitShiftExpression(CExpressionParser.ShiftExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // relationalExpression
    //    :   shiftExpression (('<'|'>'|'<='|'>=') shiftExpression)*
    //    ;
    @Override
    public Node visitRelationalExpression(CExpressionParser.RelationalExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // equalityExpression
    //    :   relationalExpression (('=='| '!=') relationalExpression)*
    //    ;
    @Override
    public Node visitEqualityExpression(CExpressionParser.EqualityExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // specialOperator
    //    :   HasAttribute ('(' specialOperatorArgument ')')?
    //    |   HasCPPAttribute ('(' specialOperatorArgument ')')?
    //    |   HasCAttribute ('(' specialOperatorArgument ')')?
    //    |   HasBuiltin ('(' specialOperatorArgument ')')?
    //    |   HasInclude ('(' (PathLiteral | StringLiteral) ')')?
    //    |   Defined ('(' specialOperatorArgument ')')
    //    |   Defined specialOperatorArgument?
    //    ;
    @Override
    public Node visitSpecialOperator(CExpressionParser.SpecialOperatorContext ctx) {
        // We have to abstract the special operator
        return abstractToLiteral(ctx);
    }

    // specialOperatorArgument
    //    :   HasAttribute
    //    |   HasCPPAttribute
    //    |   HasCAttribute
    //    |   HasBuiltin
    //    |   HasInclude
    //    |   Defined
    //    |   Identifier
    //    ;
    @Override
    public Node visitSpecialOperatorArgument(CExpressionParser.SpecialOperatorArgumentContext ctx) {
        return abstractToLiteral(ctx);
    }

    // macroExpression
    //    :   Identifier '(' argumentExpressionList? ')'
    //    ;
    @Override
    public Node visitMacroExpression(CExpressionParser.MacroExpressionContext ctx) {
        return abstractToLiteral(ctx);
    }

    // argumentExpressionList
    //    :   assignmentExpression (',' assignmentExpression)*
    //    |   assignmentExpression (assignmentExpression)*
    //    ;
    @Override
    public Node visitArgumentExpressionList(CExpressionParser.ArgumentExpressionListContext ctx) {
        throw new IllegalStateException("Unreachable code");
    }

    // assignmentExpression
    //    :   conditionalExpression
    //    |   DigitSequence // for
    //    |   PathLiteral
    //    |   StringLiteral
    //    |   primaryExpression assignmentOperator assignmentExpression
    //    ;
    @Override
    public Node visitAssignmentExpression(CExpressionParser.AssignmentExpressionContext ctx) {
        if (ctx.conditionalExpression() != null) {
            return ctx.conditionalExpression().accept(this);
        } else {
            return abstractToLiteral(ctx);
        }
    }

    // assignmentOperator
    //    :   '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|='
    //    ;
    @Override
    public Node visitAssignmentOperator(CExpressionParser.AssignmentOperatorContext ctx) {
        throw new IllegalStateException("Unreachable code");
    }

    // expression
    //    :   assignmentExpression (',' assignmentExpression)*
    //    ;
    @Override
    public Node visitExpression(CExpressionParser.ExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // andExpression
    //    :   equalityExpression ( '&' equalityExpression)*
    //    ;
    @Override
    public Node visitAndExpression(CExpressionParser.AndExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // exclusiveOrExpression
    //    :   andExpression ('^' andExpression)*
    //    ;
    @Override
    public Node visitExclusiveOrExpression(CExpressionParser.ExclusiveOrExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // inclusiveOrExpression
    //    :   exclusiveOrExpression ('|' exclusiveOrExpression)*
    //    ;
    @Override
    public Node visitInclusiveOrExpression(CExpressionParser.InclusiveOrExpressionContext ctx) {
        return recurseOnSingleChild(ctx);
    }

    // logicalAndExpression
    //    :   logicalOperand ( '&&' logicalOperand)*
    //    ;
    @Override
    public Node visitLogicalAndExpression(CExpressionParser.LogicalAndExpressionContext ctx) {
        return visitLogicalExpression(ctx, FormulaUtils::and);
    }

    // logicalOrExpression
    //    :   logicalAndExpression ( '||' logicalAndExpression)*
    //    ;
    @Override
    public Node visitLogicalOrExpression(CExpressionParser.LogicalOrExpressionContext ctx) {
        return visitLogicalExpression(ctx, FormulaUtils::or);
    }

    // logicalOperand
    //    :   inclusiveOrExpression
    //    ;
    @Override
    public Node visitLogicalOperand(CExpressionParser.LogicalOperandContext ctx) {
        return ctx.inclusiveOrExpression().accept(this);
    }

    // logicalAndExpression
    //    :   logicalOperand ( '&&' logicalOperand)*
    //    ;
    // logicalOrExpression
    //    :   logicalAndExpression ( '||' logicalAndExpression)*
    //    ;
    protected Node visitLogicalExpression(ParserRuleContext expressionContext, Function<Node[], Node> newLogicNode) {
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

    protected Node recurseOnSingleChild(ParserRuleContext ctx) {
        if (ctx.getChildCount() > 1) {
            // We have to abstract the expression if there is more than one operand
            return abstractToLiteral(ctx);
        } else {
            // There is exactly one child expression so we recurse
            return ctx.getChild(0).accept(this);
        }
    }

    protected Node abstractToLiteral(ParserRuleContext ctx) {
        return var(ctx.accept(abstractingVisitor).toString());
    }
}
