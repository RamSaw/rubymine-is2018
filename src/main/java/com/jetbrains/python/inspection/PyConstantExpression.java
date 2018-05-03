package com.jetbrains.python.inspection;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public class PyConstantExpression extends PyInspection {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly,
                                          @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, session);
    }

    private static class Visitor extends PyInspectionVisitor {

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

                if (binaryExpression.getLeftExpression() instanceof PyNumericLiteralExpression &&
                        binaryExpression.getRightExpression() instanceof PyNumericLiteralExpression) {
                    PyNumericLiteralExpression leftNumberExpression = (PyNumericLiteralExpression) binaryExpression.getLeftExpression();
                    PyNumericLiteralExpression rightNumberExpression = (PyNumericLiteralExpression) binaryExpression.getRightExpression();
                    PyElementType operator = (binaryExpression).getOperator();

                    if (operator != null &&
                            leftNumberExpression.isIntegerLiteral() && rightNumberExpression.isIntegerLiteral()) {
                        String comparisonOperatorName = operator.getSpecialMethodName();
                        BigInteger leftNumber = leftNumberExpression.getBigIntegerValue();
                        BigInteger rightNumber = rightNumberExpression.getBigIntegerValue();

                        if (comparisonOperatorName != null && leftNumber != null && rightNumber != null) {
                            Boolean valueOfConstantExpression = null;
                            switch (comparisonOperatorName) {
                                case "__lt__":
                                    valueOfConstantExpression = leftNumber.compareTo(rightNumber) < 0;
                                    break;
                                case "__gt__":
                                    valueOfConstantExpression = leftNumber.compareTo(rightNumber) > 0;
                                    break;
                                case "__le__":
                                    valueOfConstantExpression = leftNumber.compareTo(rightNumber) <= 0;
                                    break;
                                case "__ge__":
                                    valueOfConstantExpression = leftNumber.compareTo(rightNumber) >= 0;
                                    break;
                                case "__eq__":
                                    valueOfConstantExpression = leftNumber.compareTo(rightNumber) == 0;
                                    break;
                                case "__ne__":
                                    valueOfConstantExpression = leftNumber.compareTo(rightNumber) != 0;
                                    break;
                            }
                            if (valueOfConstantExpression != null) {
                                registerProblem(condition, "The condition is always " + valueOfConstantExpression);
                            }
                        }
                    }
                }
            }
        }
    }
}
