package com.jetbrains.python.inspection;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static com.jetbrains.python.PyTokenTypes.*;

public class PyConstantExpression extends PyInspection {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly,
                                          @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, session);
    }

    private static class Visitor extends PyInspectionVisitor {
        private static final Map<PyElementType, BiFunction<BigDecimal, BigDecimal, Boolean>> decimalComparisonOperatorsFunctions;

        static {
            Map<PyElementType, BiFunction<BigDecimal, BigDecimal, Boolean>> mapForInitialization = new HashMap<>();
            mapForInitialization.put(LT, (leftNumber, rightNumber) -> leftNumber.compareTo(rightNumber) < 0);
            mapForInitialization.put(GT, (leftNumber, rightNumber) -> leftNumber.compareTo(rightNumber) > 0);
            mapForInitialization.put(LE, (leftNumber, rightNumber) -> leftNumber.compareTo(rightNumber) <= 0);
            mapForInitialization.put(GE, (leftNumber, rightNumber) -> leftNumber.compareTo(rightNumber) >= 0);
            mapForInitialization.put(EQEQ, (leftNumber, rightNumber) -> leftNumber.compareTo(rightNumber) == 0);
            mapForInitialization.put(NE, (leftNumber, rightNumber) -> leftNumber.compareTo(rightNumber) != 0);
            mapForInitialization.put(NE_OLD, (leftNumber, rightNumber) -> leftNumber.compareTo(rightNumber) != 0);
            decimalComparisonOperatorsFunctions = Collections.unmodifiableMap(mapForInitialization);
        }

        private Visitor(@Nullable ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
            super(holder, session);
        }

        @Override
        public void visitPyIfStatement(PyIfStatement node) {
            super.visitPyIfStatement(node);
            processIfPart(node.getIfPart());
            for (PyIfPart part : node.getElifParts()) {
                processIfPart(part);
            }
        }

        private void processIfPart(@NotNull PyIfPart pyIfPart) {
            final PyExpression condition = pyIfPart.getCondition();
            if (condition instanceof PyBoolLiteralExpression) {
                registerProblem(condition, "The condition is always " + ((PyBoolLiteralExpression) condition).getValue());
            } else if (condition instanceof PyBinaryExpression) {
                PyBinaryExpression binaryExpression = (PyBinaryExpression) condition;
                BigDecimal leftNumber = getDecimalNumber(binaryExpression.getLeftExpression());
                BigDecimal rightNumber = getDecimalNumber(binaryExpression.getRightExpression());
                PyElementType operator = (binaryExpression).getOperator();

                if (leftNumber != null && rightNumber != null) {
                    if (decimalComparisonOperatorsFunctions.containsKey(operator)) {
                        Boolean valueOfConstantExpression = decimalComparisonOperatorsFunctions.get(operator).
                                apply(leftNumber, rightNumber);
                        registerProblem(condition, "The condition is always " + valueOfConstantExpression);
                    }
                }
            }
        }

        @Nullable
        private BigDecimal getDecimalNumber(PyExpression expression) {
            if (expression instanceof PyNumericLiteralExpression) {
                PyNumericLiteralExpression numberExpression = (PyNumericLiteralExpression) expression;
                return numberExpression.getBigDecimalValue();
            } else if (expression instanceof PyPrefixExpression) {
                PyPrefixExpression prefixExpression = (PyPrefixExpression) expression;
                if (prefixExpression.getOperator().equals(MINUS) &&
                        prefixExpression.getOperand() instanceof PyNumericLiteralExpression) {
                    PyNumericLiteralExpression numberExpression = (PyNumericLiteralExpression) prefixExpression.getOperand();
                    BigDecimal decimal = numberExpression.getBigDecimalValue();
                    if (decimal != null) {
                        return decimal.negate();
                    }
                }
            }
            return null;
        }
    }
}
