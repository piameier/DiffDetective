package org.variantsync.diffdetective.datasets.predefined;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.prop4j.Node;
import org.variantsync.diffdetective.feature.antlr.CExpressionParser;
import org.variantsync.diffdetective.feature.cpp.ControllingCExpressionVisitor;

import java.util.List;

import static org.variantsync.diffdetective.util.fide.FormulaUtils.negate;

/**
 * Parses C preprocessor annotations in the marlin firmware.
 * <p>
 * In contrast to {@link ControllingCExpressionVisitor},
 * this class resolves the {@code ENABLED} and {@code DISABLED} macros
 * that are used in Marlin to check for features being (de-)selected.
 *
 * @author Paul Bittner, Benjamin Moosherr
 */
public class MarlinControllingCExpressionVisitor extends ControllingCExpressionVisitor {
    @Override
    public Node visitPrimaryExpression(CExpressionParser.PrimaryExpressionContext ctx) {
        if (ctx.macroExpression() != null) {
            TerminalNode name = ctx.macroExpression().Identifier();
            List<CExpressionParser.AssignmentExpressionContext> arguments = ctx.macroExpression().argumentExpressionList().assignmentExpression();

            if (arguments.size() == 1) {
                if (name.getText().equals("ENABLED")) {
                    return abstractToLiteral(arguments.get(0));
                }
                if (name.getText().equals("DISABLED")) {
                    return negate(abstractToLiteral(arguments.get(0)));
                }
            }
        }

        return super.visitPrimaryExpression(ctx);
    }
}
