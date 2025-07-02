package org.variantsync.diffdetective.feature.cpp;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.variantsync.diffdetective.feature.antlr.CExpressionParser;

/**
 * Unparses the syntax tree into a string for use as a boolean abstraction.
 * This visitor produces almost the same string as
 * {@link org.antlr.v4.runtime.tree.ParseTree#getText()}
 * i.e., the result is the string passed to the parser where all characters ignored by the parser
 * are removed. Removed characters are mostly whitespace and comments, although the semantics of the
 * strings are equivalent (e.g., spaces in strings literals are kept).
 * The difference lies in some special cases:
 *
 * <ul>
 * <li>We handle the parse rule {@code Defined specialOperatorArgument?} specially and return
 * {@code "defined(argument)"} instead of {@code "definedargument"}. This prevents clashes with
 * macros called {@code "definedargument"} and makes the result consistent with the semantically
 * equivalent rule {@code Defined ('(' specialOperatorArgument ')')}.
 * </ul>
 *
 * @author Benjamin Moosherr
 */
public class AbstractingCExpressionVisitor extends AbstractParseTreeVisitor<StringBuilder> {
    @Override
    public StringBuilder visitTerminal(TerminalNode node) {
        return new StringBuilder(node.getSymbol().getText());
    }

    @Override
    protected StringBuilder defaultResult() {
        return new StringBuilder();
    }

    @Override
    protected StringBuilder aggregateResult(StringBuilder aggregate, StringBuilder nextResult) {
        aggregate.append(nextResult);
        return aggregate;
    }

    @Override
    public StringBuilder visitChildren(RuleNode node) {
        // Some rules need special cases
        if (node instanceof CExpressionParser.SpecialOperatorContext ctx) {
            return visitSpecialOperator(ctx);
        } else {
            return super.visitChildren(node);
        }
    }

    // specialOperator
    //    :   HasAttribute ('(' specialOperatorArgument ')')?
    //    |   HasCPPAttribute ('(' specialOperatorArgument ')')?
    //    |   HasCAttribute ('(' specialOperatorArgument ')')?
    //    |   HasBuiltin ('(' specialOperatorArgument ')')?
    //    |   HasInclude ('(' specialOperatorArgument ')')?
    //    |   Defined ('(' specialOperatorArgument ')')
    //    |   Defined specialOperatorArgument?
    //    ;
    public StringBuilder visitSpecialOperator(CExpressionParser.SpecialOperatorContext ctx) {
        if (ctx.getChildCount() == 2) {
            StringBuilder sb = ctx.specialOperatorArgument().accept(this);
            sb.insert(0, "defined(");
            sb.append(")");
            return sb;
        } else {
            return super.visitChildren(ctx);
        }
    }
}
