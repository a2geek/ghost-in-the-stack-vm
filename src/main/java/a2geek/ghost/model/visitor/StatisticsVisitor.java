package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.VisitorContext;
import a2geek.ghost.model.expression.AddressOfFunction;
import a2geek.ghost.model.statement.GotoGosubStatement;
import a2geek.ghost.model.statement.LabelStatement;
import a2geek.ghost.model.statement.OnErrorStatement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class StatisticsVisitor extends Visitor {
    private static final Pattern LINE_NUMBER = Pattern.compile("_L\\d+_\\d+", Pattern.CASE_INSENSITIVE);

    private final Set<Symbol> usedLabels = new HashSet<>();
    private final Map<Symbol, Symbol> replacementLabels = new HashMap<>();

    public Set<Symbol> getUsedLabels() {
        return usedLabels;
    }

    public Map<Symbol, Symbol> getReplacementLabels() {
        return replacementLabels;
    }

    @Override
    public void visit(LabelStatement statement, VisitorContext context) {
        boolean isLineNumber = LINE_NUMBER.matcher(statement.getLabel().name()).matches();
        if (isLineNumber) {
            // we need to assume all line number formatted labels are used due to the legacy basic processing
            usedLabels.add(statement.getLabel());
        }
        // If we have two consecutive labels, we can get rid of the first...
        if (context.nextStatement() instanceof LabelStatement statement2) {
            if (isLineNumber) {
                // we can't get rid of the line number labels, so in this case we get rid of the 2nd
                replacementLabels.put(statement2.getLabel(), statement.getLabel());
            } else {
                replacementLabels.put(statement.getLabel(), statement2.getLabel());
            }
        }
    }

    @Override
    public void visit(OnErrorStatement statement, VisitorContext context) {
        usedLabels.add(statement.getLabel());
    }

    @Override
    public void visit(GotoGosubStatement statement, VisitorContext context) {
        usedLabels.add(statement.getLabel());
    }

    @Override
    public Expression visit(AddressOfFunction expression) {
        usedLabels.add(expression.getSymbol());
        return null;
    }
}
